package net.runelite.client.plugins.aoewarnings;

import java.awt.*;

public class ColorUtil {
    public ColorUtil() {
    }

    public static int setAlphaComponent(Color color, int alpha) {
        return setAlphaComponent(color.getRGB(), alpha);
    }

    public static int setAlphaComponent(int color, int alpha) {
        if (alpha >= 0 && alpha <= 255) {
            return color & 16777215 | alpha << 24;
        } else {
            throw new IllegalArgumentException("alpha must be between 0 and 255.");
        }
    }}

