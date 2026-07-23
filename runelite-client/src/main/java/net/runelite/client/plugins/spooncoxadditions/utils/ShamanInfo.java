package net.runelite.client.plugins.spooncoxadditions.utils;

import net.runelite.api.NPC;
import net.runelite.api.coords.LocalPoint;

public class ShamanInfo
{
	public NPC shaman;
	public LocalPoint interactingLoc;
	public boolean jumping;

	public ShamanInfo(NPC shaman, LocalPoint interactingLoc, boolean jumping)
	{
		this.shaman = shaman;
		this.interactingLoc = interactingLoc;
		this.jumping = jumping;
	}
}
