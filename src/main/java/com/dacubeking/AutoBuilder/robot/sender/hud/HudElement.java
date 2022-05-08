package com.dacubeking.AutoBuilder.robot.sender.hud;

import edu.wpi.first.networktables.NetworkTableEntry;
import edu.wpi.first.wpilibj.util.Color8Bit;
import org.jetbrains.annotations.NotNull;

import java.text.DecimalFormat;


/**
 * Represents a hud element that can be sent to the GUI.
 *
 * @implNote This class is not thread safe. Each hud element should only be handled by one thread. (Multiple threads can be used
 * to modify different hud elements, but never the same hud element.)
 */
public class HudElement {
    private boolean dirty;

    private @NotNull Color8Bit color;
    private @NotNull NetworkTableEntry entry;
    private @NotNull String label;

    float width;

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
        this.color = color;
        dirty = true;
        return this;
    }

    public @NotNull NetworkTableEntry getEntry() {
        return entry;
    }

    public HudElement setEntry(@NotNull NetworkTableEntry entry) {
        this.entry = entry;
        dirty = true;
        return this;
    }

    public @NotNull String getLabel() {
        return label;
    }

    public HudElement setLabel(@NotNull String label) {
        this.label = label;
        dirty = true;
        return this;
    }

    public float getWidth() {
        return width;
    }

    public HudElement setWidth(float width) {
        this.width = width;
        dirty = true;
        return this;
    }

    public @NotNull DecimalFormat getDecimalFormat() {
        return decimalFormat;
    }

    public HudElement setDecimalFormat(@NotNull DecimalFormat decimalFormat) {
        this.decimalFormat = decimalFormat;
        dirty = true;
        return this;
    }

    public String getColorAsHex() {
        return String.format("#%02x%02x%02x", color.red, color.green, color.blue);
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
        return entry.getName() + "," + label + "," + getColorAsHex() + "," + width + "," + decimalFormat.toPattern();
    }
}
