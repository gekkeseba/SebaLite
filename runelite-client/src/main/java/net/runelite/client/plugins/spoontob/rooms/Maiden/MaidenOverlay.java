package net.runelite.client.plugins.spoontob.rooms.Maiden;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.util.ArrayList;
import java.util.function.BiFunction;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.NPC;
import net.runelite.api.NPCComposition;
import net.runelite.api.Perspective;
import net.runelite.api.Point;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.spoontob.RoomOverlay;
import net.runelite.client.plugins.spoontob.SpoonTobConfig;
import net.runelite.client.ui.overlay.outline.ModelOutlineRenderer;

public class MaidenOverlay extends RoomOverlay
{
	private static final BiFunction<Integer, Integer, Color> FROZEN_TICKS_COLOR =
		(max, current) -> new Color(255 * (max - current) / max, 255 * current / max, 0);

	@Inject
	private Maiden maiden;

	@Inject
	private Client client;

	private final ModelOutlineRenderer modelOutlineRenderer;

	@Inject
	protected MaidenOverlay(SpoonTobConfig config, ModelOutlineRenderer modelOutlineRenderer)
	{
		super(config);
		this.modelOutlineRenderer = modelOutlineRenderer;
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (!this.maiden.isMaidenActive() || this.maiden.getMaidenNPC() == null)
		{
			return null;
		}

		if (this.config.maidenBlood() != SpoonTobConfig.maidenBloodSplatMode.OFF)
		{
			for (WorldPoint wp : this.maiden.getMaidenBloodSplatters())
			{
				drawTile(graphics, wp, this.config.bloodTossColour(), 1, this.config.bloodTossColour().getAlpha(), this.config.bloodTossFill());
			}
		}

		if (this.config.bloodTossTicks())
		{
			for (MaidenBloodSplatInfo info : this.maiden.getMaidenBloodSplatterProj())
			{
				String text = String.valueOf(info.projectile.getRemainingCycles() / 30);
				Point canvasPoint = Perspective.getCanvasTextLocation(this.client, graphics, info.lp, text, 0);
				if (canvasPoint != null)
				{
					if (this.config.fontStyle())
					{
						renderTextLocation(graphics, text, Color.WHITE, canvasPoint);
					}
					else
					{
						renderSteroidsTextLocation(graphics, text, 14, 1, Color.WHITE, canvasPoint);
					}
				}
			}
		}

		if (this.config.maidenSpawns())
		{
			for (WorldPoint point : this.maiden.getMaidenBloodSpawnLocations())
			{
				drawTile(graphics, point, this.config.bloodSpawnsColor(), 2, 180, 20);
			}
			if (this.config.maidenSpawnsTrail())
			{
				for (WorldPoint point : this.maiden.getMaidenBloodSpawnTrailingLocations())
				{
					drawTile(graphics, point, this.config.bloodSpawnsColor(), 1, 120, 10);
				}
			}
		}

		if (this.config.maidenTickCounter() && !this.maiden.getMaidenNPC().isDead())
		{
			String text = String.valueOf(this.maiden.ticksUntilAttack);
			Point canvasPoint = this.maiden.getMaidenNPC().getCanvasTextLocation(graphics, text, 30);
			if (canvasPoint != null)
			{
				Color col = this.maiden.maidenSpecialWarningColor();
				if (this.config.fontStyle())
				{
					renderTextLocation(graphics, text, col, canvasPoint);
				}
				else
				{
					renderResizeTextLocation(graphics, text, 14, 1, col, canvasPoint);
				}
			}
		}

		if ((this.config.maidenFreezeTimer() == SpoonTobConfig.maidenFreezeTimerMode.TILE || this.config.maidenScuffedCrab()) && !this.maiden.maidenCrabInfoList.isEmpty())
		{
			int maidenX = 0;
			if (this.maiden.getMaidenNPC() != null)
			{
				WorldPoint maidenWp = this.maiden.getMaidenNPC().getWorldLocation();
				maidenX = maidenWp.getX();
				NPCComposition maidenModel = this.maiden.getMaidenNPC().getTransformedComposition();
				if (maidenModel != null)
				{
					maidenX += maidenModel.getSize();
				}
			}
			for (MaidenCrabInfo mci : this.maiden.maidenCrabInfoList)
			{
				if (mci.crab.isDead())
				{
					continue;
				}
				int healerX = mci.crab.getWorldLocation().getX();
				int deltaX = Math.max(0, healerX - maidenX);
				if (deltaX <= 0)
				{
					continue;
				}
				NPCComposition npcComposition = mci.crab.getTransformedComposition();
				if (npcComposition == null)
				{
					continue;
				}
				int size = npcComposition.getSize();
				LocalPoint lp = mci.crab.getLocalLocation();
				Polygon tilePoly = Perspective.getCanvasTileAreaPoly(this.client, lp, size);
				if (mci.frozenTicks != -1)
				{
					renderPoly(graphics, FROZEN_TICKS_COLOR.apply(32, mci.frozenTicks), tilePoly);
				}
				else if (this.config.maidenScuffedCrab() && mci.scuffed && this.maiden.crabTicksSinceSpawn > 0)
				{
					this.modelOutlineRenderer.drawOutline(mci.crab, 2, this.config.maidenScuffedCrabColor(), 4);
				}
			}
		}

		if ((this.config.showMaidenCrabsDistance() || this.config.maidenFreezeTimer() == SpoonTobConfig.maidenFreezeTimerMode.TICKS) && !this.maiden.maidenCrabInfoList.isEmpty())
		{
			renderCrabInfo(graphics);
		}

		if (this.config.bloodSpawnFreezeTimer() && !this.maiden.frozenBloodSpawns.isEmpty())
		{
			this.maiden.frozenBloodSpawns.forEach((npc, ticks) -> {
				if (ticks >= 0)
				{
					String text = String.valueOf(ticks);
					Point canvasPoint = npc.getCanvasTextLocation(graphics, text, 30);
					if (canvasPoint != null)
					{
						if (this.config.fontStyle())
						{
							renderTextLocation(graphics, text, Color.WHITE, canvasPoint);
						}
						else
						{
							renderSteroidsTextLocation(graphics, text, 14, 1, Color.WHITE, canvasPoint);
						}
					}
				}
			});
		}
		return null;
	}

