package net.runelite.client.plugins.spoontob.rooms.Nylocas;

import com.google.common.collect.ImmutableSet;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.api.GameState;
import net.runelite.api.MenuEntry;
import net.runelite.api.NPC;
import net.runelite.api.Point;
import net.runelite.api.Scene;
import net.runelite.api.Tile;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.AnimationChanged;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.ClientTick;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.MenuEntryAdded;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.events.NpcChanged;
import net.runelite.api.events.NpcDespawned;
import net.runelite.api.events.NpcSpawned;
import net.runelite.api.events.VarbitChanged;
import net.runelite.api.gameval.NpcID;
import net.runelite.api.gameval.ObjectID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.api.kit.KitType;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.spoontob.Room;
import net.runelite.client.plugins.spoontob.SpoonTobConfig;
import net.runelite.client.plugins.spoontob.SpoonTobPlugin;
import net.runelite.client.plugins.spoontob.util.TheatreRegions;
import net.runelite.client.plugins.spoontob.util.WeaponMap;
import net.runelite.client.plugins.spoontob.util.WeaponStyle;
import org.apache.commons.lang3.ObjectUtils;

public class Nylocas extends Room
{
	@Inject
	private Client client;

	@Inject
	private NylocasOverlay nylocasOverlay;

	@Inject
	public NylocasAliveCounterOverlay nylocasAliveCounterOverlay;

	@Inject
	private NyloWaveSpawnInfobox waveSpawnInfobox;

	protected static final Set<Integer> NYLO_BOSS_IDS = ImmutableSet.of(
		NpcID.NYLOCAS_BOSS_MELEE, NpcID.NYLOCAS_BOSS_MAGIC, NpcID.NYLOCAS_BOSS_RANGED,
		NpcID.NYLOCAS_BOSS_MELEE_STORY, NpcID.NYLOCAS_BOSS_MAGIC_STORY, NpcID.NYLOCAS_BOSS_RANGED_STORY,
		NpcID.NYLOCAS_BOSS_MELEE_HARD, NpcID.NYLOCAS_BOSS_MAGIC_HARD, NpcID.NYLOCAS_BOSS_RANGED_HARD);

	protected static final Set<Integer> NYLO_DEMI_BOSS_IDS = ImmutableSet.of(
		NpcID.NYLOCAS_MINIBOSS_MELEE_HARD, NpcID.NYLOCAS_MINIBOSS_MAGIC_HARD, NpcID.NYLOCAS_MINIBOSS_RANGED_HARD);

	private static final Set<Integer> NYLO_SPAWNING_BOSS_IDS = ImmutableSet.of(
		NpcID.NYLOCAS_BOSS_SPAWNING, NpcID.NYLOCAS_BOSS_MELEE, NpcID.NYLOCAS_BOSS_MAGIC, NpcID.NYLOCAS_BOSS_RANGED,
		NpcID.NYLOCAS_BOSS_SPAWNING_STORY, NpcID.NYLOCAS_BOSS_MELEE_STORY, NpcID.NYLOCAS_BOSS_MAGIC_STORY, NpcID.NYLOCAS_BOSS_RANGED_STORY,
		NpcID.NYLOCAS_BOSS_SPAWNING_HARD, NpcID.NYLOCAS_BOSS_MELEE_HARD, NpcID.NYLOCAS_BOSS_MAGIC_HARD, NpcID.NYLOCAS_BOSS_RANGED_HARD);

	private static final Set<Integer> NYLO_PILLAR_IDS = ImmutableSet.of(
		NpcID.TOB_NYLOCAS_SUPPORT, NpcID.TOB_NYLOCAS_SUPPORT_STORY, NpcID.TOB_NYLOCAS_SUPPORT_HARD);

	private static final Set<Integer> NYLO_TYPE_IDS = ImmutableSet.of(
		NpcID.TOB_NYLOCAS_INCOMING_MELEE, NpcID.TOB_NYLOCAS_INCOMING_RANGED, NpcID.TOB_NYLOCAS_INCOMING_MAGIC,
		NpcID.TOB_NYLOCAS_BIG_INCOMING_MELEE, NpcID.TOB_NYLOCAS_BIG_INCOMING_RANGED, NpcID.TOB_NYLOCAS_BIG_INCOMING_MAGIC,
		NpcID.TOB_NYLOCAS_FIGHTING_MELEE, NpcID.TOB_NYLOCAS_FIGHTING_RANGED, NpcID.TOB_NYLOCAS_FIGHTING_MAGIC,
		NpcID.TOB_NYLOCAS_BIG_FIGHTING_MELEE, NpcID.TOB_NYLOCAS_BIG_FIGHTING_RANGED, NpcID.TOB_NYLOCAS_BIG_FIGHTING_MAGIC,
		NpcID.TOB_NYLOCAS_INCOMING_MELEE_STORY, NpcID.TOB_NYLOCAS_INCOMING_RANGED_STORY, NpcID.TOB_NYLOCAS_INCOMING_MAGIC_STORY,
		NpcID.TOB_NYLOCAS_BIG_INCOMING_MELEE_STORY, NpcID.TOB_NYLOCAS_BIG_INCOMING_RANGED_STORY, NpcID.TOB_NYLOCAS_BIG_INCOMING_MAGIC_STORY,
		NpcID.TOB_NYLOCAS_FIGHTING_MELEE_STORY, NpcID.TOB_NYLOCAS_FIGHTING_RANGED_STORY, NpcID.TOB_NYLOCAS_FIGHTING_MAGIC_STORY,
		NpcID.TOB_NYLOCAS_BIG_FIGHTING_MELEE_STORY, NpcID.TOB_NYLOCAS_BIG_FIGHTING_RANGED_STORY, NpcID.TOB_NYLOCAS_BIG_FIGHTING_MAGIC_STORY,
		NpcID.TOB_NYLOCAS_INCOMING_MELEE_HARD, NpcID.TOB_NYLOCAS_INCOMING_RANGED_HARD, NpcID.TOB_NYLOCAS_INCOMING_MAGIC_HARD,
		NpcID.TOB_NYLOCAS_BIG_INCOMING_MELEE_HARD, NpcID.TOB_NYLOCAS_BIG_INCOMING_RANGED_HARD, NpcID.TOB_NYLOCAS_BIG_INCOMING_MAGIC_HARD,
		NpcID.TOB_NYLOCAS_FIGHTING_MELEE_HARD, NpcID.TOB_NYLOCAS_FIGHTING_RANGED_HARD, NpcID.TOB_NYLOCAS_FIGHTING_MAGIC_HARD,
		NpcID.TOB_NYLOCAS_BIG_FIGHTING_MELEE_HARD, NpcID.TOB_NYLOCAS_BIG_FIGHTING_RANGED_HARD, NpcID.TOB_NYLOCAS_BIG_FIGHTING_MAGIC_HARD,
		NpcID.NYLOCAS_MINIBOSS_MELEE_HARD, NpcID.NYLOCAS_MINIBOSS_MAGIC_HARD, NpcID.NYLOCAS_MINIBOSS_RANGED_HARD);

