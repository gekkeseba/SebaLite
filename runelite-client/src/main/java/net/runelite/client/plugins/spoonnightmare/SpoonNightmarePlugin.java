package net.runelite.client.plugins.spoonnightmare;

import com.google.common.collect.ImmutableSet;
import com.google.inject.Provides;
import java.awt.Color;
import java.io.BufferedInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import javax.inject.Inject;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.FloatControl;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.EnumComposition;
import net.runelite.api.EnumID;
import net.runelite.api.GameObject;
import net.runelite.api.GameState;
import net.runelite.api.GraphicsObject;
import net.runelite.api.HitsplatID;
import net.runelite.api.ItemComposition;
import net.runelite.api.MenuAction;
import net.runelite.api.MenuEntry;
import net.runelite.api.NPC;
import net.runelite.api.ParamID;
import net.runelite.api.Player;
import net.runelite.api.Point;
import net.runelite.api.Prayer;
import net.runelite.api.Scene;
import net.runelite.api.ScriptID;
import net.runelite.api.Tile;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.ActorDeath;
import net.runelite.api.events.AnimationChanged;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.ClientTick;
import net.runelite.api.events.GameObjectDespawned;
import net.runelite.api.events.GameObjectSpawned;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.GraphicsObjectCreated;
import net.runelite.api.events.HitsplatApplied;
import net.runelite.api.events.MenuEntryAdded;
import net.runelite.api.events.NpcChanged;
import net.runelite.api.events.NpcDespawned;
import net.runelite.api.events.NpcSpawned;
import net.runelite.api.events.ScriptPostFired;
import net.runelite.api.events.SoundEffectPlayed;
import net.runelite.api.events.WidgetLoaded;
import net.runelite.api.gameval.AnimationID;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.NpcID;
import net.runelite.api.gameval.ObjectID;
import net.runelite.api.gameval.SpotanimID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.Text;

@PluginDescriptor(
	name = "Phosani's Nightmare",
	description = "Visual helper overlays for The Nightmare and Phosani's Nightmare",
	tags = {"nightmare", "ashihama", "phosani", "spoon"},
	enabledByDefault = false
)
public class SpoonNightmarePlugin extends Plugin
{
	// The base "Protect from X" prayers exist in every prayer book, but RuneLite no longer exposes
	// fixed widget constants for them - since prayer tab slots became reorderable, positions are
	// dynamic. This resolves the current on-screen widget for a given prayer the same way the
	// vanilla prayer-reorder plugin (PrayerReorder.java) does: match the prayer's item name within
	// the currently-unlocked prayer book enum, then read its widget component via OC_PRAYER_COMPONENT.
	private static final String PRAYER_NAME_MAGIC = "Protect from Magic";
	private static final String PRAYER_NAME_MISSILES = "Protect from Missiles";
	private static final String PRAYER_NAME_MELEE = "Protect from Melee";

	// Prayer icon sprite IDs, matching NightmareAttack's sprite/childId scheme.
	private static final int SPRITE_MAGIC = 127;
	private static final int SPRITE_MISSILES = 128;
	private static final int SPRITE_MELEE = 129;

	// Sound effects for the black hands spawning - no gameval SoundEffectID constant exists for these.
	private static final Set<Integer> HAND_SPAWN_SOUND_IDS = ImmutableSet.of(4307, 4274, 4228, 4322);

	private static final Set<Integer> TOTEM_READY_IDS = ImmutableSet.of(
		NpcID.NIGHTMARE_TOTEM_1_READY, NpcID.NIGHTMARE_TOTEM_2_READY, NpcID.NIGHTMARE_TOTEM_3_READY, NpcID.NIGHTMARE_TOTEM_4_READY);
	private static final Set<Integer> TOTEM_CHARGED_IDS = ImmutableSet.of(
		NpcID.NIGHTMARE_TOTEM_1_CHARGED, NpcID.NIGHTMARE_TOTEM_2_CHARGED, NpcID.NIGHTMARE_TOTEM_3_CHARGED, NpcID.NIGHTMARE_TOTEM_4_CHARGED);
	private static final Set<Integer> HUSK_IDS = ImmutableSet.of(
		NpcID.NIGHTMARE_HUSK_MAGIC, NpcID.NIGHTMARE_HUSK_RANGED, NpcID.NIGHTMARE_CHALLENGE_HUSK_MAGIC, NpcID.NIGHTMARE_CHALLENGE_HUSK_RANGED);

	// Prayer names to keep visible when "Hide Prayers" is on - everything else in the book is hidden.
	private static final String[] KEPT_PRAYER_NAMES = {
		"piety", "augury", "preserve", "protect from melee", "protect from magic", "protect from missiles", "redemption", "rapid heal"
	};

	@Inject
	private Client client;
	@Inject
	private OverlayManager overlayManager;
	@Inject
	private SpoonNightmareOverlay overlay;
	@Inject
	private PrayerOverlay prayOverlay;
	@Inject
	private SanfewOverlay sanfewOverlay;
	@Inject
	private TickOverlay ticksOverlay;
	@Inject
	private NightmarePrayerOverlay prayerOverlay;
	@Inject
	private YawnOverlay yawnOverlay;
	@Inject
	private SpoonNightmareConfig config;
	@Inject
	private ClientThread clientThread;

