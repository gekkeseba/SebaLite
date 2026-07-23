package net.runelite.client.plugins.spoontob.rooms.Verzik;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.Stroke;
import java.util.ArrayList;
import javax.inject.Inject;
import net.runelite.api.Perspective;
import net.runelite.api.Point;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.NpcID;
import net.runelite.client.plugins.spoontob.RoomOverlay;
import net.runelite.client.plugins.spoontob.SpoonTobConfig;

public class YellowGrouperOverlay extends RoomOverlay
{
	@Inject
	private Verzik verzik;

	@Inject
	protected YellowGrouperOverlay(SpoonTobConfig config)
	{
		super(config);
	}

	public Dimension render(Graphics2D graphics)
	{
		if (this.verzik.yellowGroups.isEmpty() || this.config.showVerzikYellows() != SpoonTobConfig.verzikYellowsMode.GROUPS
			|| !this.verzik.yellowsOut || this.verzik.getVerzikNPC() == null || this.verzik.getVerzikNPC().getId() != NpcID.VERZIK_PHASE3_HARD)
		{
			return null;
		}

		String text = String.valueOf(this.verzik.yellowTimer);
		if (this.config.yellowTicksOnPlayer() && this.client.getLocalPlayer() != null)
		{
			Point point = Perspective.getCanvasTextLocation(this.client, graphics, this.client.getLocalPlayer().getLocalLocation(), "#", this.config.yellowsOffset());
			if (this.config.fontStyle())
			{
				renderTextLocation(graphics, text, Color.WHITE, point);
			}
			else
			{
				renderSteroidsTextLocation(graphics, text, this.config.yellowsSize(), 1, Color.WHITE, point);
			}
		}

		int group = 0;
		for (ArrayList<WorldPoint> list : this.verzik.yellowGroups)
		{
			for (WorldPoint next : list)
			{
				LocalPoint localPoint = LocalPoint.fromWorld(this.client, next);
				if (localPoint == null)
				{
					continue;
				}
				Polygon poly = Perspective.getCanvasTilePoly(this.client, localPoint);
				if (poly == null)
				{
					continue;
				}
				graphics.setColor(Color.BLACK);
				Stroke originalStroke = graphics.getStroke();
				graphics.setStroke(new BasicStroke(2.0F));
				graphics.draw(poly);
				Color fill = groupColor(group);
				graphics.setColor(new Color(fill.getRed(), fill.getGreen(), fill.getBlue(), 130));
				graphics.fill(poly);
				graphics.setStroke(originalStroke);
				if (!this.config.yellowTicksOnPlayer())
				{
					Point point = Perspective.getCanvasTextLocation(this.client, graphics, localPoint, text, 0);
					if (this.config.fontStyle())
					{
						renderTextLocation(graphics, text, Color.WHITE, point);
					}
					else
					{
						renderResizeTextLocation(graphics, text, 12, 1, Color.WHITE, point);
					}
				}
			}
			group++;
		}
		return null;
	}

	private Color groupColor(int group)
	{
		switch (group)
		{
			case 0:
				return Color.RED;
			case 1:
				return Color.BLUE;
			case 2:
				return Color.GREEN;
			case 3:
				return Color.MAGENTA;
			case 4:
				return Color.ORANGE;
			default:
				return new Color(250, 50, 100);
		}
	}
}
