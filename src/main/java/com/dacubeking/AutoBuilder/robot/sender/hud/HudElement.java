package com.dacubeking.AutoBuilder.robot.sender.hud;

import edu.wpi.first.networktables.NetworkTableEntry;
import edu.wpi.first.wpilibj.util.Color8Bit;

import java.text.DecimalFormat;

public class HudElement {
    Color8Bit color;
    NetworkTableEntry entry;
    String label;

    float width;

    DecimalFormat decimalFormat;

    public HudElement(NetworkTableEntry entry, String label, Color8Bit color, float width, DecimalFormat decimalFormat) {
        this.entry = entry;
        this.label = label;
        this.color = color;
        this.width = width;
        this.decimalFormat = decimalFormat;
    }

    public String getColorAsHex() {
        return String.format("#%02x%02x%02x", color.red, color.green, color.blue);
    }

    @Override
    public String toString() {
        return entry.getName() + "," + label + "," + getColorAsHex() + "," + width + "," + decimalFormat.toPattern();
    }
}
