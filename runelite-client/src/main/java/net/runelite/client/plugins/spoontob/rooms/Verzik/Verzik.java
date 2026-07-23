package net.runelite.client.plugins.spoontob.rooms.Verzik;

import com.google.common.collect.ImmutableSet;
import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import javax.inject.Inject;
import net.runelite.api.Actor;
import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.MenuAction;
import net.runelite.api.MenuEntry;
import net.runelite.api.NPC;
import net.runelite.api.Player;
import net.runelite.api.PlayerComposition;
import net.runelite.api.Projectile;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.ActorDeath;
import net.runelite.api.events.ClientTick;
import net.runelite.api.events.GameObjectDespawned;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.GraphicChanged;
import net.runelite.api.events.GraphicsObjectCreated;
import net.runelite.api.events.MenuEntryAdded;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.events.NpcChanged;
import net.runelite.api.events.NpcDespawned;
import net.runelite.api.events.NpcSpawned;
import net.runelite.api.events.ProjectileMoved;
import net.runelite.api.gameval.NpcID;
import net.runelite.api.gameval.ObjectID;
import net.runelite.api.gameval.SpotanimID;
import net.runelite.api.kit.KitType;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.spoontob.Room;
import net.runelite.client.plugins.spoontob.SpoonTobConfig;
import net.runelite.client.plugins.spoontob.SpoonTobPlugin;
import net.runelite.client.plugins.spoontob.util.PoisonStyle;
import net.runelite.client.plugins.spoontob.util.PoisonWeaponMap;
import net.runelite.client.ui.overlay.Overlay;
import org.apache.commons.lang3.ObjectUtils;

public class Verzik extends Room
{
	@Inject
	private Client client;

	@Inject
	private VerzikOverlay verzikOverlay;

	@Inject
	private LightningPanel lightningPanel;

	@Inject
	private PurpleCrabPanel purpleCrabPanel;

	@Inject
	private GreenBallPanel greenBallPanel;

	@Inject
	private YellowGrouperOverlay yellowGroupOverlay;

	@Inject
	private VerzikRedsOverlay redsOverlay;

	private PoisonStyle poisonStyle;
	private boolean skipTickCheck = false;

	protected static final Set<Integer> BEFORE_START_IDS = ImmutableSet.of(NpcID.VERZIK_INITIAL, NpcID.VERZIK_INITIAL_STORY, NpcID.VERZIK_INITIAL_HARD);
	protected static final Set<Integer> P1_IDS = ImmutableSet.of(NpcID.VERZIK_PHASE1, NpcID.VERZIK_PHASE1_STORY, NpcID.VERZIK_PHASE1_HARD);
	protected static final Set<Integer> P12_TRANSITION_IDS = ImmutableSet.of(NpcID.VERZIK_PHASE1_TO2_TRANSITION, NpcID.VERZIK_PHASE1_TO2_TRANSITION_STORY, NpcID.VERZIK_PHASE1_TO2_TRANSITION_HARD);
	protected static final Set<Integer> P2_IDS = ImmutableSet.of(NpcID.VERZIK_PHASE2, NpcID.VERZIK_PHASE2_STORY, NpcID.VERZIK_PHASE2_HARD);
	protected static final Set<Integer> P23_TRANSITION_IDS = ImmutableSet.of(NpcID.VERZIK_PHASE2_TO3_TRANSITION, NpcID.VERZIK_PHASE2_TO3_TRANSITION_STORY, NpcID.VERZIK_PHASE2_TO3_TRANSITION_HARD);
	protected static final Set<Integer> P3_IDS = ImmutableSet.of(NpcID.VERZIK_PHASE3, NpcID.VERZIK_PHASE3_STORY, NpcID.VERZIK_PHASE3_HARD);
	protected static final Set<Integer> DEAD_IDS = ImmutableSet.of(NpcID.VERZIK_DEATH_BAT, NpcID.VERZIK_DEATH_BAT_STORY, NpcID.VERZIK_DEATH_BAT_HARD);
	protected static final Set<Integer> WEB_IDS = ImmutableSet.of(NpcID.VERZIK_WEB_NPC, NpcID.VERZIK_WEB_NPC_STORY, NpcID.VERZIK_WEB_NPC_HARD);
	protected static final Set<Integer> AGGRO_IDS = ImmutableSet.of(
		NpcID.VERZIK_NYLOCAS_MELEE, NpcID.VERZIK_NYLOCAS_RANGED, NpcID.VERZIK_NYLOCAS_MAGIC,
		NpcID.VERZIK_NYLOCAS_MELEE_STORY, NpcID.VERZIK_NYLOCAS_RANGED_STORY, NpcID.VERZIK_NYLOCAS_MAGIC_STORY,
		NpcID.VERZIK_NYLOCAS_MELEE_HARD, NpcID.VERZIK_NYLOCAS_RANGED_HARD, NpcID.VERZIK_NYLOCAS_MAGIC_HARD);
	protected static final Set<Integer> NADO_IDS = ImmutableSet.of(NpcID.TOB_VERZIK_CREEPER, NpcID.TOB_VERZIK_CREEPER_STORY, NpcID.TOB_VERZIK_CREEPER_HARD);
	protected static final Set<Integer> VERZIK_ACTIVE_IDS = ImmutableSet.<Integer>builder().addAll(P1_IDS).addAll(P12_TRANSITION_IDS).addAll(P2_IDS).addAll(P23_TRANSITION_IDS).addAll(P3_IDS).build();

