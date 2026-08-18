package net.runelite.client.plugins.spoonnightmare;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.Prayer;
import net.runelite.client.game.SpriteManager;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayPriority;
import net.runelite.client.ui.overlay.components.InfoBoxComponent;
import net.runelite.client.ui.overlay.components.PanelComponent;

public class NightmarePrayerOverlay extends Overlay
{
	private final Client client;
	private final SpoonNightmarePlugin plugin;
	private final SpoonNightmareConfig config;
	private final SpriteManager spriteManager;
	private final PanelComponent panelComponent = new PanelComponent();

	@Inject
	NightmarePrayerOverlay(Client client, SpoonNightmarePlugin plugin, SpoonNightmareConfig config, SpriteManager spriteManager)
	{
		this.client = client;
		this.plugin = plugin;
		this.config = config;
		this.spriteManager = spriteManager;
		setPriority(OverlayPriority.HIGH);
		setPosition(OverlayPosition.ABOVE_CHATBOX_RIGHT);
	}

	public Dimension render(Graphics2D graphics)
	{
		this.panelComponent.getChildren().clear();
		if (this.config.prayerHelper() && this.plugin.isActiveFight() && this.plugin.getNightmareNpc() != null && !this.plugin.getCorrectPray().isEmpty())
		{
			InfoBoxComponent prayComponent = new InfoBoxComponent();
			Prayer prayer;
			boolean curse = this.plugin.isCursePhase();
			switch (this.plugin.getCorrectPray())
			{
				case "magic":
					if (curse)
					{
						prayComponent.setImage(this.spriteManager.getSprite(NightmareAttack.MELEE_ATTACK.getSpriteId(), 0));
						prayer = Prayer.PROTECT_FROM_MELEE;
					}
					else
					{
						prayComponent.setImage(this.spriteManager.getSprite(NightmareAttack.MAGIC_ATTACK.getSpriteId(), 0));
						prayer = Prayer.PROTECT_FROM_MAGIC;
					}
					break;
				case "missiles":
					if (curse)
					{
						prayComponent.setImage(this.spriteManager.getSprite(NightmareAttack.MAGIC_ATTACK.getSpriteId(), 0));
						prayer = Prayer.PROTECT_FROM_MAGIC;
					}
					else
					{
						prayComponent.setImage(this.spriteManager.getSprite(NightmareAttack.RANGED_ATTACK.getSpriteId(), 0));
						prayer = Prayer.PROTECT_FROM_MISSILES;
					}
					break;
				case "melee":
					if (curse)
					{
						prayComponent.setImage(this.spriteManager.getSprite(NightmareAttack.RANGED_ATTACK.getSpriteId(), 0));
						prayer = Prayer.PROTECT_FROM_MISSILES;
					}
					else
					{
						prayComponent.setImage(this.spriteManager.getSprite(NightmareAttack.MELEE_ATTACK.getSpriteId(), 0));
						prayer = Prayer.PROTECT_FROM_MELEE;
					}
					break;
				default:
					return this.panelComponent.render(graphics);
			}
			prayComponent.setBackgroundColor(this.client.isPrayerActive(prayer) ? new Color(0, 255, 0, 25) : new Color(255, 0, 0, 25));
			prayComponent.setPreferredSize(new Dimension(40, 40));
			this.panelComponent.getChildren().add(prayComponent);
			this.panelComponent.setPreferredSize(new Dimension(40, 0));
			this.panelComponent.setBorder(new Rectangle(0, 0, 0, 0));
		}
		return this.panelComponent.render(graphics);
	}
}
