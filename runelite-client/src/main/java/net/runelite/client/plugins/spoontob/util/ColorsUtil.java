package net.runelite.client.plugins.spoontob.util;

import java.awt.Color;

/**
 * Converts between standard RGB and OSRS's packed model face-color format
 * (6-bit hue / 3-bit saturation / 7-bit brightness), used to recolor 3D model faces directly.
 */
public class ColorsUtil
{
	public static int RGBtoRS2HSB(int r, int g, int b)
	{
		float[] hsb = Color.RGBtoHSB(r, g, b, null);
		float hue = hsb[0];
		float saturation = hsb[1];
		float brightness = hsb[2] - Math.min(hue, hsb[2] / 2.0f);

		int encodedHue = (int) (hue * 63.0f);
		int encodedSaturation = (int) (saturation * 7.0f);
		int encodedBrightness = (int) (brightness * 127.0f);
		return (encodedHue << 10) + (encodedSaturation << 7) + encodedBrightness;
	}

	public static int RS2HSBtoRGB(int rs2hsb)
	{
		int hue = (rs2hsb >> 10) & 0x3F;
		int saturation = (rs2hsb >> 7) & 0x7;
		int brightness = rs2hsb & 0x7F;
		return Color.HSBtoRGB(hue / 63.0f, saturation / 7.0f, brightness / 127.0f);
	}
}