	private NPC verzikNPC;
	private boolean verzikActive;
	private final HashSet<NPC> verzikAggros = new HashSet<>();
	private int verzikTicksUntilAttack = -1;
	private int verzikTotalTicksUntilAttack = 0;
	private boolean verzikEnraged = false;
	private boolean verzikFirstEnraged = false;
	private int verzikAttackCount;
	protected Phase verzikPhase;
	private boolean verzikTickPaused = true;
	protected boolean verzikRedPhase = false;
	private SpecialAttack verzikSpecial;
	private int verzikLastAnimation;

	public final Map<Projectile, WorldPoint> verzikRangeProjectiles = new HashMap<>();
	private final Map<Projectile, WorldPoint> purpleCrabLandingPoint = new HashMap<>();
	private NPC purpleCrabNpc = null;
	public int purpleAttacksLeft = 0;

	public final List<Integer> SERPS = List.of(ItemID.SERPENTINE_HELM_CHARGED, ItemID.SERPENTINE_HELM_CHARGED_CYAN, ItemID.SERPENTINE_HELM_CHARGED_RED);

	public boolean yellowsOut;
	public int yellowTimer;
	public int hmYellowSpotNum;

	public final ArrayList<GameObject> acidSpots = new ArrayList<>();
	public final ArrayList<Integer> acidSpotsTimer = new ArrayList<>();

	public int lightningAttacks;
	public int lightningAttacksDelay;
	private final Map<Projectile, Integer> verzikLightningProjectiles = new HashMap<>();

	public ArrayList<ArrayList<WorldPoint>> yellowGroups;
	private ArrayList<WorldPoint> yellows;
	public ArrayList<WorldPoint> yellowsList;

	private NPC personalNado = null;
	public ArrayList<TornadoTracker> nadoList;
	private WorldPoint prevPlayerWp;
	private int nadosOut;
	private int personalNadoRespawn = 0;
	private final ArrayList<String> partyMembersNames = new ArrayList<>();

	public int greenBallBounces = 0;
	public boolean greenBallOut = false;
	public int greenBallDelay = 0;

	public static final Predicate<Integer> valueIsZero = v -> v <= 0;
	public static final BiFunction<Object, Integer, Integer> updateTicks = (k, v) -> v - 1;

	@Inject
	private Verzik(SpoonTobPlugin plugin, SpoonTobConfig config)
	{
		super(plugin, config);
		this.verzikSpecial = SpecialAttack.NONE;
		this.verzikLastAnimation = -1;
		this.yellowTimer = 14;
		this.hmYellowSpotNum = 1;
		this.lightningAttacks = 4;
		this.lightningAttacksDelay = 0;
		this.yellowGroups = new ArrayList<>();
		this.yellows = new ArrayList<>();
		this.yellowsList = new ArrayList<>();
		this.nadosOut = 0;
		this.nadoList = new ArrayList<>();
	}

	public void load()
	{
		this.overlayManager.add((Overlay) this.verzikOverlay);
		this.overlayManager.add((Overlay) this.lightningPanel);
		this.overlayManager.add((Overlay) this.yellowGroupOverlay);
		this.overlayManager.add((Overlay) this.greenBallPanel);
		this.overlayManager.add((Overlay) this.purpleCrabPanel);
		this.overlayManager.add((Overlay) this.redsOverlay);
		this.poisonStyle = null;
	}

