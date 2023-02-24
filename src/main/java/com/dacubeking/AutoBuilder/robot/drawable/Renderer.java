package com.dacubeking.AutoBuilder.robot.drawable;

import edu.wpi.first.networktables.NetworkTableEntry;
import edu.wpi.first.networktables.NetworkTableInstance;

import java.nio.ByteBuffer;

public class Renderer {
    private static final NetworkTableEntry drawables = NetworkTableInstance.getDefault().getEntry("autodata/drawables");

    public static void render(Drawable... drawable) {
        int size = 0;
        for (Drawable d : drawable) {
            size += d.size() + Integer.BYTES;
        }

        ByteBuffer buffer = ByteBuffer.allocate(size);
        for (Drawable d : drawable) {
            buffer.putInt(d.size());
            buffer.put(d.toBytes());
        }

        drawables.setRaw(buffer.array());
    }
}
