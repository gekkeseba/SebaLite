package net.runelite.client.plugins.spoontob.rooms.Verzik;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import javax.inject.Inject;
import net.runelite.client.plugins.spoontob.SpoonTobConfig;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayPriority;
import net.runelite.client.ui.overlay.components.LineComponent;

public class GreenBallPanel extends OverlayPanel
{
	private final SpoonTobConfig config;
	private final Verzik verzik;

	@Inject
	private GreenBallPanel(SpoonTobConfig config, Verzik verzik)
	{
		this.config = config;
		this.verzik = verzik;
		setPriority(OverlayPriority.HIGH);
		setPosition(OverlayPosition.ABOVE_CHATBOX_RIGHT);
	}

	public Dimension render(Graphics2D graphics)
	{
		this.panelComponent.getChildren().clear();
		if (this.config.greenBouncePanel() == SpoonTobConfig.greenBouncePanelMode.OFF || !this.verzik.isVerzikActive()
			|| this.verzik.verzikPhase != Verzik.Phase.PHASE3 || !this.verzik.greenBallOut || this.verzik.getVerzikNPC() == null)
		{
			return null;
		}

		boolean isHardMode = this.verzik.getVerzikNPC().getId() == 10852;
		double damage = computeRemainingDamage(isHardMode);
		String leftText;
		String rightText;
		Color rightColor = Color.WHITE;

		if (this.config.greenBouncePanel() == SpoonTobConfig.greenBouncePanelMode.BOUNCES)
		{
			leftText = "Bounces:";
			rightText = Integer.toString(this.verzik.greenBallBounces);
			this.panelComponent.setPreferredSize(new Dimension(95, 24));
		}
		else if (this.config.greenBouncePanel() == SpoonTobConfig.greenBouncePanelMode.DAMAGE)
		{
			leftText = "Damage:";
			rightText = (this.verzik.greenBallBounces == 0 && !isHardMode) ? "74" : Double.toString(damage);
			this.panelComponent.setPreferredSize(new Dimension(90, 24));
		}
		else
		{
			leftText = "Bounces(Dmg):";
			String dmgText = (this.verzik.greenBallBounces == 0 && !isHardMode) ? "74" : Double.toString(damage);
			rightText = this.verzik.greenBallBounces + "(" + dmgText + ")";
			this.panelComponent.setPreferredSize(new Dimension(130, 24));
		}

		if (this.verzik.greenBallBounces == 0 && isHardMode)
		{
			rightColor = Color.RED;
			rightText = "Death";
		}

		this.panelComponent.getChildren().add(LineComponent.builder()
			.left(leftText)
			.rightColor(rightColor)
			.right(rightText)
			.build());
		return super.render(graphics);
	}

	private double computeRemainingDamage(boolean isHardMode)
	{
		double baseDamage = isHardMode ? 99.0D : 74.0D;
		return baseDamage - this.verzik.greenBallBounces * 0.25D * baseDamage;
	}
}