	public void unload()
	{
		this.overlayManager.remove((Overlay) this.verzikOverlay);
		this.overlayManager.remove((Overlay) this.lightningPanel);
		this.overlayManager.remove((Overlay) this.yellowGroupOverlay);
		this.overlayManager.remove((Overlay) this.greenBallPanel);
		this.overlayManager.remove((Overlay) this.purpleCrabPanel);
		this.overlayManager.remove((Overlay) this.redsOverlay);
		verzikCleanup();
		this.plugin.clearHiddenNpcs();
		this.poisonStyle = null;
	}

	@Subscribe
	public void onNpcSpawned(NpcSpawned npcSpawned)
	{
		NPC npc = npcSpawned.getNpc();
		int id = npc.getId();
		if (P1_IDS.contains(id))
		{
			this.verzikPhase = Phase.PHASE1;
			verzikSpawn(npc);
		}
		else if (P2_IDS.contains(id))
		{
			this.verzikPhase = Phase.PHASE2;
			verzikSpawn(npc);
			this.lightningAttacks = 4;
		}
		else if (P3_IDS.contains(id))
		{
			this.verzikPhase = Phase.PHASE3;
			verzikSpawn(npc);
		}
		else if (BEFORE_START_IDS.contains(id) || P12_TRANSITION_IDS.contains(id) || P23_TRANSITION_IDS.contains(id) || DEAD_IDS.contains(id))
		{
			verzikSpawn(npc);
		}
		else if (WEB_IDS.contains(id))
		{
			if (this.verzikNPC != null && this.verzikNPC.getInteracting() == null)
			{
				this.verzikSpecial = SpecialAttack.WEBS;
			}
		}
		else if (AGGRO_IDS.contains(id))
		{
			this.verzikAggros.add(npc);
		}
		else if (NADO_IDS.contains(id))
		{
			if (this.personalNado == null && this.personalNadoRespawn == 0)
			{
				this.nadoList.add(new TornadoTracker(npc));
			}
			if (!this.verzikEnraged)
			{
				this.verzikEnraged = true;
				this.verzikFirstEnraged = true;
			}
			this.nadosOut++;
		}

		if ("Nylocas Athanatos".equals(npc.getName()))
		{
			this.purpleCrabNpc = npc;
		}
	}

	@Subscribe
	public void onNpcChanged(NpcChanged event)
	{
		int id = event.getNpc().getId();
		if (DEAD_IDS.contains(id))
		{
			verzikCleanup();
		}
		else if (P1_IDS.contains(id))
		{
			this.partyMembersNames.clear();
			for (int i = 330; i < 335; i++)
			{
				String varcStr = this.client.getVarcStrValue(i);
				if (varcStr != null && !varcStr.isEmpty())
				{
					this.partyMembersNames.add(varcStr.replaceAll("[^A-Za-z0-9_-]", " ").trim());
				}
			}
		}
	}

	@Subscribe
	public void onNpcDespawned(NpcDespawned npcDespawned)
	{
		NPC npc = npcDespawned.getNpc();
		int id = npc.getId();
		if (VERZIK_ACTIVE_IDS.contains(id) || BEFORE_START_IDS.contains(id) || DEAD_IDS.contains(id))
		{
			verzikCleanup();
		}
		if (AGGRO_IDS.contains(id))
		{
			this.verzikAggros.remove(npc);
		}
		else if (NADO_IDS.contains(id))
		{
			if (this.personalNado == npc)
			{
				this.personalNado = null;
				this.personalNadoRespawn = 18;
			}
			this.nadoList.removeIf(tt -> tt.getNpc() == npc);
			this.nadosOut--;
			if (this.plugin.hiddenIndices.contains(npc.getIndex()))
			{
				this.plugin.setHiddenNpc(npc, false);
			}
		}

		if ("Nylocas Athanatos".equals(npc.getName()))
		{
			this.purpleCrabNpc = null;
		}
	}

