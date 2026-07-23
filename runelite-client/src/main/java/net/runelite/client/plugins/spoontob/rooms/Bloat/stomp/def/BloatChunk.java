package net.runelite.client.plugins.spoontob.rooms.Bloat.stomp.def;

import java.util.EnumSet;

public enum BloatChunk
{
	NW("Northwest", 411, 556),
	NE("Northeast", 412, 556),
	SW("Southwest", 411, 555),
	SE("Southeast", 412, 555),
	UNKNOWN("Unknown", 0, 0);

	private final String name;
	private final int zone;

	BloatChunk(String name, int x, int y)
	{
		this.name = name;
		this.zone = (x & 0x3FF) << 14 | (y & 0x3FF) << 3;
	}

	public static BloatChunk getOccupiedChunk(int chunk)
	{
		if (chunk == -1)
		{
			return UNKNOWN;
		}
		return EnumSet.allOf(BloatChunk.class).stream().filter(c -> c.zone == chunk).findFirst().orElse(UNKNOWN);
	}

	@Override
	public String toString()
	{
		return this.name;
	}
}