	private String correctPray = "";
	private boolean cursePhase;
	private boolean impregnated;
	private WorldPoint bossLoc;
	private int parasiteTicks;
	private boolean preggers;
	private final ArrayList<NPC> parasiteList = new ArrayList<>();
	private boolean preparedForTakeoff = false;
	private int flightTime = 5;
	private final ArrayList<Color> raveRunway = new ArrayList<>();
	private boolean totemsActive = false;
	private final ArrayList<TotemInfo> totemList = new ArrayList<>();
	private final ArrayList<GameObject> shrooms = new ArrayList<>();
	private int mushroomTicks = 31;
	private boolean mushroomActive = false;
	private final ArrayList<NPC> husks = new ArrayList<>();
	private int ticksUntilAttack = -1;
	private int eventTicks = -1;
	private NPC nightmareNpc;
	private boolean activeFight = false;
	private Clip clip;
	private int handsDelay = 5;
	private boolean handsOut = false;
	private final ArrayList<GraphicsObject> handsLocation = new ArrayList<>();
	private final ArrayList<Color> raveHandsColors = new ArrayList<>();
	private int sleepwalkerAlive = 0;
	private boolean yawning = false;
	private int yawnTicks = 26;
	private boolean flowersOut = false;
	private final ArrayList<Color> raveTotemColors = new ArrayList<>();
	private int sleepwalkerCount = 0;
	private int huskCount = 0;
	private int parasiteCount = 0;
	private int nightmareHealingCount = 0;
	private int totemHealingCount = 0;
	private int hellPhaseSleepwalkerCount = 0;
	private Point originalMagePosition = null;
	private Point originalMissilesPosition = null;
	private Point originalMeleePosition = null;
	private boolean reorderActive = false;
	private final Set<LocalPoint> flowerTiles = new HashSet<>();
	private int flowerTickCount = 0;
	private boolean flowersActive = false;
	private boolean flowersActive2 = false;

	@Provides
	SpoonNightmareConfig getConfig(ConfigManager configManager)
	{
		return configManager.getConfig(SpoonNightmareConfig.class);
	}

	@Override
	protected void startUp()
	{
		setOriginalPositions();
		reset();
		this.overlayManager.add((Overlay) this.overlay);
		this.overlayManager.add((Overlay) this.sanfewOverlay);
		this.overlayManager.add((Overlay) this.prayOverlay);
		this.overlayManager.add((Overlay) this.ticksOverlay);
		this.overlayManager.add((Overlay) this.prayerOverlay);
		this.overlayManager.add((Overlay) this.yawnOverlay);
		try
		{
			AudioInputStream stream = AudioSystem.getAudioInputStream(new BufferedInputStream(SpoonNightmarePlugin.class.getResourceAsStream("a10Strafe.wav")));
			AudioFormat format = stream.getFormat();
			DataLine.Info info = new DataLine.Info(Clip.class, format);
			this.clip = (Clip) AudioSystem.getLine(info);
			this.clip.open(stream);
			FloatControl control = (FloatControl) this.clip.getControl(FloatControl.Type.MASTER_GAIN);
			if (control != null)
			{
				control.setValue(this.config.a10StrafeVolume() / 2 - 45);
			}
		}
		catch (Exception e)
		{
			this.clip = null;
		}
	}

	@Override
	protected void shutDown()
	{
		reset();
		this.overlayManager.remove((Overlay) this.sanfewOverlay);
		this.overlayManager.remove((Overlay) this.overlay);
		this.overlayManager.remove((Overlay) this.prayOverlay);
		this.overlayManager.remove((Overlay) this.ticksOverlay);
		this.overlayManager.remove((Overlay) this.prayerOverlay);
		this.overlayManager.remove((Overlay) this.yawnOverlay);
	}

	private void reset()
	{
		deActivateShuffle();
		this.reorderActive = false;
		this.correctPray = "";
		this.cursePhase = false;
		this.impregnated = false;
		this.preparedForTakeoff = false;
		this.flightTime = 5;
		this.raveRunway.clear();
		this.totemsActive = false;
		this.totemList.clear();
		this.shrooms.clear();
		this.mushroomTicks = 31;
		this.mushroomActive = false;
		this.husks.clear();
		this.ticksUntilAttack = -1;
		this.eventTicks = -1;
		this.activeFight = false;
		this.preggers = false;
		this.nightmareNpc = null;
		this.parasiteList.clear();
		this.handsDelay = 5;
		this.handsOut = false;
		this.handsLocation.clear();
		this.raveHandsColors.clear();
		this.sleepwalkerAlive = 0;
		this.yawning = false;
		this.yawnTicks = 26;
		this.flowersOut = false;
		this.raveTotemColors.clear();
		this.sleepwalkerCount = 0;
		this.huskCount = 0;
		this.parasiteCount = 0;
		this.nightmareHealingCount = 0;
		this.totemHealingCount = 0;
		this.hellPhaseSleepwalkerCount = 0;
		this.flowerTickCount = 0;
		this.flowerTiles.clear();
		this.flowersActive = false;
		this.flowersActive2 = false;
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		// Fires when leaving the instance (region reload) or on a dropped connection - either way,
		// if we still think a fight is in progress, its state is now stale and must be cleared.
		GameState gameState = event.getGameState();
		boolean leavingInstance = gameState == GameState.LOADING || gameState == GameState.CONNECTION_LOST;
		if (leavingInstance && (this.activeFight || this.nightmareNpc != null))
		{
			reset();
		}
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (!event.getGroup().equals("spoonnightmare"))
		{
			return;
		}
		if (event.getKey().equalsIgnoreCase("hidePrayers"))
		{
			if (this.nightmareNpc != null)
			{
				applyPrayerHiding(this.config.hidePrayer());
			}
		}
		else if (event.getKey().equalsIgnoreCase("a10StrafeVoulme") && this.clip != null)
		{
			FloatControl control = (FloatControl) this.clip.getControl(FloatControl.Type.MASTER_GAIN);
			if (control != null)
			{
				control.setValue(this.config.a10StrafeVolume() / 2 - 45);
			}
		}
	}

