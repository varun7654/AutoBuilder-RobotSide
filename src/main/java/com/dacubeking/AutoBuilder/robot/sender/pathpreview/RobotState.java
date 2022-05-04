package com.dacubeking.AutoBuilder.robot.sender.pathpreview;

import edu.wpi.first.wpilibj.Timer;

public class RobotState {
    public final double x;
    public final double y;
    public final double theta;
    public final double vx;
    public final double vy;
    public final double vTheta;
    public final double time;
    public final String name;
    public final double timeCreated;


    /**
     * Constructs a new RobotState.
     *
     * @param x      The x position.
     * @param y      The y position.
     * @param theta  The angle.
     * @param vx     The x velocity.
     * @param vy     The y velocity.
     * @param vTheta The angular velocity.
     * @param time   Timestamp of this position.
     * @param name   name of this position. (ex: odometry pose, vision pose, etc.) ("Robot Position" is the default and is the one
     *               that is plotted in the GUI)
     */
    public RobotState(double x, double y, double theta, double vx, double vy, double vTheta, double time, String name) {
        this.x = x;
        this.y = y;
        this.theta = theta;
        this.vx = vx;
        this.vy = vy;
        this.vTheta = vTheta;
        this.time = time;
        this.name = name;
        this.timeCreated = Timer.getFPGATimestamp();
    }

    /**
     * Constructs a new RobotState.
     *
     * @param x      The x position.
     * @param y      The y position.
     * @param theta  The angle.
     * @param vx     The x velocity.
     * @param vy     The y velocity.
     * @param vTheta The angular velocity.
     * @param time   Timestamp of this position.
     */
    public RobotState(double x, double y, double theta, double vx, double vy, double vTheta, double time) {
        this(x, y, theta, vx, vy, vTheta, time, "Robot Position");
    }

    /**
     * Constructs a new RobotState.
     *
     * @param x      The x position.
     * @param y      The y position.
     * @param theta  The angle.
     * @param vx     The x velocity.
     * @param vy     The y velocity.
     * @param vTheta The angular velocity.
     * @param name   name of this position. (ex: odometry pose, vision pose, etc.) ("Robot Position" is the default and is the one
     *               that is plotted in the GUI)
     */
    public RobotState(double x, double y, double theta, double vx, double vy, double vTheta, String name) {
        this(x, y, theta, vx, vy, vTheta, Timer.getFPGATimestamp(), name);
    }


    /**
     * Constructs a new RobotState.
     *
     * @param x      The x position.
     * @param y      The y position.
     * @param theta  The angle.
     * @param vx     The x velocity.
     * @param vy     The y velocity.
     * @param vTheta The angular velocity.
     */
    public RobotState(double x, double y, double theta, double vx, double vy, double vTheta) {
        this(x, y, theta, vx, vy, vTheta, "Robot Position");
    }

    /**
     * Constructs a new RobotState.
     *
     * @param x     The x position.
     * @param y     The y position.
     * @param theta The angle.
     * @param name  name of this position. (ex: odometry pose, vision pose, etc.) ("Robot Position" is the default and is the one
     *              that is plotted in the GUI)
     */
    public RobotState(double x, double y, double theta, String name) {
        this(x, y, theta, 0, 0, 0, name);
    }


    /**
     * Constructs a new RobotState.
     *
     * @param x     The x position.
     * @param y     The y position.
     * @param theta The angle.
     */
    public RobotState(double x, double y, double theta) {
        this(x, y, theta, "Robot Position");
    }

    /**
     * Constructs a new RobotState.
     *
     * @param x     The x position.
     * @param y     The y position.
     * @param theta The angle.
     * @param time  Timestamp of this position.
     */
    public RobotState(double x, double y, double theta, double time) {
        this(x, y, theta, 0, 0, 0, time, "Robot Position");
    }


    @Override
    public String toString() {
        return x + "," + y + "," + theta + "," + vx + "," + vy + "," + vTheta + "," + time + "," + name;
    }
}
