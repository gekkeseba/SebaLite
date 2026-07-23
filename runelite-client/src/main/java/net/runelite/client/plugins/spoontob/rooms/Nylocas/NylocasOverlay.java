package net.runelite.client.plugins.spoontob.rooms.Nylocas;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.Shape;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;
import net.runelite.api.NPC;
import net.runelite.api.Perspective;
import net.runelite.api.Point;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.spoontob.RoomOverlay;
import net.runelite.client.plugins.spoontob.SpoonTobConfig;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.overlay.OverlayUtil;

public class NylocasOverlay extends RoomOverlay
{
	@Inject
	private Nylocas nylocas;

	@Inject
	protected NylocasOverlay(SpoonTobConfig config)
	{
		super(config);
	}

	public Dimension render(Graphics2D graphics)
	{
		if (!this.nylocas.isInNyloRegion() || this.client.getLocalPlayer() == null)
		{
			return null;
		}

		if (this.config.showPhaseChange() != SpoonTobConfig.nyloBossPhaseChange.OFF && this.nylocas.getBossChangeTicks() > 0)
		{
			drawNylocas(graphics);
		}

		if (this.nylocas.isNyloActive())
		{
			if (this.config.showNylocasExplosions() != SpoonTobConfig.ExplosionWarning.OFF || this.config.nyloAggressiveOverlay() != SpoonTobConfig.aggroStyle.OFF)
			{
				renderNylos(graphics);
			}
			if ((this.config.waveSpawnTimer() == SpoonTobConfig.waveSpawnTimerMode.OVERLAY || this.config.waveSpawnTimer() == SpoonTobConfig.waveSpawnTimerMode.BOTH)
				&& this.client.getLocalPlayer() != null && this.nylocas.nyloWave < 31 && this.nylocas.waveSpawnTicks > -1)
			{
				renderWaveSpawnTimer(graphics);
			}
		}

		if (this.config.showBigSplits())
		{
			this.nylocas.getSplitsMap().forEach((npc, ticks) -> {
				Polygon poly = Perspective.getCanvasTileAreaPoly(this.client, npc.getLocalLocation(), 2);
				if (poly != null)
				{
					renderPolygon(graphics, poly, this.config.bigsColor());
				}
				Point textLocation = Perspective.getCanvasTextLocation(this.client, graphics, npc.getLocalLocation(), "#", 0);
				if (textLocation != null)
				{
					if (this.config.fontStyle())
					{
						renderTextLocation(graphics, Integer.toString(ticks), Color.WHITE, textLocation);
					}
					else
					{
						renderBigSplitsTextLocation(graphics, Integer.toString(ticks), textLocation);
					}
				}
			});
		}

		return null;
	}

	private void renderNylos(Graphics2D graphics)
	{
		for (NyloInfo ni : this.nylocas.nylocasNpcs)
		{
			if (!ni.alive)
			{
				continue;
			}
			NPC npc = ni.nylo;
			LocalPoint lp = npc.getLocalLocation();

			if (this.nylocas.getAggressiveNylocas().contains(npc) && lp != null)
			{
				renderAggressiveIndicator(graphics, npc, lp);
			}

			int ticksLeft = ni.ticks;
			if (ticksLeft > -1 && ticksLeft <= this.config.nyloExplosionDisplayTicks())
			{
				renderExplosionWarning(graphics, npc, lp, ticksLeft);
			}
		}
	}

	private void renderAggressiveIndicator(Graphics2D graphics, NPC npc, LocalPoint lp)
	{
		if (this.config.nyloAggressiveOverlay() == SpoonTobConfig.aggroStyle.TILE)
		{
			Polygon poly = getCanvasTileAreaPoly(this.client, lp, npc.getComposition().getSize(), -25);
			renderPoly(graphics, Color.RED, poly, this.config.nyloTileWidth());
		}
		else if (this.config.nyloAggressiveOverlay() == SpoonTobConfig.aggroStyle.HULL)
		{
			Shape objectClickbox = npc.getConvexHull();
			if (objectClickbox != null)
			{
				graphics.setColor(Color.RED);
				graphics.setStroke(new BasicStroke((float) this.config.nyloTileWidth()));
				graphics.draw(objectClickbox);
			}
		}
	}