	@Subscribe
	public void onNpcSpawned(NpcSpawned event)
	{
		NPC npc = event.getNpc();
		if (npc.getName() == null)
		{
			return;
		}
		int id = npc.getId();
		String name = npc.getName();
		if (name.equalsIgnoreCase("the nightmare") || name.equalsIgnoreCase("phosani's nightmare"))
		{
			this.nightmareNpc = npc;
			this.preggers = false;
			if (this.config.hidePrayer())
			{
				applyPrayerHiding(true);
			}
		}
		else if (name.equalsIgnoreCase("parasite"))
		{
			this.preggers = false;
			this.parasiteList.add(npc);
			this.parasiteCount++;
		}
		else if (HUSK_IDS.contains(id))
		{
			this.husks.add(npc);
			this.huskCount++;
		}
		else if (TOTEM_READY_IDS.contains(id))
		{
			this.totemList.add(new TotemInfo(npc, -1));
			this.totemsActive = true;
		}
		else if (name.equalsIgnoreCase("sleepwalker"))
		{
			this.sleepwalkerAlive++;
		}
	}

	@Subscribe
	public void onNpcDespawned(NpcDespawned event)
	{
		NPC npc = event.getNpc();
		if (npc.getName() == null)
		{
			return;
		}
		int id = npc.getId();
		String name = npc.getName();
		if (name.equalsIgnoreCase("the nightmare") || name.equalsIgnoreCase("phosani's nightmare"))
		{
			reset();
			applyPrayerHiding(false);
		}
		else if (TOTEM_READY_IDS.contains(id))
		{
			this.totemList.removeIf(ti -> ti.getNpc() == npc);
			if (this.totemList.isEmpty())
			{
				this.totemsActive = false;
			}
		}
		else if (HUSK_IDS.contains(id))
		{
			this.husks.remove(npc);
		}
		else if (name.equalsIgnoreCase("sleepwalker"))
		{
			this.sleepwalkerAlive--;
		}
	}

	@Subscribe
	public void onNpcChanged(NpcChanged event)
	{
		NPC npc = event.getNpc();
		int id = npc.getId();
		String name = npc.getName();
		if (TOTEM_READY_IDS.contains(id))
		{
			this.totemList.add(new TotemInfo(npc, -1));
			this.totemsActive = true;
		}
		else if (TOTEM_CHARGED_IDS.contains(id))
		{
			this.totemList.removeIf(ti -> ti.getNpc() == npc);
			if (this.totemList.isEmpty())
			{
				this.totemsActive = false;
			}
		}
		else if (name != null && (name.equalsIgnoreCase("the nightmare") || name.equalsIgnoreCase("phosani's nightmare"))
			&& (id == NpcID.NIGHTMARE_DYING || id == NpcID.NIGHTMARE_CHALLENGE_DYING) && this.config.displayStatsMsg())
		{
			announceKillStats(name);
		}
	}

	private void announceKillStats(String name)
	{
		if (this.config.huskStats())
		{
			this.client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", "Husks - <col=ff0000>" + this.huskCount / 2, null);
		}
		if (this.config.parasiteStats())
		{
			this.client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", "Parasites - <col=ff0000>" + this.parasiteCount, null);
		}
		if (this.config.sleepwalkerStats())
		{
			if (name.equalsIgnoreCase("phosani's nightmare"))
			{
				this.client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", "Sleepwalkers - <col=ff0000>" + this.sleepwalkerCount + "</col>     Hell Phase - <col=ff0000>" + this.hellPhaseSleepwalkerCount, null);
			}
			else
			{
				this.client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", "Sleepwalkers - <col=ff0000>" + this.sleepwalkerCount, null);
			}
		}
		if (this.config.healingStats() != SpoonNightmareConfig.healingStatsMode.OFF)
		{
			String msg;
			if (this.config.healingStats() == SpoonNightmareConfig.healingStatsMode.BOTH)
			{
				msg = "Boss Healed - <col=ff0000>" + this.nightmareHealingCount + "</col>     Totems Healed - <col=ff0000>" + this.totemHealingCount;
			}
			else if (this.config.healingStats() == SpoonNightmareConfig.healingStatsMode.BOSS)
			{
				msg = "Boss Healed - <col=ff0000>" + this.nightmareHealingCount;
			}
			else
			{
				msg = "Totems Healed - <col=ff0000>" + this.totemHealingCount;
			}
			this.client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", msg, null);
		}
		this.sleepwalkerCount = 0;
		this.huskCount = 0;
		this.parasiteCount = 0;
		this.nightmareHealingCount = 0;
		this.totemHealingCount = 0;
		this.hellPhaseSleepwalkerCount = 0;
	}

	@Subscribe
	public void onActorDeath(ActorDeath event)
	{
		if (event.getActor() instanceof NPC)
		{
			NPC npc = (NPC) event.getActor();
			String name = npc.getName();
			if (name != null && name.equalsIgnoreCase("parasite"))
			{
				this.parasiteList.remove(npc);
			}
			else if (name != null && name.equalsIgnoreCase("husk"))
			{
				this.husks.remove(npc);
			}
		}
		if (event.getActor() == this.client.getLocalPlayer())
		{
			deActivateShuffle();
		}
	}

	@Subscribe
	public void onAnimationChanged(AnimationChanged event)
	{
		if (!(event.getActor() instanceof NPC))
		{
			return;
		}
		NPC npc = (NPC) event.getActor();
		int npcId = npc.getId();
		int npcAnimId = npc.getAnimation();
		if (isNightmareId(npcId))
		{
			this.nightmareNpc = npc;
			this.activeFight = true;
			applyAttackAnimation(npcId, npcAnimId);
		}
		else if ("sleepwalker".equalsIgnoreCase(npc.getName()) && this.nightmareNpc != null && npcAnimId == AnimationID.SLEEPWALKER_ABSORB && !npc.isDead())
		{
			if (this.nightmareNpc.getId() == NpcID.NIGHTMARE_CHALLENGE_PHASE_05)
			{
				this.hellPhaseSleepwalkerCount++;
			}
			else
			{
				this.sleepwalkerCount++;
			}
		}
	}

