package net.runelite.client.plugins.aoewarnings;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.ArrayUtils;

public enum AoeProjectileInfo
{
	LIZARDMAN_SHAMAN_AOE(1293, 5),
	CRAZY_ARCHAEOLOGIST_AOE(1260, 3),
	ICE_DEMON_RANGED_AOE(1324, 3),
	ICE_DEMON_ICE_BARRAGE_AOE(366, 3),
	VASA_AWAKEN_AOE(1327, 3),
	VASA_RANGED_AOE(1329, 3),
	TEKTON_METEOR_AOE(660, 3),
	VORKATH_BOMB(1481, 3),
	VORKATH_POISON_POOL(1483, 1),
	VORKATH_SPAWN(1484, 1),
	VORKATH_TICK_FIRE(1482, 1),
	GALVEK_MINE(1495, 3),
	GALVEK_BOMB(1491, 3),
	DAWN_FREEZE(1445, 3, AoeWarningPlugin.GROTESQUE_GUARDIANS_REGION),
	// Dusk's ceiling collapse and Verzik's phase-1 rock fall reuse the same
	// projectile id (1435) in the game's own data. They're disambiguated by
	// which region is loaded when the projectile spawns - see getById below.
	DUSK_CEILING(1435, 3, AoeWarningPlugin.GROTESQUE_GUARDIANS_REGION),
	VETION_LIGHTNING(280, 1),
	CHAOS_FANATIC(551, 1),
	JUSTICIAR_LEASH(1515, 1),
	MAGE_ARENA_BOSS_FREEZE(368, 1),
	CORPOREAL_BEAST(315, 1),
	CORPOREAL_BEAST_DARK_CORE(319, 3),
	OLM_FALLING_CRYSTAL(1357, 3),
	OLM_BURNING(1349, 1),
	OLM_FALLING_CRYSTAL_TRAIL(1352, 1),
	OLM_ACID_TRAIL(1354, 1),
	OLM_FIRE_LINE(1347, 1),
	WINTERTODT_SNOW_FALL(1310, 3),
	XARPUS_POISON_AOE(1555, 1),
	ADDY_DRAG_POISON(1486, 1),
	DRAKE_BREATH(1637, 1),
	CERB_FIRE(1247, 2),
	DEMONIC_GORILLA_BOULDER(856, 1),
	MARBLE_GARGOYLE_AOE(1453, 1),
	VERZIK_PURPLE_SPAWN(1586, 3),
	VERZIK_P1_ROCKS(1435, 1, AoeWarningPlugin.VERZIK_REGION);

	private static final Map<Integer, List<AoeProjectileInfo>> map = new HashMap<>();

	private final int id;
	private final int aoeSize;
	private final int region; // 0 = not region-restricted

	AoeProjectileInfo(int id, int aoeSize)
	{
		this(id, aoeSize, 0);
	}

	AoeProjectileInfo(int id, int aoeSize, int region)
	{
		this.id = id;
		this.aoeSize = aoeSize;
		this.region = region;
	}

	/**
	 * Resolves a projectile id to its AoE definition. When multiple mechanics share the
	 * same id (currently only Dusk/Verzik's rock attacks, both 1435), the currently loaded
	 * regions disambiguate which one applies.
	 */
	public static AoeProjectileInfo getById(int id, int[] loadedRegions)
	{
		List<AoeProjectileInfo> candidates = map.get(id);
		if (candidates == null || candidates.isEmpty())
		{
			return null;
		}

		if (candidates.size() == 1)
		{
			return candidates.get(0);
		}

		for (AoeProjectileInfo candidate : candidates)
		{
			if (candidate.region != 0 && ArrayUtils.contains(loadedRegions, candidate.region))
			{
				return candidate;
			}
		}

		return candidates.get(0);
	}

	public int getId()
	{
		return this.id;
	}

	public int getAoeSize()
	{
		return this.aoeSize;
	}

	static
	{
		for (AoeProjectileInfo aoe : values())
		{
			map.computeIfAbsent(aoe.id, k -> new ArrayList<>()).add(aoe);
		}
	}
}
