package net.runelite.client.plugins.spoonnightmare;

import com.google.common.base.Strings;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import javax.inject.Inject;
import net.runelite.api.NPC;
import net.runelite.api.Point;
import net.runelite.api.gameval.AnimationID;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayPriority;

public class TickOverlay extends Overlay
{
	private final SpoonNightmarePlugin plugin;
	private final SpoonNightmareConfig config;

	@Inject
	private TickOverlay(SpoonNightmarePlugin plugin, SpoonNightmareConfig config)
	{
		this.plugin = plugin;
		this.config = config;
		setPosition(OverlayPosition.DYNAMIC);
		setPriority(OverlayPriority.HIGHEST);
		setLayer(OverlayLayer.ABOVE_SCENE);
	}

	private void renderNightmareTicks(Graphics2D graphics)
	{
		if (this.plugin.getTicksUntilAttack() <= 0 && this.plugin.getEventTicks() <= 0)
		{
			return;
		}
		NPC npc = this.plugin.getNightmareNpc();
		if (npc == null)
		{
			return;
		}
		Color color = this.config.tickCounterColor();
		if (this.plugin.getTicksUntilAttack() > 2 && this.config.matchStyleColor() && this.config.tickCounter())
		{
			int animation = npc.getAnimation();
			if ("melee".equalsIgnoreCase(this.plugin.getCorrectPray()) && animation == AnimationID.NIGHTMARE_ATTACK_MELEE)
			{
				color = Color.RED;
			}
			else if ("magic".equalsIgnoreCase(this.plugin.getCorrectPray()) && animation == AnimationID.NIGHTMARE_ATTACK_MAGIC)
			{
				color = Color.CYAN;
			}
			else if ("missiles".equalsIgnoreCase(this.plugin.getCorrectPray()) && animation == AnimationID.NIGHTMARE_ATTACK_RANGED)
			{
				color = Color.GREEN;
			}
		}
		String attTicks = Integer.toString(this.plugin.getTicksUntilAttack());
		String eventTicks = Integer.toString(this.plugin.getEventTicks());
		String nmTicks = "";
		if (this.config.tickCounter() && this.config.eventTickCounter())
		{
			if (this.plugin.getTicksUntilAttack() > 0)
			{
				nmTicks = this.plugin.getEventTicks() > 0 ? attTicks + " : " + eventTicks : attTicks;
			}
			else if (this.plugin.getEventTicks() > 0)
			{
				nmTicks = eventTicks;
			}
		}
		else if (this.config.tickCounter() && this.plugin.getTicksUntilAttack() > 0)
		{
			nmTicks = attTicks;
		}
		else if (this.config.eventTickCounter() && this.plugin.getEventTicks() > 0)
		{
			nmTicks = eventTicks;
		}
		Point p = npc.getCanvasTextLocation(graphics, nmTicks, 0);
		renderTextLocation(graphics, p, nmTicks, color, this.config.tickCounterSize(), this.config.txtOutline());
	}

	private static void renderTextLocation(Graphics2D graphics, Point txtLoc, String text, Color color, int size, boolean outline)
	{
		if (Strings.isNullOrEmpty(text) || txtLoc == null)
		{
			return;
		}
		int x = txtLoc.getX() - 5;
		int y = txtLoc.getY() + 5;
		graphics.setColor(Color.BLACK);
		graphics.setFont(new Font("Arial", 1, size));
		if (outline)
		{
			graphics.drawString(text, x, y + 1);
			graphics.drawString(text, x, y - 1);
			graphics.drawString(text, x + 1, y);
			graphics.drawString(text, x - 1, y);
		}
		else
		{
			graphics.drawString(text, x + 1, y + 1);
		}
		graphics.setColor(color);
		graphics.drawString(text, x, y);
	}

	public Dimension render(Graphics2D graphics)
	{
		if ((this.plugin.isActiveFight() || this.plugin.getNightmareNpc() != null) && (this.config.tickCounter() || this.config.eventTickCounter()))
		{
			this.renderNightmareTicks(graphics);
		}
		return null;
	}
}
