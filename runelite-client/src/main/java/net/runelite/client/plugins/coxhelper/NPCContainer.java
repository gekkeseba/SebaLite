package net.runelite.client.plugins.coxhelper;

import java.awt.Color;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import net.runelite.api.NPC;
import net.runelite.api.NPCComposition;

@Getter(AccessLevel.PACKAGE)
class NPCContainer
{
	private final NPC npc;
	private int npcSize;
	@Setter(AccessLevel.PACKAGE)
	private int ticksUntilAttack;
	@Setter(AccessLevel.PACKAGE)
	private Attackstyle attackStyle;

	NPCContainer(NPC npc)
	{
		this.npc = npc;
		this.ticksUntilAttack = 0;
		this.attackStyle = Attackstyle.UNKNOWN;

		final NPCComposition composition = npc.getTransformedComposition();
		if (composition != null)
		{
			this.npcSize = composition.getSize();
		}
	}

	@AllArgsConstructor
	@Getter(AccessLevel.PACKAGE)
	public enum Attackstyle
	{
		MAGE("Mage", Color.CYAN),
		RANGE("Range", Color.GREEN),
		MELEE("Melee", Color.RED),
		UNKNOWN("Unknown", Color.WHITE);

		private final String name;
		private final Color color;
	}
}
