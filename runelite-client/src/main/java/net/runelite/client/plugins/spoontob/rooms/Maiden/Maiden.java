package net.runelite.client.plugins.spoontob.rooms.Maiden;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import javax.inject.Inject;
import net.runelite.api.Actor;
import net.runelite.api.Client;
import net.runelite.api.HitsplatID;
import net.runelite.api.MenuAction;
import net.runelite.api.MenuEntry;
import net.runelite.api.NPC;
import net.runelite.api.NPCComposition;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.ActorDeath;
import net.runelite.api.events.AnimationChanged;
import net.runelite.api.events.ClientTick;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.GraphicChanged;
import net.runelite.api.events.HitsplatApplied;
import net.runelite.api.events.MenuEntryAdded;
import net.runelite.api.events.NpcChanged;
import net.runelite.api.events.NpcDespawned;
import net.runelite.api.events.NpcSpawned;
import net.runelite.api.events.ProjectileMoved;
import net.runelite.api.gameval.AnimationID;
import net.runelite.api.gameval.NpcID;
import net.runelite.api.gameval.SpotanimID;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.spoontob.Room;
import net.runelite.client.plugins.spoontob.SpoonTobConfig;
import net.runelite.client.plugins.spoontob.SpoonTobPlugin;
import net.runelite.client.util.ColorUtil;
import net.runelite.client.util.Text;

public class Maiden extends Room
{
	// Ice-spell freeze durations by impact graphic, in ticks.
	private static final int FREEZE_ICE_BARRAGE = 33;
	private static final int FREEZE_ICE_BURST = 25;
	private static final int FREEZE_ICE_BLITZ = 16;
	private static final int FREEZE_ICE_RUSH = 8;
	private static final int FREEZE_BIND = 24;
	private static final int FREEZE_SNARE = 16;
	private static final int FREEZE_ENTANGLE = 8;

	@Inject
	private Client client;
	@Inject
	private MaidenOverlay maidenOverlay;
	@Inject
	private ThresholdOverlay thresholdOverlay;
	@Inject
	private MaidenMaxHitOverlay maidenMaxHitOverlay;
	@Inject
	private MaidenMaxHitToolTip maidenMaxHitToolTip;
	@Inject
	private MaidenRedsOverlay redsOverlay;

	private boolean maidenActive;
	private NPC maidenNPC;
	private final List<NPC> maidenSpawns = new ArrayList<>();
	private final List<WorldPoint> maidenBloodSplatters = new ArrayList<>();
	private final ArrayList<MaidenBloodSplatInfo> maidenBloodSplatterProj = new ArrayList<>();
	public final ArrayList<Color> maidenBloodSplattersColors = new ArrayList<>();
	private final List<WorldPoint> maidenBloodSpawnLocations = new ArrayList<>();
	private final List<WorldPoint> maidenBloodSpawnTrailingLocations = new ArrayList<>();
	private int realMaidenHp = -1;
	private int thresholdHp = -1;
	private double maxHit = 36.5;
	private final Consumer<Double> setThreshold = percent -> this.thresholdHp = (int) Math.floor(getMaidenBaseHpIndex() * percent);

	public int ticksUntilAttack = 0;
	public int maidenAttSpd = 10;
	public int nyloSpawnDelay = 2;
	public int maidenPhase = 70;
	public final ArrayList<MaidenCrabInfo> maidenCrabInfoList = new ArrayList<>();
	public final Map<NPC, Integer> frozenBloodSpawns = new HashMap<>();
	public int crabTicksSinceSpawn = 0;

	@Inject
	protected Maiden(SpoonTobPlugin plugin, SpoonTobConfig config)
	{
		super(plugin, config);
	}

	@Override
	public void load()
	{
		this.overlayManager.add(this.maidenOverlay);
		this.overlayManager.add(this.thresholdOverlay);
		this.overlayManager.add(this.maidenMaxHitOverlay);
		this.overlayManager.add(this.maidenMaxHitToolTip);
		this.overlayManager.add(this.redsOverlay);
	}

