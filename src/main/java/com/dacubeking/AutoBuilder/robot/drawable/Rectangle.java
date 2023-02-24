package com.dacubeking.AutoBuilder.robot.drawable;

import com.dacubeking.AutoBuilder.robot.utility.Utils;
import com.dacubeking.AutoBuilder.robot.utility.Vector2;
import edu.wpi.first.wpilibj.util.Color8Bit;

import java.nio.ByteBuffer;

/**
 * A rectangle that can be drawn to the screen.
 */
public class Rectangle extends Drawable {
    private static final byte TYPE = 0x03;
    public final Vector2 bottomLeftCorner;

    public float width;
    public float height;
    public float rotation;


    /**
     * @param x        the x-coordinate of the bottom left corner of the rectangle
     * @param y        the y-coordinate of the bottom left corner of the rectangle
     * @param width    the width of the rectangle
     * @param height   the height of the rectangle
     * @param rotation the anticlockwise rotation in radians
     */
    public Rectangle(float x, float y, float width, float height, float rotation, Color8Bit color) {
        super(color);
        this.bottomLeftCorner = new Vector2(x, y);
        this.width = width;
        this.height = height;
        this.rotation = rotation;
    }

    /**
     * @param bottomLeftCorner the bottom left corner of the rectangle
     * @param width            the width of the rectangle
     * @param height           the height of the rectangle
     * @param rotation         the anticlockwise rotation in radians
     * @param color            the color of the rectangle
     */
    public Rectangle(Vector2 bottomLeftCorner, float width, float height, float rotation, Color8Bit color) {
        super(color);
        this.bottomLeftCorner = bottomLeftCorner;
        this.width = width;
        this.height = height;
        this.rotation = rotation;
    }

    @Override
    public String toString() {
        return "R:" + bottomLeftCorner.toString() + "," + width + "," + height + "," + rotation
                + "," + Utils.getColorAsHex(color);
    }

    @Override
    public byte[] toBytes() {
        ByteBuffer buffer = ByteBuffer.allocate(1 + 4 + 4 + 4 + 4 + 4 + 3);

        buffer.put(TYPE);
        buffer.putFloat(bottomLeftCorner.x);
        buffer.putFloat(bottomLeftCorner.y);
        buffer.putFloat(width);
        buffer.putFloat(height);
        buffer.putFloat(rotation);
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

        float x = buffer.getFloat();
        float y = buffer.getFloat();
        float width = buffer.getFloat();
        float height = buffer.getFloat();
        float rotation = buffer.getFloat();
        byte red = buffer.get();
        byte green = buffer.get();
        byte blue = buffer.get();

        return new Rectangle(x, y, width, height, rotation, new Color8Bit(red, green, blue));
    }

    @Override
    public int size() {
        return 1 + 4 + 4 + 4 + 4 + 4 + 3;
    }
}
