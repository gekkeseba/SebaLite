package net.runelite.client.plugins.spoontob;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.Player;
import net.runelite.api.Point;
import net.runelite.client.plugins.spoontob.util.TheatreRegions;

public class SituationalTickOverlay extends RoomOverlay
{
	@Inject
	private Client client;

	@Inject
	private SpoonTobConfig config;

	@Inject
	private SpoonTobPlugin plugin;

	@Inject
	protected SituationalTickOverlay(SpoonTobConfig config)
	{
		super(config);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (!this.plugin.enforceRegion() || !this.config.situationalTicks())
		{
			return null;
		}

		Player localPlayer = this.client.getLocalPlayer();
		if (localPlayer == null)
		{
			return null;
		}

		if (TheatreRegions.inRegion(this.client, TheatreRegions.BLOAT))
		{
			Integer tick = this.plugin.situationalTicksList.get(localPlayer);
			if (tick != null)
			{
				drawTick(graphics, localPlayer, tick);
			}
		}
		else if (TheatreRegions.inRegion(this.client, TheatreRegions.XARPUS))
		{
			for (Player p : this.plugin.situationalTicksList.keySet())
			{
				drawTick(graphics, p, this.plugin.situationalTicksList.get(p));
			}
		}
		return null;
	}

	private void drawTick(Graphics2D graphics, Player player, int tick)
	{
		Point canvasPoint = player.getCanvasTextLocation(graphics, String.valueOf(tick), this.config.situationalTicksOffset());
		Color color = tick == 1 ? Color.GREEN : Color.WHITE;
		if (this.config.fontStyle())
		{
			renderTextLocation(graphics, String.valueOf(tick), color, canvasPoint);
		}
		else
		{
			renderSteroidsTextLocation(graphics, String.valueOf(tick), this.config.situationalTicksSize(), 1, color, canvasPoint);
		}
	}
}