	private void applyAttackAnimation(int npcId, int npcAnimId)
	{
		if (npcAnimId == AnimationID.NIGHTMARE_ATTACK_MELEE)
		{
			this.ticksUntilAttack = 7;
			this.correctPray = "melee";
		}
		else if (npcAnimId == AnimationID.NIGHTMARE_ATTACK_MAGIC)
		{
			this.ticksUntilAttack = 7;
			this.correctPray = "magic";
		}
		else if (npcAnimId == AnimationID.NIGHTMARE_ATTACK_RANGED)
		{
			this.ticksUntilAttack = 7;
			this.correctPray = "missiles";
		}
		else if (npcAnimId == AnimationID.NIGHTMARE_ATTACK_RIFT)
		{
			this.ticksUntilAttack = 10;
		}
		else if (npcAnimId == AnimationID.NIGHTMARE_ATTACK_CONFUSION)
		{
			this.ticksUntilAttack = 7;
			this.cursePhase = true;
			this.eventTicks = 36;
		}
		else if (npcAnimId == AnimationID.NIGHTMARE_ATTACK_INFECTION || npcAnimId == AnimationID.NIGHTMARE_ATTACK_TRANCE)
		{
			this.ticksUntilAttack = 7;
		}
		else if (npcAnimId == AnimationID.NIGHTMARE_ATTACK_SEGMENT)
		{
			this.eventTicks = 31;
		}
		else if (npcAnimId == AnimationID.NIGHTMARE_ATTACK_PARASITE)
		{
			this.eventTicks = 27;
			this.ticksUntilAttack = 7;
			if (!this.preggers)
			{
				this.preggers = true;
				this.parasiteTicks = 27;
			}
		}
		else if (npcAnimId == AnimationID.NIGHTMARE_DESPAWN)
		{
			this.ticksUntilAttack = 3;
		}
		else if (npcAnimId == AnimationID.NIGHTMARE_RESPAWN)
		{
			applyRespawnAnimation(npcId);
		}
		else if (npcAnimId == AnimationID.NIGHTMARE_ATTACK_BLAST_RESPAWN)
		{
			this.ticksUntilAttack = 26;
		}
		else if (npcAnimId == AnimationID.NIGHTMARE_SPAWN_INITIAL)
		{
			this.ticksUntilAttack = 11;
		}
		else if (npcAnimId == AnimationID.NIGHTMARE_ATTACK_RIFT_PHASE_04_START)
		{
			this.ticksUntilAttack = 32;
		}
	}

	private void applyRespawnAnimation(int npcId)
	{
		boolean baseTakeoff = npcId == NpcID.NIGHTMARE_PHASE_03 || npcId == NpcID.NIGHTMARE_WEAK_PHASE_03;
		boolean phosaniTakeoff = (npcId >= NpcID.NIGHTMARE_CHALLENGE_PHASE_01 && npcId <= NpcID.NIGHTMARE_CHALLENGE_DYING)
			|| (npcId >= NpcID.NIGHTMARE_CHALLENGE_PHASE_04 && npcId <= NpcID.NIGHTMARE_CHALLENGE_WEAK_PHASE_04);
		if (baseTakeoff)
		{
			this.ticksUntilAttack = 10;
			this.eventTicks = 5;
			this.preparedForTakeoff = true;
			playA10Strafe();
		}
		else if (phosaniTakeoff)
		{
			this.ticksUntilAttack = 10;
			this.eventTicks = 5;
			WorldPoint loc = this.nightmareNpc.getWorldLocation();
			if (loc.getRegionX() != 46 || loc.getRegionY() != 45)
			{
				this.preparedForTakeoff = true;
				playA10Strafe();
			}
		}
		else
		{
			this.ticksUntilAttack = 14;
		}
	}

	private void playA10Strafe()
	{
		if (this.config.a10Strafe() && this.clip != null)
		{
			this.clip.setFramePosition(0);
			this.clip.start();
		}
	}

	@Subscribe
	public void onClientTick(ClientTick event)
	{
		if (!this.client.isInInstancedRegion())
		{
			return;
		}
		List<NPC> npcs = this.client.getNpcs();
		for (NPC n : npcs)
		{
			if (n == null || n.getName() == null)
			{
				continue;
			}
			if (n.getName().equalsIgnoreCase("the nightmare") || n.getName().equalsIgnoreCase("phosani's nightmare"))
			{
				this.bossLoc = n.getWorldLocation();
			}
		}
	}

	@Subscribe
	public void onChatMessage(ChatMessage event)
	{
		if (event.getType() != ChatMessageType.GAMEMESSAGE)
		{
			return;
		}
		String msg = event.getMessage().toLowerCase();
		if (msg.contains("the nightmare has impregnated you with a deadly parasite"))
		{
			this.impregnated = true;
		}
		else if (msg.contains("the parasite within you has been weakened") || msg.contains("the parasite bursts out of you, fully grown"))
		{
			this.impregnated = false;
		}
		else if (msg.contains("shuffling your prayers"))
		{
			this.cursePhase = true;
			activateShuffle();
		}
		else if (msg.contains("feel the effects of the nightmare's curse wear off"))
		{
			this.cursePhase = false;
			deActivateShuffle();
		}
		else if (msg.contains("the nightmare's spores have infected you, making you feel drowsy!"))
		{
			this.yawning = true;
			this.yawnTicks = 26;
		}
		else if (msg.contains("the nightmare's infection has worn off."))
		{
			this.yawning = false;
			this.yawnTicks = 26;
		}
	}

