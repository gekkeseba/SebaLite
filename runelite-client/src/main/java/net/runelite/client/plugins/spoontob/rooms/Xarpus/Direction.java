package net.runelite.client.plugins.spoontob.rooms.Xarpus;

public enum Direction
{
	NORTH("N"),
	EAST("E"),
	SOUTH("S"),
	WEST("W"),
	NORTHEAST("NE"),
	NORTHWEST("NW"),
	SOUTHEAST("SE"),
	SOUTHWEST("SW");

	private final String dirName;

	Direction(String dirName)
	{
		this.dirName = dirName;
	}

	public String getDirName()
	{
		return this.dirName;
	}

	public static Direction getPreciseDirection(int angle)
	{
		int ordinalDirection = (int) Math.round(angle / 256.0D) % 8;
		switch (ordinalDirection)
		{
			case 0:
				return SOUTH;
			case 1:
				return SOUTHWEST;
			case 2:
				return WEST;
			case 3:
				return NORTHWEST;
			case 4:
				return NORTH;
			case 5:
				return NORTHEAST;
			case 6:
				return EAST;
			case 7:
				return SOUTHEAST;
			default:
				throw new IllegalStateException();
		}
	}
}