	@Subscribe
	public void onProjectileMoved(ProjectileMoved event)
	{
		Projectile p = event.getProjectile();
		int id = p.getId();
		int ticks = (int) Math.round(p.getRemainingCycles() / 30.0D);
		if (id == SpotanimID.VERZIK_PHASE2_RANGED)
		{
			WorldPoint wp = WorldPoint.fromLocal(this.client, event.getPosition());
			this.verzikRangeProjectiles.put(p, wp);
			if (this.lightningAttacksDelay == 0)
			{
				this.lightningAttacks--;
				this.lightningAttacksDelay = 4;
			}
		}
		else if (id == SpotanimID.VERZIK_PHASE2_SPAWN_ARMOUREDTANK_PROJ)
		{
			if (!this.purpleCrabLandingPoint.containsKey(p))
			{
				this.purpleCrabLandingPoint.put(p, predictLandingPoint(p));
			}
			this.purpleAttacksLeft = 21;
		}
		else if (id == SpotanimID.VERZIK_PHASE2_LIGHTNING && this.lightningAttacks < 2)
		{
			this.lightningAttacks = 4;
			if (ticks > 0)
			{
				this.verzikLightningProjectiles.putIfAbsent(p, ticks);
			}
		}
		else if (id == SpotanimID.VERZIK_ACIDBOMB_PROJANIM)
		{
			if (!this.greenBallOut)
			{
				this.greenBallOut = true;
			}
			if (p.getRemainingCycles() == 0)
			{
				this.greenBallOut = false;
				this.greenBallBounces++;
				this.greenBallDelay = 3;
			}
		}
	}

	/**
	 * Predicts the purple (Nylocas Athanatos) spawn projectile's landing tile once, up front,
	 * rather than recording a breadcrumb trail of the ball's in-flight position every tick (the
	 * original approach) - a single stable predicted tile is far more useful than a fading trail
	 * of "where the ball recently was". {@link Projectile#getTargetPoint()} already gives the
	 * game's own authoritative landing point, so no manual velocity math is needed here (unlike
	 * yuritheatre's equivalent feature, which was written against an older API that only exposed
	 * raw velocity components).
	 */
	private WorldPoint predictLandingPoint(Projectile p)
	{
		return p.getTargetPoint();
	}

	@Subscribe
	public void onGraphicsObjectCreated(GraphicsObjectCreated event)
	{
		if (event.getGraphicsObject().getId() == SpotanimID.VERZIK_POWERBLAST_SAFEZONE && !this.yellowsOut)
		{
			WorldPoint wp = WorldPoint.fromLocal(this.client, event.getGraphicsObject().getLocation());
			if (!this.yellows.contains(wp))
			{
				this.yellows.add(wp);
			}
			if (!this.yellowsList.contains(wp))
			{
				this.yellowsList.add(wp);
			}
		}
	}

	@Subscribe
	public void onGameObjectDespawned(GameObjectDespawned event)
	{
		if (this.config.showVerzikAcid() && event.getGameObject().getId() == ObjectID.VERZIK_ACID_POOL)
		{
			int index = this.acidSpots.indexOf(event.getGameObject());
			this.acidSpots.remove(event.getGameObject());
			if (index != -1)
			{
				this.acidSpotsTimer.remove(index);
			}
		}
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (event.getGroup().equals("tobqol v2") && event.getKey().equals("hideOtherNados") && !this.config.hideOtherNados())
		{
			this.plugin.clearHiddenNpcs();
		}
	}

	@Subscribe
	public void onMenuEntryAdded(MenuEntryAdded event)
	{
		if (this.verzikNPC == null)
		{
			return;
		}
		String target = event.getTarget();
		Player player = this.client.getLocalPlayer();
		PlayerComposition playerComp = (player != null) ? player.getPlayerComposition() : null;
		MenuEntry entry = event.getMenuEntry();

		if (this.config.hidePurple() && P2_IDS.contains(this.verzikNPC.getId()) && target.contains("Nylocas Athanatos")
			&& event.getType() == MenuAction.NPC_SECOND_OPTION.getId() && this.poisonStyle == PoisonStyle.NOT)
		{
			if (playerComp != null && !this.SERPS.contains(playerComp.getEquipmentId(KitType.HEAD)))
			{
				entry.setDeprioritized(true);
			}
		}
		else if (P3_IDS.contains(this.verzikNPC.getId()) && this.config.hideAttackYellows() && this.verzikSpecial == SpecialAttack.YELLOWS
			&& this.verzikTicksUntilAttack > 8 && target.contains("Verzik Vitur") && event.getType() == MenuAction.NPC_SECOND_OPTION.getId())
		{
			entry.setDeprioritized(true);
		}
	}

