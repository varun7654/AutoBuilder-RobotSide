package com.dacubeking.AutoBuilder.robot.drawable;

import com.dacubeking.AutoBuilder.robot.utility.Utils;
import com.dacubeking.AutoBuilder.robot.utility.Vector2;
import edu.wpi.first.wpilibj.util.Color8Bit;

import java.nio.ByteBuffer;

/**
 * A path that can be drawn to the screen. (Basically a list of lines.)
 */
public class Path extends Drawable {

    public static final byte TYPE = 0x02;
    public final Vector2[] vertices;

    public Path(Vector2[] vertices, Color8Bit color8Bit) {
        super(color8Bit);
        this.vertices = vertices;
    }

    public Path(Color8Bit color8Bit, Vector2... vertices) {
        super(color8Bit);
        this.vertices = vertices;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("P:");
        for (Vector2 vertex : vertices) {
            sb.append(vertex.toString()).append(",");
        }
        sb.append(Utils.getColorAsHex(color));
        return sb.toString();
    }

    @Override
    public byte[] toBytes() {
        ByteBuffer buffer = ByteBuffer.allocate(1 + 4 * 2 * vertices.length + 3);

        buffer.put(TYPE);

        for (Vector2 vertex : vertices) {
            buffer.putFloat(vertex.x);
            buffer.putFloat(vertex.y);
        }

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

        Vector2[] vertices = new Vector2[(bytes.length - 1 - 3) / 8];
        for (int i = 0; i < vertices.length; i++) {
            float x = buffer.getFloat();
            float y = buffer.getFloat();
            vertices[i] = new Vector2(x, y);
        }

        byte red = buffer.get();
        byte green = buffer.get();
        byte blue = buffer.get();
        Color8Bit color = new Color8Bit(red, green, blue);

        return new Path(vertices, color);
    }

    @Override
    public int size() {
        return 1 + 4 * 2 * vertices.length + 3;
    }
}
