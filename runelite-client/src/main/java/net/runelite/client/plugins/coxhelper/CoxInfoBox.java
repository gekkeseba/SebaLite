package net.runelite.client.plugins.coxhelper;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.api.Prayer;
import net.runelite.api.gameval.NpcID;
// gameval SpriteID's prayer/BA-icon entries are unnamed generated constants (Prayeron._12, BarbassaultIcons._1);
// the deprecated top-level SpriteID keeps the readable names and the values are unchanged.
import net.runelite.api.SpriteID;
import net.runelite.client.game.SpriteManager;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.ComponentConstants;
import net.runelite.client.ui.overlay.components.InfoBoxComponent;
import net.runelite.client.ui.overlay.components.PanelComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;
import net.runelite.client.util.ColorUtil;
import net.runelite.client.util.ImageUtil;

@Singleton
public class CoxInfoBox extends Overlay
{
	private static final Color NOT_ACTIVATED_BACKGROUND_COLOR = new Color(150, 0, 0, 150);
	private final CoxPlugin plugin;
	private final CoxConfig config;
	private final Client client;
	private final Olm olm;
	private final SpriteManager spriteManager;
	private final PanelComponent prayAgainstPanel = new PanelComponent();
	private final PanelComponent panelComponent = new PanelComponent();

	@Inject
	CoxInfoBox(CoxPlugin plugin, CoxConfig config, Client client, Olm olm, SpriteManager spriteManager)
	{
		this.plugin = plugin;
		this.config = config;
		this.client = client;
		this.olm = olm;
		this.spriteManager = spriteManager;
		this.setPosition(OverlayPosition.BOTTOM_RIGHT);
		this.setPriority(PRIORITY_HIGH);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		this.panelComponent.getChildren().clear();
		if (this.plugin.inRaid())
		{
			this.prayAgainstPanel.getChildren().clear();

			final Prayer prayer = this.olm.getPrayer();

			if (System.currentTimeMillis() < this.olm.getLastPrayTime() + 120000 && prayer != null && this.config.prayAgainstOlm())
			{
				final int scale = this.config.prayAgainstOlmSize();
				InfoBoxComponent prayComponent = new InfoBoxComponent();
				BufferedImage prayImg = ImageUtil.resizeImage(
					this.getPrayerImage(this.olm.getPrayer()), scale, scale
				);
				prayComponent.setImage(prayImg);
				prayComponent.setColor(Color.WHITE);
				prayComponent.setBackgroundColor(this.client.isPrayerActive(prayer)
					? ComponentConstants.STANDARD_BACKGROUND_COLOR
					: NOT_ACTIVATED_BACKGROUND_COLOR
				);
				prayComponent.setPreferredSize(new Dimension(scale + 4, scale + 4));
				this.prayAgainstPanel.getChildren().add(prayComponent);

				this.prayAgainstPanel.setPreferredSize(new Dimension(scale + 4, scale + 4));
				this.prayAgainstPanel.setBorder(new Rectangle(0, 0, 0, 0));
				return this.prayAgainstPanel.render(graphics);
			}
			else
			{
				this.olm.setPrayer(null);
			}

			if (this.config.vangHealth() && this.plugin.getVanguards() > 0)
			{
				this.panelComponent.getChildren().add(TitleComponent.builder()
					.text("Vanguards")
					.color(Color.pink)
					.build());

				TableComponent tableComponent = new TableComponent();
				tableComponent.setColumnAlignments(TableAlignment.LEFT, TableAlignment.RIGHT);
				for (NPCContainer npcs : this.plugin.getNpcContainers().values())
				{
					float percent = (float) npcs.getNpc().getHealthRatio() / npcs.getNpc().getHealthScale() * 100;
					switch (npcs.getNpc().getId())
					{
						case NpcID.RAIDS_VANGUARD_MELEE:
							tableComponent.addRow(ColorUtil.prependColorTag("Melee", npcs.getAttackStyle().getColor()),
								Integer.toString((int) percent));
							break;
						case NpcID.RAIDS_VANGUARD_RANGED:
							tableComponent.addRow(ColorUtil.prependColorTag("Range", npcs.getAttackStyle().getColor()),
								Integer.toString((int) percent));
							break;
						case NpcID.RAIDS_VANGUARD_MAGIC:
							tableComponent.addRow(ColorUtil.prependColorTag("Mage", npcs.getAttackStyle().getColor()),
								Integer.toString((int) percent));
							break;
					}
				}

				this.panelComponent.getChildren().add(tableComponent);

				return this.panelComponent.render(graphics);
			}
		}
		if (this.client.getLocalPlayer().getWorldLocation().getRegionID() == 4919)
		{
			this.olm.setPrayer(null);
		}
		return null;
	}

	private BufferedImage getPrayerImage(Prayer prayer)
	{
		switch (prayer)
		{
			case PROTECT_FROM_MAGIC:
				return this.spriteManager.getSprite(SpriteID.PRAYER_PROTECT_FROM_MAGIC, 0);
			case PROTECT_FROM_MELEE:
				return this.spriteManager.getSprite(SpriteID.PRAYER_PROTECT_FROM_MELEE, 0);
			case PROTECT_FROM_MISSILES:
				return this.spriteManager.getSprite(SpriteID.PRAYER_PROTECT_FROM_MISSILES, 0);
			default:
				return this.spriteManager.getSprite(SpriteID.BARBARIAN_ASSAULT_EAR_ICON, 0);
		}
	}
}