	@Override
	public void unload()
	{
		this.overlayManager.remove(this.maidenOverlay);
		this.overlayManager.remove(this.thresholdOverlay);
		this.overlayManager.remove(this.maidenMaxHitOverlay);
		this.overlayManager.remove(this.maidenMaxHitToolTip);
		this.overlayManager.remove(this.redsOverlay);
		reset();
	}

	private void reset()
	{
		this.maidenActive = false;
		this.maidenNPC = null;
		this.maidenBloodSplatters.clear();
		this.maidenBloodSplattersColors.clear();
		this.maidenSpawns.clear();
		this.maidenBloodSpawnLocations.clear();
		this.maidenBloodSpawnTrailingLocations.clear();
		this.maidenCrabInfoList.clear();
		this.frozenBloodSpawns.clear();
		this.realMaidenHp = -1;
		this.thresholdHp = -1;
		this.maxHit = 36.5;
		this.maidenPhase = 70;
	}

	void updateMaidenMaxHit()
	{
		this.maxHit += 3.5;
	}

	private static boolean isMaidenId(int id)
	{
		switch (id)
		{
			case NpcID.TOB_MAIDEN_100:
			case NpcID.TOB_MAIDEN_70:
			case NpcID.TOB_MAIDEN_50:
			case NpcID.TOB_MAIDEN_30:
			case NpcID.TOB_MAIDEN_DYING_A:
			case NpcID.TOB_MAIDEN_DYING_B:
			case NpcID.TOB_MAIDEN_100_STORY:
			case NpcID.TOB_MAIDEN_70_STORY:
			case NpcID.TOB_MAIDEN_50_STORY:
			case NpcID.TOB_MAIDEN_30_STORY:
			case NpcID.TOB_MAIDEN_DYING_A_STORY:
			case NpcID.TOB_MAIDEN_DYING_B_STORY:
			case NpcID.TOB_MAIDEN_100_HARD:
			case NpcID.TOB_MAIDEN_70_HARD:
			case NpcID.TOB_MAIDEN_50_HARD:
			case NpcID.TOB_MAIDEN_30_HARD:
			case NpcID.TOB_MAIDEN_DYING_A_HARD:
			case NpcID.TOB_MAIDEN_DYING_B_HARD:
				return true;
			default:
				return false;
		}
	}

	private static boolean isBloodSpawnId(int id)
	{
		return id == NpcID.MAIDEN_BLOOD_SLUG || id == NpcID.MAIDEN_BLOOD_SLUG_STORY || id == NpcID.MAIDEN_BLOOD_SLUG_HARD;
	}