	protected static final Set<Integer> MELEE_IDS = ImmutableSet.of(
		NpcID.TOB_NYLOCAS_INCOMING_MELEE, NpcID.TOB_NYLOCAS_BIG_INCOMING_MELEE, NpcID.TOB_NYLOCAS_FIGHTING_MELEE, NpcID.TOB_NYLOCAS_BIG_FIGHTING_MELEE,
		NpcID.TOB_NYLOCAS_INCOMING_MELEE_STORY, NpcID.TOB_NYLOCAS_BIG_INCOMING_MELEE_STORY, NpcID.TOB_NYLOCAS_FIGHTING_MELEE_STORY, NpcID.TOB_NYLOCAS_BIG_FIGHTING_MELEE_STORY,
		NpcID.TOB_NYLOCAS_INCOMING_MELEE_HARD, NpcID.TOB_NYLOCAS_BIG_INCOMING_MELEE_HARD, NpcID.TOB_NYLOCAS_FIGHTING_MELEE_HARD, NpcID.TOB_NYLOCAS_BIG_FIGHTING_MELEE_HARD);

	protected static final Set<Integer> RANGE_IDS = ImmutableSet.of(
		NpcID.TOB_NYLOCAS_INCOMING_RANGED, NpcID.TOB_NYLOCAS_BIG_INCOMING_RANGED, NpcID.TOB_NYLOCAS_FIGHTING_RANGED, NpcID.TOB_NYLOCAS_BIG_FIGHTING_RANGED,
		NpcID.TOB_NYLOCAS_INCOMING_RANGED_STORY, NpcID.TOB_NYLOCAS_BIG_INCOMING_RANGED_STORY, NpcID.TOB_NYLOCAS_FIGHTING_RANGED_STORY, NpcID.TOB_NYLOCAS_BIG_FIGHTING_RANGED_STORY,
		NpcID.TOB_NYLOCAS_INCOMING_RANGED_HARD, NpcID.TOB_NYLOCAS_BIG_INCOMING_RANGED_HARD, NpcID.TOB_NYLOCAS_FIGHTING_RANGED_HARD, NpcID.TOB_NYLOCAS_BIG_FIGHTING_RANGED_HARD);

	protected static final Set<Integer> MAGIC_IDS = ImmutableSet.of(
		NpcID.TOB_NYLOCAS_INCOMING_MAGIC, NpcID.TOB_NYLOCAS_BIG_INCOMING_MAGIC, NpcID.TOB_NYLOCAS_FIGHTING_MAGIC, NpcID.TOB_NYLOCAS_BIG_FIGHTING_MAGIC,
		NpcID.TOB_NYLOCAS_INCOMING_MAGIC_STORY, NpcID.TOB_NYLOCAS_BIG_INCOMING_MAGIC_STORY, NpcID.TOB_NYLOCAS_FIGHTING_MAGIC_STORY, NpcID.TOB_NYLOCAS_BIG_FIGHTING_MAGIC_STORY,
		NpcID.TOB_NYLOCAS_INCOMING_MAGIC_HARD, NpcID.TOB_NYLOCAS_BIG_INCOMING_MAGIC_HARD, NpcID.TOB_NYLOCAS_FIGHTING_MAGIC_HARD, NpcID.TOB_NYLOCAS_BIG_FIGHTING_MAGIC_HARD);

	private static final Set<Integer> BIG_NYLO_IDS = ImmutableSet.of(
		NpcID.TOB_NYLOCAS_BIG_INCOMING_MELEE, NpcID.TOB_NYLOCAS_BIG_INCOMING_RANGED, NpcID.TOB_NYLOCAS_BIG_INCOMING_MAGIC,
		NpcID.TOB_NYLOCAS_BIG_INCOMING_MELEE_STORY, NpcID.TOB_NYLOCAS_BIG_INCOMING_RANGED_STORY, NpcID.TOB_NYLOCAS_BIG_INCOMING_MAGIC_STORY,
		NpcID.TOB_NYLOCAS_BIG_INCOMING_MELEE_HARD, NpcID.TOB_NYLOCAS_BIG_INCOMING_RANGED_HARD, NpcID.TOB_NYLOCAS_BIG_INCOMING_MAGIC_HARD);

	private static Runnable wave31Callback = null;
	private static Runnable endOfWavesCallback = null;

	public static Runnable getWave31Callback()
	{
		return wave31Callback;
	}

	public static void setWave31Callback(Runnable wave31Callback)
	{
		Nylocas.wave31Callback = wave31Callback;
	}

	public static Runnable getEndOfWavesCallback()
	{
		return endOfWavesCallback;
	}

	public static void setEndOfWavesCallback(Runnable endOfWavesCallback)
	{
		Nylocas.endOfWavesCallback = endOfWavesCallback;
	}

	private boolean nyloActive;
	public int nyloWave = 0;
	private int varbit6447 = -1;
	private Instant nyloWaveStart;

