package net.runelite.client.plugins.spoontob.rooms.Maiden;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Shape;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.NPC;
import net.runelite.client.plugins.spoontob.SpoonTobConfig;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.tooltip.Tooltip;
import net.runelite.client.ui.overlay.tooltip.TooltipManager;
import net.runelite.client.util.ColorUtil;

public class MaidenMaxHitToolTip extends Overlay
{
	@Inject
	private Client client;

	@Inject
	private TooltipManager tooltipManager;

	@Inject
	private Maiden maiden;

	@Inject
	private SpoonTobConfig config;

	@Inject
	private MaidenMaxHitToolTip(Client client, TooltipManager tooltipManager, Maiden maiden, SpoonTobConfig config)
	{
		this.client = client;
		this.tooltipManager = tooltipManager;
		this.maiden = maiden;
		this.config = config;
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (this.config.maidenMaxHit() == SpoonTobConfig.MaidenMaxHitTTMode.OFF || this.client.isMenuOpen() || !this.maiden.isMaidenActive())
		{
			return null;
		}

		NPC maidenNpc = this.maiden.getMaidenNPC();
		if (maidenNpc == null)
		{
			return null;
		}

		Shape clickbox = maidenNpc.getConvexHull();
		if (clickbox == null || !clickbox.contains(this.client.getMouseCanvasPosition().getX(), this.client.getMouseCanvasPosition().getY()))
		{
			return null;
		}

		int noPrayerMaxHit = (int) Math.floor(this.maiden.getMaxHit());
		int prayerMaxHit = noPrayerMaxHit / 2;
		int elyMaxHit = prayerMaxHit - (int) Math.floor(prayerMaxHit * 0.25);

		StringBuilder text = new StringBuilder();
		if (this.config.maidenMaxHit() == SpoonTobConfig.MaidenMaxHitTTMode.REGULAR || this.config.maidenMaxHit() == SpoonTobConfig.MaidenMaxHitTTMode.BOTH)
		{
			text.append(ColorUtil.wrapWithColorTag("No Prayer: ", new Color(255, 109, 97))).append("+").append(noPrayerMaxHit).append("</br>");
			text.append(ColorUtil.wrapWithColorTag("Prayer: ", Color.ORANGE)).append("+").append(prayerMaxHit).append("</br>");
		}
		if (this.config.maidenMaxHit() == SpoonTobConfig.MaidenMaxHitTTMode.ELY || this.config.maidenMaxHit() == SpoonTobConfig.MaidenMaxHitTTMode.BOTH)
		{
			text.append(ColorUtil.wrapWithColorTag("Elysian: ", Color.CYAN)).append("+").append(elyMaxHit);
		}

		this.tooltipManager.add(new Tooltip(text.toString()));
		return null;
	}
}