	@Subscribe
	public void onMenuOptionClicked(MenuOptionClicked event)
	{
		if (event.getMenuOption().equalsIgnoreCase("wield"))
		{
			PoisonStyle newStyle = PoisonWeaponMap.PoisonType.get(event.getItemId());
			if (newStyle != null)
			{
				this.skipTickCheck = true;
				this.poisonStyle = newStyle;
			}
		}
	}

	@Subscribe
	public void onActorDeath(ActorDeath event)
	{
		if (this.verzikNPC != null && event.getActor() instanceof Player && event.getActor().getName() != null)
		{
			this.partyMembersNames.remove(event.getActor().getName());
		}
	}

	@Subscribe
	public void onGraphicChanged(GraphicChanged event)
	{
		if (event.getActor() == null || event.getActor().getName() == null || !(event.getActor() instanceof Player) || this.verzikPhase != Phase.PHASE3)
		{
			return;
		}
		Actor actor = event.getActor();
		Player localPlayer = this.client.getLocalPlayer();
		if (actor.getGraphic() == 1602 && localPlayer != null && actor.getName().equals(localPlayer.getName()))
		{
			this.personalNado = null;
			this.personalNadoRespawn = 18;
			if (this.nadoList.size() == 1)
			{
				this.nadoList.clear();
			}
		}
	}

	@Subscribe
	public void onGameTick(GameTick event)
	{
		if (!this.verzikActive)
		{
			return;
		}
		updatePoisonStyle();
		if (this.lightningAttacksDelay > 0)
		{
			this.lightningAttacksDelay--;
		}
		if (this.personalNadoRespawn > 0)
		{
			this.personalNadoRespawn--;
		}
		if (this.greenBallDelay > 0)
		{
			this.greenBallDelay--;
			if (this.greenBallDelay == 0 && !this.greenBallOut)
			{
				this.greenBallBounces = 0;
			}
		}
		for (int i = 0; i < this.acidSpotsTimer.size(); i++)
		{
			this.acidSpotsTimer.set(i, this.acidSpotsTimer.get(i) - 1);
		}
		this.verzikRangeProjectiles.keySet().removeIf(projectile -> projectile.getRemainingCycles() < 1);
		updateYellows();
		updateNadoTracking();
		updateVerzikAttackTimer();
		if (!this.purpleCrabLandingPoint.isEmpty())
		{
			this.purpleCrabLandingPoint.keySet().removeIf(projectile -> projectile.getRemainingCycles() < 1);
		}
		if (this.verzikPhase == Phase.PHASE2)
		{
			for (Iterator<Projectile> it = this.verzikLightningProjectiles.keySet().iterator(); it.hasNext(); )
			{
				Projectile key = it.next();
				this.verzikLightningProjectiles.replace(key, this.verzikLightningProjectiles.get(key) - 1);
				if (this.verzikLightningProjectiles.get(key) < 0)
				{
					it.remove();
				}
			}
		}
	}

