package net.runelite.client.plugins.spooncoxadditions.overlays;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.Shape;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.api.NPC;
import net.runelite.api.Perspective;
import net.runelite.api.Player;
import net.runelite.api.Point;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.NpcID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.client.plugins.spooncoxadditions.SpoonCoxAdditionsConfig;
import net.runelite.client.plugins.spooncoxadditions.SpoonCoxAdditionsPlugin;
import net.runelite.client.plugins.spooncoxadditions.utils.ShamanInfo;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayPriority;
import net.runelite.client.ui.overlay.OverlayUtil;
import net.runelite.client.ui.overlay.outline.ModelOutlineRenderer;

@Singleton
public class CoxAdditionsOverlay extends Overlay
{
	private final Client client;
	private final SpoonCoxAdditionsPlugin plugin;
	private final SpoonCoxAdditionsConfig config;
	private final ModelOutlineRenderer modelOutlineRenderer;

	@Inject
	private CoxAdditionsOverlay(Client client, SpoonCoxAdditionsPlugin plugin, SpoonCoxAdditionsConfig config, ModelOutlineRenderer modelOutlineRenderer)
	{
		this.client = client;
		this.plugin = plugin;
		this.config = config;
		this.modelOutlineRenderer = modelOutlineRenderer;
		setPosition(OverlayPosition.DYNAMIC);
		setPriority(OverlayPriority.HIGH);
		setLayer(OverlayLayer.ABOVE_SCENE);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (this.client.getVarbitValue(VarbitID.RAIDS_CLIENT_INDUNGEON) != 1)
		{
			return null;
		}

		if (this.config.olmCrippleTimer() && this.plugin.handCripple && this.plugin.meleeHand != null)
		{
			renderCenteredText(graphics, this.plugin.meleeHand.getCanvasTextLocation(graphics, Integer.toString(this.plugin.crippleTimer), 50),
				Integer.toString(this.plugin.crippleTimer), this.config.olmCrippleTextSize(), this.config.olmCrippleText());
		}

		if (this.config.crystalMark() != SpoonCoxAdditionsConfig.crystalMarkMode.OFF && this.plugin.crystalMarkActive)
		{
			renderCrystalMark(graphics);
		}

		if (this.config.shamanSlam() && !this.plugin.shamanInfoList.isEmpty())
		{
			for (ShamanInfo sInfo : this.plugin.shamanInfoList)
			{
				if (sInfo.jumping && sInfo.interactingLoc != null)
				{
					Polygon tilePoly = Perspective.getCanvasTileAreaPoly(this.client, sInfo.interactingLoc, 3);
					renderArea(graphics, this.config.shamanSlamColor(), tilePoly);
				}
			}
		}

		if (this.config.vasaCrystalTimer() != SpoonCoxAdditionsConfig.crystalTimerMode.OFF && this.plugin.vasaCrystalTicks > 0)
		{
			for (NPC npc : this.client.getNpcs())
			{
				if (npc.getId() != NpcID.RAIDS_VASANISTIRIO_CRYSTAL)
				{
					continue;
				}
				switch (this.config.vasaCrystalTimer())
				{
					case BOLD:
						graphics.setFont(FontManager.getRunescapeBoldFont());
						break;
					case REGULAR:
						graphics.setFont(FontManager.getRunescapeFont());
						break;
					case SMALL:
						graphics.setFont(FontManager.getRunescapeSmallFont());
						break;
					case CUSTOM:
						graphics.setFont(new Font("Arial", Font.BOLD, this.config.vasaCrystalTextSize()));
						break;
					default:
						break;
				}
				String str = this.plugin.vasaAtCrystal ? ("*" + this.plugin.vasaCrystalTicks) : Integer.toString(this.plugin.vasaCrystalTicks);
				Point p = npc.getCanvasTextLocation(graphics, str, npc.getLogicalHeight() / 2);
				if (p == null)
				{
					continue;
				}
				renderCenteredText(graphics, p, str, this.config.vasaCrystalTimerColor());
			}
		}

		if (this.config.chinRope() != SpoonCoxAdditionsConfig.chinRopeMode.OFF && !this.plugin.ropeNpcs.isEmpty())
		{
			drawRopeChin(graphics);
		}

		if (this.config.meatTreeChopCycle() == SpoonCoxAdditionsConfig.meatTreeChopCycleMode.OVERLAY
			&& this.plugin.startedChopping && this.plugin.meatTreeAlive && this.plugin.meatTree != null)
		{
			String text = String.valueOf(this.plugin.ticksToChop);
			renderCenteredText(graphics, this.plugin.meatTree.getCanvasTextLocation(graphics, text, 0), text, 15, Color.WHITE);
		}

		if (this.config.ropeCross() != SpoonCoxAdditionsConfig.ropeCrossMode.OFF && this.plugin.rapidHealActive)
		{
			renderRopeCross(graphics);
		}

		return null;
	}

	private void renderCrystalMark(Graphics2D graphics)
	{
		Player localPlayer = this.client.getLocalPlayer();
		if (localPlayer == null)
		{
			return;
		}

		SpoonCoxAdditionsConfig.crystalMarkMode mode = this.config.crystalMark();

		if (mode == SpoonCoxAdditionsConfig.crystalMarkMode.AREA || mode == SpoonCoxAdditionsConfig.crystalMarkMode.BOTH)
		{
			Polygon area = Perspective.getCanvasTileAreaPoly(this.client, localPlayer.getLocalLocation(), 3);
			renderArea(graphics, this.config.crystalMarkColor(), area);
		}

		if (mode == SpoonCoxAdditionsConfig.crystalMarkMode.TEXT || mode == SpoonCoxAdditionsConfig.crystalMarkMode.BOTH)
		{
			String text = Integer.toString(this.plugin.crystalMarkTicks);
			renderCenteredText(graphics, localPlayer.getCanvasTextLocation(graphics, text, 0), text, 20, this.config.crystalMarkColor());
		}
	}