	private final HashMap<NPC, Integer> nylocasPillars = new HashMap<>();
	public final ArrayList<NyloInfo> nylocasNpcs = new ArrayList<>();
	private final HashSet<NPC> aggressiveNylocas = new HashSet<>();
	private final HashMap<NyloNPC, NPC> currentWave = new HashMap<>();
	private int ticksSinceLastWave = 0;

	private int rangeBoss = 0;
	private int mageBoss = 0;
	private int meleeBoss = 0;
	private int rangeSplits = 0;
	private int mageSplits = 0;
	private int meleeSplits = 0;
	private int preRangeSplits = 0;
	private int preMageSplits = 0;
	private int preMeleeSplits = 0;
	private int postRangeSplits = 0;
	private int postMageSplits = 0;
	private int postMeleeSplits = 0;

	private int bossChangeTicks;
	private int lastBossId;
	private NPC nylocasBoss;
	private boolean nyloBossAlive;

	public int weaponId = 0;
	private static final Set<Point> spawnTiles = ImmutableSet.of(new Point(17, 24), new Point(17, 25), new Point(31, 9), new Point(32, 9), new Point(46, 24), new Point(46, 25));

	private final Map<NPC, Integer> splitsMap = new HashMap<>();
	private final Set<NPC> bigNylos = new HashSet<>();
	public boolean showHint;

	public String tobMode = "";
	public boolean minibossAlive = false;
	public NPC nyloMiniboss = null;
	public String nyloBossStyle = "";
	public int waveSpawnTicks = 0;
	public boolean stalledWave = false;

	private boolean setAlive;
	private WeaponStyle weaponStyle;
	private boolean skipTickCheck = false;
	private int stallCheckTicks = 0;

	@Inject
	protected Nylocas(SpoonTobPlugin plugin, SpoonTobConfig config)
	{
		super(plugin, config);
	}

	public void init()
	{
		this.nylocasAliveCounterOverlay.setHidden(!this.config.nyloAlivePanel());
		this.nylocasAliveCounterOverlay.setNyloAlive(0);
		this.nylocasAliveCounterOverlay.setMaxNyloAlive(12);
		this.nyloBossAlive = false;
		this.tobMode = "";
		this.minibossAlive = false;
		this.nyloMiniboss = null;
		this.nyloBossStyle = "";
		this.waveSpawnTicks = 0;
		this.stalledWave = false;
	}

	private void startupNyloOverlay()
	{
		this.overlayManager.add(this.nylocasAliveCounterOverlay);
		this.nylocasAliveCounterOverlay.setHidden(!this.config.nyloAlivePanel());
	}

	private void shutdownNyloOverlay()
	{
		this.overlayManager.remove(this.nylocasAliveCounterOverlay);
		this.nylocasAliveCounterOverlay.setHidden(true);
	}

	public void load()
	{
		this.overlayManager.add(this.nylocasOverlay);
		this.overlayManager.add(this.waveSpawnInfobox);
		this.bossChangeTicks = -1;
		this.lastBossId = -1;
		this.weaponStyle = null;
	}

	public void unload()
	{
		this.overlayManager.remove(this.nylocasOverlay);
		this.overlayManager.remove(this.waveSpawnInfobox);
		shutdownNyloOverlay();
		this.nyloBossAlive = false;
		this.nyloWaveStart = null;
		this.nyloActive = false;
		this.tobMode = "";
		this.minibossAlive = false;
		this.nyloBossStyle = "";
		this.waveSpawnTicks = 0;
		this.stalledWave = false;
		this.weaponStyle = null;
		this.splitsMap.clear();
		this.bigNylos.clear();
	}

	private void resetNylo()
	{
		this.nyloBossAlive = false;
		this.nylocasPillars.clear();
		this.nylocasNpcs.clear();
		this.aggressiveNylocas.clear();
		setNyloWave(0);
		this.currentWave.clear();
		this.bossChangeTicks = -1;
		this.lastBossId = -1;
		this.nylocasBoss = null;
		this.weaponId = 0;
		this.weaponStyle = null;
		this.splitsMap.clear();
		this.bigNylos.clear();
		this.tobMode = "";
		this.minibossAlive = false;
		this.nyloMiniboss = null;
		this.nyloBossStyle = "";
		this.waveSpawnTicks = 0;
		this.stalledWave = false;
	}

	private void setNyloWave(int wave)
	{
		this.nyloWave = wave;
		this.nylocasAliveCounterOverlay.setWave(wave);
		if (wave != 0)
		{
			switch (this.tobMode)
			{
				case "hard":
					this.ticksSinceLastWave = NylocasWave.hmWaves.get(wave).getWaveDelay();
					break;
				case "story":
					this.ticksSinceLastWave = NylocasWave.smWaves.get(wave).getWaveDelay();
					break;
				case "normal":
					this.ticksSinceLastWave = NylocasWave.waves.get(wave).getWaveDelay();
					break;
			}
		}
		if (wave >= 20 && this.nylocasAliveCounterOverlay.getMaxNyloAlive() != 24)
		{
			this.nylocasAliveCounterOverlay.setMaxNyloAlive(24);
		}
		if (wave < 20 && this.nylocasAliveCounterOverlay.getMaxNyloAlive() != 12)
		{
			this.nylocasAliveCounterOverlay.setMaxNyloAlive(12);
		}
		if (wave == 31 && wave31Callback != null)
		{
			wave31Callback.run();
		}
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged change)
	{
		if (!change.getGroup().equals("tobqol v2"))
		{
			return;
		}
		if (change.getKey().equals("nyloAliveCounter"))
		{
			this.nylocasAliveCounterOverlay.setHidden(!this.config.nyloAlivePanel());
		}
		else if (change.getKey().equals("showLowestPillar") && !this.config.showLowestPillar())
		{
			this.client.clearHintArrow();
		}
		else if (change.getKey().equals("hideEggs"))
		{
			applyObjectHiding();
		}
	}

