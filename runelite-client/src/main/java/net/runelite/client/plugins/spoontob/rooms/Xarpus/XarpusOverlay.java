package net.runelite.client.plugins.spoontob.rooms.Xarpus;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.util.Collection;
import java.util.function.Function;
import javax.inject.Inject;
import net.runelite.api.GroundObject;
import net.runelite.api.NPC;
import net.runelite.api.Perspective;
import net.runelite.api.Point;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.spoontob.RoomOverlay;
import net.runelite.client.plugins.spoontob.SpoonTobConfig;
import org.apache.commons.lang3.tuple.Pair;

public class XarpusOverlay extends RoomOverlay
{
	@Inject
	private Xarpus xarpus;

	@Inject
	protected XarpusOverlay(SpoonTobConfig config)
	{
		super(config);
	}

	// Quadrant point sets are 8x8-tile boxes offset from Xarpus's tile toward the facing direction.
	private static final Function<WorldPoint, Point[]> NE_BOX = p -> new Point[]{new Point(p.getX(), p.getY()), new Point(p.getX(), p.getY() + 8), new Point(p.getX() + 8, p.getY() + 8), new Point(p.getX() + 8, p.getY())};
	private static final Function<WorldPoint, Point[]> NW_BOX = p -> new Point[]{new Point(p.getX() - 8, p.getY()), new Point(p.getX() - 8, p.getY() + 8), new Point(p.getX(), p.getY() + 8), new Point(p.getX(), p.getY())};
	private static final Function<WorldPoint, Point[]> SE_BOX = p -> new Point[]{new Point(p.getX(), p.getY() - 8), new Point(p.getX(), p.getY()), new Point(p.getX() + 8, p.getY()), new Point(p.getX() + 8, p.getY() - 8)};
	private static final Function<WorldPoint, Point[]> SW_BOX = p -> new Point[]{new Point(p.getX() - 8, p.getY() - 8), new Point(p.getX() - 8, p.getY()), new Point(p.getX(), p.getY()), new Point(p.getX(), p.getY() - 8)};
	private static final Function<WorldPoint, Point[]> NE_MELEE = p -> new Point[]{new Point(p.getX() + 4, p.getY() + 4), new Point(p.getX(), p.getY() + 4), new Point(p.getX(), p.getY() + 3), new Point(p.getX() + 3, p.getY() + 3), new Point(p.getX() + 3, p.getY()), new Point(p.getX() + 4, p.getY())};
	private static final Function<WorldPoint, Point[]> NW_MELEE = p -> new Point[]{new Point(p.getX() - 4, p.getY() + 4), new Point(p.getX() - 4, p.getY()), new Point(p.getX() - 3, p.getY()), new Point(p.getX() - 3, p.getY() + 3), new Point(p.getX(), p.getY() + 3), new Point(p.getX(), p.getY() + 4)};
	private static final Function<WorldPoint, Point[]> SE_MELEE = p -> new Point[]{new Point(p.getX() + 4, p.getY() - 4), new Point(p.getX() + 4, p.getY()), new Point(p.getX() + 3, p.getY()), new Point(p.getX() + 3, p.getY() - 3), new Point(p.getX(), p.getY() - 3), new Point(p.getX(), p.getY() - 4)};
	private static final Function<WorldPoint, Point[]> SW_MELEE = p -> new Point[]{new Point(p.getX() - 4, p.getY() - 4), new Point(p.getX(), p.getY() - 4), new Point(p.getX(), p.getY() - 3), new Point(p.getX() - 3, p.getY() - 3), new Point(p.getX() - 3, p.getY()), new Point(p.getX() - 4, p.getY())};

	public Dimension render(Graphics2D graphics)
	{
		if (!this.xarpus.isXarpusActive())
		{
			return null;
		}

		NPC boss = this.xarpus.getXarpusNPC();
		boolean p3SuppressedInHm = this.xarpus.isHM() && this.xarpus.isXarpusStare() && this.xarpus.isPostScreech();
		boolean showp2 = this.config.xarpusTicks() && Xarpus.P2_IDS.contains(boss.getId());
		boolean showp3 = this.config.xarpusTicks() && Xarpus.P3_IDS.contains(boss.getId()) && !p3SuppressedInHm;
		if (showp2 || showp3)
		{
			String ticksLeftStr = String.valueOf(this.xarpus.getXarpusTicksUntilAttack());
			Point canvasPoint = boss.getCanvasTextLocation(graphics, ticksLeftStr, 130);
			if (this.config.fontStyle())
			{
				renderTextLocation(graphics, ticksLeftStr, Color.WHITE, canvasPoint);
			}
			else
			{
				renderResizeTextLocation(graphics, ticksLeftStr, 14, 1, Color.WHITE, canvasPoint);
			}
		}

		if (Xarpus.P1_IDS.contains(boss.getId()) && !this.xarpus.getXarpusExhumeds().isEmpty())
		{
			renderExhumeds(graphics);
		}

		if (this.config.exhumedOnXarpus() && this.xarpus.isExhumedSpawned() && Xarpus.P1_IDS.contains(boss.getId()) && this.xarpus.getExhumedCounter() != null)
		{
			String xarpusText = (this.xarpus.getExhumedCounter().getCount() == 0) ? "NOW!" : String.valueOf(this.xarpus.getExhumedCounter().getCount());
			Point canvasPoint = boss.getCanvasTextLocation(graphics, xarpusText, 320);
			if (canvasPoint != null && !boss.isDead())
			{
				if (this.config.fontStyle())
				{
					renderTextLocation(graphics, xarpusText, Color.ORANGE, canvasPoint);
				}
				else
				{
					renderResizeTextLocation(graphics, xarpusText, 14, 1, Color.ORANGE, canvasPoint);
				}
			}
		}

		if (this.config.xarpusLos() != SpoonTobConfig.losMode.OFF)
		{
			renderLineOfSight(graphics);
		}

		return null;
	}

