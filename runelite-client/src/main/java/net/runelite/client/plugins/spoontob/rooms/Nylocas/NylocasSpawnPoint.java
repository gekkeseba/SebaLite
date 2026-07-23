package net.runelite.client.plugins.spoontob.rooms.Nylocas;

import java.util.HashMap;
import net.runelite.api.Point;

enum NylocasSpawnPoint
{
	WEST_NORTH(new Point(17, 25)),
	WEST_SOUTH(new Point(17, 24)),
	SOUTH_WEST(new Point(31, 9)),
	SOUTH_EAST(new Point(32, 9)),
	EAST_SOUTH(new Point(46, 24)),
	EAST_NORTH(new Point(46, 25)),
	EAST_BIG(new Point(47, 25)),
	WEST_BIG(new Point(18, 25)),
	SOUTH_BIG(new Point(32, 10));

	private static final HashMap<Point, NylocasSpawnPoint> lookupMap = new HashMap<>();

	static
	{
		for (NylocasSpawnPoint v : values())
		{
			lookupMap.put(v.getPoint(), v);
		}
	}

	private final Point point;

	NylocasSpawnPoint(Point point)
	{
		this.point = point;
	}

	public Point getPoint()
	{
		return this.point;
	}

	public static HashMap<Point, NylocasSpawnPoint> getLookupMap()
	{
		return lookupMap;
	}
}
