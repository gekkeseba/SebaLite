package net.runelite.client.plugins.spoontob.rooms.Bloat;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;
import net.runelite.api.Perspective;
import net.runelite.api.Point;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.spoontob.RoomOverlay;
import net.runelite.client.plugins.spoontob.SpoonTobConfig;
import net.runelite.client.plugins.spoontob.rooms.Bloat.stomp.BloatSafespot;
import net.runelite.client.plugins.spoontob.rooms.Bloat.stomp.SSLine;

public class BloatOverlay extends RoomOverlay
{
	@Inject
	private Bloat bloat;

	@Inject
	protected BloatOverlay(SpoonTobConfig config)
	{
		super(config);
	}

	public Dimension render(Graphics2D graphics)
	{
		if (!this.bloat.isBloatActive())
		{
			return null;
		}

		if (this.config.bloatIndicator() == SpoonTobConfig.BloatIndicatorMode.TILE)
		{
			renderNpcPoly(graphics, this.bloat.getBloatStateColor(), this.bloat.getBloatTilePoly(), 3.0D, this.bloat.getBloatStateColor().getAlpha());
		}
		else if (this.config.bloatIndicator() == SpoonTobConfig.BloatIndicatorMode.TRUE_LOCATION)
		{
			renderNpcTLOverlay(graphics, this.bloat.getBloatNPC(), this.bloat.getBloatStateColor(), 3, this.bloat.getBloatStateColor().getAlpha(), 0);
		}

		if (this.config.showBloatHands() != SpoonTobConfig.bloatHandsMode.OFF || this.config.bloatHandsTicks())
		{
			renderHands(graphics);
		}

		if (this.bloat.bloatVar == 1)
		{
			if (this.config.bloatUpTimer())
			{
				renderUpTimer(graphics);
			}
		}
		else if (this.bloat.bloatVar == 0 && this.config.bloatEntryTimer())
		{
			Point canvasPoint = this.bloat.getBloatNPC().getCanvasTextLocation(graphics, String.valueOf(this.bloat.getBloatUpTimer()), 60);
			if (this.config.fontStyle())
			{
				renderTextLocation(graphics, String.valueOf(this.bloat.getBloatUpTimer()), Color.WHITE, canvasPoint);
			}
			else
			{
				renderResizeTextLocation(graphics, String.valueOf(this.bloat.getBloatUpTimer()), 15, 1, Color.WHITE, canvasPoint);
			}
		}

		if ((this.bloat.getBloatState() == 2 || this.bloat.getBloatState() == 3) && this.config.bloatStompMode() != SpoonTobConfig.bloatStompMode.OFF)
		{
			renderStompSafespots(graphics);
		}

		return null;
	}

	private void renderHands(Graphics2D graphics)
	{
		Color color = this.config.bloatHandColor();
		for (WorldPoint point : this.bloat.getBloathands().keySet())
		{
			drawTile(graphics, point, color, 1, this.config.bloatHandColor().getAlpha(), this.config.bloatColorFill());
			if (this.config.bloatHandsTicks() && this.bloat.handsFalling)
			{
				String text = String.valueOf(this.bloat.handTicks);
				LocalPoint lp = LocalPoint.fromWorld(this.client, point);
				if (lp != null)
				{
					Point p = Perspective.getCanvasTextLocation(this.client, graphics, lp, text, 0);
					if (this.config.fontStyle())
					{
						renderTextLocation(graphics, text, Color.WHITE, p);
					}
					else
					{
						renderSteroidsTextLocation(graphics, text, 12, 1, Color.WHITE, p);
					}
				}
			}
		}
	}

	private void renderUpTimer(Graphics2D graphics)
	{
		Point canvasPoint = this.bloat.getBloatNPC().getCanvasTextLocation(graphics, String.valueOf(this.bloat.getBloatUpTimer()), 60);
		if (this.bloat.getBloatState() != 1 && this.bloat.getBloatState() != 4)
		{
			String str = String.valueOf(33 - this.bloat.getBloatDownCount());
			Color color = (this.bloat.getBloatDownCount() >= 26) ? Color.RED : Color.WHITE;
			if (this.config.fontStyle())
			{
				renderTextLocation(graphics, str, color, canvasPoint);
			}
			else
			{
				renderResizeTextLocation(graphics, str, 15, 1, color, canvasPoint);
			}
		}
		else
		{
			Color col = (this.bloat.getBloatUpTimer() > 37) ? Color.RED : Color.WHITE;
			if (this.config.fontStyle())
			{
				renderTextLocation(graphics, String.valueOf(this.bloat.getBloatUpTimer()), col, canvasPoint);
			}
			else
			{
				renderResizeTextLocation(graphics, String.valueOf(this.bloat.getBloatUpTimer()), 15, 1, col, canvasPoint);
			}
		}
	}

	private void renderStompSafespots(Graphics2D graphics)
	{
		if (this.bloat.getBloatDown() == null)
		{
			return;
		}
		BloatSafespot safespot = this.bloat.getBloatDown().getBloatSafespot();
		safespot.getSafespotLines().forEach(line -> drawLine(graphics, line, this.config.bloatStompColor(), this.config.bloatStompWidth()));
	}

	protected void drawLine(Graphics2D graphics, @Nullable SSLine safespotLine, @Nonnull Color lineColor, int lineStroke)
	{
		if (safespotLine == null)
		{
			return;
		}
		Point pointA = safespotLine.getTranslatedPointA(this.client);
		Point pointB = safespotLine.getTranslatedPointB(this.client);
		if (pointA != null && pointB != null)
		{
			graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			graphics.setStroke(new BasicStroke(lineStroke));
			graphics.setColor(lineColor);
			graphics.drawLine(pointA.getX(), pointA.getY(), pointB.getX(), pointB.getY());
		}
	}
}