	private void renderExhumeds(Graphics2D graphics)
	{
		Collection<Pair<GroundObject, Integer>> exhumeds = this.xarpus.getXarpusExhumeds().values();
		int maxSafeTicks = this.xarpus.isHM() ? 6 : 8;
		int minSafeTicks = 1;
		for (Pair<GroundObject, Integer> p : exhumeds)
		{
			GroundObject o = p.getLeft();
			int ticks = p.getRight();
			String text = String.valueOf(ticks);
			boolean safe = ticks <= minSafeTicks || ticks >= maxSafeTicks;

			if (this.config.xarpusExhumed() == SpoonTobConfig.exhumedMode.TILE || this.config.xarpusExhumed() == SpoonTobConfig.exhumedMode.BOTH)
			{
				Polygon poly = o.getCanvasTilePoly();
				if (poly != null)
				{
					Color color = new Color(0, 255, 0, 130);
					if (this.config.exhumedStepOffWarning() == SpoonTobConfig.stepOffMode.TILE || this.config.exhumedStepOffWarning() == SpoonTobConfig.stepOffMode.BOTH)
					{
						color = safe ? new Color(0, 255, 0, 130) : new Color(255, 0, 0, 130);
					}
					graphics.setColor(color);
					graphics.setStroke(new BasicStroke(1.0F));
					graphics.draw(poly);
				}
			}

			if (this.config.xarpusExhumed() == SpoonTobConfig.exhumedMode.BOTH || this.config.xarpusExhumed() == SpoonTobConfig.exhumedMode.TICKS)
			{
				Point textLocation = o.getCanvasTextLocation(graphics, text, 0);
				if (textLocation != null)
				{
					Color color = Color.WHITE;
					if (this.config.exhumedStepOffWarning() == SpoonTobConfig.stepOffMode.TICKS || this.config.exhumedStepOffWarning() == SpoonTobConfig.stepOffMode.BOTH)
					{
						color = safe ? Color.GREEN : Color.RED;
					}
					if (this.config.fontStyle())
					{
						renderTextLocation(graphics, text, color, textLocation);
					}
					else
					{
						renderResizeTextLocation(graphics, text, 12, 1, color, textLocation);
					}
				}
			}
		}
	}

	/**
	 * Xarpus faces a direction after the screech (P2 stare) begins; that facing is the
	 * dangerous quadrant and rotates over time. Ported from theatre's working LOS
	 * implementation - spoontob's own Direction/LineOfSight classes were dead stubs never
	 * wired up to any rendering.
	 */
	private void renderLineOfSight(Graphics2D graphics)
	{
		NPC boss = this.xarpus.getXarpusNPC();
		if (boss == null || !Xarpus.P2_IDS.contains(boss.getId()) || boss.isDead() || !this.xarpus.isPostScreech())
		{
			return;
		}

		WorldPoint xarpusWp = WorldPoint.fromLocal(this.client, boss.getLocalLocation());
		Direction dir = Direction.getPreciseDirection(boss.getOrientation());
		boolean markMeleeTiles = this.config.xarpusLos() == SpoonTobConfig.losMode.MELEE;

		Point[] points;
		switch (dir)
		{
			case NORTHEAST:
				points = markMeleeTiles ? NE_MELEE.apply(xarpusWp) : NE_BOX.apply(xarpusWp);
				break;
			case NORTHWEST:
				points = markMeleeTiles ? NW_MELEE.apply(xarpusWp) : NW_BOX.apply(xarpusWp);
				break;
			case SOUTHEAST:
				points = markMeleeTiles ? SE_MELEE.apply(xarpusWp) : SE_BOX.apply(xarpusWp);
				break;
			case SOUTHWEST:
				points = markMeleeTiles ? SW_MELEE.apply(xarpusWp) : SW_BOX.apply(xarpusWp);
				break;
			default:
				// Xarpus only ever settles on a diagonal facing for this mechanic
				return;
		}

		Polygon poly = new Polygon();
		for (Point point : points)
		{
			Point canvasPoint = localToCanvas(dir, point.getX(), point.getY());
			if (canvasPoint != null)
			{
				poly.addPoint(canvasPoint.getX(), canvasPoint.getY());
			}
		}

		Color color = this.config.xarpusLosColor();
		graphics.setColor(color);
		graphics.setStroke(new BasicStroke(2.0F));
		graphics.draw(poly);
		graphics.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), this.config.xarpusLosFill()));
		graphics.fill(poly);
	}

	private Point localToCanvas(Direction dir, int worldX, int worldY)
	{
		LocalPoint lp = LocalPoint.fromWorld(this.client, worldX, worldY);
		if (lp == null)
		{
			return null;
		}
		int x = lp.getX();
		int y = lp.getY();
		int s = 64;
		switch (dir)
		{
			case NORTHEAST:
				return Perspective.localToCanvas(this.client, new LocalPoint(x - s, y - s), this.client.getPlane());
			case NORTHWEST:
				return Perspective.localToCanvas(this.client, new LocalPoint(x + s, y - s), this.client.getPlane());
			case SOUTHEAST:
				return Perspective.localToCanvas(this.client, new LocalPoint(x - s, y + s), this.client.getPlane());
			case SOUTHWEST:
				return Perspective.localToCanvas(this.client, new LocalPoint(x + s, y + s), this.client.getPlane());
			default:
				return null;
		}
	}
}