	private void renderArea(Graphics2D graphics, Color color, Shape polygon)
	{
		if (polygon != null)
		{
			graphics.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 100));
			graphics.fill(polygon);
		}
	}

	public void drawRopeChin(Graphics2D graphics)
	{
		for (NPC npc : this.plugin.ropeNpcs)
		{
			WorldPoint wp = npc.getWorldLocation();
			boolean adjacentToAnother = false;
			for (NPC target : this.plugin.ropeNpcs)
			{
				if (target == npc)
				{
					continue;
				}
				WorldPoint tWp = target.getWorldLocation();
				if (Math.abs(tWp.getX() - wp.getX()) <= 1 && Math.abs(tWp.getY() - wp.getY()) <= 1)
				{
					adjacentToAnother = true;
					break;
				}
			}
			if (!adjacentToAnother)
			{
				continue;
			}

			if (this.config.chinRope() == SpoonCoxAdditionsConfig.chinRopeMode.HULL)
			{
				Shape poly = npc.getConvexHull();
				if (poly == null)
				{
					continue;
				}
				graphics.setColor(this.config.chinRopeColor());
				graphics.setStroke(new BasicStroke(this.config.chinRopeThiCC()));
				graphics.draw(poly);
			}
			else if (this.config.chinRope() == SpoonCoxAdditionsConfig.chinRopeMode.OUTLINE)
			{
				this.modelOutlineRenderer.drawOutline(npc, this.config.chinRopeThiCC(), this.config.chinRopeColor(), 2);
			}
		}
	}

	private void renderRopeCross(Graphics2D graphics)
	{
		Color ropeColor;
		if (this.plugin.ticksSinceHPRegen > 48 || this.plugin.ticksSinceHPRegen < 41)
		{
			ropeColor = Color.RED;
		}
		else if (this.plugin.ticksSinceHPRegen <= 43)
		{
			ropeColor = Color.GREEN;
		}
		else
		{
			ropeColor = Color.ORANGE;
		}

		SpoonCoxAdditionsConfig.ropeCrossMode mode = this.config.ropeCross();
		Player localPlayer = this.client.getLocalPlayer();

		if (mode == SpoonCoxAdditionsConfig.ropeCrossMode.HIGHLIGHT || mode == SpoonCoxAdditionsConfig.ropeCrossMode.BOTH)
		{
			for (net.runelite.api.GroundObject obj : this.plugin.rope)
			{
				if (localPlayer != null && localPlayer.getLocalLocation().distanceTo(obj.getLocalLocation()) <= 2400)
				{
					Shape hull = obj.getConvexHull();
					if (hull == null)
					{
						continue;
					}
					graphics.setColor(ropeColor);
					graphics.setStroke(new BasicStroke(1.0f));
					graphics.draw(hull);
				}
			}
		}

		if ((mode == SpoonCoxAdditionsConfig.ropeCrossMode.TICKS || mode == SpoonCoxAdditionsConfig.ropeCrossMode.BOTH) && localPlayer != null)
		{
			String text = this.config.ropeTicksDown()
				? String.valueOf(50 - this.plugin.ticksSinceHPRegen)
				: String.valueOf(this.plugin.ticksSinceHPRegen);

			SpoonCoxAdditionsConfig.ropeCrossTicksMode ticksMode = this.config.ropeCrossTicks();
			if (ticksMode == SpoonCoxAdditionsConfig.ropeCrossTicksMode.PLAYER || ticksMode == SpoonCoxAdditionsConfig.ropeCrossTicksMode.BOTH)
			{
				renderCenteredText(graphics, localPlayer.getCanvasTextLocation(graphics, text, 0), text, ropeColor);
			}
			if (ticksMode == SpoonCoxAdditionsConfig.ropeCrossTicksMode.ROPE || ticksMode == SpoonCoxAdditionsConfig.ropeCrossTicksMode.BOTH)
			{
				for (net.runelite.api.GroundObject obj : this.plugin.rope)
				{
					if (localPlayer.getLocalLocation().distanceTo(obj.getLocalLocation()) <= 2400)
					{
						renderCenteredText(graphics, obj.getCanvasTextLocation(graphics, text, 0), text, ropeColor);
					}
				}
			}
		}
	}

	private void renderCenteredText(Graphics2D graphics, Point loc, String text, int fontSize, Color color)
	{
		if (loc == null)
		{
			return;
		}
		Font oldFont = graphics.getFont();
		graphics.setFont(new Font("Arial", Font.BOLD, fontSize));
		renderCenteredText(graphics, loc, text, color);
		graphics.setFont(oldFont);
	}

	private void renderCenteredText(Graphics2D graphics, Point loc, String text, Color color)
	{
		if (loc == null)
		{
			return;
		}
		OverlayUtil.renderTextLocation(graphics, new Point(loc.getX() + 1, loc.getY() + 1), text, Color.BLACK);
		OverlayUtil.renderTextLocation(graphics, loc, text, color);
	}
}