	@Subscribe
	public void onGameTick(GameTick event)
	{
		if (!this.activeFight && this.nightmareNpc == null)
		{
			if (this.config.eventTickCounter() && this.eventTicks > 0)
			{
				this.eventTicks--;
			}
			return;
		}
		this.raveHandsColors.clear();
		for (GraphicsObject obj : this.client.getGraphicsObjects())
		{
			if (obj.getId() != SpotanimID.NIGHTMARE_RIFT)
			{
				continue;
			}
			this.raveHandsColors.add(Color.getHSBColor(new Random().nextFloat(), 1.0f, 1.0f));
		}
		this.raveTotemColors.clear();
		for (int i = 0; i < 4; i++)
		{
			this.raveTotemColors.add(Color.getHSBColor(new Random().nextFloat(), 1.0f, 1.0f));
		}
		this.raveRunway.clear();
		for (int i = 0; i < 80; i++)
		{
			this.raveRunway.add(Color.getHSBColor(new Random().nextFloat(), 1.0f, 1.0f));
		}
		this.ticksUntilAttack--;
		if (this.eventTicks > 0)
		{
			this.eventTicks--;
		}
		if (this.preggers)
		{
			this.parasiteTicks--;
		}
		if (this.preparedForTakeoff)
		{
			this.flightTime--;
			if (this.flightTime == 0)
			{
				this.preparedForTakeoff = false;
				this.flightTime = 5;
			}
		}
		if (this.mushroomActive)
		{
			this.mushroomTicks--;
			if (this.mushroomTicks == 0)
			{
				this.mushroomActive = false;
				this.mushroomTicks = 31;
			}
		}
		if (this.handsOut)
		{
			this.handsDelay--;
			if (this.handsDelay <= 0)
			{
				this.handsDelay = 5;
				this.handsOut = false;
				this.handsLocation.clear();
			}
		}
		if (this.yawning)
		{
			this.yawnTicks--;
			if (this.yawnTicks <= 0)
			{
				this.yawnTicks = 26;
				this.yawning = false;
			}
		}
		if (this.flowersActive || this.flowersActive2)
		{
			updateFlowerObjects();
		}
	}

	private void updateFlowerObjects()
	{
		Player player = this.client.getLocalPlayer();
		if (player == null)
		{
			return;
		}
		if (!this.config.lowFps())
		{
			if (this.flowersActive)
			{
				this.flowerTickCount++;
			}
			if (this.flowerTickCount >= 25)
			{
				this.flowersActive = false;
				this.flowerTickCount = 0;
				this.flowerTiles.clear();
			}
			return;
		}
		Scene scene = this.client.getScene();
		Tile[][][] tiles = scene.getTiles();
		int z = this.client.getPlane();
		boolean removedNonBlossom = false;
		for (int x = 0; x < 104; x++)
		{
			for (int y = 0; y < 104; y++)
			{
				Tile tile = tiles[z][x][y];
				if (tile == null)
				{
					continue;
				}
				GameObject[] gameObjects = tile.getGameObjects();
				if (gameObjects == null)
				{
					continue;
				}
				for (GameObject gameObject : gameObjects)
				{
					if (gameObject == null || player.getLocalLocation().distanceTo(gameObject.getLocalLocation()) > 2400)
					{
						continue;
					}
					int id = gameObject.getId();
					if (id == net.runelite.api.ObjectID.NIGHTMARE_BLOSSOM_37744 || id == net.runelite.api.ObjectID.NIGHTMARE_BLOSSOM_37745)
					{
						scene.removeGameObject(gameObject);
					}
					if (id == net.runelite.api.ObjectID.NIGHTMARE_BERRIES_37741 || id == net.runelite.api.ObjectID.NIGHTMARE_BERRIES_37742
						|| id == ObjectID.INVISIBLE_TYPE8_NONBLOCKING || id == net.runelite.api.ObjectID.NIGHTMARE_BLOSSOM || id == net.runelite.api.ObjectID.NIGHTMARE_BERRIES)
					{
						removedNonBlossom = true;
						scene.removeGameObject(gameObject);
					}
				}
			}
		}
		if (this.flowersActive)
		{
			this.flowerTickCount++;
		}
		if (this.flowerTickCount >= 25)
		{
			this.flowersActive = false;
			this.flowerTickCount = 0;
			this.flowerTiles.clear();
		}
		if (removedNonBlossom)
		{
			this.flowersActive2 = false;
		}
	}

	@Subscribe
	private void onGameObjectSpawned(GameObjectSpawned event)
	{
		GameObject gameObj = event.getGameObject();
		int id = gameObj.getId();
		if (id == net.runelite.api.ObjectID.SPORE || id == net.runelite.api.ObjectID.SPORE_37739)
		{
			this.shrooms.add(gameObj);
			this.mushroomActive = true;
		}
		else if (id == net.runelite.api.ObjectID.NIGHTMARE_BERRIES_37741 || id == net.runelite.api.ObjectID.NIGHTMARE_BLOSSOM_37744)
		{
			this.flowersOut = true;
		}
		if (id == net.runelite.api.ObjectID.NIGHTMARE_BLOSSOM_37744 || id == net.runelite.api.ObjectID.NIGHTMARE_BLOSSOM_37745)
		{
			if (!this.flowersActive)
			{
				this.flowersActive = true;
				this.flowerTickCount = 0;
			}
			this.flowerTiles.add(gameObj.getLocalLocation());
		}
		else if (id == ObjectID.INVISIBLE_TYPE8_NONBLOCKING || id == net.runelite.api.ObjectID.NIGHTMARE_BERRIES_37741
			|| id == net.runelite.api.ObjectID.NIGHTMARE_BERRIES_37742 || id == net.runelite.api.ObjectID.NIGHTMARE_BLOSSOM || id == net.runelite.api.ObjectID.NIGHTMARE_BERRIES)
		{
			this.flowersActive2 = true;
		}
	}

