package net.runelite.client.plugins.spoontob.rooms.Verzik;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import javax.inject.Inject;
import net.runelite.client.plugins.spoontob.SpoonTobConfig;
import net.runelite.client.plugins.spoontob.SpoonTobPlugin;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayPriority;
import net.runelite.client.ui.overlay.components.ImageComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;
import net.runelite.client.util.ImageUtil;

public class LightningPanel extends OverlayPanel
{
	private final SpoonTobConfig config;
	private final Verzik verzik;

	@Inject
	private LightningPanel(SpoonTobConfig config, Verzik verzik)
	{
		this.config = config;
		this.verzik = verzik;
		setPriority(OverlayPriority.HIGH);
		setPosition(OverlayPosition.ABOVE_CHATBOX_RIGHT);
	}

	public Dimension render(Graphics2D graphics)
	{
		this.panelComponent.getChildren().clear();
		if ((this.config.lightningInfobox() == SpoonTobConfig.lightningMode.INFOBOX || this.config.lightningInfobox() == SpoonTobConfig.lightningMode.BOTH)
			&& this.verzik.isVerzikActive() && this.verzik.verzikPhase == Verzik.Phase.PHASE2)
		{
			if (this.verzik.lightningAttacks > 0)
			{
				Color color = (this.verzik.lightningAttacks == 1) ? Color.RED : Color.WHITE;
				this.panelComponent.getChildren().add(TitleComponent.builder()
					.color(color)
					.text(Integer.toString(this.verzik.lightningAttacks))
					.build());
			}
			else
			{
				BufferedImage img = ImageUtil.loadImageResource(SpoonTobPlugin.class, "Zap.png");
				this.panelComponent.getChildren().add(new ImageComponent(img));
			}
			this.panelComponent.setPreferredSize(new Dimension(24, 24));
		}
		return super.render(graphics);
	}
}
