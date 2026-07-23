package net.runelite.client.plugins.spooncoxadditions.overlays;

import java.awt.Dimension;
import java.awt.Graphics2D;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.MenuAction;
import net.runelite.api.gameval.VarbitID;
import net.runelite.client.plugins.spooncoxadditions.SpoonCoxAdditionsConfig;
import net.runelite.client.plugins.spooncoxadditions.SpoonCoxAdditionsPlugin;
import net.runelite.client.ui.overlay.OverlayMenuEntry;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;

public class MeatTreeCycleOverlay extends OverlayPanel
{
	private final Client client;
	private final SpoonCoxAdditionsPlugin plugin;
	private final SpoonCoxAdditionsConfig config;

	@Inject
	public MeatTreeCycleOverlay(Client client, SpoonCoxAdditionsPlugin plugin, SpoonCoxAdditionsConfig config)
	{
		super(plugin);
		this.client = client;
		this.plugin = plugin;
		this.config = config;
		setPosition(OverlayPosition.ABOVE_CHATBOX_RIGHT);
		getMenuEntries().add(new OverlayMenuEntry(MenuAction.RUNELITE_OVERLAY_CONFIG, "Configure", "Cox Additions meat tree chop cycle"));
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		panelComponent.getChildren().clear();
		if (this.config.meatTreeChopCycle() == SpoonCoxAdditionsConfig.meatTreeChopCycleMode.INFOBOX
			&& this.client.getVarbitValue(VarbitID.RAIDS_CLIENT_INDUNGEON) == 1
			&& this.plugin.startedChopping && this.plugin.meatTreeAlive)
		{
			panelComponent.setPreferredSize(new Dimension(graphics.getFontMetrics().stringWidth("Tick:   ") + 15, 0));
			panelComponent.getChildren().add(LineComponent.builder().left("Tick: ").right(String.valueOf(this.plugin.ticksToChop)).build());
		}
		return super.render(graphics);
	}
}
