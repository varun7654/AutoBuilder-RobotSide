package com.dacubeking.AutoBuilder.robot.sender.pathpreview;

import edu.wpi.first.networktables.NetworkTableEntry;
import edu.wpi.first.networktables.NetworkTableInstance;
import edu.wpi.first.wpilibj.Timer;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

public final class RobotPositionSender {
    private static final NetworkTableEntry robotPositionsTable = NetworkTableInstance.getDefault()
            .getEntry("autodata/robotPositions");
    private final static Map<String, RobotState> robotStatesHashMap = new ConcurrentHashMap<>(4);

    private static final Predicate<RobotState> removeCondition = state -> state.timeCreated < Timer.getFPGATimestamp() - 0.2;

    public synchronized static void addRobotPosition(RobotState robotState) {
        robotStatesHashMap.put(robotState.name, robotState);
        if (robotState.name.equals("Robot Position")) {
            synchronized (robotStatesHashMap) {
                robotStatesHashMap.values().removeIf(removeCondition); // remove values older than 0.2 seconds
                int size = 0;
                for (RobotState state : robotStatesHashMap.values()) {
                    size += state.size() + Integer.BYTES;
                }

                ByteBuffer buffer = ByteBuffer.allocate(size);
                for (RobotState state : robotStatesHashMap.values()) {
                    buffer.putInt(state.size());
                    buffer.putDouble(state.x);
                    buffer.putDouble(state.y);
                    buffer.putDouble(state.theta);
                    buffer.putDouble(state.vx);
                    buffer.putDouble(state.vy);
                    buffer.putDouble(state.vTheta);
                    buffer.putDouble(state.time);
                    buffer.put(state.name.getBytes(StandardCharsets.UTF_8));
                }
                robotPositionsTable.setRaw(buffer.array());
            }
        }
    }

    public List<RobotState> getRobotStates(byte[] data) {
        ByteBuffer buffer = ByteBuffer.wrap(data);
        while (buffer.hasRemaining()) {
            int size = buffer.getInt();
            double x = buffer.getDouble();
            double y = buffer.getDouble();
            double theta = buffer.getDouble();
            double vx = buffer.getDouble();
            double vy = buffer.getDouble();
            double vTheta = buffer.getDouble();
            double time = buffer.getDouble();
            byte[] nameBytes = new byte[size - 7 * Double.BYTES];
            buffer.get(nameBytes);
            String name = new String(nameBytes, StandardCharsets.UTF_8);
            RobotState state = new RobotState(x, y, theta, vx, vy, vTheta, time, name);
            robotStatesHashMap.put(name, state);
        }
        return List.copyOf(robotStatesHashMap.values());
    }
}
