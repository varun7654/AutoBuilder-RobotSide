package com.dacubeking.AutoBuilder.robot.sender.hud;

import edu.wpi.first.networktables.NetworkTableEntry;
import edu.wpi.first.networktables.NetworkTableInstance;
import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;
import java.util.concurrent.ConcurrentHashMap;

class HudElementSender {
    private static final @NotNull NetworkTableEntry hudElementsEntry = NetworkTableInstance.getDefault()
            .getEntry("autodata/hudElements");

    private static final ConcurrentHashMap<HudElement, byte[]> hudElements = new ConcurrentHashMap<>();

    /**
     * Adds/updates a hud element on the GUI.
     *
     * @param element The hud element to add/update.
     */
    protected static void send(@NotNull HudElement element) {
        hudElements.put(element, element.toBytes());
        send();
    }

    /**
     * Sends all hud elements to the GUI.
     */
    protected static void send() {
       synchronized (hudElements) {
           int size = 0;
           for (byte[] bytes : hudElements.values()) {
               size += bytes.length + Integer.BYTES;
           }

           ByteBuffer buffer = ByteBuffer.allocate(size);
           for (byte[] bytes : hudElements.values()) {
               buffer.put(bytes);
           }

           hudElementsEntry.setRaw(buffer.array());
       }
    }
}
