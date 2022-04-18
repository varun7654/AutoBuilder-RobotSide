package com.dacubeking.AutoBuilder.RobotSide.RobotInterface;

import com.google.common.base.Preconditions;
import org.jetbrains.annotations.NotNull;

public final class AutonomousContainer {

    private static AutonomousContainer autonomousContainer = new AutonomousContainer();

    public static AutonomousContainer getInstance() {
        return autonomousContainer;
    }

    private CommandTranslator commandTranslator;
    private boolean isHolonomic;
    private boolean debugPrints = false;

    /**
     * @param isHolonomic       Is the robot using a holonomic drivetrain? (ex: swerve or mecanum)
     * @param commandTranslator The command translator to use
     */
    public void initialize(boolean isHolonomic, @NotNull CommandTranslator commandTranslator) {
        Preconditions.checkArgument(commandTranslator != null, "Command translator cannot be null");
        Preconditions.checkArgument(!isHolonomic || commandTranslator.setAutonomousRotation != null,
                "The command translator must have a setAutonomousRotation method if the robot is holonomic");

        this.isHolonomic = isHolonomic;
        this.commandTranslator = commandTranslator;
    }

    public static CommandTranslator getCommandTranslator() {
        return getInstance().commandTranslator;
    }

    public void isInitialized() {
        Preconditions.checkArgument(commandTranslator != null, "The Autonomous Container must be initialized before any " +
                "autonomous can be run");
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