	private void renderCrabInfo(Graphics2D graphics)
	{
		ArrayList<NPC> prevCrabs = new ArrayList<>();
		for (MaidenCrabInfo mci : this.maiden.maidenCrabInfoList)
		{
			if (mci.hpRatio == 0)
			{
				continue;
			}

			String text = "";
			String distanceLine = "";
			Color distanceColor = this.config.distanceColor();
			Color color = Color.GREEN;

			if (this.config.maidenFreezeTimer() == SpoonTobConfig.maidenFreezeTimerMode.TICKS && mci.frozenTicks >= 0)
			{
				text = text.isEmpty() ? String.valueOf(mci.frozenTicks) : text + " : " + text;
			}

			if (this.config.showMaidenCrabsDistance())
			{
				WorldPoint maidenWp = this.maiden.getMaidenNPC().getWorldLocation();
				int maidenX = maidenWp.getX();
				NPCComposition maidenModel = this.maiden.getMaidenNPC().getTransformedComposition();
				if (maidenModel != null)
				{
					maidenX += maidenModel.getSize();
				}
				int healerX = mci.crab.getWorldLocation().getX();
				int deltaX = Math.max(0, healerX - maidenX);
				if (this.config.singleLineDistance())
				{
					if (mci.frozenTicks == -1)
					{
						if (!text.isEmpty())
						{
							text = text + " : " + text;
						}
						else
						{
							color = Color.WHITE;
							text = Integer.toString(deltaX);
						}
					}
				}
				else if (this.config.showFrozenDistance() || mci.frozenTicks == -1)
				{
					distanceLine = Integer.toString(deltaX);
				}
			}

			int offsetTimes = 0;
			NPC firstOverlap = null;
			for (NPC crab : prevCrabs)
			{
				LocalPoint lp = crab.getLocalLocation();
				if (lp.getX() == mci.crab.getLocalLocation().getX() && lp.getY() == mci.crab.getLocalLocation().getY())
				{
					offsetTimes++;
					if (firstOverlap == null)
					{
						firstOverlap = crab;
					}
				}
			}

			Point drawPoint;
			if (offsetTimes != 0)
			{
				drawPoint = firstOverlap.getCanvasTextLocation(graphics, text, 0);
				if (drawPoint != null)
				{
					drawPoint = new Point(drawPoint.getX(), drawPoint.getY() - 15 * offsetTimes);
				}
			}
			else
			{
				drawPoint = mci.crab.getCanvasTextLocation(graphics, text, 0);
			}

			if (drawPoint != null)
			{
				if (this.config.fontStyle())
				{
					renderTextLocation(graphics, text, color, drawPoint);
					if (!distanceLine.isEmpty())
					{
						Point offsetPoint = text.contains(":")
							? new Point(drawPoint.getX() + 15, drawPoint.getY() - 10)
							: new Point(drawPoint.getX() + 5, drawPoint.getY() - 10);
						renderTextLocation(graphics, distanceLine, distanceColor, offsetPoint);
					}
				}
				else
				{
					renderResizeTextLocation(graphics, text, 11, 1, color, drawPoint);
					if (!distanceLine.isEmpty())
					{
						Point offsetPoint = text.contains(":")
							? new Point(drawPoint.getX() + 15, drawPoint.getY() - 10)
							: new Point(drawPoint.getX() + 5, drawPoint.getY() - 10);
						renderResizeTextLocation(graphics, distanceLine, 11, 1, distanceColor, offsetPoint);
					}
				}
			}
			prevCrabs.add(mci.crab);
		}
	}
}
