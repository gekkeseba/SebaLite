package net.runelite.client.plugins.spoontob.rooms.Sotetseg;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import javax.inject.Inject;
import net.runelite.api.Perspective;
import net.runelite.api.Point;
import net.runelite.api.Projectile;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.spoontob.RoomOverlay;
import net.runelite.client.plugins.spoontob.SpoonTobConfig;
import net.runelite.client.ui.overlay.OverlayUtil;

public class SotetsegOverlay extends RoomOverlay
{
	@Inject
	private Sotetseg sotetseg;

	@Inject
	protected SotetsegOverlay(SpoonTobConfig config)
	{
		super(config);
	}

	public Dimension render(Graphics2D graphics)
	{
		if (!this.sotetseg.isSotetsegActive())
		{
			return null;
		}

		displaySotetsegCounters(graphics);

		if (this.config.sotetsegMaze())
		{
			int counter = 1;
			for (Point p : this.sotetseg.getRedTiles())
			{
				WorldPoint wp = this.sotetseg.worldPointFromMazePoint(p);
				drawTile(graphics, wp, Color.GREEN, 1, 255, 0);
				LocalPoint lp = LocalPoint.fromWorld(this.client, wp);
				if (lp != null && !this.sotetseg.isWasInUnderWorld())
				{
					Point textPoint = Perspective.getCanvasTextLocation(this.client, graphics, lp, String.valueOf(counter), 0);
					if (textPoint != null)
					{
						renderTextLocation(graphics, String.valueOf(counter), Color.GREEN, textPoint);
					}
				}
				counter++;
			}
		}

		if (this.config.sotetsegShowOrbs() != SpoonTobConfig.soteOrbMode.OFF || this.config.sotetsegShowNuke() != SpoonTobConfig.soteDeathballOverlayMode.OFF)
		{
			for (Projectile p : this.client.getProjectiles())
			{
				renderProjectile(graphics, p);
			}
		}

		return null;
	}

	private void renderProjectile(Graphics2D graphics, Projectile p)
	{
		int id = p.getId();
		Point point = Perspective.localToCanvas(this.client, new LocalPoint((int) p.getX(), (int) p.getY()), 0, Perspective.getTileHeight(this.client, new LocalPoint((int) p.getX(), (int) p.getY()), p.getFloor()) - (int) p.getZ());
		if (point == null)
		{
			return;
		}
		String ticks = String.valueOf(p.getRemainingCycles() / 30);
		boolean targetingLocalPlayer = p.getInteracting() == this.client.getLocalPlayer();

		if ((id == Sotetseg.SOTETSEG_MAGE_ORB || id == Sotetseg.SOTETSEG_RANGE_ORB) && targetingLocalPlayer)
		{
			if (this.config.sotetsegShowOrbs() == SpoonTobConfig.soteOrbMode.HATS || this.config.sotetsegShowOrbs() == SpoonTobConfig.soteOrbMode.BOTH)
			{
				BufferedImage icon = (id == Sotetseg.SOTETSEG_MAGE_ORB) ? this.sotetseg.mageIcon : this.sotetseg.rangeIcon;
				Point iconLocation = new Point(point.getX() - icon.getWidth() / 2, point.getY() - 30);
				OverlayUtil.renderImageLocation(graphics, iconLocation, icon);
			}
			if (this.config.sotetsegShowOrbs() == SpoonTobConfig.soteOrbMode.TICKS || this.config.sotetsegShowOrbs() == SpoonTobConfig.soteOrbMode.BOTH)
			{
				Color color = (id == Sotetseg.SOTETSEG_MAGE_ORB) ? Color.CYAN : Color.GREEN;
				if (this.config.fontStyle())
				{
					renderTextLocation(graphics, ticks, color, point);
				}
				else
				{
					renderSteroidsTextLocation(graphics, ticks, 17, 1, color, point);
				}
			}
		}

		if (id == Sotetseg.SOTETSEG_BIG_AOE_ORB)
		{
			renderDeathBall(graphics, p, point, ticks);
		}
	}

