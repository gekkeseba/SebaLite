package net.runelite.client.plugins.spoontob.rooms.Verzik;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.Shape;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;
import net.runelite.api.Actor;
import net.runelite.api.GameObject;
import net.runelite.api.GraphicsObject;
import net.runelite.api.NPC;
import net.runelite.api.NPCComposition;
import net.runelite.api.Perspective;
import net.runelite.api.Player;
import net.runelite.api.Point;
import net.runelite.api.Projectile;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.SpotanimID;
import net.runelite.client.plugins.spoontob.RoomOverlay;
import net.runelite.client.plugins.spoontob.SpoonTobConfig;

public class VerzikOverlay extends RoomOverlay
{
	@Inject
	private Verzik verzik;

	@Inject
	protected VerzikOverlay(SpoonTobConfig config)
	{
		super(config);
	}

	public Dimension render(Graphics2D graphics)
	{
		if (!this.verzik.isVerzikActive() || this.verzik.getVerzikNPC() == null)
		{
			return null;
		}

		int id = this.verzik.getVerzikNPC().getId();
		if (Verzik.VERZIK_ACTIVE_IDS.contains(id))
		{
			if (this.config.displayGreenBall() != SpoonTobConfig.greenBallMode.OFF || this.config.displayGreenBallTicks())
			{
				displayProjectiles(graphics);
			}
			if (this.config.purpleAoe())
			{
				displayPurpleCrabAOE(graphics);
			}
		}

		renderVerzikMeleeTile(graphics);
		renderTankTarget(graphics);

		if (this.config.showVerzikRangeAttack())
		{
			renderRangeProjectiles(graphics);
		}
		if ((this.config.showVerzikYellows() == SpoonTobConfig.verzikYellowsMode.YELLOW || (this.config.showVerzikYellows() == SpoonTobConfig.verzikYellowsMode.GROUPS && this.verzik.yellowGroups.isEmpty())) && this.verzik.yellowsOut)
		{
			renderYellows(graphics);
		}
		if (this.config.showVerzikRocks() && this.verzik.verzikPhase == Verzik.Phase.PHASE1)
		{
			renderRocks(graphics);
		}
		if (this.config.showVerzikAcid() && this.verzik.verzikPhase == Verzik.Phase.PHASE2 && this.client.getLocalPlayer() != null)
		{
			renderAcidSpots(graphics);
		}

		this.verzik.getVerzikAggros().forEach(k -> renderAggro(graphics, k));

		if (this.config.showVerzikTicks() || this.config.showVerzikAttacks() != SpoonTobConfig.verzikAttacksMode.OFF || this.config.showVerzikTotalTicks())
		{
			renderAttackTicks(graphics);
		}
		if (this.config.lightningInfobox() == SpoonTobConfig.lightningMode.OVERLAY || this.config.lightningInfobox() == SpoonTobConfig.lightningMode.BOTH)
		{
			renderLightningOverlay(graphics);
		}
		if (this.config.lightningAttackTick() && this.verzik.verzikPhase == Verzik.Phase.PHASE2)
		{
			renderLightningAttackTick(graphics);
		}

		return null;
	}

	private void renderVerzikMeleeTile(Graphics2D graphics)
	{
		if (this.config.verzikMelee() == SpoonTobConfig.meleeTileMode.OFF)
		{
			return;
		}
		int size = 1;
		NPCComposition composition = this.verzik.getVerzikNPC().getTransformedComposition();
		if (composition != null)
		{
			size = composition.getSize();
		}
		LocalPoint lp = LocalPoint.fromWorld(this.client, this.verzik.getVerzikNPC().getWorldLocation());
		if (lp == null)
		{
			return;
		}
		Polygon tilePoly = getCanvasTileAreaPoly(this.client, lp, size, false);
		if (tilePoly == null || this.verzik.verzikPhase != Verzik.Phase.PHASE3)
		{
			return;
		}
		if (this.config.verzikMelee() == SpoonTobConfig.meleeTileMode.TANK_NOTIFIER)
		{
			Color color = (this.verzik.getVerzikNPC().getInteracting() == this.client.getLocalPlayer()) ? this.config.p3AggroColor() : this.config.verzikMeleeColor();
			renderPoly(graphics, color, tilePoly);
		}
		else
		{
			renderPoly(graphics, this.config.verzikMeleeColor(), tilePoly);
		}
	}