	private void applyObjectHiding()
	{
		if (!isInNyloRegion())
		{
			return;
		}
		if (this.config.hideEggs())
		{
			removeGameObjectsFromScene(ImmutableSet.of(ObjectID.TOB_DUNGEON_NYLOCAS_TWISTY_MULTI, ObjectID.TOB_DUNGEON_NYLOCAS_DEAD_MERC_MULTI, 2739, ObjectID.TOB_NYLOCAS_DEATH_WEB), 0);
		}
	}

	@Subscribe
	public void onNpcSpawned(NpcSpawned npcSpawned)
	{
		NPC npc = npcSpawned.getNpc();
		int id = npc.getId();

		if (NYLO_TYPE_IDS.contains(id))
		{
			onNyloTypeSpawned(npc, id);
		}
		else if (NYLO_SPAWNING_BOSS_IDS.contains(id))
		{
			onNyloBossSpawned(npc, id);
		}
		else if (NYLO_PILLAR_IDS.contains(id))
		{
			onNyloPillarSpawned(npc, id);
		}

		if (this.nyloActive)
		{
			if (BIG_NYLO_IDS.contains(id))
			{
				this.bigNylos.add(npc);
			}
			trackSplits(npc, id);
		}
	}

	private void onNyloTypeSpawned(NPC npc, int id)
	{
		if (this.nyloActive)
		{
			if (id == NpcID.NYLOCAS_MINIBOSS_MELEE_HARD)
			{
				this.minibossAlive = true;
				this.nyloMiniboss = npc;
				this.bossChangeTicks = 10;
			}
			else
			{
				this.nylocasNpcs.add(new NyloInfo(npc));
			}
			this.nylocasAliveCounterOverlay.setNyloAlive(this.nylocasNpcs.size() + (this.minibossAlive ? 3 : 0));

			NyloNPC nyloNPC = matchNpc(npc);
			if (nyloNPC != null)
			{
				this.currentWave.put(nyloNPC, npc);
				if (this.currentWave.size() > 2)
				{
					matchWave();
				}
			}
		}
		this.setAlive = true;
	}

	private void onNyloBossSpawned(NPC npc, int id)
	{
		this.showHint = false;
		this.nyloBossStyle = "melee";
		this.client.clearHintArrow();
		this.nyloBossAlive = true;
		this.lastBossId = id;
		this.nylocasBoss = npc;
		this.meleeBoss = 0;
		this.mageBoss = 0;
		this.rangeBoss = 0;
		if (id == NpcID.NYLOCAS_BOSS_MELEE || id == NpcID.NYLOCAS_BOSS_MELEE_STORY || id == NpcID.NYLOCAS_BOSS_MELEE_HARD)
		{
			this.bossChangeTicks = (id == NpcID.NYLOCAS_BOSS_MELEE_STORY) ? 15 : 10;
			this.meleeBoss++;
		}
	}

	private void onNyloPillarSpawned(NPC npc, int id)
	{
		this.nyloActive = true;
		this.showHint = true;
		if (this.nylocasPillars.size() > 3)
		{
			this.nylocasPillars.clear();
		}
		this.nylocasPillars.putIfAbsent(npc, 100);
		if (id == NpcID.TOB_NYLOCAS_SUPPORT_HARD)
		{
			this.tobMode = "hard";
		}
		else if (id == NpcID.TOB_NYLOCAS_SUPPORT_STORY)
		{
			this.tobMode = "story";
		}
		else
		{
			this.tobMode = "normal";
		}
		this.mageSplits = 0;
		this.rangeSplits = 0;
		this.meleeSplits = 0;
		this.preRangeSplits = 0;
		this.preMageSplits = 0;
		this.preMeleeSplits = 0;
		this.postRangeSplits = 0;
		this.postMageSplits = 0;
		this.postMeleeSplits = 0;
	}

	private void trackSplits(NPC npc, int id)
	{
		WorldPoint worldPoint = WorldPoint.fromLocalInstance(this.client, npc.getLocalLocation());
		Point spawnLoc = new Point(worldPoint.getRegionX(), worldPoint.getRegionY());
		if (spawnTiles.contains(spawnLoc) || npc.getName() == null)
		{
			return;
		}
		if (npc.getName().contains("Hagios") && MAGIC_IDS.contains(id))
		{
			this.mageSplits++;
			if (this.nyloWave < 20)
			{
				this.preMageSplits++;
			}
			else
			{
				this.postMageSplits++;
			}
		}
		else if (npc.getName().contains("Toxobolos") && RANGE_IDS.contains(id))
		{
			this.rangeSplits++;
			if (this.nyloWave < 20)
			{
				this.preRangeSplits++;
			}
			else
			{
				this.postRangeSplits++;
			}
		}
		else if (npc.getName().contains("Ischyros") && MELEE_IDS.contains(id))
		{
			this.meleeSplits++;
			if (this.nyloWave < 20)
			{
				this.preMeleeSplits++;
			}
			else
			{
				this.postMeleeSplits++;
			}
		}
	}

	@Subscribe
	public void onNpcChanged(NpcChanged event)
	{
		NPC npc = event.getNpc();
		int id = npc.getId();
		if (NYLO_BOSS_IDS.contains(id) || NYLO_DEMI_BOSS_IDS.contains(id))
		{
			this.bossChangeTicks = (id == NpcID.NYLOCAS_BOSS_MELEE_STORY || id == NpcID.NYLOCAS_BOSS_MAGIC_STORY || id == NpcID.NYLOCAS_BOSS_RANGED_STORY) ? 16 : 11;
			this.lastBossId = id;
			if (NYLO_DEMI_BOSS_IDS.contains(id))
			{
				this.nyloMiniboss = npc;
			}
		}
		if (id == NpcID.NYLOCAS_BOSS_MELEE || id == NpcID.NYLOCAS_BOSS_MELEE_STORY || id == NpcID.NYLOCAS_BOSS_MELEE_HARD)
		{
			this.meleeBoss++;
			this.nyloBossStyle = "melee";
		}
		else if (id == NpcID.NYLOCAS_BOSS_MAGIC || id == NpcID.NYLOCAS_BOSS_MAGIC_STORY || id == NpcID.NYLOCAS_BOSS_MAGIC_HARD)
		{
			this.mageBoss++;
			this.nyloBossStyle = "mage";
		}
		else if (id == NpcID.NYLOCAS_BOSS_RANGED || id == NpcID.NYLOCAS_BOSS_RANGED_STORY || id == NpcID.NYLOCAS_BOSS_RANGED_HARD)
		{
			this.rangeBoss++;
			this.nyloBossStyle = "range";
		}
	}

