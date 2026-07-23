package net.runelite.client.plugins.spoontob.rooms.Nylocas;

import java.util.Objects;

class NyloNPC
{
	private final NylocasType nyloType;
	private final NylocasSpawnPoint spawnPoint;
	private boolean aggressive = false;

	NyloNPC(NylocasType nyloType, NylocasSpawnPoint nylocasSpawnPoint)
	{
		this.nyloType = nyloType;
		this.spawnPoint = nylocasSpawnPoint;
	}

	NyloNPC(NylocasType nyloType, NylocasSpawnPoint nylocasSpawnPoint, boolean aggressive)
	{
		this(nyloType, nylocasSpawnPoint);
		this.aggressive = aggressive;
	}

	public NylocasType getNyloType()
	{
		return this.nyloType;
	}

	public NylocasSpawnPoint getSpawnPoint()
	{
		return this.spawnPoint;
	}

	public boolean isAggressive()
	{
		return this.aggressive;
	}

	@Override
	public String toString()
	{
		return "NyloNPC(nyloType=" + getNyloType() + ", spawnPoint=" + getSpawnPoint() + ", aggressive=" + isAggressive() + ")";
	}

	@Override
	public boolean equals(Object other)
	{
		if (!(other instanceof NyloNPC))
		{
			return false;
		}
		NyloNPC otherNpc = (NyloNPC) other;
		return this.nyloType.equals(otherNpc.getNyloType()) && this.spawnPoint.equals(otherNpc.getSpawnPoint());
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(this.nyloType, this.spawnPoint);
	}
}
