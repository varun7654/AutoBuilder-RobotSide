package com.dacubeking.AutoBuilder.robot.sender.hud;

import com.dacubeking.AutoBuilder.robot.utility.Utils;
import edu.wpi.first.networktables.NetworkTableEntry;
import edu.wpi.first.networktables.NetworkTableInstance;
import edu.wpi.first.wpilibj.util.Color8Bit;
import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;


/**
 * Represents a hud element that can be sent to the GUI.
 * <p>
 * This class is not thread safe. Each hud element should only be handled by one thread. (Multiple threads can be used to modify different hud elements, but never the same hud element.)
 */
public class HudElement {
    private boolean dirty;

    private @NotNull Color8Bit color;
    private @NotNull NetworkTableEntry entry;
    private @NotNull String label;

    private float width;

    private @NotNull DecimalFormat decimalFormat;

    public HudElement(@NotNull NetworkTableEntry entry, @NotNull String label, @NotNull Color8Bit color, float width,
                      @NotNull DecimalFormat decimalFormat) {
        this.entry = entry;
        this.label = label;
        this.color = color;
        this.width = width;
        this.decimalFormat = decimalFormat;
        dirty = true;
    }

    public @NotNull Color8Bit getColor() {
        return color;
    }

    public HudElement setColor(@NotNull Color8Bit color) {
        if (this.color.equals(color)) {
            return this;
        }
        this.color = color;
        dirty = true;
        return this;
    }

    public @NotNull NetworkTableEntry getEntry() {
        return entry;
    }

    public HudElement setEntry(@NotNull NetworkTableEntry entry) {
        if (this.entry.getName().equals(entry.getName())) {
            return this;
        }
        this.entry = entry;
        dirty = true;
        return this;
    }

    public @NotNull String getLabel() {
        return label;
    }

    public HudElement setLabel(@NotNull String label) {
        if (this.label.equals(label)) {
            return this;
        }
        this.label = label;
        dirty = true;
        return this;
    }

    public float getWidth() {
        return width;
    }

    public HudElement setWidth(float width) {
        if (this.width == width) {
            return this;
        }
        this.width = width;
        dirty = true;
        return this;
    }

    public @NotNull DecimalFormat getDecimalFormat() {
        return decimalFormat;
    }

    public HudElement setDecimalFormat(@NotNull DecimalFormat decimalFormat) {
        if (this.decimalFormat.equals(decimalFormat)) {
            return this;
        }
        this.decimalFormat = decimalFormat;
        dirty = true;
        return this;
    }

    /**
     * Publishes the hud element to the network table to be displayed on the GUI.
     */
    public void push() {
        if (dirty) {
            HudElementSender.send(this);
            dirty = false;
        }
    }

    @Override
    public String toString() {
        return entry.getName() + "," + label + "," + Utils.getColorAsHex(color) + "," + width + "," + decimalFormat.toPattern();
    }

    public byte[] toBytes() {
        byte[] entryName = entry.getName().getBytes(StandardCharsets.UTF_8);
        byte[] labelBytes = label.getBytes(StandardCharsets.UTF_8);
        byte[] decimalFormatPattern = decimalFormat.toPattern().getBytes(StandardCharsets.UTF_8);
        ByteBuffer buffer = ByteBuffer.allocate(
                4 + // Length
                        entryName.length +
                        labelBytes.length +
                        3 + // Color
                        4 + // Width
                        decimalFormatPattern.length
        );
        buffer.putInt(buffer.capacity());
        buffer.put(entryName);
        buffer.put(labelBytes);
        buffer.put((byte) color.red);
        buffer.put((byte) color.green);
        buffer.put((byte) color.blue);
        buffer.putFloat(width);
        buffer.put(decimalFormatPattern);

        return buffer.array();
    }

    public static HudElement fromBytes(byte[] bytes) {
        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        int length = buffer.getInt();
        byte[] entryName = new byte[length];
        buffer.get(entryName);
        byte[] labelBytes = new byte[length];
        buffer.get(labelBytes);
        byte red = buffer.get();
        byte green = buffer.get();
        byte blue = buffer.get();
        float width = buffer.getFloat();
        byte[] decimalFormatPattern = new byte[length];
        buffer.get(decimalFormatPattern);

        return new HudElement(
                NetworkTableInstance.getDefault().getEntry(new String(entryName, StandardCharsets.UTF_8)),
                new String(labelBytes, StandardCharsets.UTF_8),
                new Color8Bit(red, green, blue),
                width,
                new DecimalFormat(new String(decimalFormatPattern, StandardCharsets.UTF_8))
        );
    }
}