	@Subscribe
	public void onNpcSpawned(NpcSpawned npcSpawned)
	{
		NPC npc = npcSpawned.getNpc();
		String name = npc.getName();

		if (isMaidenId(npc.getId()))
		{
			this.maidenActive = true;
			this.maidenNPC = npc;
			if (npc.getHealthRatio() == npc.getHealthScale())
			{
				this.ticksUntilAttack = 10;
			}
			else
			{
				this.ticksUntilAttack = -1;
			}
			this.maidenAttSpd = 10;
			this.maidenCrabInfoList.clear();
			if (this.realMaidenHp < 0)
			{
				this.realMaidenHp = getMaidenBaseHpIndex();
			}
			this.setThreshold.accept(0.7);
		}
		else if (isBloodSpawnId(npc.getId()))
		{
			this.maidenSpawns.add(npc);
		}

		if (name != null && name.equalsIgnoreCase("Nylocas Matomenos") && this.maidenActive && this.maidenNPC != null)
		{
			this.crabTicksSinceSpawn = 8;
			this.nyloSpawnDelay = 2;
			String position = "??";
			boolean scuffed = false;
			int x = npc.getWorldLocation().getRegionX();
			int y = npc.getWorldLocation().getRegionY();
			if (x == 21 && y == 40)
			{
				position = "N1";
			}
			else if (x == 22 && y == 41)
			{
				position = "N1";
				scuffed = true;
			}
			else if (x == 25 && y == 40)
			{
				position = "N2";
			}
			else if (x == 26 && y == 41)
			{
				position = "N2";
				scuffed = true;
			}
			else if (x == 29 && y == 40)
			{
				position = "N3";
			}
			else if (x == 30 && y == 41)
			{
				position = "N3";
				scuffed = true;
			}
			else if (x == 33 && y == 40 || x == 33 && y == 38)
			{
				position = "N4";
			}
			else if (x == 34 && y == 41 || x == 34 && y == 39)
			{
				position = "N4";
				scuffed = true;
			}
			else if (x == 21 && y == 20)
			{
				position = "S1";
			}
			else if (x == 22 && y == 19)
			{
				position = "S1";
				scuffed = true;
			}
			else if (x == 25 && y == 20)
			{
				position = "S2";
			}
			else if (x == 26 && y == 19)
			{
				position = "S2";
				scuffed = true;
			}
			else if (x == 29 && y == 20)
			{
				position = "S3";
			}
			else if (x == 30 && y == 19)
			{
				position = "S3";
				scuffed = true;
			}
			else if (x == 33 && y == 20 || x == 33 && y == 22)
			{
				position = "S4";
			}
			else if (x == 34 && y == 19 || x == 34 && y == 20)
			{
				position = "S4";
				scuffed = true;
			}

			for (NPC n : this.client.getNpcs())
			{
				int id = n.getId();
				if (id == NpcID.TOB_MAIDEN_70 || id == NpcID.TOB_MAIDEN_70_STORY || id == NpcID.TOB_MAIDEN_70_HARD)
				{
					this.maidenPhase = 70;
					break;
				}
				if (id == NpcID.TOB_MAIDEN_50 || id == NpcID.TOB_MAIDEN_50_STORY || id == NpcID.TOB_MAIDEN_50_HARD)
				{
					this.maidenPhase = 50;
					break;
				}
				if (id == NpcID.TOB_MAIDEN_30 || id == NpcID.TOB_MAIDEN_30_STORY || id == NpcID.TOB_MAIDEN_30_HARD)
				{
					this.maidenPhase = 30;
					break;
				}
			}

			this.maidenCrabInfoList.add(new MaidenCrabInfo(npc, this.maidenPhase, position, -1, -1, -1, scuffed));
		}
	}

	@Subscribe
	public void onNpcDespawned(NpcDespawned npcDespawned)
	{
		NPC npc = npcDespawned.getNpc();
		if (isMaidenId(npc.getId()))
		{
			reset();
		}
		else if (isBloodSpawnId(npc.getId()))
		{
			this.maidenSpawns.remove(npc);
		}
	}

	@Subscribe
	public void onNpcChanged(NpcChanged event)
	{
		if (!this.maidenActive || this.maidenNPC == null)
		{
			return;
		}
		int id = event.getNpc().getId();
		if (id == NpcID.TOB_MAIDEN_70 || id == NpcID.TOB_MAIDEN_70_STORY || id == NpcID.TOB_MAIDEN_70_HARD)
		{
			this.maidenPhase = 70;
		}
		else if (id == NpcID.TOB_MAIDEN_50 || id == NpcID.TOB_MAIDEN_50_STORY || id == NpcID.TOB_MAIDEN_50_HARD)
		{
			this.maidenPhase = 50;
			this.setThreshold.accept(0.5);
		}
		else if (id == NpcID.TOB_MAIDEN_30 || id == NpcID.TOB_MAIDEN_30_STORY || id == NpcID.TOB_MAIDEN_30_HARD)
		{
			this.maidenPhase = 30;
			this.setThreshold.accept(0.3);
		}
	}

