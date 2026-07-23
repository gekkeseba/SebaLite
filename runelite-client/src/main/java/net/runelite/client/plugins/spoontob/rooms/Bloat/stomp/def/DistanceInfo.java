package net.runelite.client.plugins.spoontob.rooms.Bloat.stomp.def;

import java.util.Objects;

public final class DistanceInfo
{
	private static final int MAX = 7;
	private static final int MIN = 4;

	private final int distance;

	public DistanceInfo(int distance)
	{
		this.distance = distance;
	}

	public int getDistance()
	{
		return this.distance;
	}

	public boolean isCorner()
	{
		return this.distance >= MAX || this.distance <= MIN;
	}

	public boolean shouldModifyCorner(BloatPath path)
	{
		boolean mod = (path == BloatPath.N_PATH || path == BloatPath.S_PATH) && (this.distance == 8 || this.distance == 3);
		return mod || this.distance == 7 || this.distance == 4;
	}

	public byte getCornerIndex(boolean isClockwise)
	{
		int cidx = (this.distance >= MAX) ? 0 : 1;
		if (!isClockwise)
		{
			cidx ^= 1;
		}
		return (byte) cidx;
	}

	public boolean isSideMin()
	{
		return this.distance == 6;
	}

	public boolean isSideMax()
	{
		return this.distance == 5;
	}

	@Override
	public boolean equals(Object other)
	{
		return other instanceof DistanceInfo && this.distance == ((DistanceInfo) other).distance;
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(this.distance);
	}
}
