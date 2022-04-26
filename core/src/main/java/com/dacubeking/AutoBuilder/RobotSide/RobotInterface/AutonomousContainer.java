package com.dacubeking.AutoBuilder.RobotSide.RobotInterface;

import com.dacubeking.AutoBuilder.RobotSide.GuiAuto;
import com.google.common.base.Preconditions;
import edu.wpi.first.wpilibj.DriverStation;
import edu.wpi.first.wpilibj.Filesystem;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
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

    /**
     * @param isHolonomic       Is the robot using a holonomic drivetrain? (ex: swerve or mecanum)
     * @param commandTranslator The command translator to use
     */
    public void initialize(boolean isHolonomic, @NotNull CommandTranslator commandTranslator, boolean crashOnError) {
        Preconditions.checkArgument(this.commandTranslator == null, "The Autonomous Container has already been initialized");
        Preconditions.checkArgument(commandTranslator != null, "Command translator cannot be null");
        Preconditions.checkArgument(!isHolonomic || commandTranslator.setAutonomousRotation != null,
                "The command translator must have a setAutonomousRotation method if the robot is holonomic");

        this.isHolonomic = isHolonomic;
        this.commandTranslator = commandTranslator;

        long startLoadingTime = System.currentTimeMillis();

        findAutosAndLoadAutos(Filesystem.getDeployDirectory(), crashOnError);

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

        System.out.println("Successfully loaded " + autonomousList.size() + " autos with "
                + (loadedAutosCount.get() - successfullyLoadedAutosCount.get()) + " failures in "
                + ((double) (System.currentTimeMillis() - startLoadingTime)) / 1000 + "s");
    }


    private void findAutosAndLoadAutos(File directory, boolean crashOnError) {
        String[] autos = directory.list();
        if (autos == null) {
            System.out.println("No autos files found");
        } else {
            for (String stringFile : autos) {
                File file = new File(Filesystem.getDeployDirectory(), stringFile);
                if (file.isDirectory()) {
                    findAutosAndLoadAutos(file, crashOnError);
                    continue;
                }

                if (stringFile.endsWith(".json")) {
                    if (stringFile.contains("NOTDEPLOYABLE")) {
                        System.out.println("Skipping " + stringFile + " because it is marked as NOTDEPLOYABLE");
                        if (crashOnError) throw new RuntimeException("An un-deployable file was found");
                        continue;
                    }
                    printDebug("Found auto file: " + stringFile);

                    CompletableFuture.runAsync(() -> {
                        try {
                            autonomousList.put(file.getAbsolutePath(), new GuiAuto(new File(stringFile)));
                            successfullyLoadedAutosCount.incrementAndGet();
                        } catch (IOException e) {
                            if (debugPrints) {
                                DriverStation.reportError("Failed to deserialize auto. " + e.getLocalizedMessage(),
                                        e.getStackTrace());
                            }
                        }
                    }).thenRun(this::incrementLoadedAutos);
                    numAutonomousFiles++;
                }
            }
        }
    }

    int numAutonomousFiles = 0;
    private final AtomicInteger successfullyLoadedAutosCount = new AtomicInteger(0);
    private final AtomicInteger loadedAutosCount = new AtomicInteger(0);
    @Nullable Thread blockedThread = null;

    public void incrementLoadedAutos() {
        loadedAutosCount.incrementAndGet();
        if (blockedThread != null) {
            blockedThread.interrupt();
        }
    }

    public static CommandTranslator getCommandTranslator() {
        return getInstance().commandTranslator;
    }

    public void isInitialized() {
        Preconditions.checkArgument(commandTranslator != null, "The Autonomous Container must be initialized before any " +
                "autonomous can be run. (Call initialize(...) first)");
    }

    public boolean isHolonomic() {
        return isHolonomic;
    }

    public boolean areDebugPrintsEnabled() {
        return debugPrints;
    }

    public void printDebug(String message) {
        if (debugPrints) {
            System.out.println(message);
        }
    }

    public void setDebugPrints(boolean debugPrints) {
        this.debugPrints = debugPrints;
    }
}
