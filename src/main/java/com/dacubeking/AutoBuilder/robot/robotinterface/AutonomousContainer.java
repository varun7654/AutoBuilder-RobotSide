package com.dacubeking.AutoBuilder.robot.robotinterface;

import com.dacubeking.AutoBuilder.robot.GuiAuto;
import com.dacubeking.AutoBuilder.robot.NetworkAuto;
import com.dacubeking.AutoBuilder.robot.annotations.AutoBuilderAccessible;
import com.google.common.base.Preconditions;
import edu.wpi.first.networktables.EntryListenerFlags;
import edu.wpi.first.networktables.NetworkTable;
import edu.wpi.first.networktables.NetworkTableEntry;
import edu.wpi.first.networktables.NetworkTableInstance;
import edu.wpi.first.wpilibj.DriverStation;
import edu.wpi.first.wpilibj.Filesystem;
import edu.wpi.first.wpilibj.TimedRobot;
import edu.wpi.first.wpilibj.Timer;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public final class AutonomousContainer {

    private final @NotNull NetworkTableInstance instance = NetworkTableInstance.getDefault();
    private final @NotNull NetworkTable autoDataTable = instance.getTable("autodata");
    private final @NotNull NetworkTableEntry autoPath = autoDataTable.getEntry("autoPath");
    private final @NotNull NetworkTableEntry pathProcessingStatusEntry = autoDataTable.getEntry("processing");

    private final @NotNull Lock networkAutoLock = new ReentrantLock();
    private @Nullable NetworkAuto networkAuto = null;
    private final @NotNull ExecutorService deserializerExecutor = Executors.newSingleThreadExecutor();

    private @Nullable Thread autoThread = null;
    private final @NotNull Object autoThreadLock = new Object();

    private static final AutonomousContainer autonomousContainer = new AutonomousContainer();

    public static AutonomousContainer getInstance() {
        return autonomousContainer;
    }

    private CommandTranslator commandTranslator;
    private boolean isHolonomic;
    private boolean debugPrints = false;

    private final HashMap<String, GuiAuto> autonomousList = new HashMap<>();
    private final @NotNull HashMap<String, Object> parentObjects = new HashMap<>();

    private static final String AUTO_DIRECTORY = Filesystem.getDeployDirectory().getAbsoluteFile() + "/autos/";

    /**
     * @param isHolonomic       Is the robot using a holonomic drivetrain? (ex: swerve or mecanum)
     * @param commandTranslator The command translator to use
     * @param crashOnError      Should the robot crash on error? If this is enabled, and an auto fails to load, the robot will
     *                          crash. If this is disabled, the robot will skip the invalid auto and continue to the next one.
     * @param parentObjects     Objects that can be used to access other objects annotated with {@link AutoBuilderAccessible}.
     * @param timedRobot        The timed robot to use to create the period function for the autos. This can be null if you're
     *                          running autos completely asynchronously.
     */
    public synchronized void initialize(
            boolean isHolonomic,
            @NotNull CommandTranslator commandTranslator,
            boolean crashOnError,
            @Nullable TimedRobot timedRobot,
            Object... parentObjects


    ) {
        Preconditions.checkArgument(this.commandTranslator == null, "The Autonomous Container has already been initialized");
        Preconditions.checkArgument(commandTranslator != null, "Command translator cannot be null");
        Preconditions.checkArgument(!isHolonomic || commandTranslator.setAutonomousRotation != null,
                "The command translator must have a setAutonomousRotation method if the robot is holonomic");

        if (commandTranslator.runOnMainThread) {
            Preconditions.checkArgument(timedRobot != null,
                    "TimedRobot cannot be null if autonomous commands are being issues on the main thread");
            timedRobot.addPeriodic(this::onAutoPeriodic, 0.001);
        }

        initializeAccessibleInstances(parentObjects, crashOnError);

        //Create the listener for network autos
        autoPath.addListener(event ->
                deserializerExecutor.execute(() -> { //Start deserializing on another thread
                            System.out.println("Starting to Parse Network Autonomous");
                            //Set networktable entries for the gui notifications
                            pathProcessingStatusEntry.setDouble(1);
                            networkAutoLock.lock();
                            try {
                                networkAuto = new NetworkAuto(); //Create the auto object which will start deserializing the json and the auto
                            } finally {
                                networkAutoLock.unlock();
                            }

                            // ready to be run
                            System.out.println("Done Parsing Network Autonomous");
                            //Set networktable entries for the gui notifications
                            pathProcessingStatusEntry.setDouble(2);
                        }
                ), EntryListenerFlags.kNew | EntryListenerFlags.kUpdate | EntryListenerFlags.kImmediate);

        this.isHolonomic = isHolonomic;
        this.commandTranslator = commandTranslator;

        long startLoadingTime = System.currentTimeMillis();

        findAutosAndLoadAutos(new File(AUTO_DIRECTORY), crashOnError);

        blockedThread = Thread.currentThread();
        // Wait for all autos to be loaded
        while (loadedAutosCount.get() < numAutonomousFiles) {
            try {
                //noinspection BusyWait
                Thread.sleep(1000);
            } catch (InterruptedException ignored) {
            }
        }

        if (crashOnError) {
            Preconditions.checkState(loadedAutosCount.get() == numAutonomousFiles,
                    "Not all autonomous files were successfully loaded");
        }

        System.out.println("Successfully loaded " + autonomousList.size() + " auto"
                + (autonomousList.size() == 1 ? "" : "s") + " with "
                + (loadedAutosCount.get() - successfullyLoadedAutosCount.get()) +
                " failure" + (loadedAutosCount.get() - successfullyLoadedAutosCount.get() == 1 ? "" : "s") + " in "
                + ((double) (System.currentTimeMillis() - startLoadingTime)) / 1000 + "s");
    }

    /**
     * Uses the given parent objects to find all fields/methods/constructors annotated with {@link AutoBuilderAccessible} to store
     * them for use in autos.
     *
     * @param parentObjects Objects that can be used to access other objects annotated with {@link AutoBuilderAccessible}.
     */
    private synchronized void initializeAccessibleInstances(Object[] parentObjects, boolean crashOnError) {
        for (Object parentObject : parentObjects) {
            Arrays.stream(parentObject.getClass().getDeclaredMethods())
                    .filter(method -> Arrays.stream(method.getDeclaredAnnotations())
                            .anyMatch(annotation -> annotation.annotationType() == AutoBuilderAccessible.class))
                    .forEach(method -> {
                        try {
                            method.setAccessible(true);
                            Object instance = method.invoke(parentObject);
                            this.parentObjects.put(instance.getClass().getName(), instance);
                        } catch (IllegalAccessException | InvocationTargetException e) {
                            DriverStation.reportError("Failed to get accessible instance: " + e.getMessage(), e.getStackTrace());
                            if (crashOnError) {
                                throw new RuntimeException(e);
                            }
                        }
                    });

            Arrays.stream(parentObject.getClass().getDeclaredFields())
                    .filter(field -> Arrays.stream(field.getDeclaredAnnotations())
                            .anyMatch(annotation -> annotation.annotationType().isAssignableFrom(AutoBuilderAccessible.class)))
                    .forEach(field -> {
                        try {
                            field.setAccessible(true);
                            Object instance = field.get(parentObject);
                            this.parentObjects.put(instance.getClass().getName(), instance);
                        } catch (IllegalAccessException e) {
                            DriverStation.reportError("Failed to get accessible instance: " + e.getMessage(), e.getStackTrace());
                            if (crashOnError) {
                                throw new RuntimeException(e);
                            }
                        }
                    });
        }
    }


    /**
     * Recursively finds all autonomous files in the given directory and loads them.
     *
     * @param directory    The directory to search in.
     * @param crashOnError Should the robot crash on error? If this is enabled, and an auto fails to load, the robot will crash.
     *                     If this is disabled, the robot will skip the invalid auto and continue to the next one.
     */
    private synchronized void findAutosAndLoadAutos(File directory, boolean crashOnError) {
        File[] autos = directory.listFiles();
        if (autos == null) {
            System.out.println("No autos files found");
        } else {
            for (File file : autos) {
                if (file.isDirectory()) {
                    findAutosAndLoadAutos(file, crashOnError);
                    continue;
                }

                if (file.getName().endsWith(".json")) {
                    if (file.getName().contains("NOTDEPLOYABLE")) {
                        System.out.println("Skipping " + file.getAbsolutePath() + " because it is marked as NOTDEPLOYABLE");
                        if (crashOnError) throw new RuntimeException("An un-deployable file was found");
                        continue;
                    }
                    printDebug("Found auto file: " + file.getAbsolutePath());

                    CompletableFuture.runAsync(() -> {
                        try {
                            autonomousList.put(file.getAbsolutePath(), new GuiAuto(file));
                            successfullyLoadedAutosCount.incrementAndGet();
                        } catch (IOException e) {
                            if (debugPrints) {
                                DriverStation.reportError("Failed to deserialize auto: " + e.getLocalizedMessage(),
                                        e.getStackTrace());
                            }
                        }
                    }).thenRun(this::incrementLoadedAutos);
                    numAutonomousFiles++;
                }
            }
        }
    }

    private int numAutonomousFiles = 0;
    private final AtomicInteger successfullyLoadedAutosCount = new AtomicInteger(0);
    private final AtomicInteger loadedAutosCount = new AtomicInteger(0);
    @Nullable private Thread blockedThread = null;

    private void incrementLoadedAutos() {
        loadedAutosCount.incrementAndGet();
        if (blockedThread != null) {
            blockedThread.interrupt();
        }
    }

    @Internal
    public synchronized static CommandTranslator getCommandTranslator() {
        return getInstance().commandTranslator;
    }

    @Internal
    public synchronized void isInitialized() {
        Preconditions.checkArgument(commandTranslator != null, "The Autonomous Container must be initialized before any " +
                "autonomous can be run. (Call initialize(...) first)");
    }

    @Internal
    public synchronized boolean isHolonomic() {
        return isHolonomic;
    }

    @Internal
    public synchronized boolean areDebugPrintsEnabled() {
        return debugPrints;
    }

    @Internal
    public synchronized void printDebug(String message) {
        if (debugPrints) {
            System.out.println(message);
        }
    }

    @Internal
    public synchronized @NotNull HashMap<String, Object> getAccessibleInstances() {
        return parentObjects;
    }

    public synchronized void setDebugPrints(boolean debugPrints) {
        this.debugPrints = debugPrints;
    }


    /**
     * @return A list of the names of all the autos that have been loaded. The name is the name of the file, without the .json
     */
    public synchronized ArrayList<String> getAutonomousNames() {
        ArrayList<String> names = new ArrayList<>(autonomousList.size());
        for (String absoluteFilePath : autonomousList.keySet()) {
            String[] splitFilePath = absoluteFilePath.split("/");
            names.add(splitFilePath[splitFilePath.length - 1].replace(".json", ""));
        }
        return names;
    }

    /**
     * @return A list of the absolute paths of all the autos that have been loaded.
     */
    public synchronized Set<String> getAbsoluteAutonomousPaths() {
        return autonomousList.keySet();
    }

    /**
     * Run an auto with the given name and side. These names should match the names given by {@link #getAutonomousNames()}.
     * <p>
     * If the auto is not found with the given side, the code will look for an auto with no side. This means you don't have to
     * perform checks to determine if the auto is sided or not.
     *
     * @param name                    The name of the auto to run.
     * @param side                    The side of the robot that the auto will be run on.
     * @param allowRunningNetworkAuto Weather to allow network autos to be run instead of the selected auto. If a network auto is
     *                                loaded and this is true, it will be run instead of the selected auto.
     */
    public synchronized void runAutonomous(String name, String side, boolean allowRunningNetworkAuto) {
        networkAutoLock.lock();
        @Nullable GuiAuto selectedAuto;
        try {
            if (allowRunningNetworkAuto && networkAuto != null) {
                selectedAuto = networkAuto;
            } else {
                String autoPath = AUTO_DIRECTORY + side + (side.endsWith("/") ? "" : "/") + name + ".json";
                if (!autonomousList.containsKey(autoPath)) {
                    autoPath = AUTO_DIRECTORY + name + ".json"; // Try the name without the side
                }

                selectedAuto = autonomousList.get(autoPath);
            }
        } finally {
            networkAutoLock.unlock();
        }

        // If the auto is null, it means that the auto was not found.
        if (selectedAuto == null) {
            DriverStation.reportError("Could not find auto: " + name +
                            "\nExpected to find the auto in:\n"
                            + AUTO_DIRECTORY + side + (side.endsWith("/") ? "" : "/") + name + ".json" +
                            "\nOr:\n" + AUTO_DIRECTORY + name + ".json",
                    false);
            return;
        }

        // Ensure that no other autos are currently running
        killAuto();
        commandTranslator.onPeriodic(); // Ensure that data is not stale

        // We then create a new thread to run the auto and run it
        synchronized (autoThreadLock) {
            autoThread = new Thread(selectedAuto);
            autoThread.start();
        }
    }

    /**
     * Kills the currently running autonomous.
     */
    public void killAuto() {
        synchronized (autoThreadLock) {
            if (autoThread != null && autoThread.isAlive()) {
                autoThread.interrupt();

                double nextStackTracePrint = Timer.getFPGATimestamp() + 1;
                while (!autoThread.isAlive()) {
                    if (Timer.getFPGATimestamp() > nextStackTracePrint) {
                        Exception throwable = new Exception(
                                "Waiting for auto to die. autoThread.getState() = " + autoThread.getState() +
                                        "\n Take a look at the stack trace for the auto thread bellow. Ensure that your auto will " +
                                        "exit when it is interrupted.");
                        throwable.setStackTrace(autoThread.getStackTrace());
                        throwable.printStackTrace();
                        nextStackTracePrint = Timer.getFPGATimestamp() + 5;
                    }

                    try {
                        //noinspection BusyWait
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                getCommandTranslator().stopRobot();
            }
        }
    }

    private void onAutoPeriodic() {
        commandTranslator.onPeriodic();
    }
}