	private void matchWave()
	{
		HashSet<NyloNPC> potentialWave = null;
		Set<NyloNPC> currentWaveKeySet = this.currentWave.keySet();
		for (int wave = this.nyloWave + 1; wave <= NylocasWave.MAX_WAVE; wave++)
		{
			switch (this.tobMode)
			{
				case "hard":
					potentialWave = NylocasWave.hmWaves.get(wave).getWaveData();
					break;
				case "story":
					potentialWave = NylocasWave.smWaves.get(wave).getWaveData();
					break;
				case "normal":
					potentialWave = NylocasWave.waves.get(wave).getWaveData();
					break;
			}
			boolean matched = true;
			for (NyloNPC nyloNpc : potentialWave)
			{
				if (!currentWaveKeySet.contains(nyloNpc))
				{
					matched = false;
					break;
				}
			}
			if (matched)
			{
				setNyloWave(wave);
				this.stalledWave = false;
				this.waveSpawnTicks = this.ticksSinceLastWave > 0 ? this.ticksSinceLastWave : 4;
				for (NyloNPC nyloNPC : potentialWave)
				{
					if (nyloNPC.isAggressive())
					{
						this.aggressiveNylocas.add(this.currentWave.get(nyloNPC));
					}
				}
				this.currentWave.clear();
				return;
			}
		}
	}

	private NyloNPC matchNpc(NPC npc)
	{
		WorldPoint p = WorldPoint.fromLocalInstance(this.client, npc.getLocalLocation());
		Point point = new Point(p.getRegionX(), p.getRegionY());
		NylocasSpawnPoint spawnPoint = NylocasSpawnPoint.getLookupMap().get(point);
		if (spawnPoint == null)
		{
			return null;
		}
		NylocasType nylocasType = NylocasType.getLookupMap().get(npc.getId());
		if (nylocasType == null)
		{
			return null;
		}
		return new NyloNPC(nylocasType, spawnPoint);
	}

	@Subscribe
	public void onNpcDespawned(NpcDespawned npcDespawned)
	{
		NPC npc = npcDespawned.getNpc();
		int id = npc.getId();
		if (NYLO_TYPE_IDS.contains(id))
		{
			onNyloTypeDespawned(npc, id);
		}
		else if (NYLO_SPAWNING_BOSS_IDS.contains(id))
		{
			this.nyloBossAlive = false;
			this.nylocasBoss = null;
		}
		else if (NYLO_PILLAR_IDS.contains(id))
		{
			if (this.nylocasPillars.containsKey(npc))
			{
				this.nylocasPillars.remove(npc);
			}
			if (this.nylocasPillars.isEmpty())
			{
				this.nyloWaveStart = null;
				this.nyloActive = false;
			}
		}
	}

	private void onNyloTypeDespawned(NPC npc, int id)
	{
		if (this.nylocasNpcs.removeIf(n -> n.nylo != null && n.nylo == npc) || NYLO_DEMI_BOSS_IDS.contains(id))
		{
			if (NYLO_DEMI_BOSS_IDS.contains(id))
			{
				this.nyloMiniboss = null;
				this.minibossAlive = false;
				this.bossChangeTicks = -1;
			}
			this.nylocasAliveCounterOverlay.setNyloAlive(this.nylocasNpcs.size() + (this.minibossAlive ? 3 : 0));
		}
		this.aggressiveNylocas.remove(npc);
		if (this.nyloWave == 31 && this.nylocasNpcs.isEmpty())
		{
			announceSplits(SpoonTobConfig.splitsMsgTiming.CLEANUP);
			if (endOfWavesCallback != null)
			{
				endOfWavesCallback.run();
			}
		}
		this.setAlive = false;
	}

	private void announceSplits(SpoonTobConfig.splitsMsgTiming timing)
	{
		if ((this.config.nyloSplitsMsg() != SpoonTobConfig.nyloSplitsMessage.WAVES && this.config.nyloSplitsMsg() != SpoonTobConfig.nyloSplitsMessage.BOTH) || this.config.splitMsgTiming() != timing)
		{
			return;
		}
		if (this.config.smallSplitsType() == SpoonTobConfig.smallSplitsMode.CAP || this.config.smallSplitsType() == SpoonTobConfig.smallSplitsMode.BOTH)
		{
			this.client.addChatMessage(ChatMessageType.FRIENDSCHATNOTIFICATION, "", "Pre-cap splits: <col=00FFFF>" + this.preMageSplits + "</col> - <col=00FF00>" + this.preRangeSplits + "</col> - <col=ff0000>" + this.preMeleeSplits + "</col> Post-cap splits: <col=00FFFF>" + this.postMageSplits + "</col> - <col=00FF00>" + this.postRangeSplits + "</col> - <col=ff0000>" + this.postMeleeSplits, null);
		}
		if (this.config.smallSplitsType() == SpoonTobConfig.smallSplitsMode.TOTAL || this.config.smallSplitsType() == SpoonTobConfig.smallSplitsMode.BOTH)
		{
			this.client.addChatMessage(ChatMessageType.FRIENDSCHATNOTIFICATION, "", "Small splits: <col=00FFFF>" + this.mageSplits + "</col> - <col=00FF00>" + this.rangeSplits + "</col> - <col=ff0000>" + this.meleeSplits + "</col> ", null);
		}
	}

