package net.runelite.client.plugins.spoonnightmare;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.Shape;
import java.util.ArrayList;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.api.GraphicsObject;
import net.runelite.api.NPC;
import net.runelite.api.Perspective;
import net.runelite.api.Player;
import net.runelite.api.Point;
import net.runelite.api.Projectile;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.NpcID;
import net.runelite.api.gameval.SpotanimID;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayPriority;
import net.runelite.client.ui.overlay.OverlayUtil;
import net.runelite.client.ui.overlay.outline.ModelOutlineRenderer;

public class SpoonNightmareOverlay extends Overlay
{
	private final Client client;
	private final SpoonNightmarePlugin plugin;
	private final SpoonNightmareConfig config;
	private final ModelOutlineRenderer modelOutlineRenderer;

	@Inject
	private SpoonNightmareOverlay(Client client, SpoonNightmarePlugin plugin, SpoonNightmareConfig config, ModelOutlineRenderer modelOutlineRenderer)
	{
		this.client = client;
		this.plugin = plugin;
		this.config = config;
		this.modelOutlineRenderer = modelOutlineRenderer;
		setPosition(OverlayPosition.DYNAMIC);
		setPriority(OverlayPriority.HIGH);
		setLayer(OverlayLayer.ABOVE_SCENE);
	}

	public Dimension render(Graphics2D graphics)
	{
		if (this.config.nightmareHands() != SpoonNightmareConfig.handsMode.OFF && !this.plugin.getHandsLocation().isEmpty())
		{
			renderHands(graphics);
		}
		if (this.config.p3Runway() != SpoonNightmareConfig.runwayMode.OFF && this.plugin.isPreparedForTakeoff())
		{
			renderRunway(graphics);
		}
		if (this.plugin.isTotemsActive() && (this.config.totemHighlight() != SpoonNightmareConfig.totemHighlightMode.OFF || this.config.totemHP()))
		{
			renderTotems(graphics);
		}
		if (!this.plugin.getShrooms().isEmpty() && (this.config.highlightSpores() || this.config.sporesTickCounter()))
		{
			renderShrooms(graphics, this.plugin.getShrooms());
		}
		if (this.config.huskHighlight() && !this.plugin.getHusks().isEmpty())
		{
			highlightHusks(graphics);
		}
		if (this.config.huskTarget() && this.plugin.isActiveFight())
		{
			for (Projectile p : this.client.getProjectiles())
			{
				if (p.getId() != SpotanimID.NIGHTMARE_TRANCE_TRAVEL || !(p.getInteracting() instanceof Player))
				{
					continue;
				}
				Polygon poly = Perspective.getCanvasTilePoly(this.client, p.getInteracting().getLocalLocation());
				renderPoly(graphics, this.config.huskTargetColor(), poly, this.config.huskWidth(), this.config.huskOpacity());
			}
		}
		if (this.plugin.isFlowersActive() && this.config.lowFps())
		{
			for (LocalPoint lp : this.plugin.getFlowerTiles())
			{
				drawTile(graphics, WorldPoint.fromLocal(this.client, lp), Color.GREEN, 0, 0, 50);
			}
		}
		return null;
	}