	@Subscribe
	public void onMenuEntryAdded(MenuEntryAdded event)
	{
		if (!this.maidenActive || this.maidenNPC == null)
		{
			return;
		}

		if (this.config.maidenRecolourNylos() && event.getTarget().contains("Matomenos") && !this.maidenCrabInfoList.isEmpty())
		{
			NPC npc = this.client.getNpcs().stream().filter(n -> n.getIndex() == event.getIdentifier()).findFirst().orElse(null);
			for (MaidenCrabInfo mci : this.maidenCrabInfoList)
			{
				if (mci.crab != npc)
				{
					continue;
				}
				double crabHealthPcent = (double) mci.hpRatio / mci.hpScale * 100.0;
				Color color = this.config.oldHpThreshold() ? this.plugin.oldHitpointsColor(crabHealthPcent) : this.plugin.calculateHitpointsColor(crabHealthPcent);

				String crabHp = Double.toString(crabHealthPcent);
				if (crabHp.contains("."))
				{
					crabHp = crabHp.substring(0, crabHp.indexOf(".") + 2);
				}

				MenuEntry[] menuEntries = this.client.getMenuEntries();
				MenuEntry menuEntry = menuEntries[menuEntries.length - 1];
				String target = ColorUtil.prependColorTag(Text.removeTags(event.getTarget() + " - " + crabHp + "%"), color);
				menuEntry.setTarget(target);
				this.client.setMenuEntries(menuEntries);
				break;
			}
		}

		SpoonTobConfig.maidenBloodsMode bloodsMode = this.config.removeMaidenBloods();
		if (event.getTarget().contains("Blood spawn") && event.getType() == MenuAction.NPC_SECOND_OPTION.getId()
			&& (bloodsMode == SpoonTobConfig.maidenBloodsMode.ATTACK || bloodsMode == SpoonTobConfig.maidenBloodsMode.BOTH))
		{
			event.getMenuEntry().setDeprioritized(true);
		}
		else if (event.getTarget().contains("Blood spawn") && event.getTarget().contains("Ice B") && event.getType() == MenuAction.WIDGET_TARGET_ON_NPC.getId()
			&& (bloodsMode == SpoonTobConfig.maidenBloodsMode.CAST || bloodsMode == SpoonTobConfig.maidenBloodsMode.BOTH))
		{
			event.getMenuEntry().setDeprioritized(true);
		}
	}

	@Subscribe
	public void onClientTick(ClientTick event)
	{
		if (this.maidenNPC == null || !this.maidenActive || !this.config.maidenCrabHpPriority())
		{
			return;
		}

		this.maidenCrabInfoList.forEach(mci -> {
			if (this.nyloSpawnDelay == 0 && mci.crab.getHealthRatio() >= 0)
			{
				mci.hpRatio = mci.crab.getHealthRatio();
				mci.hpScale = mci.crab.getHealthScale();
			}
		});

		MenuEntry[] npcEntries = Arrays.stream(this.client.getMenuEntries())
			.filter(entry -> entry.getTarget().contains("Nylocas Matomenos") && (entry.getOption().contains("Attack") || entry.getOption().contains("Cast")))
			.toArray(MenuEntry[]::new);
		if (npcEntries.length <= 1)
		{
			return;
		}

		int highestHp = -1;
		int highestHpNpcIndex = -1;
		for (MaidenCrabInfo mci : this.maidenCrabInfoList)
		{
			boolean clickable = Arrays.stream(npcEntries).anyMatch(entry -> entry.getIdentifier() == mci.crab.getIndex());
			if (clickable && mci.hpRatio > highestHp)
			{
				highestHp = mci.hpRatio;
				highestHpNpcIndex = mci.crab.getIndex();
			}
		}
		if (highestHpNpcIndex == -1)
		{
			return;
		}

		MenuEntry[] entries = this.client.getMenuEntries();
		boolean foundTarget = false;
		for (MenuEntry entry : entries)
		{
			if (entry.getTarget().contains("Nylocas Matomenos") && (entry.getOption().contains("Attack") || entry.getOption().contains("Cast"))
				&& entry.getIdentifier() != highestHpNpcIndex)
			{
				entry.setDeprioritized(true);
			}
			else if (entry.getIdentifier() == highestHpNpcIndex)
			{
				foundTarget = true;
			}
		}
		if (foundTarget)
		{
			this.client.setMenuEntries(entries);
		}
	}