	@Subscribe
	public void onVarbitChanged(VarbitChanged event)
	{
		int[] varps = this.client.getVarps();
		int newVarbit6447 = this.client.getVarbitValue(varps, VarbitID.TOB_CLIENT_WAVEPROGRESS_TYPE);
		if (isInNyloRegion() && newVarbit6447 != 0 && newVarbit6447 != this.varbit6447)
		{
			this.nyloWaveStart = Instant.now();
			this.nylocasAliveCounterOverlay.setNyloWaveStart(this.nyloWaveStart);
		}
		if (TheatreRegions.inRegion(this.client, TheatreRegions.NYLOCAS))
		{
			this.nyloActive = this.client.getVarbitValue(VarbitID.TOB_CLIENT_WAVEPROGRESS_TYPE) != 0;
		}
		this.varbit6447 = newVarbit6447;
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged gameStateChanged)
	{
		if (gameStateChanged.getGameState() != GameState.LOGGED_IN)
		{
			return;
		}
		if (isInNyloRegion())
		{
			startupNyloOverlay();
			applyObjectHiding();
		}
		else
		{
			if (!this.nylocasAliveCounterOverlay.isHidden())
			{
				shutdownNyloOverlay();
			}
			resetNylo();
		}
	}

	@Subscribe
	public void onGameTick(GameTick event)
	{
		if (this.nyloActive)
		{
			updateWeaponStyle();
			updateWaveSpawnTicks();
			updateNylocasNpcs();
			updatePillars();
			maybeAnnounceStall();
			this.ticksSinceLastWave = Math.max(0, this.ticksSinceLastWave - 1);
			updateBossChangeTicks();
			if (!this.splitsMap.isEmpty())
			{
				this.splitsMap.values().removeIf(value -> value <= 1);
				this.splitsMap.replaceAll((key, value) -> value - 1);
			}
		}
		this.stallCheckTicks = (this.stallCheckTicks + 1) % 4;
	}

	private void updateWeaponStyle()
	{
		if (this.skipTickCheck)
		{
			this.skipTickCheck = false;
			return;
		}
		if (this.client.getLocalPlayer() == null || this.client.getLocalPlayer().getPlayerComposition() == null)
		{
			return;
		}
		int equippedWeapon = ObjectUtils.defaultIfNull(this.client.getLocalPlayer().getPlayerComposition().getEquipmentId(KitType.WEAPON), -1);
		this.weaponStyle = WeaponMap.StyleMap.get(equippedWeapon);
	}

	private void updateWaveSpawnTicks()
	{
		if (this.waveSpawnTicks < 0)
		{
			return;
		}
		this.waveSpawnTicks--;
		if (this.waveSpawnTicks < 0 && this.nylocasAliveCounterOverlay.getNyloAlive() >= this.nylocasAliveCounterOverlay.getMaxNyloAlive())
		{
			this.waveSpawnTicks = 3;
			this.stalledWave = true;
		}
	}

	private void updateNylocasNpcs()
	{
		for (int i = this.nylocasNpcs.size() - 1; i >= 0; i--)
		{
			NyloInfo ni = this.nylocasNpcs.get(i);
			ni.ticks--;
			if (ni.ticks < 0 || ni.nylo.isDead() || !ni.alive)
			{
				this.nylocasNpcs.remove(ni);
			}
		}
	}

	private void updatePillars()
	{
		for (NPC pillar : this.nylocasPillars.keySet())
		{
			int healthPercent = pillar.getHealthRatio();
			if (healthPercent > -1)
			{
				this.nylocasPillars.replace(pillar, healthPercent);
			}
		}

		boolean foundPillar = this.client.getNpcs().stream().anyMatch(npc -> NYLO_PILLAR_IDS.contains(npc.getId()));
		if (!foundPillar)
		{
			this.nylocasPillars.clear();
			return;
		}

		NPC minNPC = null;
		int minHealth = 100;
		for (NPC npc : this.nylocasPillars.keySet())
		{
			int health = npc.getHealthRatio() > -1 ? npc.getHealthRatio() : this.nylocasPillars.get(npc);
			this.nylocasPillars.replace(npc, health);
			if (health < minHealth)
			{
				minHealth = health;
				minNPC = npc;
			}
		}
		if (minNPC != null && this.config.showLowestPillar() && this.showHint)
		{
			this.client.setHintArrow(minNPC);
		}
	}

	private void maybeAnnounceStall()
	{
		if ((this.stallCheckTicks + 1) % 4 == 1 && this.nyloWave < NylocasWave.MAX_WAVE && this.ticksSinceLastWave < 2
			&& this.config.nyloStallMessage() && this.nylocasAliveCounterOverlay.getNyloAlive() >= this.nylocasAliveCounterOverlay.getMaxNyloAlive())
		{
			this.client.addChatMessage(ChatMessageType.FRIENDSCHATNOTIFICATION, "", "Stalled wave: <col=FF0000>" + this.nyloWave + " </col>Time:<col=FF0000> " + this.nylocasAliveCounterOverlay.getFormattedTime() + " </col>Nylos alive:<col=FF0000> " + this.nylocasAliveCounterOverlay.getNyloAlive() + "/" + this.nylocasAliveCounterOverlay.getMaxNyloAlive(), "", false);
		}
	}

	private void updateBossChangeTicks()
	{
		if (this.nylocasBoss != null && this.nyloBossAlive)
		{
			this.bossChangeTicks--;
			if (this.nylocasBoss.getId() != this.lastBossId)
			{
				this.lastBossId = this.nylocasBoss.getId();
				this.bossChangeTicks = (this.nylocasBoss.getId() == NpcID.NYLOCAS_BOSS_MELEE_STORY || this.nylocasBoss.getId() == NpcID.NYLOCAS_BOSS_MAGIC_STORY || this.nylocasBoss.getId() == NpcID.NYLOCAS_BOSS_RANGED_STORY) ? 15 : 10;
			}
		}
		else if (this.minibossAlive && this.nyloMiniboss != null)
		{
			this.bossChangeTicks--;
		}
	}

