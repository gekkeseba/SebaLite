package net.runelite.client.plugins.aoewarnings;

import java.time.Instant;
import net.runelite.api.GameObject;
import net.runelite.api.coords.WorldPoint;

class CrystalBomb
{
	private final GameObject gameObject;
	private final Instant plantedOn;
	private final int objectId;
	private final int tickStarted;
	private final WorldPoint worldLocation;
	private Instant lastClockUpdate;

	CrystalBomb(GameObject gameObject, int startTick)
	{
		this.gameObject = gameObject;
		this.objectId = gameObject.getId();
		this.plantedOn = Instant.now();
		// Initialized here, not left null until the first bombClockUpdate() tick,
		// so BombOverlay can safely read it on the very first render after spawn.
		this.lastClockUpdate = this.plantedOn;
		this.worldLocation = gameObject.getWorldLocation();
		this.tickStarted = startTick;
	}

	void bombClockUpdate()
	{
		this.lastClockUpdate = Instant.now();
	}

	GameObject getGameObject()
	{
		return this.gameObject;
	}

	Instant getPlantedOn()
	{
		return this.plantedOn;
	}

	Instant getLastClockUpdate()
	{
		return this.lastClockUpdate;
	}

	int getObjectId()
	{
		return this.objectId;
	}

	int getTickStarted()
	{
		return this.tickStarted;
	}

	WorldPoint getWorldLocation()
	{
		return this.worldLocation;
	}
}
