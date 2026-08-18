package net.runelite.client.plugins.spoonnightmare;

import net.runelite.api.NPC;

public class TotemInfo
{
	private NPC npc;
	private int ratio;

	TotemInfo(NPC npc, int ratio)
	{
		this.npc = npc;
		this.ratio = ratio;
	}

	NPC getNpc()
	{
		return this.npc;
	}

	void setNpc(NPC npc)
	{
		this.npc = npc;
	}

	int getRatio()
	{
		return this.ratio;
	}

	void setRatio(int ratio)
	{
		this.ratio = ratio;
	}
}
