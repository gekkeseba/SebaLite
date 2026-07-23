package net.runelite.client.plugins.spoontob.rooms.Sotetseg;

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

public class DeathBallPanel extends OverlayPanel
{
	private final SpoonTobConfig config;
	private final Sotetseg sotetseg;

	@Inject
	private DeathBallPanel(SpoonTobConfig config, Sotetseg sotetseg)
	{
		this.config = config;
		this.sotetseg = sotetseg;
		setPriority(OverlayPriority.HIGH);
		setPosition(OverlayPosition.ABOVE_CHATBOX_RIGHT);
	}

	public Dimension render(Graphics2D graphics)
	{
		this.panelComponent.getChildren().clear();
		if ((this.config.deathballInfobox() == SpoonTobConfig.soteDeathballMode.INFOBOX || this.config.deathballInfobox() == SpoonTobConfig.soteDeathballMode.BOTH) && this.sotetseg.isSotetsegActive())
		{
			int attacksLeft = this.sotetseg.sotetsegAttacksLeft;
			if (attacksLeft > 0)
			{
				Color color = (attacksLeft == 1) ? Color.RED : Color.WHITE;
				this.panelComponent.getChildren().add(TitleComponent.builder()
					.color(color)
					.text(Integer.toString(attacksLeft))
					.build());
			}
			else
			{
				BufferedImage img = ImageUtil.loadImageResource(SpoonTobPlugin.class, "NukeSprite.png");
				this.panelComponent.getChildren().add(new ImageComponent(img));
			}
			this.panelComponent.setPreferredSize(new Dimension(24, 24));
		}
		return super.render(graphics);
	}
}