	@Subscribe
	private void onGameObjectDespawned(GameObjectDespawned event)
	{
		GameObject obj = event.getGameObject();
		int id = obj.getId();
		if (id == net.runelite.api.ObjectID.SPORE || id == net.runelite.api.ObjectID.SPORE_37739)
		{
			this.shrooms.remove(obj);
		}
		else if (id == net.runelite.api.ObjectID.NIGHTMARE_BLOSSOM_37745 || id == net.runelite.api.ObjectID.NIGHTMARE_BERRIES_37742)
		{
			this.flowersOut = false;
		}
	}

	@Subscribe
	public void onMenuEntryAdded(MenuEntryAdded event)
	{
		if (this.nightmareNpc == null)
		{
			return;
		}
		String target = Text.removeTags(event.getTarget()).toLowerCase();
		if (this.config.hideAttack() != SpoonNightmareConfig.hideAttackMode.OFF
			&& (target.contains("the nightmare") || target.contains("phosani's nightmare"))
			&& event.getType() == MenuAction.NPC_SECOND_OPTION.getId())
		{
			if (shouldHideAttack())
			{
				removeLastMenuEntry();
			}
		}
		else if (target.contains("sleepwalker") && event.getType() == MenuAction.NPC_SECOND_OPTION.getId()
			&& this.nightmareNpc.getId() == NpcID.NIGHTMARE_CHALLENGE_PHASE_05 && this.config.hideAttackSleepwalkers())
		{
			removeLastMenuEntry();
		}
	}

	private void removeLastMenuEntry()
	{
		MenuEntry[] entries = this.client.getMenuEntries();
		if (entries.length > 0)
		{
			this.client.setMenuEntries(Arrays.copyOf(entries, entries.length - 1));
		}
	}

	private boolean shouldHideAttack()
	{
		SpoonNightmareConfig.hideAttackMode mode = this.config.hideAttack();
		boolean eventOutstanding = (this.totemsActive && (mode == SpoonNightmareConfig.hideAttackMode.TOTEM || mode == SpoonNightmareConfig.hideAttackMode.ALL))
			|| (!this.parasiteList.isEmpty() && (mode == SpoonNightmareConfig.hideAttackMode.PARASITE || mode == SpoonNightmareConfig.hideAttackMode.ALL))
			|| (!this.husks.isEmpty() && (mode == SpoonNightmareConfig.hideAttackMode.HUSK || mode == SpoonNightmareConfig.hideAttackMode.ALL));
		SpoonNightmareConfig.hideAttackIgnoreMode ignore = this.config.hideAttackIgnore();
		boolean sporesOk = this.shrooms.isEmpty() || (ignore != SpoonNightmareConfig.hideAttackIgnoreMode.BOTH && ignore != SpoonNightmareConfig.hideAttackIgnoreMode.SPORES);
		boolean flowersOk = !this.flowersOut || (ignore != SpoonNightmareConfig.hideAttackIgnoreMode.BOTH && ignore != SpoonNightmareConfig.hideAttackIgnoreMode.FLOWERS);
		boolean sleepwalkerLeak = this.sleepwalkerAlive > 0 && this.nightmareNpc.getId() != NpcID.NIGHTMARE_CHALLENGE_PHASE_05;
		return (eventOutstanding && sporesOk && flowersOk) || sleepwalkerLeak;
	}

	@Subscribe
	public void onSoundEffectPlayed(SoundEffectPlayed event)
	{
		if (this.config.muteHands() && HAND_SPAWN_SOUND_IDS.contains(event.getSoundId()))
		{
			event.consume();
		}
	}

	@Subscribe
	public void onGraphicsObjectCreated(GraphicsObjectCreated event)
	{
		if (event.getGraphicsObject().getId() == SpotanimID.NIGHTMARE_RIFT)
		{
			this.handsLocation.add(event.getGraphicsObject());
			if (!this.handsOut)
			{
				this.handsOut = true;
				this.handsDelay = 5;
			}
		}
	}

	@Subscribe
	public void onHitsplatApplied(HitsplatApplied event)
	{
		if (!(event.getActor() instanceof NPC))
		{
			return;
		}
		NPC npc = (NPC) event.getActor();
		String name = npc.getName();
		int splatType = event.getHitsplat().getHitsplatType();
		if (name != null && splatType == HitsplatID.HEAL
			&& (name.equalsIgnoreCase("the nightmare") || name.equalsIgnoreCase("phosani's nightmare")) && event.getHitsplat().getAmount() != 149)
		{
			this.nightmareHealingCount += event.getHitsplat().getAmount();
		}
		else if (TOTEM_READY_IDS.contains(npc.getId()) && splatType == HitsplatID.DAMAGE_OTHER_WHITE)
		{
			this.totemHealingCount += event.getHitsplat().getAmount();
		}
	}

	private boolean isNightmareId(int npcId)
	{
		return (npcId >= NpcID.NIGHTMARE_PHASE_01 && npcId <= NpcID.NIGHTMARE_DYING)
			|| (npcId >= NpcID.NIGHTMARE_CHALLENGE_PHASE_01 && npcId <= NpcID.NIGHTMARE_CHALLENGE_DYING)
			|| (npcId >= NpcID.NIGHTMARE_CHALLENGE_PHASE_04 && npcId <= NpcID.NIGHTMARE_CHALLENGE_WEAK_PHASE_04);
	}

	@Subscribe(priority = -1.0f)
	public void onWidgetLoaded(WidgetLoaded widgetLoaded)
	{
		if (widgetLoaded.getGroupId() == InterfaceID.PRAYERBOOK)
		{
			setOriginalPositions();
		}
	}