	private void renderDeathBall(Graphics2D graphics, Projectile p, Point point, String ticks)
	{
		Color color = Color.ORANGE;
		if (this.config.sotetsegShowNuke() == SpoonTobConfig.soteDeathballOverlayMode.TICKS || this.config.sotetsegShowNuke() == SpoonTobConfig.soteDeathballOverlayMode.BOTH)
		{
			if (this.config.deathTicksOnPlayer())
			{
				point = Perspective.getCanvasTextLocation(this.client, graphics, p.getInteracting().getLocalLocation(), ticks, this.config.deathballOffset());
				if (point != null)
				{
					if (this.config.fontStyle())
					{
						renderTextLocation(graphics, ticks, Color.WHITE, point);
					}
					else
					{
						renderSteroidsTextLocation(graphics, ticks, this.config.deathballSize(), 1, color, point);
					}
				}
			}
			else if (this.config.fontStyle())
			{
				renderTextLocation(graphics, ticks, color, point);
			}
			else
			{
				renderSteroidsTextLocation(graphics, ticks, 20, 1, color, point);
			}
		}

		if (point == null)
		{
			return;
		}

		if (this.config.displayDeathBall())
		{
			renderPoly(graphics, this.config.displayDeathBallColor(), p.getInteracting().getCanvasTilePoly());
		}

		if (this.config.sotetsegShowNuke() == SpoonTobConfig.soteDeathballOverlayMode.NUKE || this.config.sotetsegShowNuke() == SpoonTobConfig.soteDeathballOverlayMode.BOTH)
		{
			Point imageLocation = new Point(point.getX() - Sotetseg.TACTICAL_NUKE_OVERHEAD.getWidth() / 2, point.getY() - 60);
			OverlayUtil.renderImageLocation(graphics, imageLocation, Sotetseg.TACTICAL_NUKE_OVERHEAD);
		}
	}

	private void displaySotetsegCounters(Graphics2D graphics)
	{
		if (this.sotetseg.sotetsegTicks <= 0 || this.sotetseg.sotetsegNPC == null)
		{
			return;
		}

		String text = "";
		String attacksLeftText = "";
		if (this.config.deathballInfobox() == SpoonTobConfig.soteDeathballMode.OVERLAY || this.config.deathballInfobox() == SpoonTobConfig.soteDeathballMode.BOTH)
		{
			String nukeCounter = (this.sotetseg.sotetsegAttacksLeft == 0) ? "Nuke" : String.valueOf(this.sotetseg.sotetsegAttacksLeft);
			if (this.config.deathballSingleLine())
			{
				text = nukeCounter;
			}
			else
			{
				attacksLeftText = nukeCounter;
			}
		}

		if (this.config.showSotetsegAttackTicks())
		{
			text = text.isEmpty() ? String.valueOf(this.sotetseg.sotetsegTicks) : text + " : " + this.sotetseg.sotetsegTicks;
		}

		Point textLocation = this.sotetseg.sotetsegNPC.getCanvasTextLocation(graphics, text, 50);
		if (this.config.fontStyle())
		{
			renderTextLocation(graphics, text, Color.WHITE, textLocation);
			if (!this.config.deathballSingleLine() && !attacksLeftText.isEmpty())
			{
				Point attacksLeftLocation = this.sotetseg.sotetsegNPC.getCanvasTextLocation(graphics, attacksLeftText, 200);
				renderTextLocation(graphics, attacksLeftText, Color.ORANGE, attacksLeftLocation);
			}
		}
		else
		{
			renderResizeTextLocation(graphics, text, 14, 1, Color.WHITE, textLocation);
			if (!this.config.deathballSingleLine() && !attacksLeftText.isEmpty())
			{
				Point attacksLeftLocation = this.sotetseg.sotetsegNPC.getCanvasTextLocation(graphics, attacksLeftText, 200);
				renderResizeTextLocation(graphics, attacksLeftText, 14, 1, Color.ORANGE, attacksLeftLocation);
			}
		}
	}
}
