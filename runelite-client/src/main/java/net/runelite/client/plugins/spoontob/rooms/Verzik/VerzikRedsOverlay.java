package net.runelite.client.plugins.spoontob.rooms.Verzik;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Polygon;
import javax.inject.Inject;
import net.runelite.api.NPC;
import net.runelite.api.NPCComposition;
import net.runelite.api.coords.LocalPoint;
import net.runelite.client.plugins.spoontob.RoomOverlay;
import net.runelite.client.plugins.spoontob.SpoonTobConfig;
import net.runelite.client.plugins.spoontob.SpoonTobPlugin;
import net.runelite.client.plugins.spoontob.util.TheatreRegions;

public class VerzikRedsOverlay extends RoomOverlay
{
	@Inject
	private SpoonTobPlugin plugin;

	@Inject
	private Verzik verzik;

	@Inject
	protected VerzikRedsOverlay(SpoonTobConfig config)
	{
		super(config);
	}

	public Dimension render(Graphics2D graphics)
	{
		if ((this.config.redsTL() == SpoonTobConfig.redsTlMode.OFF && !this.config.redsFreezeWarning()) || !this.plugin.enforceRegion())
		{
			return null;
		}
		for (NPC reds : this.client.getNpcs())
		{
			if (reds.getName() == null || !reds.getName().equalsIgnoreCase("nylocas matomenos"))
			{
				continue;
			}
			NPCComposition composition = reds.getComposition();
			int size = composition.getSize();
			LocalPoint lp = LocalPoint.fromWorld(this.client, reds.getWorldLocation());
			if (lp == null)
			{
				continue;
			}
			lp = new LocalPoint(lp.getX() + size * 128 / 2 - 64, lp.getY() + size * 128 / 2 - 64);
			Polygon tilePoly = getCanvasTileAreaPoly(this.client, lp, size, true);
			if (tilePoly != null && (this.config.redsTL() == SpoonTobConfig.redsTlMode.VERZIK || this.config.redsTL() == SpoonTobConfig.redsTlMode.BOTH)
				&& TheatreRegions.inRegion(this.client, TheatreRegions.VERZIK))
			{
				renderOutlineAndFill(graphics, tilePoly, this.config.redsTLColor(), this.config.redsTLColor().getAlpha(), 0);
			}
		}
		return null;
	}

	private void renderOutlineAndFill(Graphics2D graphics, Polygon polygon, Color color, int outlineOpacity, int fillOpacity)
	{
		graphics.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), outlineOpacity));
		graphics.setStroke(new BasicStroke(1.0F));
		graphics.draw(polygon);
		graphics.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), fillOpacity));
		graphics.fill(polygon);
	}
}
