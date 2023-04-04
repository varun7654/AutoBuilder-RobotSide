package com.dacubeking.AutoBuilder.robot.drawable;

import com.dacubeking.AutoBuilder.robot.utility.Utils;
import com.dacubeking.AutoBuilder.robot.utility.Vector2;
import edu.wpi.first.wpilibj.util.Color8Bit;

import java.nio.ByteBuffer;

/**
 * A line that can be drawn to the screen.
 */
public class Line extends Drawable {
    private static final byte TYPE = 0x01;
    public final Vector2 start;
    public final Vector2 end;

    public Line(Vector2 start, Vector2 end, Color8Bit color8Bit) {
        super(color8Bit);
        this.start = start;
        this.end = end;
    }

    public Line(float startX, float startY, float endX, float endY, Color8Bit color8Bit) {
        super(color8Bit);
        this.start = new Vector2(startX, startY);
        this.end = new Vector2(endX, endY);
    }

    @Override
    public String toString() {
        return "L:" + start.toString() + "," + end.toString() + "," + Utils.getColorAsHex(color);
    }

    @Override
    public byte[] toBytes() {
        ByteBuffer buffer = ByteBuffer.allocate(1 + 4 + 4 + 4 + 4 + 3);

        buffer.put(TYPE);
        buffer.putFloat(start.x);
        buffer.putFloat(start.y);
        buffer.putFloat(end.x);
        buffer.putFloat(end.y);
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

        float startX = buffer.getFloat();
        float startY = buffer.getFloat();
        float endX = buffer.getFloat();
        float endY = buffer.getFloat();
        byte red = buffer.get();
        byte green = buffer.get();
        byte blue = buffer.get();

        return new Line(startX, startY, endX, endY, new Color8Bit(red, green, blue));
    }

    @Override
    public int size() {
        return 1 + 4 + 4 + 4 + 4 + 3;
    }
}