	@Subscribe
	protected void onClientTick(ClientTick event)
	{
		applyNyloPrioritySort();
	}

	/**
	 * Reorders wave-nylo attack options so the newest (furthest from despawning) nylo is the
	 * default left-click - damage on a fresh nylo is never at risk of being wasted by it expiring
	 * mid-fight - optionally weighted to prefer big nylos first (killing one forces a valuable
	 * split), with a safety-valve override so an old small nylo can still jump the queue once
	 * it's genuinely close to despawning unattacked. Ported from ariatob's [A] Tob plugin, which
	 * reorders the live menu-entry array directly (via {@code client.setMenuEntries}) rather than
	 * {@code MenuEntry.setDeprioritized} - the mechanism this feature already used and was tuned
	 * against, so it's kept as-is rather than rebuilt on the deprioritize-flag mechanism the rest
	 * of this plugin otherwise uses.
	 */
	private void applyNyloPrioritySort()
	{
		if (!this.config.nyloPrio() || !this.nyloActive || this.client.isMenuOpen())
		{
			return;
		}
		MenuEntry[] entries = this.client.getMenuEntries();
		List<MenuEntry> sortedNylos = new ArrayList<>();
		List<MenuEntry> leftovers = new ArrayList<>();
		for (MenuEntry entry : entries)
		{
			NPC npc = isNyloTarget(entry) ? npcFromIndex(entry.getIdentifier()) : null;
			if (npc != null && ticksAlive(npc) != -1)
			{
				sortedNylos.add(entry);
			}
			else
			{
				leftovers.add(entry);
			}
		}
		if (sortedNylos.isEmpty())
		{
			return;
		}

		sortedNylos.sort(Comparator.comparingInt((MenuEntry entry) -> ticksAlive(npcFromIndex(entry.getIdentifier()))).reversed());
		if (this.config.nyloPrioBigs())
		{
			sortedNylos.sort((a, b) -> compareBigPriority(npcFromIndex(a.getIdentifier()), npcFromIndex(b.getIdentifier())));
		}

		MenuEntry[] finalEntries = new MenuEntry[entries.length];
		int i = 0;
		for (MenuEntry entry : leftovers)
		{
			finalEntries[i++] = entry;
		}
		for (MenuEntry entry : sortedNylos)
		{
			finalEntries[i++] = entry;
		}
		this.client.setMenuEntries(finalEntries);
	}

	private boolean isNyloTarget(MenuEntry entry)
	{
		return entry.getTarget() != null && entry.getTarget().contains("Nylocas");
	}

	private NPC npcFromIndex(int identifier)
	{
		return this.client.getNpcs().stream().filter(n -> n.getIndex() == identifier).findFirst().orElse(null);
	}

	private int ticksAlive(NPC npc)
	{
		if (npc == null)
		{
			return -1;
		}
		for (NyloInfo ni : this.nylocasNpcs)
		{
			if (ni.nylo == npc)
			{
				return 52 - ni.ticks;
			}
		}
		return -1;
	}

	private int compareBigPriority(NPC aNpc, NPC bNpc)
	{
		boolean aBig = BIG_NYLO_IDS.contains(aNpc.getId());
		boolean bBig = BIG_NYLO_IDS.contains(bNpc.getId());
		if (aBig == bBig)
		{
			return 0;
		}
		boolean prio35 = this.config.nyloPrio35s();
		int aLifetime = ticksAlive(aNpc);
		int bLifetime = ticksAlive(bNpc);
		if (prio35 && aBig && bLifetime > 35)
		{
			return aLifetime > 35 ? -1 : 1;
		}
		if (aBig)
		{
			return -1;
		}
		if (prio35 && bBig && aLifetime > 35)
		{
			return bLifetime > 35 ? 1 : -1;
		}
		return 1;
	}

	@Subscribe
	private void onAnimationChanged(AnimationChanged event)
	{
		if (this.bigNylos.isEmpty() || !(event.getActor() instanceof NPC))
		{
			return;
		}
		NPC npc = (NPC) event.getActor();
		if (!this.bigNylos.contains(npc))
		{
			return;
		}
		int anim = npc.getAnimation();
		if (anim == 8005 || anim == 7991 || anim == 7998)
		{
			this.splitsMap.putIfAbsent(npc, 6);
			this.bigNylos.remove(npc);
		}
		if (anim == 8006 || anim == 7992 || anim == 8000)
		{
			this.splitsMap.putIfAbsent(npc, 4);
			this.bigNylos.remove(npc);
		}
	}

	@Subscribe
	public void onChatMessage(ChatMessage event)
	{
		String mes = event.getMessage();
		if (!mes.contains("Wave 'The Nylocas'") || !mes.contains("complete!<br>Duration: <col=ff0000>"))
		{
			return;
		}
		announceSplits(SpoonTobConfig.splitsMsgTiming.FINISHED);
		if (this.config.nyloSplitsMsg() == SpoonTobConfig.nyloSplitsMessage.BOSS || this.config.nyloSplitsMsg() == SpoonTobConfig.nyloSplitsMessage.BOTH)
		{
			this.client.addChatMessage(ChatMessageType.FRIENDSCHATNOTIFICATION, "", "Boss phases: <col=00FFFF>" + this.mageBoss + "</col> - <col=00FF00>" + this.rangeBoss + "</col> - <col=ff0000>" + this.meleeBoss + "</col> ", null);
		}
	}

