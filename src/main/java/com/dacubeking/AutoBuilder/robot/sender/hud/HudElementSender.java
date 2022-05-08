package com.dacubeking.AutoBuilder.robot.sender.hud;

import edu.wpi.first.networktables.NetworkTableEntry;
import edu.wpi.first.networktables.NetworkTableInstance;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Set;

public final class HudElementSender {
    private static final @NotNull NetworkTableEntry hudElementsEntry = NetworkTableInstance.getDefault()
            .getEntry("autodata.hudElements");

    private static Set<HudElement> hudElements = new HashSet<>();

    /**
     * Adds/updates a hud element on the GUI.
     *
     * @param element The hud element to add/update.
     */
    public static void send(@NotNull HudElement element) {
        hudElements.add(element);
        StringBuilder sb = new StringBuilder();
        for (HudElement hudElement : hudElements) {
            sb.append(hudElement.toString()).append(";");
        }
        hudElementsEntry.setString(sb.toString());
    }
}
