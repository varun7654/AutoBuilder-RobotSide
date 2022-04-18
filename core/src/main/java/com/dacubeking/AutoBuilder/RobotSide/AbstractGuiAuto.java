package com.dacubeking.AutoBuilder.RobotSide;

import com.dacubeking.AutoBuilder.RobotSide.RobotInterface.AutonomousContainer;
import com.dacubeking.AutoBuilder.RobotSide.serialization.AbstractAutonomousStep;
import com.dacubeking.AutoBuilder.RobotSide.serialization.Autonomous;
import com.dacubeking.AutoBuilder.RobotSide.serialization.Serializer;
import com.dacubeking.AutoBuilder.RobotSide.serialization.TrajectoryAutonomousStep;
import com.dacubeking.AutoBuilder.RobotSide.serialization.command.CommandExecutionFailedException;
import com.dacubeking.AutoBuilder.RobotSide.serialization.command.SendableScript;
import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.trajectory.Trajectory;
import edu.wpi.first.wpilibj.DriverStation;
import edu.wpi.first.wpilibj.Timer;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.dacubeking.AutoBuilder.RobotSide.RobotInterface.AutonomousContainer.getCommandTranslator;

public abstract class AbstractGuiAuto implements Runnable {

    private Autonomous autonomous;
    Pose2d initialPose;

    /**
     * Ensure you are creating the objects for your auto on robot init. The roborio will take multiple seconds to initialize the
     * auto.
     *
     * @param autonomousFile File location of the auto
     */
    public AbstractGuiAuto(File autonomousFile) {
        try {
            autonomous = (Autonomous) Serializer.deserializeFromFile(autonomousFile, Autonomous.class);
        } catch (IOException e) {
            //e.printStackTrace();
            DriverStation.reportError("Failed to deserialize auto. " + e.getLocalizedMessage(), e.getStackTrace());
        }
        init();
    }

    /**
     * Ensure you are creating the objects for your auto before you run them. The roborio will take multiple seconds to initialize
     * the auto.
     *
     * @param autonomousJson String of the autonomous
     */
    public AbstractGuiAuto(String autonomousJson) {
        try {
            autonomous = (Autonomous) Serializer.deserialize(autonomousJson, Autonomous.class);
        } catch (IOException e) {
            DriverStation.reportError("Failed to deserialize auto. " + e.getMessage(), e.getStackTrace());
        }
        init();
    }

    private void init() {
        //Find and save the initial pose
        for (AbstractAutonomousStep autonomousStep : autonomous.getAutonomousSteps()) {
            if (autonomousStep instanceof TrajectoryAutonomousStep) {
                TrajectoryAutonomousStep trajectoryAutonomousStep = (TrajectoryAutonomousStep) autonomousStep;
                Trajectory.State initialState = trajectoryAutonomousStep.getTrajectory().getStates().get(0);
                initialPose = new Pose2d(initialState.poseMeters.getTranslation(),
                        trajectoryAutonomousStep.getRotations().get(0).rotation);
                break;
            }
        }
    }

    @Override
    public void run() {
        AutonomousContainer.getInstance().isInitialized();

        Thread.currentThread().setUncaughtExceptionHandler((t, e) -> {
            DriverStation.reportError("Uncaught exception in auto thread: " + e.getMessage(), e.getStackTrace());
            getCommandTranslator().stopRobot();
        });

        AutonomousContainer.getInstance().printDebug("Started Running: " + Timer.getFPGATimestamp());


        //Set our initial pose in our robot tracker
        if (initialPose != null) {
            getCommandTranslator().setRobotPose(initialPose);
        }

        //Loop though all the steps and execute them
        List<SendableScript> scriptsToExecuteByTime = new ArrayList<>();
        List<SendableScript> scriptsToExecuteByPercent = new ArrayList<>();

        for (AbstractAutonomousStep autonomousStep : autonomous.getAutonomousSteps()) {
            AutonomousContainer.getInstance().printDebug("Doing a step: " + Timer.getFPGATimestamp());

            if (Thread.interrupted()) {
                getCommandTranslator().stopRobot();
                AutonomousContainer.getInstance().printDebug("Auto was interrupted " + Timer.getFPGATimestamp());
                return;
            }

            try {
                autonomousStep.execute(scriptsToExecuteByTime, scriptsToExecuteByPercent);
            } catch (InterruptedException e) {
                getCommandTranslator().stopRobot();
                AutonomousContainer.getInstance().printDebug("Auto prematurely stopped at " + Timer.getFPGATimestamp() +
                        ". This is not an error if you disabled your robot.");
                if (AutonomousContainer.getInstance().areDebugPrintsEnabled()) {
                    e.printStackTrace();
                }
                return;
            } catch (CommandExecutionFailedException e) {
                getCommandTranslator().stopRobot();
                e.printStackTrace(); // We should always print this out since it is a fatal error
                return;
            }
        }

        System.out.println("Finished Autonomous at " + Timer.getFPGATimestamp());
        getCommandTranslator().stopRobot();
    }
}