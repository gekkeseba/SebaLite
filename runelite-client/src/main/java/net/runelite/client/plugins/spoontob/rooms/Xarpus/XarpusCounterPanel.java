package net.runelite.client.plugins.spoontob.rooms.Xarpus;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import javax.inject.Inject;
import net.runelite.client.plugins.spoontob.SpoonTobConfig;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;

public class XarpusCounterPanel extends OverlayPanel
{
	private final SpoonTobConfig config;
	private final Xarpus xarpus;

	@Inject
	private XarpusCounterPanel(SpoonTobConfig config, Xarpus xarpus)
	{
		this.config = config;
		this.xarpus = xarpus;
		setPosition(OverlayPosition.ABOVE_CHATBOX_RIGHT);
	}

	public Dimension render(Graphics2D graphics)
	{
		if (!this.xarpus.isXarpusActive() || !this.xarpus.isExhumedSpawned() || this.xarpus.getExhumedCounter() == null || this.xarpus.getXarpusNPC() == null
			|| !Xarpus.P1_IDS.contains(this.xarpus.getXarpusNPC().getId()) || !this.config.xarpusExhumedInfo())
		{
			return null;
		}

		String exhumeds = Integer.toString(this.xarpus.getExhumedCounter().getCount());
		String healed = Integer.toString(this.xarpus.getHealCount());
		this.panelComponent.getChildren().clear();
		String overlayTitle = "Exhume Counter";
		this.panelComponent.getChildren().add(TitleComponent.builder().text(overlayTitle).color(Color.GREEN).build());
		this.panelComponent.setPreferredSize(new Dimension(graphics.getFontMetrics().stringWidth(overlayTitle) + 30, 0));
		this.panelComponent.getChildren().add(LineComponent.builder().left("Exhumes: ").right(exhumeds).build());
		this.panelComponent.getChildren().add(LineComponent.builder().left("Healed: ").right(healed).build());
		return super.render(graphics);
	}
}
