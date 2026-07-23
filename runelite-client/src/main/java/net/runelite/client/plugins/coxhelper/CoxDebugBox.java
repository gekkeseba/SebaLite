package net.runelite.client.plugins.coxhelper;

import java.awt.Dimension;
import java.awt.Graphics2D;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.PanelComponent;

@Singleton
public class CoxDebugBox extends Overlay
{
	private final Client client;
	private final CoxPlugin plugin;
	private final CoxConfig config;
	private final Olm olm;
	private final PanelComponent panelComponent = new PanelComponent();

	@Inject
	CoxDebugBox(Client client, CoxPlugin plugin, CoxConfig config, Olm olm)
	{
		this.client = client;
		this.plugin = plugin;
		this.config = config;
		this.olm = olm;
		this.setPosition(OverlayPosition.BOTTOM_LEFT);
		this.setPriority(PRIORITY_HIGH);
		this.panelComponent.setPreferredSize(new Dimension(270, 0));
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (!this.config.olmDebug() || !plugin.inRaid())
		{
			return null;
		}

		this.panelComponent.getChildren().clear();
		TableComponent tableComponent = new TableComponent();
		tableComponent.setColumnAlignments(TableAlignment.LEFT, TableAlignment.RIGHT);
		tableComponent.addRow("ticks", String.valueOf(client.getTickCount()));
		tableComponent.addRow("active", String.valueOf(this.olm.isActive()));
		tableComponent.addRow("handAnim", String.valueOf(this.olm.getHandAnimation()));
		tableComponent.addRow("headAnim", String.valueOf(this.olm.getHeadAnimation()));
		tableComponent.addRow("firstPhase", String.valueOf(this.olm.isFirstPhase()));
		tableComponent.addRow("finalPhase", String.valueOf(this.olm.isFinalPhase()));
		tableComponent.addRow("attackTicks", String.valueOf(this.olm.getTicksUntilNextAttack()));
		tableComponent.addRow("attackCycle", String.valueOf(this.olm.getAttackCycle()));
		tableComponent.addRow("specialCycle", String.valueOf(this.olm.getSpecialCycle()));
		tableComponent.addRow("portalTicks", String.valueOf(this.olm.getPortalTicks()));
		tableComponent.addRow("handCrippled", String.valueOf(this.olm.isCrippled()));
		tableComponent.addRow("crippleTicks", String.valueOf(this.olm.getCrippleTicks()));

		tableComponent.addRow("tektonActive", String.valueOf(this.plugin.isTektonActive()));
		tableComponent.addRow("tektonAttackTicks", String.valueOf(this.plugin.getTektonAttackTicks()));
		for (NPCContainer npcs : this.plugin.getNpcContainers().values())
		{
			switch (npcs.getNpc().getId())
			{
				case net.runelite.api.gameval.NpcID.RAIDS_TEKTON_WAITING:
				case net.runelite.api.gameval.NpcID.RAIDS_TEKTON_WALKING_STANDARD:
				case net.runelite.api.gameval.NpcID.RAIDS_TEKTON_FIGHTING_STANDARD:
				case net.runelite.api.gameval.NpcID.RAIDS_TEKTON_HAMMERING:
				case net.runelite.api.gameval.NpcID.RAIDS_TEKTON_WALKING_ENRAGED:
				case net.runelite.api.gameval.NpcID.RAIDS_TEKTON_FIGHTING_ENRAGED:
					tableComponent.addRow("tektonNpcId", String.valueOf(npcs.getNpc().getId()));
					tableComponent.addRow("tektonAnim", String.valueOf(npcs.getNpc().getAnimation()));
					tableComponent.addRow("tektonTicksUntilAttack", String.valueOf(npcs.getTicksUntilAttack()));
					break;
			}
		}

		this.panelComponent.getChildren().add(tableComponent);

		return this.panelComponent.render(graphics);
	}
}
