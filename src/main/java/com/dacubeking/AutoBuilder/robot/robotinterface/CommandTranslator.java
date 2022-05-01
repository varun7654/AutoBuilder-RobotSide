package com.dacubeking.AutoBuilder.robot.robotinterface;

import com.google.common.base.Preconditions;
import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.trajectory.Trajectory;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.DoubleSupplier;

public class CommandTranslator {
    protected final @NotNull Consumer<Trajectory> setNewTrajectory;
    protected final @NotNull Runnable stopRobot;
    protected final @Nullable Consumer<Rotation2d> setAutonomousRotation;
    private final @NotNull BooleanSupplier isTrajectoryDone;
    private final @NotNull DoubleSupplier getTrajectoryElapsedTime;
    private final Consumer<Pose2d> setRobotPose;
    @Internal public final boolean runOnMainThread;

    /**
     * @param setNewTrajectory         The consumer to call to set the new trajectory
     * @param stopRobot                The runnable to call to stop the robot
     * @param setAutonomousRotation    The consumer to call to set the autonomous rotation (can be null is the robot is not
     *                                 holonomic)
     * @param isTrajectoryDone         The boolean supplier to call to check if the trajectory is done. This lambada should return
     *                                 false
     * @param getTrajectoryElapsedTime The double supplier to call to get the elapsed time of the trajectory. This lambada must
     *                                 return 0.0 immediately after a new trajectory is set and should return the elapsed time of
     *                                 the current trajectory that is being driven.
     * @param setRobotPose             The consumer to call to set the initial pose of the robot at the start of autonomous
     */
    public CommandTranslator(
            @NotNull Consumer<Trajectory> setNewTrajectory,
            @NotNull Runnable stopRobot,
            @Nullable Consumer<Rotation2d> setAutonomousRotation,
            @NotNull BooleanSupplier isTrajectoryDone,
            @NotNull DoubleSupplier getTrajectoryElapsedTime,
            @NotNull Consumer<Pose2d> setRobotPose,
            boolean runOnMainThread
    ) {
        Preconditions.checkArgument(setNewTrajectory != null, "setNewTrajectory cannot be null");
        Preconditions.checkArgument(stopRobot != null, "stopRobot cannot be null");
        Preconditions.checkArgument(isTrajectoryDone != null, "isTrajectoryDone cannot be null");
        Preconditions.checkArgument(getTrajectoryElapsedTime != null, "getTrajectoryElapsedTime cannot be null");
        Preconditions.checkArgument(setRobotPose != null, "setRobotPose cannot be null");

        this.setNewTrajectory = setNewTrajectory;
        this.stopRobot = stopRobot;
        this.setAutonomousRotation = setAutonomousRotation;
        this.isTrajectoryDone = isTrajectoryDone;
        this.getTrajectoryElapsedTime = getTrajectoryElapsedTime;
        this.setRobotPose = setRobotPose;
        this.runOnMainThread = runOnMainThread;
    }

    private final @NotNull ConcurrentLinkedQueue<Runnable> commandQueue = new ConcurrentLinkedQueue<>();

    @Internal
    public void setNewTrajectory(@NotNull Trajectory trajectory) {
        if (runOnMainThread) {
            commandQueue.add(() -> setNewTrajectory.accept(trajectory));
        } else {
            setNewTrajectory.accept(trajectory);
        }
    }

    @Internal
    public void stopRobot() {
        if (runOnMainThread) {
            commandQueue.add(stopRobot);
        } else {
            stopRobot.run();
        }
    }

    @Internal
    public void setAutonomousRotation(@NotNull Rotation2d rotation) {
        if (AutonomousContainer.getInstance().isHolonomic()) {
            assert setAutonomousRotation != null;
            if (runOnMainThread) {
                commandQueue.add(() -> setAutonomousRotation.accept(rotation));
            } else {
                setAutonomousRotation.accept(rotation);
            }
        }
    }

    @Internal
    public void setRobotPose(@NotNull Pose2d pose) {
        if (runOnMainThread) {
            commandQueue.add(() -> setRobotPose.accept(pose));
        } else {
            setRobotPose.accept(pose);
        }
    }

    @Internal
    public boolean isTrajectoryDone() throws ExecutionException, InterruptedException {
        if (runOnMainThread) {
            CompletableFuture<Boolean> future = new CompletableFuture<>();
            commandQueue.add(() -> future.complete(isTrajectoryDone.getAsBoolean()));
            return future.get();
        } else {
            return isTrajectoryDone.getAsBoolean();
        }
    }

    @Internal
    public double getTrajectoryElapsedTime() throws ExecutionException, InterruptedException {
        if (runOnMainThread) {
            CompletableFuture<Double> future = new CompletableFuture<>();
            commandQueue.add(() -> future.complete(getTrajectoryElapsedTime.getAsDouble()));
            return future.get();
        } else {
            return getTrajectoryElapsedTime.getAsDouble();
        }
    }

    @Internal
    public void runOnMainThread(@NotNull Runnable runnable) {
        commandQueue.add(runnable);
    }

    @Internal
    protected void onPeriodic() {
        while (!commandQueue.isEmpty()) {
            commandQueue.poll().run();
        }
    }
}
