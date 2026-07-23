package net.runelite.client.plugins.spoontob.rooms.Nylocas;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.client.plugins.spoontob.SpoonTobConfig;
import net.runelite.client.plugins.spoontob.util.TheatreRegions;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;

public class NyloWaveSpawnInfobox extends OverlayPanel
{
	private final Client client;
	private final SpoonTobConfig config;
	private final Nylocas nylo;

	@Inject
	private NyloWaveSpawnInfobox(Client client, SpoonTobConfig config, Nylocas nylo)
	{
		this.client = client;
		this.nylo = nylo;
		this.config = config;
		setPosition(OverlayPosition.ABOVE_CHATBOX_RIGHT);
	}

	public Dimension render(Graphics2D graphics)
	{
		this.panelComponent.getChildren().clear();
		if ((this.config.waveSpawnTimer() == SpoonTobConfig.waveSpawnTimerMode.INFOBOX || this.config.waveSpawnTimer() == SpoonTobConfig.waveSpawnTimerMode.BOTH)
			&& TheatreRegions.inRegion(this.client, TheatreRegions.NYLOCAS) && this.nylo.isNyloActive() && this.nylo.nyloWave < 31 && this.nylo.waveSpawnTicks > -1)
		{
			this.panelComponent.setPreferredSize(new Dimension(graphics.getFontMetrics().stringWidth("Next Wave:   ") + 20, 0));
			LineComponent.LineComponentBuilder builder = LineComponent.builder()
				.left("Next Wave: ")
				.right(String.valueOf(this.nylo.waveSpawnTicks));
			if (this.nylo.stalledWave)
			{
				builder.rightColor(Color.RED);
			}
			this.panelComponent.getChildren().add(builder.build());
		}
		return super.render(graphics);
	}
}