	private void activateShuffle()
	{
		if (!this.config.swapPrayers() || this.reorderActive)
		{
			return;
		}
		this.reorderActive = setPrayerPositions();
		if (this.reorderActive)
		{
			setPrayerIcons();
		}
	}

	private void deActivateShuffle()
	{
		if (!this.reorderActive)
		{
			return;
		}
		this.reorderActive = !resetPrayer();
	}

	@Subscribe
	public void onScriptPostFired(ScriptPostFired event)
	{
		if (!this.reorderActive)
		{
			return;
		}
		int scriptId = event.getScriptId();
		if (scriptId == ScriptID.PRAYER_UPDATEBUTTON || scriptId == ScriptID.PRAYER_REDRAW || scriptId == ScriptID.QUICKPRAYER_INIT)
		{
			if (setPrayerPositions())
			{
				setPrayerIcons();
			}
		}
	}

	// Same technique the vanilla PrayerReorder plugin uses to resolve the currently-unlocked prayer
	// book's enum of (prayer id -> prayer item id).
	private EnumComposition getPrayerBookEnum(int prayerbook)
	{
		if (prayerbook == 1)
		{
			return this.client.getEnum(EnumID.PRAYERS_RUINOUS);
		}
		boolean deadeye = this.client.getVarbitValue(VarbitID.PRAYER_DEADEYE_UNLOCKED) != 0;
		boolean vigour = this.client.getVarbitValue(VarbitID.PRAYER_MYSTIC_VIGOUR_UNLOCKED) != 0;
		if (deadeye && vigour)
		{
			return this.client.getEnum(EnumID.PRAYERS_NORMAL_DEADEYE_MYSTIC_VIGOUR);
		}
		else if (deadeye)
		{
			return this.client.getEnum(EnumID.PRAYERS_NORMAL_DEADEYE);
		}
		else if (vigour)
		{
			return this.client.getEnum(EnumID.PRAYERS_NORMAL_MYSTIC_VIGOUR);
		}
		return this.client.getEnum(EnumID.PRAYERS_NORMAL);
	}

	Widget getPrayerWidget(Prayer prayer)
	{
		String targetName;
		if (prayer == Prayer.PROTECT_FROM_MAGIC)
		{
			targetName = PRAYER_NAME_MAGIC;
		}
		else if (prayer == Prayer.PROTECT_FROM_MISSILES)
		{
			targetName = PRAYER_NAME_MISSILES;
		}
		else if (prayer == Prayer.PROTECT_FROM_MELEE)
		{
			targetName = PRAYER_NAME_MELEE;
		}
		else
		{
			return null;
		}
		int prayerbook = this.client.getVarbitValue(VarbitID.PRAYERBOOK);
		EnumComposition prayers = getPrayerBookEnum(prayerbook);
		for (int key : prayers.getKeys())
		{
			int prayerObjId = prayers.getIntValue(key);
			ItemComposition itemComp = this.client.getItemDefinition(prayerObjId);
			if (targetName.equalsIgnoreCase(itemComp.getName()))
			{
				return this.client.getWidget(itemComp.getIntValue(ParamID.OC_PRAYER_COMPONENT));
			}
		}
		return null;
	}

	private void applyPrayerHiding(boolean hide)
	{
		int prayerbook = this.client.getVarbitValue(VarbitID.PRAYERBOOK);
		EnumComposition prayers = getPrayerBookEnum(prayerbook);
		for (int key : prayers.getKeys())
		{
			int prayerObjId = prayers.getIntValue(key);
			ItemComposition itemComp = this.client.getItemDefinition(prayerObjId);
			String name = itemComp.getName();
			if (name == null || isKeptPrayer(name))
			{
				continue;
			}
			Widget w = this.client.getWidget(itemComp.getIntValue(ParamID.OC_PRAYER_COMPONENT));
			if (w != null)
			{
				w.setHidden(hide);
			}
		}
	}

	private static boolean isKeptPrayer(String name)
	{
		String lower = name.toLowerCase();
		for (String kept : KEPT_PRAYER_NAMES)
		{
			if (lower.contains(kept))
			{
				return true;
			}
		}
		return false;
	}

	private void setOriginalPositions()
	{
		if (this.client.getGameState() != GameState.LOGGED_IN)
		{
			return;
		}
		if (this.reorderActive && this.originalMagePosition != null && this.originalMissilesPosition != null && this.originalMeleePosition != null)
		{
			return;
		}
		Widget widgetMage = getPrayerWidget(Prayer.PROTECT_FROM_MAGIC);
		Widget widgetMissiles = getPrayerWidget(Prayer.PROTECT_FROM_MISSILES);
		Widget widgetMelee = getPrayerWidget(Prayer.PROTECT_FROM_MELEE);
		if (widgetMage == null || widgetMissiles == null || widgetMelee == null)
		{
			return;
		}
		this.originalMagePosition = new Point(widgetMage.getOriginalX(), widgetMage.getOriginalY());
		this.originalMissilesPosition = new Point(widgetMissiles.getOriginalX(), widgetMissiles.getOriginalY());
		this.originalMeleePosition = new Point(widgetMelee.getOriginalX(), widgetMelee.getOriginalY());
	}

