package com.dacubeking.AutoBuilder.robot.drawable;

import com.dacubeking.AutoBuilder.robot.utility.Utils;
import com.dacubeking.AutoBuilder.robot.utility.Vector2;
import edu.wpi.first.wpilibj.util.Color8Bit;

import java.nio.ByteBuffer;

/**
 * A circle that can be drawn to the screen.
 */
public class Circle extends Drawable {
    private static final byte TYPE = 0x00;
    public final Vector2 center;
    public final float radius;

    public Circle(Vector2 center, float radius, Color8Bit color8Bit) {
        super(color8Bit);
        this.center = center;
        this.radius = radius;
    }

    public Circle(float centerX, float centerY, float radius, Color8Bit color8Bit) {
        super(color8Bit);
        this.center = new Vector2(centerX, centerY);
        this.radius = radius;
    }

    @Override
    public String toString() {
        return "C:" + center.toString() + "," + radius + "," + Utils.getColorAsHex(color);
    }

    @Override
    public byte[] toBytes() {
        ByteBuffer buffer = ByteBuffer.allocate(1 + 4 + 4 + 4 + 3);

        buffer.put(TYPE);
        buffer.putFloat(center.x);
        buffer.putFloat(center.y);
        buffer.putFloat(radius);
        buffer.put((byte) color.red);
        buffer.put((byte) color.green);
        buffer.put((byte) color.blue);

        return buffer.array();
    }

    @Override
    public Drawable fromBytes(byte[] bytes) {
        ByteBuffer buffer = ByteBuffer.wrap(bytes);

        byte type = buffer.get();
        if (type != TYPE) {
            throw new IllegalArgumentException("Invalid type");
        }

        float centerX = buffer.getFloat();
        float centerY = buffer.getFloat();
        float radius = buffer.getFloat();
        byte red = buffer.get();
        byte green = buffer.get();
        byte blue = buffer.get();

        return new Circle(centerX, centerY, radius, new Color8Bit(red, green, blue));
    }

    @Override
    public int size() {
        return 1 + 4 + 4 + 4 + 3;
    }
}