	private void renderHands(Graphics2D graphics)
	{
		if (this.client.getLocalPlayer() == null)
		{
			return;
		}
		WorldPoint playerLp = WorldPoint.fromLocal(this.client, this.client.getLocalPlayer().getLocalLocation());
		ArrayList<GraphicsObject> hands = this.plugin.getHandsLocation();
		for (int i = 0; i < hands.size(); i++)
		{
			GraphicsObject obj = hands.get(i);
			WorldPoint handLp = WorldPoint.fromLocal(this.client, obj.getLocation());
			if (this.config.handsDistance() && playerLp.distanceTo(handLp) > this.config.handsDistanceLimit())
			{
				continue;
			}
			Color color = this.config.raveHands() ? this.plugin.getRaveHandsColors().get(i) : this.config.nightmareHandsColor();
			if (this.config.nightmareHands() == SpoonNightmareConfig.handsMode.TILE)
			{
				Polygon poly = Perspective.getCanvasTilePoly(this.client, obj.getLocation());
				if (poly != null)
				{
					graphics.setStroke(new BasicStroke(this.config.handsWidth()));
					graphics.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 255));
					graphics.draw(poly);
					graphics.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), this.config.handsOpacity()));
					graphics.fill(poly);
				}
			}
			else if (this.config.nightmareHands() == SpoonNightmareConfig.handsMode.OUTLINE)
			{
				this.modelOutlineRenderer.drawOutline(obj, this.config.handsWidth(), color, this.config.handsGlow());
			}
			if (!this.config.handsTicks())
			{
				continue;
			}
			Color tickColor = this.plugin.getHandsDelay() == 1 ? Color.RED : Color.WHITE;
			String text = Integer.toString(this.plugin.getHandsDelay());
			Point point = Perspective.getCanvasTextLocation(this.client, graphics, obj.getLocation(), text, 0);
			Font oldFont = graphics.getFont();
			graphics.setFont(new Font("Arial", 1, 12));
			if (point != null)
			{
				Point pointShadow = new Point(point.getX() + 1, point.getY() + 1);
				OverlayUtil.renderTextLocation(graphics, pointShadow, text, Color.BLACK);
				OverlayUtil.renderTextLocation(graphics, point, text, tickColor);
			}
			graphics.setFont(oldFont);
		}
	}

	private void renderRunway(Graphics2D graphics)
	{
		WorldPoint bossLoc = this.plugin.getBossLoc();
		if (bossLoc == null)
		{
			return;
		}
		int angle = this.plugin.getNightmareNpc().getOrientation();
		int round = angle >>> 9;
		if ((angle & 0x100) != 0)
		{
			round++;
		}
		int directionNum = round & 3;
		Color color = Color.WHITE;
		if (this.config.p3Runway() == SpoonNightmareConfig.runwayMode.COLOR)
		{
			color = this.config.p3RunwayColor();
		}
		else if (this.config.p3Runway() == SpoonNightmareConfig.runwayMode.RAVE)
		{
			color = this.plugin.getRaveRunway().get(0);
		}
		int index = 0;
		if (directionNum == 0)
		{
			for (int i = 0; i < 15; i++)
			{
				index = drawRunwayRow(graphics, bossLoc.getX(), bossLoc.getY() - 1 - i, 1, 0, index, color);
			}
		}
		else if (directionNum == 1)
		{
			for (int i = 0; i < 14; i++)
			{
				index = drawRunwayRow(graphics, bossLoc.getX() - 1 - i, bossLoc.getY(), 0, -1, index, color);
			}
		}
		else if (directionNum == 2)
		{
			for (int i = 0; i < 15; i++)
			{
				index = drawRunwayRow(graphics, bossLoc.getX(), bossLoc.getY() + 5 + i, 1, 0, index, color);
			}
		}
		else
		{
			for (int i = 0; i < 14; i++)
			{
				index = drawRunwayRow(graphics, bossLoc.getX() + 5 + i, bossLoc.getY(), 0, -1, index, color);
			}
		}
	}

	private int drawRunwayRow(Graphics2D graphics, int baseX, int baseY, int dx, int dy, int index, Color color)
	{
		WorldPoint[] tiles = new WorldPoint[5];
		for (int j = 0; j < 5; j++)
		{
			tiles[j] = new WorldPoint(baseX + dx * j, baseY + dy * j, this.client.getPlane());
		}
		if (this.config.p3Runway() == SpoonNightmareConfig.runwayMode.RAVEST)
		{
			for (WorldPoint tile : tiles)
			{
				drawTile(graphics, tile, this.plugin.getRaveRunway().get(index++), 0, 0, 100);
			}
		}
		else
		{
			for (WorldPoint tile : tiles)
			{
				drawTile(graphics, tile, color, 0, 0, 100);
			}
			index += 5;
		}
		return index;
	}

	private void renderTotems(Graphics2D graphics)
	{
		ArrayList<TotemInfo> totems = this.plugin.getTotemList();
		for (int i = 0; i < totems.size(); i++)
		{
			TotemInfo ti = totems.get(i);
			if (this.config.totemHighlight() != SpoonNightmareConfig.totemHighlightMode.OFF)
			{
				Color totemColor = this.config.totemHighlightColor();
				if (this.config.totemColorMode() == SpoonNightmareConfig.totemColorMode.RAVE)
				{
					totemColor = this.plugin.getRaveTotemColors().get(0);
				}
				else if (this.config.totemColorMode() == SpoonNightmareConfig.totemColorMode.RAVEST)
				{
					totemColor = this.plugin.getRaveTotemColors().get(i);
				}
				if (this.config.totemHighlight() == SpoonNightmareConfig.totemHighlightMode.AREA)
				{
					Shape poly = ti.getNpc().getConvexHull();
					if (poly != null)
					{
						graphics.setColor(new Color(totemColor.getRed(), totemColor.getGreen(), totemColor.getBlue(), 50));
						graphics.fill(poly);
					}
				}
				else
				{
					this.modelOutlineRenderer.drawOutline(ti.getNpc(), this.config.totemWidth(), totemColor, this.config.totemGlow());
				}
			}
			if (!this.config.totemHP())
			{
				continue;
			}
			LocalPoint lp = ti.getNpc().getLocalLocation();
			int scale = this.client.getPlayers().size();
			int id = this.plugin.getNightmareNpc().getId();
			boolean phosaniVariant = (id >= NpcID.NIGHTMARE_CHALLENGE_PHASE_01 && id <= NpcID.NIGHTMARE_CHALLENGE_DYING)
				|| (id >= NpcID.NIGHTMARE_CHALLENGE_PHASE_04 && id <= NpcID.NIGHTMARE_CHALLENGE_WEAK_PHASE_04);
			int totemHealth = phosaniVariant ? 200 : (scale >= 6 ? scale * 30 : 300);
			int healthRatio = ti.getNpc().getHealthRatio();
			double healthRatioDec;
			if (healthRatio > 0)
			{
				healthRatioDec = healthRatio / 100.0;
				ti.setRatio(healthRatio);
			}
			else
			{
				healthRatioDec = ti.getRatio() > 0 ? ti.getRatio() / 100.0 : 0.0;
			}
			double currentHealth = totemHealth - totemHealth * healthRatioDec;
			Color textColor = Color.GREEN;
			if (currentHealth <= 98.0)
			{
				textColor = Color.RED;
			}
			else if (currentHealth < totemHealth)
			{
				textColor = Color.ORANGE;
			}
			String healthStr = Integer.toString((int) currentHealth);
			Point point = Perspective.getCanvasTextLocation(this.client, graphics, lp, healthStr, 0);
			Font oldFont = graphics.getFont();
			graphics.setFont(new Font("Arial", 1, this.config.totemHPSize()));
			if (point != null)
			{
				Point pointShadow = new Point(point.getX() + 1, point.getY() + 1);
				OverlayUtil.renderTextLocation(graphics, pointShadow, healthStr, Color.BLACK);
				OverlayUtil.renderTextLocation(graphics, point, healthStr, textColor);
			}
			graphics.setFont(oldFont);
		}
	}

	protected void drawTile(Graphics2D graphics, WorldPoint point, Color color, int strokeWidth, int outlineAlpha, int fillAlpha)
	{
		if (this.client.getLocalPlayer() == null)
		{
			return;
		}
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

	private void renderShrooms(Graphics2D graphics, ArrayList<GameObject> shrooms)
	{
		for (GameObject obj : shrooms)
		{
			if (this.config.highlightSpores())
			{
				Polygon poly = Perspective.getCanvasTileAreaPoly(this.client, obj.getLocalLocation(), 3);
				if (poly != null)
				{
					graphics.setStroke(new BasicStroke(this.config.sporeWidth()));
					graphics.setColor(this.config.sporeBorderColor());
					graphics.draw(poly);
					Color c = this.config.sporeBorderColor();
					graphics.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), this.config.sporeOpacity()));
					graphics.fill(poly);
				}
			}
			if (!this.config.sporesTickCounter())
			{
				continue;
			}
			String ticks = Integer.toString(this.plugin.getMushroomTicks());
			Point point = Perspective.getCanvasTextLocation(this.client, graphics, obj.getLocalLocation(), ticks, 0);
			if (point == null)
			{
				continue;
			}
			Font oldFont = graphics.getFont();
			graphics.setFont(new Font("Arial", 1, 12));
			Point pointShadow = new Point(point.getX() + 1, point.getY() + 1);
			OverlayUtil.renderTextLocation(graphics, pointShadow, ticks, Color.BLACK);
			OverlayUtil.renderTextLocation(graphics, point, ticks, this.config.sporesTickColor());
			graphics.setFont(oldFont);
		}
	}

	private void highlightHusks(Graphics2D graphics)
	{
		for (NPC npc : this.plugin.getHusks())
		{
			int id = npc.getId();
			Color color = (id == NpcID.NIGHTMARE_HUSK_RANGED || id == NpcID.NIGHTMARE_CHALLENGE_HUSK_RANGED) ? Color.GREEN : Color.BLUE;
			Polygon poly = Perspective.getCanvasTilePoly(this.client, npc.getLocalLocation());
			renderPoly(graphics, color, poly, this.config.huskWidth(), this.config.huskOpacity());
		}
	}

	private static void renderPoly(Graphics2D graphics, Color color, Shape polygon, int stroke, int opacity)
	{
		if (polygon == null)
		{
			return;
		}
		graphics.setColor(color);
		graphics.setStroke(new BasicStroke(stroke));
		graphics.draw(polygon);
		graphics.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), opacity));
		graphics.fill(polygon);
	}
}
