package net.runelite.client.plugins.aoewarnings;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.time.Instant;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.api.Perspective;
import net.runelite.api.Point;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayUtil;

@Singleton
public class BombOverlay extends Overlay
{
	private static final NumberFormat TIME_LEFT_FORMATTER = DecimalFormat.getInstance(Locale.US);

	static
	{
		((DecimalFormat) TIME_LEFT_FORMATTER).applyPattern("#0.0");
	}

	private final Client client;
	private final AoeWarningPlugin plugin;
	private final AoeWarningConfig config;

	@Inject
	public BombOverlay(Client client, AoeWarningPlugin plugin, AoeWarningConfig config)
	{
		this.client = client;
		this.plugin = plugin;
		this.config = config;
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_SCENE);
		setPriority(PRIORITY_MED);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (this.config.bombDisplay())
		{
			drawDangerZone(graphics);
		}
		return null;
	}

	private void drawDangerZone(Graphics2D graphics)
	{
		WorldPoint loc = this.client.getLocalPlayer().getWorldLocation();
		Map<WorldPoint, Integer> aoeTiles = new HashMap<>();

		this.plugin.getBombs().forEach(bomb ->
		{
			LocalPoint localLoc = LocalPoint.fromWorld(this.client, bomb.getWorldLocation());
			WorldPoint worldLoc = bomb.getWorldLocation();
			if (localLoc == null)
			{
				return;
			}

			double distanceX = Math.abs(worldLoc.getX() - loc.getX());
			double distanceY = Math.abs(worldLoc.getY() - loc.getY());
			Color colorCode = Color.decode("#00cc00");
			if (distanceX < 1.0 && distanceY < 1.0)
			{
				colorCode = Color.decode("#cc0000");
			}
			else if (distanceX < 2.0 && distanceY < 2.0)
			{
				colorCode = Color.decode("#ff6600");
			}
			else if (distanceX < 3.0 && distanceY < 3.0)
			{
				colorCode = Color.decode("#ff9933");
			}
			else if (distanceX < 4.0 && distanceY < 4.0)
			{
				colorCode = Color.decode("#ffff00");
			}

			Polygon poly = Perspective.getCanvasTileAreaPoly(this.client, localLoc, 7);

			if (this.config.bombHeatmap())
			{
				for (int x = -3; x < 4; x++)
				{
					for (int y = -3; y < 4; y++)
					{
						WorldPoint aoeTile = new WorldPoint(worldLoc.getX() + x, worldLoc.getY() + y, loc.getPlane());
						int severity = 1;
						int absX = Math.abs(x);
						int absY = Math.abs(y);
						if (absX < 1 && absY < 1)
						{
							severity = 4;
						}
						else if (absX < 2 && absY < 2)
						{
							severity = 3;
						}
						else if (absX < 3 && absY < 3)
						{
							severity = 2;
						}
						aoeTiles.merge(aoeTile, severity, Integer::sum);
					}
				}
			}

			if (poly != null)
			{
				graphics.setColor(colorCode);
				graphics.setStroke(new BasicStroke(1.0f));
				graphics.drawPolygon(poly);
				graphics.setColor(new Color(0, 0, 0, 10));
				graphics.fillPolygon(poly);
			}

			Instant now = Instant.now();
			double timeLeft = (8 - this.client.getTickCount() - bomb.getTickStarted()) * 0.6 - (now.toEpochMilli() - bomb.getLastClockUpdate().toEpochMilli()) / 1000.0;
			timeLeft = Math.max(0.0, timeLeft);
			String bombTimerString = TIME_LEFT_FORMATTER.format(timeLeft);
			int textWidth = graphics.getFontMetrics().stringWidth(bombTimerString);
			int textHeight = graphics.getFontMetrics().getAscent();
			Point canvasPoint = Perspective.localToCanvas(this.client, localLoc.getX(), localLoc.getY(), worldLoc.getPlane());
			if (canvasPoint != null)
			{
				Point canvasCenterPoint = new Point(canvasPoint.getX() - textWidth / 2, canvasPoint.getY() + textHeight / 2);
				OverlayUtil.renderTextLocation(graphics, canvasCenterPoint, bombTimerString, colorCode);
			}
		});

		aoeTiles.forEach((tile, count) ->
		{
			LocalPoint localPoint = LocalPoint.fromWorld(this.client, tile);
			if (localPoint == null)
			{
				return;
			}
			Color color = Color.decode("#00cc00");
			if (count == 2)
			{
				color = Color.decode("#ffff00");
			}
			if (count == 3)
			{
				color = Color.decode("#ff9933");
			}
			if (count == 4)
			{
				color = Color.decode("#ff6600");
			}
			if (count >= 5)
			{
				color = Color.decode("#cc0000");
			}
			graphics.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), this.config.bombHeatmapOpacity()));
			graphics.fill(Perspective.getCanvasTilePoly(this.client, localPoint));
		});
	}
}
