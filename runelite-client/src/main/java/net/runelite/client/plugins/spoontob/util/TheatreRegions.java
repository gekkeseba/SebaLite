package net.runelite.client.plugins.spoontob.util;

import java.util.Objects;
import net.runelite.api.Client;
import net.runelite.api.Player;
import net.runelite.api.coords.WorldPoint;

public enum TheatreRegions
{
	MAIDEN(12613),
	BLOAT(13125),
	NYLOCAS(13122),
	SOTETSEG(13123),
	SOTETSEG_MAZE(13379),
	XARPUS(12612),
	VERZIK(12611),
	LOOT_ROOM(12867);

	private final int regionId;

	TheatreRegions(int regionId)
	{
		this.regionId = regionId;
	}

	public int getRegionId()
	{
		return this.regionId;
	}

	public static boolean inRegion(Client client, TheatreRegions tobRegion)
	{
		return inRegion(client, tobRegion.getRegionId());
	}

	public static boolean inRegion(Client client, int regionId)
	{
		return Objects.equals(getCurrentRegionID(client), regionId);
	}

	public static int getCurrentRegionID(Client client)
	{
		if (!client.getTopLevelWorldView().isInstance())
		{
			return -1;
		}
		Player localPlayer = client.getLocalPlayer();
		if (localPlayer == null)
		{
			return -1;
		}
		WorldPoint wp = WorldPoint.fromLocalInstance(client, localPlayer.getLocalLocation());
		return wp == null ? -1 : wp.getRegionID();
	}
}
