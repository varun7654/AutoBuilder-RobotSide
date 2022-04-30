package com.dacubeking.AutoBuilder.robot.robotinterface;

import com.dacubeking.AutoBuilder.robot.GuiAuto;
import com.dacubeking.AutoBuilder.robot.annotations.AutoBuilderAccessible;
import com.google.common.base.Preconditions;
import edu.wpi.first.wpilibj.DriverStation;
import edu.wpi.first.wpilibj.Filesystem;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

public final class AutonomousContainer {

    private static final AutonomousContainer autonomousContainer = new AutonomousContainer();

    public static AutonomousContainer getInstance() {
        return autonomousContainer;
    }

    private CommandTranslator commandTranslator;
    private boolean isHolonomic;
    private boolean debugPrints = false;

    private final HashMap<String, GuiAuto> autonomousList = new HashMap<>();

    private final @NotNull HashMap<String, Object> parentObjects = new HashMap<>();

    /**
     * @param isHolonomic       Is the robot using a holonomic drivetrain? (ex: swerve or mecanum)
     * @param commandTranslator The command translator to use
     * @param crashOnError      Should the robot crash on error? If this is enabled, and an auto fails to load, the robot will
     *                          crash. If this is disabled, the robot will skip the invalid auto and continue to the next one.
     * @param parentObjects     Objects that can be used to access other objects annotated with {@link AutoBuilderAccessible}.
     */
    public void initialize(
            boolean isHolonomic,
            @NotNull CommandTranslator commandTranslator,
            boolean crashOnError,
            Object... parentObjects
    ) {
        Preconditions.checkArgument(this.commandTranslator == null, "The Autonomous Container has already been initialized");
        Preconditions.checkArgument(commandTranslator != null, "Command translator cannot be null");
        Preconditions.checkArgument(!isHolonomic || commandTranslator.setAutonomousRotation != null,
                "The command translator must have a setAutonomousRotation method if the robot is holonomic");

        initializeAccessibleInstances(parentObjects, crashOnError);

        this.isHolonomic = isHolonomic;
        this.commandTranslator = commandTranslator;

        long startLoadingTime = System.currentTimeMillis();

        findAutosAndLoadAutos(new File(Filesystem.getDeployDirectory().getAbsoluteFile() + "/autos"), crashOnError);

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
    private void initializeAccessibleInstances(Object[] parentObjects, boolean crashOnError) {
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


    private void findAutosAndLoadAutos(File directory, boolean crashOnError) {
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
    @Nullable Thread blockedThread = null;

    private void incrementLoadedAutos() {
        loadedAutosCount.incrementAndGet();
        if (blockedThread != null) {
            blockedThread.interrupt();
        }
    }

    @Internal
    public static CommandTranslator getCommandTranslator() {
        return getInstance().commandTranslator;
    }

    @Internal
    public void isInitialized() {
        Preconditions.checkArgument(commandTranslator != null, "The Autonomous Container must be initialized before any " +
                "autonomous can be run. (Call initialize(...) first)");
    }

    @Internal
    public boolean isHolonomic() {
        return isHolonomic;
    }

    @Internal
    public boolean areDebugPrintsEnabled() {
        return debugPrints;
    }

    @Internal
    public void printDebug(String message) {
        if (debugPrints) {
            System.out.println(message);
        }
    }

    @Internal
    public @NotNull HashMap<String, Object> getAccessibleInstances() {
        return parentObjects;
    }

    public void setDebugPrints(boolean debugPrints) {
        this.debugPrints = debugPrints;
    }
}