	private void renderExplosionWarning(Graphics2D graphics, NPC npc, LocalPoint lp, int ticksLeft)
	{
		int ticksAlive = ticksLeft;
		if (this.config.nyloTimeAliveCountStyle() == SpoonTobConfig.nylotimealive.COUNTUP)
		{
			ticksAlive = 52 - ticksLeft;
		}
		Point textLocation = npc.getCanvasTextLocation(graphics, String.valueOf(ticksAlive), 60);
		if ((this.config.showNylocasExplosions() == SpoonTobConfig.ExplosionWarning.BOTH || this.config.showNylocasExplosions() == SpoonTobConfig.ExplosionWarning.TILE) && ticksLeft <= 6 && lp != null && npc.getComposition() != null)
		{
			if (this.config.nyloExplosionType() == SpoonTobConfig.nyloExplosionType.TILE)
			{
				renderPoly(graphics, Color.YELLOW, getCanvasTileAreaPoly(this.client, lp, npc.getComposition().getSize(), -15), this.config.nyloTileWidth());
			}
			else
			{
				renderPoly(graphics, Color.YELLOW, getCanvasTileAreaPoly(this.client, lp, npc.getComposition().getSize() + 4, 0), this.config.nyloTileWidth());
			}
		}
		if (textLocation != null && (this.config.showNylocasExplosions() == SpoonTobConfig.ExplosionWarning.BOTH || this.config.showNylocasExplosions() == SpoonTobConfig.ExplosionWarning.TICKS))
		{
			boolean urgent = (ticksAlive >= 44 && this.config.nyloTimeAliveCountStyle() == SpoonTobConfig.nylotimealive.COUNTUP)
				|| (ticksAlive <= 8 && this.config.nyloTimeAliveCountStyle() == SpoonTobConfig.nylotimealive.COUNTDOWN);
			Color color = urgent ? Color.RED : Color.WHITE;
			if (this.config.fontStyle())
			{
				renderTextLocation(graphics, String.valueOf(ticksAlive), color, textLocation);
			}
			else
			{
				renderSteroidsTextLocation(graphics, String.valueOf(ticksAlive), 13, 1, color, textLocation);
			}
		}
	}

	private void renderWaveSpawnTimer(Graphics2D graphics)
	{
		String text = String.valueOf(this.nylocas.waveSpawnTicks);
		int regionId = this.client.getLocalPlayer().getWorldLocation().getRegionID();
		int plane = this.client.getLocalPlayer().getWorldLocation().getPlane();
		LocalPoint eastLp = LocalPoint.fromWorld(this.client, WorldPoint.fromRegion(regionId, 42, 25, plane));
		LocalPoint westLp = LocalPoint.fromWorld(this.client, WorldPoint.fromRegion(regionId, 5, 25, plane));
		LocalPoint southLp = LocalPoint.fromWorld(this.client, WorldPoint.fromRegion(regionId, 24, 6, plane));
		Color color = this.nylocas.stalledWave ? Color.RED : this.config.waveSpawnTimerColor();

		for (LocalPoint lp : new LocalPoint[]{eastLp, westLp, southLp})
		{
			if (lp == null)
			{
				continue;
			}
			Point textLocation = Perspective.getCanvasTextLocation(this.client, graphics, lp, text, 0);
			if (this.config.fontStyle())
			{
				renderTextLocation(graphics, text, color, textLocation);
			}
			else
			{
				renderResizeTextLocation(graphics, text, 14, 1, color, textLocation);
			}
		}
	}

	public void drawNylocas(Graphics2D graphics)
	{
		NPC npc = null;
		if (this.nylocas.minibossAlive && this.nylocas.nyloMiniboss != null && this.config.showPhaseChange() == SpoonTobConfig.nyloBossPhaseChange.BOTH)
		{
			npc = this.nylocas.nyloMiniboss;
		}
		else if (this.nylocas.getNylocasBoss() != null)
		{
			npc = this.nylocas.getNylocasBoss();
		}
		if (npc == null)
		{
			return;
		}
		LocalPoint lp = npc.getLocalLocation();
		if (lp == null)
		{
			return;
		}
		String str = Integer.toString(this.nylocas.getBossChangeTicks());
		Point loc = Perspective.getCanvasTextLocation(this.client, graphics, lp, str, 0);
		if (loc == null)
		{
			return;
		}
		if (this.config.fontStyle())
		{
			renderTextLocation(graphics, str, Color.WHITE, loc);
		}
		else
		{
			renderResizeTextLocation(graphics, str, 14, 1, Color.WHITE, loc);
		}
	}

	protected void renderPolygon(Graphics2D graphics, @Nullable Shape polygon, @Nonnull Color color)
	{
		if (polygon == null)
		{
			return;
		}
		graphics.setColor(color);
		graphics.setStroke(new BasicStroke(2.0F));
		graphics.draw(polygon);
		graphics.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 0));
		graphics.fill(polygon);
	}

	protected void renderBigSplitsTextLocation(Graphics2D graphics, String txtString, Point canvasPoint)
	{
		graphics.setFont(new Font(FontManager.getRunescapeSmallFont().getName(), 1, 13));
		if (canvasPoint == null)
		{
			return;
		}
		Point shadow = new Point(canvasPoint.getX() + 1, canvasPoint.getY() + 1);
		OverlayUtil.renderTextLocation(graphics, shadow, txtString, Color.BLACK);
		OverlayUtil.renderTextLocation(graphics, canvasPoint, txtString, Color.WHITE);
	}
}
