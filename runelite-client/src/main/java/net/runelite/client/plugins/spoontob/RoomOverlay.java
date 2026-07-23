package net.runelite.client.plugins.spoontob;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Polygon;
import javax.annotation.Nonnull;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.NPC;
import net.runelite.api.NPCComposition;
import net.runelite.api.Perspective;
import net.runelite.api.Point;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayPriority;
import net.runelite.client.ui.overlay.OverlayUtil;

public abstract class RoomOverlay extends Overlay
{
	protected final SpoonTobConfig config;

	@Inject
	protected Client client;

	@Inject
	protected RoomOverlay(SpoonTobConfig config)
	{
		this.config = config;
		setPosition(OverlayPosition.DYNAMIC);
		setPriority(OverlayPriority.HIGH);
		setLayer(OverlayLayer.ABOVE_SCENE);
	}

	protected void drawTile(Graphics2D graphics, WorldPoint point, Color color, int strokeWidth, int outlineAlpha, int fillAlpha)
	{
		WorldPoint playerLocation = this.client.getLocalPlayer().getWorldLocation();
		if (point.distanceTo(playerLocation) >= 32)
		{
			return;
		}
		LocalPoint lp = LocalPoint.fromWorld(this.client, point);
		if (lp == null)
		{
			return;
		}
		Polygon poly = Perspective.getCanvasTilePoly(this.client, lp);
		if (poly == null)
		{
			return;
		}
		graphics.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), outlineAlpha));
		graphics.setStroke(new BasicStroke(strokeWidth));
		graphics.draw(poly);
		graphics.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), fillAlpha));
		graphics.fill(poly);
	}

	protected void drawTL(Graphics2D graphics, WorldPoint point, Color color, int strokeWidth, int outlineAlpha, int fillAlpha, NPC npc)
	{
		WorldPoint playerLocation = this.client.getLocalPlayer().getWorldLocation();
		if (point.distanceTo(playerLocation) >= 32)
		{
			return;
		}
		int size = 1;
		NPCComposition composition = npc.getTransformedComposition();
		if (composition != null)
		{
			size = composition.getSize();
		}
		LocalPoint lp = LocalPoint.fromWorld(this.client, point);
		if (lp == null)
		{
			return;
		}
		Polygon tilePoly = Perspective.getCanvasTileAreaPoly(this.client, lp, size);
		if (tilePoly == null)
		{
			return;
		}
		graphics.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), outlineAlpha));
		graphics.setStroke(new BasicStroke(strokeWidth));
		graphics.draw(tilePoly);
		graphics.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), fillAlpha));
		graphics.fill(tilePoly);
	}

	protected void renderNpcOverlay(Graphics2D graphics, NPC actor, Color color, int outlineWidth, int outlineAlpha, int fillAlpha)
	{
		int size = 1;
		NPCComposition composition = actor.getTransformedComposition();
		if (composition != null)
		{
			size = composition.getSize();
		}
		LocalPoint lp = actor.getLocalLocation();
		Polygon tilePoly = Perspective.getCanvasTileAreaPoly(this.client, lp, size);
		if (tilePoly == null)
		{
			return;
		}
		graphics.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), outlineAlpha));
		graphics.setStroke(new BasicStroke(outlineWidth));
		graphics.draw(tilePoly);
		graphics.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), fillAlpha));
		graphics.fill(tilePoly);
	}

	/** Like {@link #renderNpcOverlay}, but anchored to the NPC's logical world tile rather than its (possibly mid-walk-animation) true local location. */
	protected void renderNpcTLOverlay(Graphics2D graphics, NPC actor, Color color, int outlineWidth, int outlineAlpha, int fillAlpha)
	{
		int size = 1;
		NPCComposition composition = actor.getTransformedComposition();
		if (composition != null)
		{
			size = composition.getSize();
		}
		LocalPoint lp = LocalPoint.fromWorld(this.client, actor.getWorldLocation());
		if (lp == null)
		{
			return;
		}
		lp = new LocalPoint(lp.getX() + size * 128 / 2 - 64, lp.getY() + size * 128 / 2 - 64);
		Polygon tilePoly = Perspective.getCanvasTileAreaPoly(this.client, lp, size);
		if (tilePoly == null)
		{
			return;
		}
		graphics.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), outlineAlpha));
		graphics.setStroke(new BasicStroke(outlineWidth));
		graphics.draw(tilePoly);
		graphics.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), fillAlpha));
		graphics.fill(tilePoly);
	}

	protected void renderNpcPoly(Graphics2D graphics, Color color, Polygon polygon, double width, int alpha)
	{
		if (polygon == null)
		{
			return;
		}
		graphics.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha));
		graphics.setStroke(new BasicStroke((float) width));
		graphics.draw(polygon);
	}

	/**
	 * Canvas polygon for an NPC-sized tile area. When {@code centered} is true, anchors on the exact
	 * local point given (e.g. an NPC's true, mid-walk-animation location); when false, anchors on the
	 * point as the area's corner (e.g. a stationary NPC's logical world tile).
	 */
	public static Polygon getCanvasTileAreaPoly(@Nonnull Client client, @Nonnull LocalPoint localLocation, int size, int borderOffset)
	{
		return getCanvasTileAreaPoly(client, localLocation, size, borderOffset, true);
	}

	public static Polygon getCanvasTileAreaPoly(@Nonnull Client client, @Nonnull LocalPoint localLocation, int size, boolean centered)
	{
		return getCanvasTileAreaPoly(client, localLocation, size, 0, centered);
	}

	public static Polygon getCanvasTileAreaPoly(@Nonnull Client client, @Nonnull LocalPoint localLocation, int size, int borderOffset, boolean centered)
	{
		int plane = client.getPlane();
		int swX;
		int swY;
		int neX;
		int neY;
		if (centered)
		{
			swX = localLocation.getX() - size * (128 + borderOffset) / 2;
			swY = localLocation.getY() - size * (128 + borderOffset) / 2;
			neX = localLocation.getX() + size * (128 + borderOffset) / 2;
			neY = localLocation.getY() + size * (128 + borderOffset) / 2;
		}
		else
		{
			swX = localLocation.getX() - (128 + borderOffset) / 2;
			swY = localLocation.getY() - (128 + borderOffset) / 2;
			neX = localLocation.getX() - (128 + borderOffset) / 2 + size * (128 + borderOffset);
			neY = localLocation.getY() - (128 + borderOffset) / 2 + size * (128 + borderOffset);
		}

		int sceneX = localLocation.getSceneX();
		int sceneY = localLocation.getSceneY();
		if (sceneX < 0 || sceneY < 0 || sceneX >= 104 || sceneY >= 104)
		{
			return null;
		}

		byte[][][] tileSettings = client.getTileSettings();
		int tilePlane = plane;
		if (plane < 3 && (tileSettings[1][sceneX][sceneY] & 2) == 2)
		{
			tilePlane = plane + 1;
		}

		int swHeight = getHeight(client, swX, swY, tilePlane);
		int nwHeight = getHeight(client, neX, swY, tilePlane);
		int neHeight = getHeight(client, neX, neY, tilePlane);
		int seHeight = getHeight(client, swX, neY, tilePlane);
		Point p1 = Perspective.localToCanvas(client, swX, swY, swHeight);
		Point p2 = Perspective.localToCanvas(client, neX, swY, nwHeight);
		Point p3 = Perspective.localToCanvas(client, neX, neY, neHeight);
		Point p4 = Perspective.localToCanvas(client, swX, neY, seHeight);
		if (p1 == null || p2 == null || p3 == null || p4 == null)
		{
			return null;
		}
		Polygon poly = new Polygon();
		poly.addPoint(p1.getX(), p1.getY());
		poly.addPoint(p2.getX(), p2.getY());
		poly.addPoint(p3.getX(), p3.getY());
		poly.addPoint(p4.getX(), p4.getY());
		return poly;
	}

	private static int getHeight(@Nonnull Client client, int localX, int localY, int plane)
	{
		int sceneX = localX >> 7;
		int sceneY = localY >> 7;
		if (sceneX < 0 || sceneY < 0 || sceneX >= 104 || sceneY >= 104)
		{
			return 0;
		}
		int[][][] tileHeights = client.getTileHeights();
		int x = localX & 127;
		int y = localY & 127;
		int a = (x * tileHeights[plane][sceneX + 1][sceneY] + (128 - x) * tileHeights[plane][sceneX][sceneY]) >> 7;
		int b = (tileHeights[plane][sceneX][sceneY + 1] * (128 - x) + x * tileHeights[plane][sceneX + 1][sceneY + 1]) >> 7;
		return ((128 - y) * a + y * b) >> 7;
	}

	protected void renderTextLocation(Graphics2D graphics, String txtString, Color fontColor, Point canvasPoint)
	{
		if (canvasPoint == null)
		{
			return;
		}
		Point shadow = new Point(canvasPoint.getX() + 1, canvasPoint.getY() + 1);
		OverlayUtil.renderTextLocation(graphics, shadow, txtString, Color.BLACK);
		OverlayUtil.renderTextLocation(graphics, canvasPoint, txtString, fontColor);
	}

	protected void renderSteroidsTextLocation(Graphics2D graphics, String txtString, int fontSize, int fontStyle, Color fontColor, Point canvasPoint)
	{
		graphics.setFont(new Font(FontManager.getRunescapeSmallFont().getName(), fontStyle, fontSize));
		renderTextLocation(graphics, txtString, fontColor, canvasPoint);
	}

	protected void renderResizeTextLocation(Graphics2D graphics, String txtString, int fontSize, int fontStyle, Color fontColor, Point canvasPoint)
	{
		if (this.config.resizeFont())
		{
			graphics.setFont(this.config.fontType().deriveFont(this.config.fontWeight().getFont(), this.config.tobFontSize()));
		}
		else
		{
			graphics.setFont(new Font(FontManager.getRunescapeSmallFont().getName(), fontStyle, fontSize));
		}
		renderTextLocation(graphics, txtString, fontColor, canvasPoint);
	}

	protected void renderPoly(Graphics2D graphics, Color color, Polygon polygon)
	{
		renderPoly(graphics, color, polygon, 2.0);
	}

	protected void renderPoly(Graphics2D graphics, Color color, Polygon polygon, double width)
	{
		if (polygon == null)
		{
			return;
		}
		graphics.setColor(color);
		graphics.setStroke(new BasicStroke((float) width));
		graphics.draw(polygon);
	}
}
