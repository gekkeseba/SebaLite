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

public class YawnOverlay extends Overlay
{
	private final SpoonNightmarePlugin plugin;
	private final SpoonNightmareConfig config;
	private final PanelComponent panelComponent = new PanelComponent();

	@Inject
	private YawnOverlay(SpoonNightmarePlugin plugin, SpoonNightmareConfig config)
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
		if (!this.config.yawnTimer())
		{
			return null;
		}
		if (this.plugin.isYawning() && this.plugin.getYawnTicks() >= 0)
		{
			String text = this.plugin.getYawnTicks() < 10 ? " " + this.plugin.getYawnTicks() : String.valueOf(this.plugin.getYawnTicks());
			this.panelComponent.getChildren().add(LineComponent.builder().left(text).build());
			this.panelComponent.setPreferredSize(new Dimension(24, 0));
			this.panelComponent.setBackgroundColor(new Color(52, 52, 52, 150));
		}
		return this.panelComponent.render(graphics);
	}
}