	private boolean setPrayerPositions()
	{
		Widget widgetMage = getPrayerWidget(Prayer.PROTECT_FROM_MAGIC);
		Widget widgetMissiles = getPrayerWidget(Prayer.PROTECT_FROM_MISSILES);
		Widget widgetMelee = getPrayerWidget(Prayer.PROTECT_FROM_MELEE);
		if (widgetMage == null || widgetMissiles == null || widgetMelee == null
			|| this.originalMagePosition == null || this.originalMissilesPosition == null || this.originalMeleePosition == null)
		{
			return false;
		}
		setWidgetPosition(widgetMage, this.originalMissilesPosition.getX(), this.originalMissilesPosition.getY());
		setWidgetPosition(widgetMissiles, this.originalMeleePosition.getX(), this.originalMeleePosition.getY());
		setWidgetPosition(widgetMelee, this.originalMagePosition.getX(), this.originalMagePosition.getY());
		return true;
	}

	private boolean setPrayerIcons()
	{
		Widget widgetMage = getPrayerWidget(Prayer.PROTECT_FROM_MAGIC);
		Widget widgetMissiles = getPrayerWidget(Prayer.PROTECT_FROM_MISSILES);
		Widget widgetMelee = getPrayerWidget(Prayer.PROTECT_FROM_MELEE);
		if (widgetMage == null || widgetMissiles == null || widgetMelee == null)
		{
			return false;
		}
		Widget widgetMageChild = widgetMage.getChild(1);
		Widget widgetMissilesChild = widgetMissiles.getChild(1);
		Widget widgetMeleeChild = widgetMelee.getChild(1);
		if (widgetMageChild == null || widgetMissilesChild == null || widgetMeleeChild == null)
		{
			return false;
		}
		setWidgetIcon(widgetMageChild, SPRITE_MISSILES);
		setWidgetIcon(widgetMissilesChild, SPRITE_MELEE);
		setWidgetIcon(widgetMeleeChild, SPRITE_MAGIC);
		return true;
	}

	private boolean resetPrayer()
	{
		Widget widgetMage = getPrayerWidget(Prayer.PROTECT_FROM_MAGIC);
		Widget widgetMissiles = getPrayerWidget(Prayer.PROTECT_FROM_MISSILES);
		Widget widgetMelee = getPrayerWidget(Prayer.PROTECT_FROM_MELEE);
		if (widgetMage == null || widgetMissiles == null || widgetMelee == null
			|| this.originalMagePosition == null || this.originalMissilesPosition == null || this.originalMeleePosition == null)
		{
			return false;
		}
		Widget widgetMageChild = widgetMage.getChild(1);
		Widget widgetMissilesChild = widgetMissiles.getChild(1);
		Widget widgetMeleeChild = widgetMelee.getChild(1);
		if (widgetMageChild == null || widgetMissilesChild == null || widgetMeleeChild == null)
		{
			return false;
		}
		setWidgetPosition(widgetMage, this.originalMagePosition.getX(), this.originalMagePosition.getY());
		setWidgetPosition(widgetMissiles, this.originalMissilesPosition.getX(), this.originalMissilesPosition.getY());
		setWidgetPosition(widgetMelee, this.originalMeleePosition.getX(), this.originalMeleePosition.getY());
		setWidgetIcon(widgetMageChild, SPRITE_MAGIC);
		setWidgetIcon(widgetMissilesChild, SPRITE_MISSILES);
		setWidgetIcon(widgetMeleeChild, SPRITE_MELEE);
		return true;
	}

	private void setWidgetPosition(Widget widget, int x, int y)
	{
		Runnable r = () -> {
			widget.setPos(x, y);
			widget.revalidate();
		};
		if (this.client.isClientThread())
		{
			r.run();
		}
		else
		{
			this.clientThread.invoke(r);
		}
	}

	private void setWidgetIcon(Widget widget, int iconId)
	{
		Runnable r = () -> {
			widget.setSpriteId(iconId);
			widget.revalidate();
		};
		if (this.client.isClientThread())
		{
			r.run();
		}
		else
		{
			this.clientThread.invoke(r);
		}
	}

	public boolean isPreggers()
	{
		return this.preggers;
	}

	public int getParasiteTicks()
	{
		return this.parasiteTicks;
	}

	public boolean isImpregnated()
	{
		return this.impregnated;
	}

	public boolean isYawning()
	{
		return this.yawning;
	}

	public int getYawnTicks()
	{
		return this.yawnTicks;
	}

	public int getTicksUntilAttack()
	{
		return this.ticksUntilAttack;
	}

	public int getEventTicks()
	{
		return this.eventTicks;
	}

	public NPC getNightmareNpc()
	{
		return this.nightmareNpc;
	}

	public boolean isActiveFight()
	{
		return this.activeFight;
	}

	public String getCorrectPray()
	{
		return this.correctPray;
	}

	public boolean isCursePhase()
	{
		return this.cursePhase;
	}

	public ArrayList<GraphicsObject> getHandsLocation()
	{
		return this.handsLocation;
	}

	public boolean isPreparedForTakeoff()
	{
		return this.preparedForTakeoff;
	}

	public boolean isTotemsActive()
	{
		return this.totemsActive;
	}

	public ArrayList<GameObject> getShrooms()
	{
		return this.shrooms;
	}

	public ArrayList<NPC> getHusks()
	{
		return this.husks;
	}

	public boolean isFlowersActive()
	{
		return this.flowersActive;
	}

	public Set<LocalPoint> getFlowerTiles()
	{
		return this.flowerTiles;
	}

	public ArrayList<Color> getRaveHandsColors()
	{
		return this.raveHandsColors;
	}

	public int getHandsDelay()
	{
		return this.handsDelay;
	}

	public WorldPoint getBossLoc()
	{
		return this.bossLoc;
	}

	public ArrayList<Color> getRaveRunway()
	{
		return this.raveRunway;
	}

	public ArrayList<TotemInfo> getTotemList()
	{
		return this.totemList;
	}

	public ArrayList<Color> getRaveTotemColors()
	{
		return this.raveTotemColors;
	}

	public int getMushroomTicks()
	{
		return this.mushroomTicks;
	}
}