	@Subscribe
	public void onGameTick(GameTick event)
	{
		if (!this.maidenActive)
		{
			return;
		}

		if (this.maidenNPC != null)
		{
			this.ticksUntilAttack--;
		}
		if (this.crabTicksSinceSpawn > 0)
		{
			this.crabTicksSinceSpawn--;
		}

		Iterator<NPC> it = this.frozenBloodSpawns.keySet().iterator();
		while (it.hasNext())
		{
			NPC npc = it.next();
			int ticksLeft = this.frozenBloodSpawns.get(npc) - 1;
			if (ticksLeft < -5)
			{
				it.remove();
			}
			else
			{
				this.frozenBloodSpawns.put(npc, ticksLeft);
			}
		}

		this.maidenBloodSplatters.clear();
		this.maidenBloodSplattersColors.clear();
		for (var obj : this.client.getTopLevelWorldView().getGraphicsObjects())
		{
			if (obj.getId() == SpotanimID.MAIDEN_LINGERING_BLOOD)
			{
				this.maidenBloodSplatters.add(WorldPoint.fromLocal(this.client, obj.getLocation()));
			}
		}

		this.maidenBloodSpawnTrailingLocations.clear();
		this.maidenBloodSpawnTrailingLocations.addAll(this.maidenBloodSpawnLocations);
		this.maidenBloodSpawnLocations.clear();
		this.maidenSpawns.forEach(s -> this.maidenBloodSpawnLocations.add(s.getWorldLocation()));

		if (!this.maidenCrabInfoList.isEmpty())
		{
			if (this.nyloSpawnDelay == 0)
			{
				for (MaidenCrabInfo mci : this.maidenCrabInfoList)
				{
					if (mci.frozenTicks != -1)
					{
						mci.frozenTicks--;
					}
				}
			}
			else
			{
				this.nyloSpawnDelay--;
			}
		}

		this.maidenBloodSplatterProj.removeIf(info -> info.projectile.getRemainingCycles() / 30 <= 0);
	}

	@Subscribe
	public void onAnimationChanged(AnimationChanged event)
	{
		Actor actor = event.getActor();
		if (actor.getName() == null || this.maidenNPC == null || !this.maidenActive)
		{
			return;
		}

		if (actor.getName().equals("Nylocas Matomenos") && actor.getAnimation() == AnimationID.ELEMENTAL_DEATH)
		{
			NPC npc = (NPC) actor;
			for (int i = this.maidenCrabInfoList.size() - 1; i >= 0; i--)
			{
				MaidenCrabInfo mci = this.maidenCrabInfoList.get(i);
				if (npc != mci.crab)
				{
					continue;
				}
				NPCComposition nComp = this.maidenNPC.getComposition();
				int distance = npc.getWorldLocation().getX() - (this.maidenNPC.getWorldLocation().getX() + nComp.getSize());
				if ((distance == -1 || distance == 0) && (npc.getHealthRatio() > 0 || npc.getHealthRatio() == -1))
				{
					if (distance == 0)
					{
						updateMaidenMaxHit();
					}
				}
				this.maidenCrabInfoList.remove(i);
				break;
			}
		}
		else if (actor.getName().equals("The Maiden of Sugadinti") && (actor.getAnimation() == AnimationID.MAIDEN_ATTACK_BLOOD || actor.getAnimation() == AnimationID.MAIDEN_ATTACK_SPECIAL))
		{
			if (this.ticksUntilAttack > 1 && this.maidenNPC.getId() >= NpcID.TOB_MAIDEN_100_HARD)
			{
				this.maidenAttSpd -= this.ticksUntilAttack - 1;
				if (this.maidenAttSpd < 3)
				{
					this.maidenAttSpd = 3;
				}
			}
			this.ticksUntilAttack = this.maidenAttSpd + 1;
		}
	}