	private void renderTankTarget(Graphics2D graphics)
	{
		if (!this.config.verzikTankTarget() || this.verzik.verzikPhase != Verzik.Phase.PHASE3 || this.verzik.getVerzikNPC().getInteracting() == null)
		{
			return;
		}
		Actor actor = this.verzik.getVerzikNPC().getInteracting();
		Polygon tilePoly = getCanvasTileAreaPoly(this.client, actor.getLocalLocation(), 1, false);
		if (tilePoly != null)
		{
			renderPoly(graphics, this.config.p3AggroColor(), tilePoly);
		}
	}

	private void renderRangeProjectiles(Graphics2D graphics)
	{
		for (WorldPoint p : this.verzik.verzikRangeProjectiles.values())
		{
			LocalPoint point = LocalPoint.fromWorld(this.client, p);
			if (point == null)
			{
				continue;
			}
			Polygon poly = Perspective.getCanvasTilePoly(this.client, point);
			if (poly == null)
			{
				continue;
			}
			Color color = this.config.verzikRangeAttacksColor();
			graphics.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha()));
			graphics.drawPolygon(poly);
			graphics.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), this.config.verzikRangeAttacksFill()));
			graphics.fillPolygon(poly);
		}
	}

	private void renderYellows(Graphics2D graphics)
	{
		String text = Integer.toString(this.verzik.yellowTimer);
		if (this.config.yellowTicksOnPlayer() && this.client.getLocalPlayer() != null)
		{
			Point point = Perspective.getCanvasTextLocation(this.client, graphics, this.client.getLocalPlayer().getLocalLocation(), text, this.config.yellowsOffset());
			if (this.config.fontStyle())
			{
				renderTextLocation(graphics, text, Color.WHITE, point);
			}
			else
			{
				renderSteroidsTextLocation(graphics, text, this.config.yellowsSize(), 1, Color.WHITE, point);
			}
		}
		for (WorldPoint wp : this.verzik.yellowsList)
		{
			drawTile(graphics, wp, Color.YELLOW, 2, 255, 0);
			if (this.config.yellowTicksOnPlayer())
			{
				continue;
			}
			LocalPoint lp = LocalPoint.fromWorld(this.client, wp);
			if (lp == null)
			{
				continue;
			}
			Point point = Perspective.getCanvasTextLocation(this.client, graphics, lp, text, 0);
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

	private void renderRocks(Graphics2D graphics)
	{
		for (GraphicsObject object : this.client.getGraphicsObjects())
		{
			if (object.getId() == SpotanimID.GARGBOSS_EXPLODING_PROJECTILE)
			{
				drawTile(graphics, WorldPoint.fromLocal(this.client, object.getLocation()), this.config.showVerzikRocksColor(), 2, 255, 50);
			}
		}
	}

	private void renderAcidSpots(Graphics2D graphics)
	{
		int index = 0;
		for (GameObject object : this.verzik.acidSpots)
		{
			if (this.client.getLocalPlayer().getWorldLocation().distanceTo(object.getWorldLocation()) <= this.config.showVerzikAcidDistance())
			{
				LocalPoint lp = object.getLocalLocation();
				String text = String.valueOf(this.verzik.acidSpotsTimer.get(index));
				Point point = Perspective.getCanvasTextLocation(this.client, graphics, lp, text, 0);
				if (this.config.fontStyle())
				{
					renderTextLocation(graphics, text, Color.WHITE, point);
				}
				else
				{
					renderSteroidsTextLocation(graphics, text, 12, 1, Color.WHITE, point);
				}
				drawTile(graphics, WorldPoint.fromLocal(this.client, lp), this.config.showVerzikAcidColor(), 2, 255, 0);
			}
			index++;
		}
	}

	private void renderAggro(Graphics2D graphics, NPC k)
	{
		if (this.config.verzikNyloAggroWarning() && k.getInteracting() != null && !k.isDead())
		{
			String targetText = k.getInteracting().getName() != null ? k.getInteracting().getName() : "";
			Point textLocation = k.getCanvasTextLocation(graphics, targetText, 80);
			if (textLocation != null)
			{
				Color color = k.getInteracting().equals(this.client.getLocalPlayer()) ? Color.RED : Color.GREEN;
				if (this.config.fontStyle())
				{
					renderTextLocation(graphics, targetText, color, textLocation);
				}
				else
				{
					renderResizeTextLocation(graphics, targetText, 14, 1, color, textLocation);
				}
			}
		}
		boolean showExplodeRange = this.config.verzikNyloExplodeRange() == SpoonTobConfig.VerzikNyloSetting.ALL_CRABS
			|| (this.config.verzikNyloExplodeRange() == SpoonTobConfig.VerzikNyloSetting.MY_CRABS && this.client.getLocalPlayer() != null && this.client.getLocalPlayer().equals(k.getInteracting()));
		if (!showExplodeRange)
		{
			return;
		}
		int thickSize = 1;
		int size = 1;
		NPCComposition composition = k.getTransformedComposition();
		if (composition != null)
		{
			size = composition.getSize() + 2 * thickSize;
		}
		LocalPoint lp = LocalPoint.fromWorld(this.client, k.getWorldLocation());
		if (lp == null)
		{
			return;
		}
		lp = new LocalPoint(lp.getX() - thickSize * 128, lp.getY() - thickSize * 128);
		Polygon tilePoly = getCanvasTileAreaPoly(this.client, lp, size, false);
		if (tilePoly != null)
		{
			renderPoly(graphics, this.config.verzikNyloExplodeTileColor(), tilePoly);
		}
	}

	private void renderAttackTicks(Graphics2D graphics)
	{
		StringBuilder tickText = new StringBuilder();
		boolean showAttackCount = (this.config.showVerzikAttacks() == SpoonTobConfig.verzikAttacksMode.ALL && this.verzik.getVerzikSpecial() != Verzik.SpecialAttack.WEBS)
			|| (this.config.showVerzikAttacks() == SpoonTobConfig.verzikAttacksMode.P2 && this.verzik.verzikPhase == Verzik.Phase.PHASE2)
			|| (this.config.showVerzikAttacks() == SpoonTobConfig.verzikAttacksMode.REDS && this.verzik.verzikRedPhase);
		if (showAttackCount)
		{
			tickText.append("[A] ").append(this.verzik.getVerzikAttackCount());
			if (this.config.showVerzikTicks() || this.config.showVerzikTotalTicks())
			{
				tickText.append(" : ");
			}
		}
		boolean showTicks = this.config.showVerzikTicks() && this.verzik.getVerzikSpecial() != Verzik.SpecialAttack.WEBS
			&& (this.verzik.verzikPhase == Verzik.Phase.PHASE1 || this.verzik.verzikPhase == Verzik.Phase.PHASE2 || this.verzik.verzikPhase == Verzik.Phase.PHASE3);
		if (showTicks)
		{
			tickText.append(this.verzik.getVerzikTicksUntilAttack());
			if (this.config.showVerzikTotalTicks())
			{
				tickText.append(" : ");
			}
		}
		if (this.config.showVerzikTotalTicks())
		{
			tickText.append("(").append(this.verzik.getVerzikTotalTicksUntilAttack()).append(")");
		}
		Point canvasPoint = this.verzik.getVerzikNPC().getCanvasTextLocation(graphics, tickText.toString(), 60);
		if (canvasPoint == null)
		{
			return;
		}
		Color col = this.verzik.verzikSpecialWarningColor();
		if (this.config.fontStyle())
		{
			renderTextLocation(graphics, tickText.toString(), col, canvasPoint);
		}
		else
		{
			renderResizeTextLocation(graphics, tickText.toString(), 15, 1, col, canvasPoint);
		}
	}

	private void renderLightningOverlay(Graphics2D graphics)
	{
		if (this.verzik.verzikPhase != Verzik.Phase.PHASE2)
		{
			return;
		}
		String zapText = (this.verzik.lightningAttacks > 0) ? Integer.toString(this.verzik.lightningAttacks) : "ZAP";
		Point canvasPoint = this.verzik.getVerzikNPC().getCanvasTextLocation(graphics, zapText, 270);
		if (canvasPoint != null && !this.verzik.getVerzikNPC().isDead())
		{
			if (this.config.fontStyle())
			{
				renderTextLocation(graphics, zapText, Color.ORANGE, canvasPoint);
			}
			else
			{
				renderResizeTextLocation(graphics, zapText, 15, 1, Color.ORANGE, canvasPoint);
			}
		}
	}

	private void renderLightningAttackTick(Graphics2D graphics)
	{
		Player localPlayer = this.client.getLocalPlayer();
		if (localPlayer == null)
		{
			return;
		}
		for (java.util.Map.Entry<Projectile, Integer> entry : this.verzik.getVerzikLightningProjectiles().entrySet())
		{
			if (entry.getKey().getInteracting() != localPlayer)
			{
				continue;
			}
			int ticks = entry.getValue();
			String tickstring = String.valueOf(ticks);
			Point point = Perspective.getCanvasTextLocation(this.client, graphics, localPlayer.getLocalLocation(), tickstring, this.config.zapOffset());
			if (point == null)
			{
				continue;
			}
			Color color = (ticks > 0) ? Color.WHITE : Color.ORANGE;
			if (this.config.fontStyle())
			{
				renderTextLocation(graphics, tickstring, color, point);
			}
			else
			{
				renderSteroidsTextLocation(graphics, tickstring, this.config.zapSize(), 1, color, point);
			}
		}
	}

	private void displayProjectiles(Graphics2D graphics)
	{
		for (Projectile p : this.client.getProjectiles())
		{
			Actor interacting = p.getInteracting();
			if (p.getId() != SpotanimID.VERZIK_ACIDBOMB_PROJANIM || interacting == null)
			{
				continue;
			}
			if (this.config.displayGreenBall() != SpoonTobConfig.greenBallMode.OFF)
			{
				int size = (this.config.displayGreenBall() == SpoonTobConfig.greenBallMode.TILE) ? 1 : 3;
				Polygon tilePoly = Perspective.getCanvasTileAreaPoly(this.client, interacting.getLocalLocation(), size);
				renderPolygon(graphics, tilePoly, Color.GREEN);
			}
			if (this.config.displayGreenBallTicks())
			{
				String text = String.valueOf(p.getRemainingCycles() / 30);
				LocalPoint lp = interacting.getLocalLocation();
				Point point = Perspective.getCanvasTextLocation(this.client, graphics, lp, text, this.config.greenBallOffset());
				if (this.config.fontStyle())
				{
					renderTextLocation(graphics, text, Color.RED, point);
				}
				else
				{
					renderSteroidsTextLocation(graphics, text, this.config.greenBallSize(), 1, Color.RED, point);
				}
			}
		}
	}

	private void displayPurpleCrabAOE(Graphics2D graphics)
	{
		if (!Verzik.P2_IDS.contains(this.verzik.getVerzikNPC().getId()) || this.verzik.getPurpleCrabLandingPoint().isEmpty())
		{
			return;
		}
		this.verzik.getPurpleCrabLandingPoint().forEach((projectile, landingWp) -> {
			LocalPoint lp = LocalPoint.fromWorld(this.client, landingWp);
			if (lp == null)
			{
				return;
			}
			String ticks = String.valueOf(projectile.getRemainingCycles() / 30);
			Point textLocation = Perspective.getCanvasTextLocation(this.client, graphics, lp, "#", 0);
			if (this.config.fontStyle())
			{
				renderTextLocation(graphics, ticks, Color.WHITE, textLocation);
			}
			else
			{
				renderSteroidsTextLocation(graphics, ticks, 13, 1, Color.WHITE, textLocation);
			}
			Polygon tileAreaPoly = Perspective.getCanvasTileAreaPoly(this.client, lp, 3);
			renderPolygon(graphics, tileAreaPoly, new Color(106, 61, 255));
		});
	}

	protected void renderPolygon(Graphics2D graphics, @Nullable Shape polygon, @Nonnull Color color)
	{
		if (polygon != null)
		{
			graphics.setColor(color);
			graphics.setStroke(new BasicStroke(2.0F));
			graphics.draw(polygon);
			graphics.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 0));
			graphics.fill(polygon);
		}
	}
}
