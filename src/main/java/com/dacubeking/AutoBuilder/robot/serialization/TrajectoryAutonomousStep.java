package com.dacubeking.AutoBuilder.robot.serialization;

import com.dacubeking.AutoBuilder.robot.serialization.command.CommandExecutionFailedException;
import com.dacubeking.AutoBuilder.robot.serialization.command.SendableScript;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import edu.wpi.first.math.trajectory.Trajectory;
import edu.wpi.first.math.trajectory.Trajectory.State;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;

import static com.dacubeking.AutoBuilder.robot.robotinterface.AutonomousContainer.getCommandTranslator;

@JsonIgnoreProperties(ignoreUnknown = true)
public class TrajectoryAutonomousStep extends AbstractAutonomousStep {
    private final @NotNull Trajectory trajectory;
    private final List<TimedRotation> rotations;

    @JsonCreator
    public TrajectoryAutonomousStep(@JsonProperty(required = true, value = "states") List<State> states,
                                    @JsonProperty(required = true, value = "rotations") List<TimedRotation> rotations) {
        this.trajectory = new Trajectory(states);
        this.rotations = rotations;
    }

    public @NotNull
    Trajectory getTrajectory() {
        return trajectory;
    }

    public List<TimedRotation> getRotations() {
        return rotations;
    }

    @Override
    public void execute(@NotNull List<SendableScript> scriptsToExecuteByTime,
                        @NotNull List<SendableScript> scriptsToExecuteByPercent)
            throws InterruptedException, CommandExecutionFailedException, ExecutionException {
        //Sort the lists to make sure they are sorted by time
        Collections.sort(scriptsToExecuteByTime);
        Collections.sort(scriptsToExecuteByPercent);

        //
        getCommandTranslator().setAutonomousRotation(rotations.get(0).rotation);
        getCommandTranslator().setNewTrajectory(trajectory); //Send the auto to our drive class to be executed
        getCommandTranslator().setAutonomousRotation(rotations.get(0).rotation);

        int rotationIndex = 1; // Start at the second rotation (the first is the starting rotation)
        while (!getCommandTranslator().isTrajectoryDone()) { // Wait till the auto is done
            if (rotationIndex < rotations.size() &&
                    getCommandTranslator().getTrajectoryElapsedTime() > rotations.get(rotationIndex).time) {
                // We've passed the time for the next rotation
                getCommandTranslator().setAutonomousRotation(rotations.get(rotationIndex).rotation); //Set the rotation
                rotationIndex++; // Increment the rotation index
            }

            if (!scriptsToExecuteByTime.isEmpty() &&
                    scriptsToExecuteByTime.get(0).getDelay() <= getCommandTranslator().getTrajectoryElapsedTime()) {
                // We have a script to execute, and it is time to execute it
                scriptsToExecuteByTime.get(0).execute();
                scriptsToExecuteByTime.remove(0);
            }

            if (!scriptsToExecuteByPercent.isEmpty() && scriptsToExecuteByPercent.get(0).getDelay() <=
                    (getCommandTranslator().getTrajectoryElapsedTime() / trajectory.getTotalTimeSeconds())) {
                // We have a script to execute, and it is time to execute it
                scriptsToExecuteByPercent.get(0).execute();
                scriptsToExecuteByPercent.remove(0);
            }
            //noinspection BusyWait
            Thread.sleep(10); // Throws an exception to exit if Interrupted
        }
        getCommandTranslator().stopRobot();

        //Execute any remaining scripts
        for (SendableScript sendableScript : scriptsToExecuteByTime) {
            sendableScript.execute();
        }
        for (SendableScript sendableScript : scriptsToExecuteByPercent) {
            sendableScript.execute();
        }

        scriptsToExecuteByTime.clear();
        scriptsToExecuteByPercent.clear();
    }
}