	@Subscribe
	public void onMenuOptionClicked(MenuOptionClicked event)
	{
		if (event.getMenuOption().equalsIgnoreCase("wield"))
		{
			WeaponStyle newStyle = WeaponMap.StyleMap.get(event.getItemId());
			if (newStyle != null)
			{
				this.skipTickCheck = true;
				this.weaponStyle = newStyle;
			}
			return;
		}
		if ((this.config.wheelchairNylo() != SpoonTobConfig.wheelchairMode.BOSS && this.config.wheelchairNylo() != SpoonTobConfig.wheelchairMode.BOTH)
			|| this.nylocasBoss == null || !event.getMenuTarget().contains("Nylocas Vasilias") || !event.getMenuOption().equalsIgnoreCase("attack") || this.weaponStyle == null)
		{
			return;
		}
		switch (this.weaponStyle)
		{
			case TRIDENTS:
			case MAGIC:
				if (this.nylocasBoss.getId() != NpcID.NYLOCAS_BOSS_MAGIC && this.nylocasBoss.getId() != NpcID.NYLOCAS_BOSS_MAGIC_STORY && this.nylocasBoss.getId() != NpcID.NYLOCAS_BOSS_MAGIC_HARD)
				{
					event.consume();
				}
				break;
			case MELEE:
				if (this.nylocasBoss.getId() != NpcID.NYLOCAS_BOSS_MELEE && this.nylocasBoss.getId() != NpcID.NYLOCAS_BOSS_MELEE_STORY && this.nylocasBoss.getId() != NpcID.NYLOCAS_BOSS_MELEE_HARD)
				{
					event.consume();
				}
				break;
			case RANGE:
				if (this.nylocasBoss.getId() != NpcID.NYLOCAS_BOSS_RANGED && this.nylocasBoss.getId() != NpcID.NYLOCAS_BOSS_RANGED_STORY && this.nylocasBoss.getId() != NpcID.NYLOCAS_BOSS_RANGED_HARD)
				{
					event.consume();
				}
				break;
		}
	}

	@Subscribe
	public void onMenuEntryAdded(MenuEntryAdded event)
	{
		if (!this.nyloActive)
		{
			return;
		}
		String target = event.getTarget();
		String option = event.getOption();
		MenuEntry entry = event.getMenuEntry();

		if ((this.config.wheelchairNylo() == SpoonTobConfig.wheelchairMode.WAVES || this.config.wheelchairNylo() == SpoonTobConfig.wheelchairMode.BOTH) && option.equalsIgnoreCase("attack") && this.weaponStyle != null)
		{
			deprioritizeByWeaponStyle(entry, target);
		}

		if ((this.config.wheelchairNylo() == SpoonTobConfig.wheelchairMode.BOSS || this.config.wheelchairNylo() == SpoonTobConfig.wheelchairMode.BOTH) && this.nyloMiniboss != null && target.contains("Nylocas Prinkipas") && option.equalsIgnoreCase("attack") && this.weaponStyle != null)
		{
			deprioritizeMiniboss(entry);
		}
	}

	private void deprioritizeByWeaponStyle(MenuEntry entry, String target)
	{
		switch (this.weaponStyle)
		{
			case TRIDENTS:
				if (target.contains("Nylocas Ischyros") || target.contains("Nylocas Toxobolos"))
				{
					entry.setDeprioritized(true);
				}
				break;
			case MAGIC:
				if (this.config.manualCast())
				{
					if (target.contains("Nylocas Ischyros") || target.contains("Nylocas Toxobolos") || target.contains("Nylocas Hagios"))
					{
						entry.setDeprioritized(true);
					}
					break;
				}
				if (target.contains("Nylocas Ischyros") || target.contains("Nylocas Toxobolos"))
				{
					entry.setDeprioritized(true);
				}
				break;
			case MELEE:
				if (target.contains("Nylocas Toxobolos") || target.contains("Nylocas Hagios"))
				{
					entry.setDeprioritized(true);
				}
				break;
			case RANGE:
				if (target.contains("Nylocas Ischyros") || target.contains("Nylocas Hagios"))
				{
					entry.setDeprioritized(true);
				}
				break;
			case CHINS:
				if (!this.config.ignoreChins() && (target.contains("Nylocas Ischyros") || target.contains("Nylocas Hagios")))
				{
					entry.setDeprioritized(true);
				}
				break;
		}
	}

	private void deprioritizeMiniboss(MenuEntry entry)
	{
		switch (this.weaponStyle)
		{
			case TRIDENTS:
			case MAGIC:
				if (this.nyloMiniboss.getId() != NpcID.NYLOCAS_MINIBOSS_MAGIC_HARD)
				{
					entry.setDeprioritized(true);
				}
				break;
			case MELEE:
				if (this.nyloMiniboss.getId() != NpcID.NYLOCAS_MINIBOSS_MELEE_HARD)
				{
					entry.setDeprioritized(true);
				}
				break;
			case RANGE:
				if (this.nyloMiniboss.getId() != NpcID.NYLOCAS_MINIBOSS_RANGED_HARD)
				{
					entry.setDeprioritized(true);
				}
				break;
			default:
				break;
		}
	}

	public void removeGameObjectsFromScene(Set<Integer> objectIDs, int plane)
	{
		Scene scene = this.client.getScene();
		Tile[][] tiles = scene.getTiles()[plane];
		for (int x = 0; x < 104; x++)
		{
			for (int y = 0; y < 104; y++)
			{
				Tile tile = tiles[x][y];
				if (tile == null)
				{
					continue;
				}
				Arrays.stream(tile.getGameObjects())
					.filter(obj -> obj != null && objectIDs.contains(obj.getId()))
					.findFirst()
					.ifPresent(scene::removeGameObject);
			}
		}
	}

	boolean isInNyloRegion()
	{
		return TheatreRegions.inRegion(this.client, TheatreRegions.NYLOCAS);
	}

	public boolean isNyloActive()
	{
		return this.nyloActive;
	}

	public Instant getNyloWaveStart()
	{
		return this.nyloWaveStart;
	}

	public HashMap<NPC, Integer> getNylocasPillars()
	{
		return this.nylocasPillars;
	}

	public HashSet<NPC> getAggressiveNylocas()
	{
		return this.aggressiveNylocas;
	}

	public int getBossChangeTicks()
	{
		return this.bossChangeTicks;
	}

	public NPC getNylocasBoss()
	{
		return this.nylocasBoss;
	}

	public Map<NPC, Integer> getSplitsMap()
	{
		return this.splitsMap;
	}
}
