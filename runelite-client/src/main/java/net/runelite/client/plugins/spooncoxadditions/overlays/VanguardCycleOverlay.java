package net.runelite.client.plugins.spooncoxadditions.overlays;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.MenuAction;
import net.runelite.client.plugins.spooncoxadditions.SpoonCoxAdditionsConfig;
import net.runelite.client.plugins.spooncoxadditions.SpoonCoxAdditionsPlugin;
import net.runelite.client.ui.overlay.OverlayMenuEntry;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayPriority;
import net.runelite.client.ui.overlay.components.LineComponent;

public class VanguardCycleOverlay extends OverlayPanel
{
	private final Client client;
	private final SpoonCoxAdditionsPlugin plugin;
	private final SpoonCoxAdditionsConfig config;

	@Inject
	private VanguardCycleOverlay(Client client, SpoonCoxAdditionsPlugin plugin, SpoonCoxAdditionsConfig config)
	{
		super(plugin);
		setPosition(OverlayPosition.TOP_LEFT);
		setPriority(OverlayPriority.LOW);
		this.client = client;
		this.plugin = plugin;
		this.config = config;
		getMenuEntries().add(new OverlayMenuEntry(MenuAction.RUNELITE_OVERLAY_CONFIG, "Configure", "Vanguard Tick Cycle"));
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (this.config.vangsCycle() == SpoonCoxAdditionsConfig.VangsTicksMode.OFF)
		{
			return super.render(graphics);
		}

		panelComponent.getChildren().clear();
		if (this.plugin.vangsAlive)
		{
			panelComponent.setPreferredSize(new Dimension(110, 0));
			boolean overdue = this.plugin.vangsTicks >= 20;

			switch (this.config.vangsCycle())
			{
				case BOTH:
					addLine(overdue, this.plugin.vangs4Ticks + " (" + this.plugin.vangsTicks + ")");
					break;
				case TOTAL_TICKS:
					addLine(overdue, String.valueOf(this.plugin.vangsTicks));
					break;
				case FOUR_TICK_CYCLE:
					addLine(overdue, String.valueOf(this.plugin.vangs4Ticks));
					break;
				default:
					break;
			}
		}
		return super.render(graphics);
	}

	private void addLine(boolean overdue, String value)
	{
		LineComponent.LineComponentBuilder builder = LineComponent.builder().left("Ticks Alive:").right(value);
		if (overdue)
		{
			builder.rightColor(Color.RED);
		}
		panelComponent.getChildren().add(builder.build());
	}
}
