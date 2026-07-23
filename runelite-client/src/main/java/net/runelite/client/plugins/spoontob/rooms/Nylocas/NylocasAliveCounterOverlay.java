package net.runelite.client.plugins.spoontob.rooms.Nylocas;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import javax.inject.Inject;
import net.runelite.client.plugins.spoontob.SpoonTobConfig;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayPriority;
import net.runelite.client.ui.overlay.components.LineComponent;

public class NylocasAliveCounterOverlay extends OverlayPanel
{
	private final SpoonTobConfig config;

	private Instant nyloWaveStart;
	private int nyloAlive = 0;
	private int maxNyloAlive = 12;
	private int wave = 0;
	private boolean hidden = false;

	@Inject
	private NylocasAliveCounterOverlay(SpoonTobConfig config)
	{
		this.config = config;
		setPosition(OverlayPosition.TOP_LEFT);
		setPriority(OverlayPriority.HIGH);
		refreshPanel();
	}

	public void setNyloWaveStart(Instant nyloWaveStart)
	{
		this.nyloWaveStart = nyloWaveStart;
	}

	public int getNyloAlive()
	{
		return this.nyloAlive;
	}

	public void setNyloAlive(int aliveCount)
	{
		this.nyloAlive = aliveCount;
		refreshPanel();
	}

	public int getMaxNyloAlive()
	{
		return this.maxNyloAlive;
	}

	public void setMaxNyloAlive(int maxAliveCount)
	{
		this.maxNyloAlive = maxAliveCount;
		refreshPanel();
	}

	public int getWave()
	{
		return this.wave;
	}

	public void setWave(int wave)
	{
		this.wave = wave;
		refreshPanel();
	}

	public boolean isHidden()
	{
		return this.hidden;
	}

	public void setHidden(boolean hidden)
	{
		this.hidden = hidden;
	}

	private void refreshPanel()
	{
		LineComponent lineComponent = LineComponent.builder()
			.left("Alive: ")
			.right(this.nyloAlive + "/" + this.maxNyloAlive)
			.build();
		lineComponent.setRightColor(this.nyloAlive >= this.maxNyloAlive ? Color.ORANGE : Color.GREEN);

		LineComponent waveComponent = LineComponent.builder().left("Wave: " + this.wave).build();

		this.panelComponent.getChildren().clear();
		this.panelComponent.getChildren().add(waveComponent);
		this.panelComponent.getChildren().add(lineComponent);
	}

	public Dimension render(Graphics2D graphics)
	{
		if (this.config.nyloAlivePanel() && !isHidden())
		{
			this.panelComponent.setPreferredSize(new Dimension(graphics.getFontMetrics().stringWidth("Alive: 24/24") + 10, 0));
			return this.panelComponent.render(graphics);
		}
		return null;
	}

	public String getFormattedTime()
	{
		Duration duration = Duration.between(this.nyloWaveStart, Instant.now());
		LocalTime localTime = LocalTime.ofSecondOfDay(duration.getSeconds());
		return localTime.format(DateTimeFormatter.ofPattern("mm:ss"));
	}
}