	@Subscribe
	public void onGraphicChanged(GraphicChanged event)
	{
		if (!this.maidenActive || this.maidenNPC == null || !(event.getActor() instanceof NPC))
		{
			return;
		}
		NPC npc = (NPC) event.getActor();
		int ticks = 0;
		int graphic = npc.getGraphic();
		if (graphic == SpotanimID.ICE_BARRAGE_IMPACT)
		{
			ticks = FREEZE_ICE_BARRAGE;
		}
		else if (graphic == SpotanimID.ICE_BURST_IMPACT)
		{
			ticks = FREEZE_ICE_BURST;
		}
		else if (graphic == SpotanimID.ICE_BLITZ_IMPACT)
		{
			ticks = FREEZE_ICE_BLITZ;
		}
		else if (graphic == SpotanimID.ICE_RUSH_IMPACT)
		{
			ticks = FREEZE_ICE_RUSH;
		}
		else if (graphic == SpotanimID.BIND_IMPACT)
		{
			ticks = FREEZE_BIND;
		}
		else if (graphic == SpotanimID.SNARE_IMPACT)
		{
			ticks = FREEZE_SNARE;
		}
		else if (graphic == SpotanimID.ENTANGLE_IMPACT)
		{
			ticks = FREEZE_ENTANGLE;
		}

		if (npc.getName() != null && ticks > 0)
		{
			if (this.config.bloodSpawnFreezeTimer() && npc.getName().equalsIgnoreCase("blood spawn"))
			{
				this.frozenBloodSpawns.putIfAbsent(npc, ticks);
			}
			else if (npc.getName().equalsIgnoreCase("nylocas matomenos"))
			{
				for (MaidenCrabInfo mci : this.maidenCrabInfoList)
				{
					if (mci.crab == npc && mci.frozenTicks == -1)
					{
						mci.frozenTicks = ticks;
						break;
					}
				}
			}
		}
	}

	@Subscribe
	public void onActorDeath(ActorDeath event)
	{
		if (event.getActor() instanceof NPC)
		{
			this.frozenBloodSpawns.remove(event.getActor());
		}
	}

	@Subscribe
	public void onProjectileMoved(ProjectileMoved event)
	{
		if (event.getProjectile().getId() == SpotanimID.MAIDEN_BLOOD_PROJ)
		{
			this.maidenBloodSplatterProj.add(new MaidenBloodSplatInfo(event.getProjectile(), event.getPosition()));
		}
	}

	@Subscribe
	public void onHitsplatApplied(HitsplatApplied event)
	{
		if (this.maidenActive && event.getActor() == this.maidenNPC && event.getHitsplat().getHitsplatType() != HitsplatID.HEAL)
		{
			this.realMaidenHp -= event.getHitsplat().getAmount();
		}
	}

	Color maidenSpecialWarningColor()
	{
		if (this.maidenNPC != null && this.maidenNPC.getInteracting() != null && this.maidenNPC.getInteracting().getName() != null && this.client.getLocalPlayer() != null
			&& this.maidenNPC.getInteracting().getName().equals(this.client.getLocalPlayer().getName()))
		{
			return Color.ORANGE;
		}
		return Color.GREEN;
	}

	private int getMaidenBaseHpIndex()
	{
		switch (SpoonTobPlugin.partySize)
		{
			case 4:
				return 3062;
			case 5:
				return 3500;
			default:
				return 2625;
		}
	}

	public boolean isMaidenActive()
	{
		return this.maidenActive;
	}

	public NPC getMaidenNPC()
	{
		return this.maidenNPC;
	}

	public List<WorldPoint> getMaidenBloodSplatters()
	{
		return this.maidenBloodSplatters;
	}

	public ArrayList<MaidenBloodSplatInfo> getMaidenBloodSplatterProj()
	{
		return this.maidenBloodSplatterProj;
	}

	public List<WorldPoint> getMaidenBloodSpawnLocations()
	{
		return this.maidenBloodSpawnLocations;
	}

	public List<WorldPoint> getMaidenBloodSpawnTrailingLocations()
	{
		return this.maidenBloodSpawnTrailingLocations;
	}

	public int getRealMaidenHp()
	{
		return this.realMaidenHp;
	}

	public int getThresholdHp()
	{
		return this.thresholdHp;
	}

	public double getMaxHit()
	{
		return this.maxHit;
	}
}