	private void updatePoisonStyle()
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
		this.poisonStyle = PoisonWeaponMap.PoisonType.get(equippedWeapon);
	}

	private void updateYellows()
	{
		if (this.verzikPhase != Phase.PHASE3 || this.yellowsList.isEmpty())
		{
			return;
		}
		if (!this.yellowsOut)
		{
			if (this.verzikNPC.getId() == NpcID.VERZIK_PHASE3_HARD)
			{
				this.yellowGroups = findYellows(this.yellows);
			}
			this.yellowsOut = true;
			return;
		}
		this.yellowTimer--;
		if (this.yellowTimer > 0)
		{
			return;
		}
		if (this.verzikNPC.getId() != NpcID.VERZIK_PHASE3_HARD)
		{
			resetYellows();
			return;
		}
		if (this.hmYellowSpotNum >= 3)
		{
			resetYellows();
			this.hmYellowSpotNum = 1;
			return;
		}
		this.yellowTimer = 3;
		this.hmYellowSpotNum++;
		removeSteppedOnYellows();
	}

	private void resetYellows()
	{
		this.yellowsOut = false;
		this.yellowTimer = 14;
		this.yellows.clear();
		this.yellowsList.clear();
		this.yellowGroups.clear();
	}

	private void removeSteppedOnYellows()
	{
		for (Player p : this.client.getPlayers())
		{
			if (p.getName() == null || !this.partyMembersNames.contains(p.getName()))
			{
				continue;
			}
			WorldPoint wp = WorldPoint.fromLocal(this.client, p.getLocalLocation());
			int index = 0;
			boolean removedFromGroup = false;
			for (ArrayList<WorldPoint> yg : this.yellowGroups)
			{
				if (yg.contains(wp))
				{
					this.yellowGroups.get(index).remove(wp);
					removedFromGroup = true;
					break;
				}
				for (int j = yg.size() - 1; j >= 0; j--)
				{
					if (yg.get(j).distanceTo(wp) <= 1)
					{
						this.yellowGroups.get(index).remove(j);
						removedFromGroup = true;
						break;
					}
				}
				if (removedFromGroup)
				{
					break;
				}
				index++;
			}
			if (this.yellowsList.contains(wp))
			{
				this.yellowsList.remove(wp);
				continue;
			}
			for (int i = this.yellowsList.size() - 1; i >= 0; i--)
			{
				if (this.yellowsList.get(i).distanceTo(wp) <= 1)
				{
					this.yellowsList.remove(i);
					break;
				}
			}
		}
	}

	private void updateNadoTracking()
	{
		if (this.verzikPhase != Phase.PHASE3 || this.nadoList.isEmpty() || this.client.getLocalPlayer() == null)
		{
			return;
		}
		boolean recalc = false;
		for (int i = this.nadoList.size() - 1; i >= 0; i--)
		{
			TornadoTracker tracker = this.nadoList.get(i);
			if (tracker.checkMovement(this.prevPlayerWp, tracker.getNpc().getWorldLocation()) != -1)
			{
				this.nadoList.remove(i);
				if (this.nadoList.isEmpty())
				{
					for (NPC npc : this.client.getNpcs())
					{
						if (NADO_IDS.contains(npc.getId()))
						{
							this.nadoList.add(new TornadoTracker(npc));
						}
						this.personalNado = null;
						recalc = true;
						if (this.plugin.hiddenIndices.contains(npc.getIndex()))
						{
							this.plugin.setHiddenNpc(npc, false);
						}
					}
				}
			}
			else
			{
				tracker.setPrevLoc(tracker.getNpc().getWorldLocation());
			}
		}
		if (this.nadoList.size() == 1 && this.personalNado == null && !recalc)
		{
			this.personalNado = this.nadoList.get(0).getNpc();
		}
		this.prevPlayerWp = this.client.getLocalPlayer().getWorldLocation();
	}

	private void updateVerzikAttackTimer()
	{
		Function<Integer, Integer> adjustForEnrage = i -> isVerzikEnraged() ? (i - 2) : i;
		if (this.verzikTickPaused)
		{
			int id = this.verzikNPC.getId();
			if (P1_IDS.contains(id))
			{
				this.verzikPhase = Phase.PHASE1;
				this.verzikAttackCount = 0;
				this.verzikTicksUntilAttack = 18;
				this.verzikTickPaused = false;
			}
			else if (P2_IDS.contains(id))
			{
				this.verzikPhase = Phase.PHASE2;
				this.verzikAttackCount = 0;
				this.verzikTicksUntilAttack = 3;
				this.verzikTickPaused = false;
				this.purpleAttacksLeft = 21;
			}
			else if (P3_IDS.contains(id))
			{
				this.verzikPhase = Phase.PHASE3;
				this.verzikAttackCount = 0;
				this.verzikTicksUntilAttack = 6;
				this.verzikTickPaused = false;
			}
			return;
		}
		if (this.verzikSpecial == SpecialAttack.WEBS)
		{
			this.verzikTotalTicksUntilAttack++;
			if (this.verzikNPC.getInteracting() != null)
			{
				this.verzikSpecial = SpecialAttack.WEB_COOLDOWN;
				this.verzikAttackCount = 10;
				this.verzikTicksUntilAttack = 10;
				this.verzikFirstEnraged = false;
			}
			return;
		}

		this.verzikTicksUntilAttack = Math.max(0, this.verzikTicksUntilAttack - 1);
		this.verzikTotalTicksUntilAttack++;
		int animationID = this.verzikNPC.getAnimation();
		if (animationID > -1 && this.verzikPhase == Phase.PHASE1 && this.verzikTicksUntilAttack < 5 && animationID != this.verzikLastAnimation && animationID == 8109)
		{
			this.verzikTicksUntilAttack = 14;
			this.verzikAttackCount++;
		}
		if (animationID > -1 && this.verzikPhase == Phase.PHASE2 && this.verzikTicksUntilAttack < 3 && animationID != this.verzikLastAnimation)
		{
			if (animationID == 8114 || animationID == 8116)
			{
				this.verzikTicksUntilAttack = 4;
				this.verzikAttackCount++;
				this.purpleAttacksLeft--;
				if (this.verzikAttackCount == 7 && this.verzikRedPhase)
				{
					this.verzikTicksUntilAttack = 8;
				}
			}
			else if (animationID == 8117)
			{
				this.verzikRedPhase = true;
				this.verzikAttackCount = 0;
				this.verzikTicksUntilAttack = 12;
			}
		}
		this.verzikLastAnimation = animationID;
		if (this.verzikPhase == Phase.PHASE3)
		{
			this.verzikAttackCount %= 20;
			if (this.verzikTicksUntilAttack <= 0)
			{
				this.verzikAttackCount++;
				if (this.verzikAttackCount < 15)
				{
					this.verzikSpecial = SpecialAttack.NONE;
					this.verzikTicksUntilAttack = adjustForEnrage.apply(7);
				}
				else if (this.verzikAttackCount < 16)
				{
					this.verzikSpecial = SpecialAttack.YELLOWS;
					this.verzikTicksUntilAttack = (this.verzikNPC.getId() == NpcID.VERZIK_PHASE3_HARD) ? 27 : 21;
				}
				else if (this.verzikAttackCount < 20)
				{
					this.verzikSpecial = SpecialAttack.NONE;
					this.verzikTicksUntilAttack = adjustForEnrage.apply(7);
				}
				else if (this.verzikAttackCount < 21)
				{
					this.verzikSpecial = SpecialAttack.GREEN;
					this.verzikTicksUntilAttack = 12;
				}
				else
				{
					this.verzikSpecial = SpecialAttack.NONE;
					this.verzikTicksUntilAttack = adjustForEnrage.apply(7);
				}
			}
			if (this.verzikFirstEnraged)
			{
				this.verzikFirstEnraged = false;
				if (this.verzikSpecial != SpecialAttack.YELLOWS || this.verzikTicksUntilAttack <= 7)
				{
					this.verzikTicksUntilAttack = 5;
				}
			}
		}
	}

	Color verzikSpecialWarningColor()
	{
		if (this.verzikPhase != Phase.PHASE3)
		{
			return Color.WHITE;
		}
		switch (this.verzikAttackCount)
		{
			case 4:
				return Color.MAGENTA;
			case 9:
				return Color.ORANGE;
			case 14:
				return Color.YELLOW;
			case 19:
				return Color.GREEN;
			default:
				return Color.WHITE;
		}
	}

	private void verzikSpawn(NPC npc)
	{
		this.verzikEnraged = false;
		this.verzikRedPhase = false;
		this.verzikFirstEnraged = false;
		this.verzikTicksUntilAttack = 0;
		this.verzikAttackCount = 0;
		this.verzikNPC = npc;
		this.verzikActive = true;
		this.verzikTickPaused = true;
		this.verzikSpecial = SpecialAttack.NONE;
		this.verzikTotalTicksUntilAttack = 0;
		this.verzikLastAnimation = -1;
	}

	private void verzikCleanup()
	{
		this.verzikAggros.clear();
		this.verzikEnraged = false;
		this.verzikFirstEnraged = false;
		this.verzikRedPhase = false;
		this.verzikActive = false;
		this.yellowsList.clear();
		this.yellowGroups.clear();
		this.yellowsOut = false;
		this.yellowTimer = 14;
		this.hmYellowSpotNum = 1;
		this.nadoList.clear();
		this.prevPlayerWp = null;
		this.personalNado = null;
		this.nadosOut = 0;
		this.verzikNPC = null;
		this.verzikPhase = null;
		this.verzikTickPaused = true;
		this.verzikSpecial = SpecialAttack.NONE;
		this.verzikTotalTicksUntilAttack = 0;
		this.verzikLastAnimation = -1;
		this.acidSpots.clear();
		this.acidSpotsTimer.clear();
		this.lightningAttacks = 4;
		this.lightningAttacksDelay = 0;
		this.greenBallBounces = 0;
		this.greenBallOut = false;
		this.greenBallDelay = 0;
		this.purpleCrabLandingPoint.clear();
	}

	enum SpecialAttack
	{
		WEB_COOLDOWN, WEBS, YELLOWS, GREEN, NONE
	}

	public enum Phase
	{
		PHASE1, PHASE2, PHASE3
	}

	public WorldPoint getNearestPoint(WorldPoint corner, ArrayList<WorldPoint> points)
	{
		double minDistance = Double.MAX_VALUE;
		WorldPoint point = new WorldPoint(corner.getX(), corner.getY(), corner.getPlane());
		for (WorldPoint p : points)
		{
			double distance = distanceBetween(p, corner);
			if (distance < minDistance)
			{
				minDistance = distance;
				point = p;
			}
		}
		return point;
	}

	public int isSetSpawn(WorldPoint p)
	{
		if (p.getRegionX() == 7 && p.getRegionY() == 11)
		{
			return 1;
		}
		if (p.getRegionX() == 16 && p.getRegionY() == 17)
		{
			return 2;
		}
		if (p.getRegionX() == 25 && p.getRegionY() == 11)
		{
			return 3;
		}
		if (p.getRegionX() == 7 && p.getRegionY() == 23)
		{
			return 4;
		}
		if (p.getRegionX() == 25 && p.getRegionY() == 23)
		{
			return 5;
		}
		return -1;
	}

	public WorldPoint getNextValidPoint(ArrayList<WorldPoint> points)
	{
		for (WorldPoint p : points)
		{
			if (isSetSpawn(p) != -1)
			{
				return p;
			}
		}
		return null;
	}

	public ArrayList<ArrayList<WorldPoint>> findYellows(ArrayList<WorldPoint> points)
	{
		ArrayList<ArrayList<WorldPoint>> groups = new ArrayList<>();
		while (!points.isEmpty())
		{
			ArrayList<WorldPoint> group = new ArrayList<>();
			WorldPoint initial = getNextValidPoint(points);
			group.add(initial);
			points.remove(initial);
			WorldPoint second = getNearestPoint(initial, points);
			group.add(second);
			points.remove(second);
			WorldPoint third = getNearestPoint(initial, points);
			group.add(third);
			points.remove(third);
			groups.add(group);
		}
		return groups;
	}

	public double distanceBetween(WorldPoint a, WorldPoint b)
	{
		return Math.sqrt(Math.pow(a.getRegionX() - b.getRegionX(), 2.0D) + Math.pow(a.getRegionY() - b.getRegionY(), 2.0D));
	}

	@Subscribe
	public void onClientTick(ClientTick event)
	{
		if (!this.config.hideOtherNados())
		{
			return;
		}
		for (NPC npc : this.client.getNpcs())
		{
			if (npc != null && NADO_IDS.contains(npc.getId()) && this.personalNado != null && this.personalNado.getIndex() != npc.getIndex()
				&& !this.plugin.hiddenIndices.contains(npc.getIndex()))
			{
				this.plugin.setHiddenNpc(npc, true);
			}
		}
	}

	public NPC getVerzikNPC()
	{
		return this.verzikNPC;
	}

	public boolean isVerzikActive()
	{
		return this.verzikActive;
	}

	public HashSet<NPC> getVerzikAggros()
	{
		return this.verzikAggros;
	}

	public int getVerzikTicksUntilAttack()
	{
		return this.verzikTicksUntilAttack;
	}

	public int getVerzikTotalTicksUntilAttack()
	{
		return this.verzikTotalTicksUntilAttack;
	}

	public boolean isVerzikEnraged()
	{
		return this.verzikEnraged;
	}

	public int getVerzikAttackCount()
	{
		return this.verzikAttackCount;
	}

	public Phase getVerzikPhase()
	{
		return this.verzikPhase;
	}

	public SpecialAttack getVerzikSpecial()
	{
		return this.verzikSpecial;
	}

	public Map<Projectile, WorldPoint> getVerzikRangeProjectiles()
	{
		return this.verzikRangeProjectiles;
	}

	public Map<Projectile, WorldPoint> getPurpleCrabLandingPoint()
	{
		return this.purpleCrabLandingPoint;
	}

	public NPC getPurpleCrabNpc()
	{
		return this.purpleCrabNpc;
	}

	public Map<Projectile, Integer> getVerzikLightningProjectiles()
	{
		return this.verzikLightningProjectiles;
	}

	public NPC getPersonalNado()
	{
		return this.personalNado;
	}
}
