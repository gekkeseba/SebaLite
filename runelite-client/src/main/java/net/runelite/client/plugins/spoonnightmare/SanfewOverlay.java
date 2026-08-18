package net.runelite.client.plugins.spoonnightmare;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import javax.inject.Inject;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayPriority;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.PanelComponent;

public class SanfewOverlay extends Overlay
{
	private final SpoonNightmarePlugin plugin;
	private final SpoonNightmareConfig config;
	private final PanelComponent panelComponent = new PanelComponent();
	private int opacity = 5;

	@Inject
	private SanfewOverlay(SpoonNightmarePlugin plugin, SpoonNightmareConfig config)
	{
		this.plugin = plugin;
		this.config = config;
		setPosition(OverlayPosition.ABOVE_CHATBOX_RIGHT);
		setPriority(OverlayPriority.LOW);
		setLayer(OverlayLayer.UNDER_WIDGETS);
	}

	public Dimension render(Graphics2D graphics)
	{
		this.panelComponent.getChildren().clear();
		if (!this.config.parasiteTimer())
		{
			return null;
		}
		if (!this.plugin.isPreggers() || this.plugin.getParasiteTicks() < 0)
		{
			this.opacity = 5;
			return null;
		}
		if (this.plugin.isImpregnated())
		{
			this.opacity = this.opacity >= 80 ? 5 : (this.opacity += 3);
			this.panelComponent.setBackgroundColor(new Color(255, 0, 0, this.opacity));
		}
		else
		{
			this.opacity = 80;
			this.panelComponent.setBackgroundColor(new Color(0, 255, 0, this.opacity));
		}
		String text = this.plugin.getParasiteTicks() < 10 ? " " + this.plugin.getParasiteTicks() : String.valueOf(this.plugin.getParasiteTicks());
		this.panelComponent.getChildren().add(LineComponent.builder().left(text).build());
		this.panelComponent.setPreferredSize(new Dimension(24, 0));
		return this.panelComponent.render(graphics);
	}
}
