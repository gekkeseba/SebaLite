package net.runelite.client.plugins.spoonnightmare;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.Point;
import net.runelite.api.Prayer;
import net.runelite.api.widgets.Widget;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayPriority;

public class PrayerOverlay extends Overlay
{
	private final Client client;
	private final SpoonNightmarePlugin plugin;
	private final SpoonNightmareConfig config;

	@Inject
	private PrayerOverlay(Client client, SpoonNightmarePlugin plugin, SpoonNightmareConfig config)
	{
		this.client = client;
		this.plugin = plugin;
		this.config = config;
		setPosition(OverlayPosition.DYNAMIC);
		setPriority(OverlayPriority.HIGHEST);
		setLayer(OverlayLayer.ABOVE_WIDGETS);
	}

	public Dimension render(Graphics2D graphics)
	{
		if (!this.config.easyPrayer() || this.plugin.getCorrectPray().isEmpty())
		{
			return null;
		}
		boolean curse = this.plugin.isCursePhase();
		Widget target;
		switch (this.plugin.getCorrectPray())
		{
			case "melee":
				target = curse ? this.plugin.getPrayerWidget(Prayer.PROTECT_FROM_MISSILES) : this.plugin.getPrayerWidget(Prayer.PROTECT_FROM_MELEE);
				break;
			case "missiles":
				target = curse ? this.plugin.getPrayerWidget(Prayer.PROTECT_FROM_MAGIC) : this.plugin.getPrayerWidget(Prayer.PROTECT_FROM_MISSILES);
				break;
			case "magic":
				target = curse ? this.plugin.getPrayerWidget(Prayer.PROTECT_FROM_MELEE) : this.plugin.getPrayerWidget(Prayer.PROTECT_FROM_MAGIC);
				break;
			default:
				return null;
		}
		if (target == null || target.isHidden() || target.isSelfHidden())
		{
			return null;
		}
		this.drawBox(graphics, target.getCanvasLocation());
		return null;
	}

	private void drawBox(Graphics2D graphics, Point startLoc)
	{
		if (startLoc == null || (startLoc.getX() == 0 && startLoc.getY() == 0))
		{
			return;
		}
		int startX = startLoc.getX();
		int startY = startLoc.getY();
		boolean curse = this.plugin.isCursePhase();
		boolean correct = curse
			? (this.plugin.getCorrectPray().equals("missiles") && this.client.isPrayerActive(Prayer.PROTECT_FROM_MAGIC))
				|| (this.plugin.getCorrectPray().equals("magic") && this.client.isPrayerActive(Prayer.PROTECT_FROM_MELEE))
				|| (this.plugin.getCorrectPray().equals("melee") && this.client.isPrayerActive(Prayer.PROTECT_FROM_MISSILES))
			: (this.plugin.getCorrectPray().equals("missiles") && this.client.isPrayerActive(Prayer.PROTECT_FROM_MISSILES))
				|| (this.plugin.getCorrectPray().equals("magic") && this.client.isPrayerActive(Prayer.PROTECT_FROM_MAGIC))
				|| (this.plugin.getCorrectPray().equals("melee") && this.client.isPrayerActive(Prayer.PROTECT_FROM_MELEE));
		graphics.setColor(correct ? Color.GREEN : Color.RED);
		graphics.setStroke(new BasicStroke(this.config.prayerStrokeSize()));
		graphics.drawLine(startX, startY, startX + 33, startY);
		graphics.drawLine(startX + 33, startY, startX + 33, startY + 33);
		graphics.drawLine(startX + 33, startY + 33, startX, startY + 33);
		graphics.drawLine(startX, startY + 33, startX, startY);
	}
}
