package net.runelite.client.plugins.spoontob.rooms.Verzik;

import net.runelite.api.NPC;
import net.runelite.api.coords.WorldPoint;

/**
 * Identifies which of several spawned Verzik P3 tornadoes is the one chasing this player, by
 * checking whether the tornado's distance to the player's *previous* position changed between
 * ticks - a nado tracking you gets meaningfully closer/farther as you move; nados assigned to
 * other players don't correlate with your movement. Used to filter down a multi-nado field to
 * "mine" for the personal-nado overlay/hide-others features, not to predict a landing tile -
 * tornadoes continuously chase rather than following a fixed ballistic path.
 */
public class TornadoTracker
{
	private final NPC npc;
	private WorldPoint prevLoc;

	TornadoTracker(NPC npc)
	{
		this.npc = npc;
		this.prevLoc = null;
	}

	NPC getNpc()
	{
		return this.npc;
	}

	void setPrevLoc(WorldPoint prevLoc)
	{
		this.prevLoc = prevLoc;
	}

	public int checkMovement(WorldPoint playerWp, WorldPoint nadoWp)
	{
		if (this.prevLoc == null || nadoWp == null || this.prevLoc.distanceTo(nadoWp) == 0)
		{
			return -1;
		}
		return playerWp.distanceTo(nadoWp) - playerWp.distanceTo(this.prevLoc);
	}
}
