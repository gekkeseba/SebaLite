package net.runelite.client.plugins.spooncoxadditions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;
import javax.inject.Inject;
import net.runelite.api.Actor;
import net.runelite.api.GameObject;
import net.runelite.api.GroundObject;
import net.runelite.api.Client;
import net.runelite.api.InventoryID;
import net.runelite.api.MenuAction;
import net.runelite.api.MenuEntry;
import net.runelite.api.NPC;
import net.runelite.api.Player;
import net.runelite.api.Prayer;
import net.runelite.api.TileObject;
import net.runelite.api.events.AnimationChanged;
import net.runelite.api.events.ActorDeath;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.ClientTick;
import net.runelite.api.events.GameObjectDespawned;
import net.runelite.api.events.GameObjectSpawned;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.GroundObjectSpawned;
import net.runelite.api.events.NpcChanged;
import net.runelite.api.events.NpcDespawned;
import net.runelite.api.events.NpcSpawned;
import net.runelite.api.gameval.AnimationID;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.NpcID;
import net.runelite.api.gameval.ObjectID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.api.kit.KitType;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.spooncoxadditions.overlays.CoxAdditionsOverlay;
import net.runelite.client.plugins.spooncoxadditions.overlays.MeatTreeCycleOverlay;
import net.runelite.client.plugins.spooncoxadditions.overlays.ShortcutOverlay;
import net.runelite.client.plugins.spooncoxadditions.overlays.VanguardCycleOverlay;
import net.runelite.client.plugins.spooncoxadditions.utils.ShamanInfo;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.Text;

import com.google.inject.Provides;

@PluginDescriptor(
	name = "<html><font color=#25c550>[S] Cox Additionals",
	description = "Additional plugins for the Chambers of Xeric",
	tags = {"xeric", "olm", "chambers", "cox", "spoon"},
	enabledByDefault = false
)
public class SpoonCoxAdditionsPlugin extends Plugin
{
	// Items that count as "having an axe" for the removeChop menu filter (bare hand axes
	// plus every axe head that can be wielded), mirroring the original plugin's list.
	private static final List<Integer> AXES = Arrays.asList(
		1349, 1351, 1353, 1355, 1357, 1359, 1361, 6739, 13241, 13242,
		25110, 23673, 25066, 25371, 25378
	);

	@Inject
	private Client client;

	@Inject
	private SpoonCoxAdditionsConfig config;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private CoxAdditionsOverlay overlay;

	@Inject
	private VanguardCycleOverlay vanguardCycleOverlay;

	@Inject
	private ShortcutOverlay shortcutOverlay;

	@Inject
	private MeatTreeCycleOverlay meatTreeCycleOverlay;

	// Olm hand cripple
	public boolean handCripple;
	public int crippleTimer = 45;
	public NPC meleeHand;
	public NPC mageHand;

	// Olm crystal-phase personal mark ("chosen you as its target")
	public boolean crystalMarkActive;
	public int crystalMarkTicks;

	// Lizardman shaman slam prediction
	public final ArrayList<ShamanInfo> shamanInfoList = new ArrayList<>();

	// Vasa crystal timer
	private NPC vasa;
	public int vasaCrystalTicks;
	public boolean vasaAtCrystal;

	// Vespula menu filters
	public boolean vespAlive;
	public boolean vespDied;

	// Meat tree chop cycle
	public boolean meatTreeAlive;
	public NPC meatTree;
	public boolean startedChopping;
	public int ticksToChop = 5;

	// Vanguard attack-cycle timer
	public boolean vangsActive;
	public int vangsTicks = 1;
	public int vangs4Ticks = 1;
	public boolean vangsAlive;

	// Raid shortcuts
	private final List<TileObject> shortcut = new ArrayList<>();
	private boolean highlightShortcuts;

	// Tightrope chin-crossing helper
	public final ArrayList<NPC> ropeNpcs = new ArrayList<>();
	public final ArrayList<GroundObject> rope = new ArrayList<>();
	public int ropeSpawnDelay;
	public int ticksSinceHPRegen;
	public boolean rapidHealActive;

	private final Predicate<MenuEntry> filterMenuEntries = entry ->
	{
		if (this.client.getVarbitValue(VarbitID.RAIDS_CLIENT_INDUNGEON) != 1)
		{
			return true;
		}

		String option = Text.standardize(entry.getOption());
		String target = Text.standardize(entry.getTarget());

		if (this.config.hideAttackHead() && (this.meleeHand != null || this.mageHand != null)
			&& target.contains("great olm") && !target.contains("(left claw)") && !target.contains("(right claw)"))
		{
			return false;
		}
		if (this.config.hideVesp() && target.equals("vespula"))
		{
			return false;
		}
		if (this.config.removeChop() && target.equals("sapling") && option.equals("chop") && !hasAxe())
		{
			return false;
		}
		if (this.config.removeFeed() && target.equals("lux grub")
			&& this.client.getItemContainer(InventoryID.INVENTORY).count(ItemID.RAIDS_VESPULA_HERB) == 0)
		{
			return false;
		}
		if (this.config.removePickRoot() && target.equals("medivaemia root") && this.vespDied && option.equals("pick"))
		{
			return false;
		}
		if (this.config.removeCastCoX() && option.equals("cast") && entry.getType() == MenuAction.WIDGET_TARGET_ON_PLAYER)
		{
			String[] spells = {
				"ice barrage", "ice burst", "ice blitz", "ice rush", "entangle", "snare", "bind", "blood barrage",
				"blood burst", "blood rush", "blood blitz", "fire surge", "fire wave"
			};
			for (String spell : spells)
			{
				if (target.contains(spell))
				{
					return false;
				}
			}
		}
		return true;
	};

	private boolean hasAxe()
	{
		Player localPlayer = this.client.getLocalPlayer();
		if (localPlayer == null)
		{
			return false;
		}
		int weapon = localPlayer.getPlayerComposition().getEquipmentId(KitType.WEAPON);
		if (AXES.contains(weapon))
		{
			return true;
		}
		for (int axe : AXES)
		{
			if (this.client.getItemContainer(InventoryID.INVENTORY).count(axe) > 0)
			{
				return true;
			}
		}
		return false;
	}

	public List<TileObject> getShortcut()
	{
		return this.shortcut;
	}

	public boolean isHighlightShortcuts()
	{
		return this.highlightShortcuts;
	}

	@Provides
	SpoonCoxAdditionsConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(SpoonCoxAdditionsConfig.class);
	}

	private void reset()
	{
		this.meleeHand = null;
		this.mageHand = null;
		this.crippleTimer = 45;
		this.handCripple = false;
		this.crystalMarkActive = false;
		this.crystalMarkTicks = 0;
		this.shamanInfoList.clear();
		this.vasa = null;
		this.vasaCrystalTicks = 0;
		this.vasaAtCrystal = false;
		this.vespAlive = false;
		this.vespDied = false;
		this.meatTreeAlive = false;
		this.meatTree = null;
		this.startedChopping = false;
		this.ticksToChop = 5;
		this.vangsActive = false;
		this.vangsAlive = false;
		this.vangsTicks = 1;
		this.vangs4Ticks = 1;
		this.ropeNpcs.clear();
		this.rope.clear();
		this.ropeSpawnDelay = 0;
		this.shortcut.clear();
	}

	@Override
	protected void startUp()
	{
		reset();
		this.highlightShortcuts = this.config.highlightShortcuts();
		this.overlayManager.add(this.overlay);
		this.overlayManager.add(this.vanguardCycleOverlay);
		this.overlayManager.add(this.shortcutOverlay);
		this.overlayManager.add(this.meatTreeCycleOverlay);
	}

	@Override
	protected void shutDown()
	{
		reset();
		this.overlayManager.remove(this.overlay);
		this.overlayManager.remove(this.vanguardCycleOverlay);
		this.overlayManager.remove(this.shortcutOverlay);
		this.overlayManager.remove(this.meatTreeCycleOverlay);
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged e)
	{
		if (!e.getGroup().equals("CoxAdditions"))
		{
			return;
		}
		switch (e.getKey())
		{
			case "vangsCycle":
				if (this.config.vangsCycle() != SpoonCoxAdditionsConfig.VangsTicksMode.OFF)
				{
					this.vangsAlive = false;
					this.vangsActive = false;
					for (NPC npc : this.client.getNpcs())
					{
						if (npc.getId() == NpcID.RAIDS_VANGUARD_MELEE || npc.getId() == NpcID.RAIDS_VANGUARD_RANGED || npc.getId() == NpcID.RAIDS_VANGUARD_MAGIC)
						{
							this.vangsAlive = true;
							this.vangsActive = true;
						}
						else if (npc.getId() == NpcID.RAIDS_VANGUARD_WALKING)
						{
							this.vangsAlive = true;
						}
					}
				}
				else
				{
					this.vangsAlive = false;
					this.vangsActive = false;
					this.vangsTicks = 1;
					this.vangs4Ticks = 1;
				}
				break;
			case "highlightShortcuts":
				this.highlightShortcuts = this.config.highlightShortcuts();
				break;
		}
	}

	@Subscribe
	public void onChatMessage(ChatMessage event)
	{
		if (this.client.getVarbitValue(VarbitID.RAIDS_CLIENT_INDUNGEON) != 1)
		{
			return;
		}

		String msg = Text.standardize(event.getMessageNode().getValue());
		if (msg.equalsIgnoreCase("the great olm's left claw clenches to protect itself temporarily."))
		{
			this.handCripple = true;
		}
		else if (msg.equalsIgnoreCase("the great olm has chosen you as its target - watch out!"))
		{
			this.crystalMarkActive = true;
			this.crystalMarkTicks = 23;
		}
		else if (msg.equalsIgnoreCase("the great olm is giving its all. this is its final stand."))
		{
			this.mageHand = null;
			this.meleeHand = null;
		}
		else if (msg.equalsIgnoreCase("you swing your axe...") && this.meatTreeAlive && this.meatTree != null)
		{
			this.startedChopping = true;
		}
		else if (msg.equalsIgnoreCase("you hack away some of the meat.") && this.meatTreeAlive && this.meatTree != null)
		{
			this.ticksToChop = 6;
		}
	}

	@Subscribe
	public void onGameTick(GameTick event)
	{
		if (this.client.getVarbitValue(VarbitID.RAIDS_CLIENT_INDUNGEON) != 1)
		{
			reset();
		}
		else
		{
			if (this.handCripple)
			{
				this.crippleTimer--;
				if (this.crippleTimer <= 0)
				{
					this.handCripple = false;
					this.crippleTimer = 45;
				}
			}
			if (this.crystalMarkActive)
			{
				this.crystalMarkTicks--;
				if (this.crystalMarkTicks <= 0)
				{
					this.crystalMarkActive = false;
					this.crystalMarkTicks = 0;
				}
			}
			if (this.vasa != null)
			{
				if (this.vasa.getId() == NpcID.RAIDS_VASANISTIRIO_HEALING)
				{
					if (this.vasaCrystalTicks == 0)
					{
						this.vasaCrystalTicks = 67;
					}
					else
					{
						this.vasaCrystalTicks--;
					}
				}
				else if (this.vasa.getId() == NpcID.RAIDS_VASANISTIRIO_WALKING && this.vasa.getAnimation() == AnimationID.VASA_STUN_SPAWN)
				{
					this.vasaCrystalTicks = 67;
					this.vasaAtCrystal = false;
				}
			}
			if (this.vangsActive)
			{
				this.vangsTicks++;
				this.vangs4Ticks++;
				if (this.vangs4Ticks > 4)
				{
					this.vangs4Ticks = 1;
				}
			}
			this.shortcut.removeIf(object -> object.getCanvasLocation() == null);
			if (this.startedChopping)
			{
				this.ticksToChop--;
				if (this.ticksToChop <= 0)
				{
					this.ticksToChop = 5;
				}
			}
			if (this.ropeSpawnDelay > 0)
			{
				this.ropeSpawnDelay--;
			}
		}
		boolean rapidHealNow = this.client.isPrayerActive(Prayer.RAPID_HEAL);
		if (this.rapidHealActive != rapidHealNow)
		{
			this.ticksSinceHPRegen = 0;
		}
		this.rapidHealActive = rapidHealNow;

		this.ticksSinceHPRegen++;
		if ((this.ticksSinceHPRegen == 50 && this.rapidHealActive) || this.ticksSinceHPRegen == 100)
		{
			this.ticksSinceHPRegen = 0;
		}
	}

	@Subscribe
	public void onGameObjectSpawned(GameObjectSpawned event)
	{
		if (this.client.getVarbitValue(VarbitID.RAIDS_CLIENT_INDUNGEON) != 1)
		{
			return;
		}
		int id = event.getGameObject().getId();
		if (id == ObjectID.RAIDS_CORRIDOR_ROOTS || id == ObjectID.RAIDS_CORRIDOR_ROCKS || id == ObjectID.RAIDS_CORRIDOR_BOULDER)
		{
			this.shortcut.add(event.getGameObject());
		}
	}

	@Subscribe
	public void onGameObjectDespawned(GameObjectDespawned event)
	{
		if (this.client.getVarbitValue(VarbitID.RAIDS_CLIENT_INDUNGEON) != 1)
		{
			return;
		}
		this.shortcut.remove(event.getGameObject());
	}

	@Subscribe
	public void onGroundObjectSpawned(GroundObjectSpawned event)
	{
		if (this.client.getVarbitValue(VarbitID.RAIDS_CLIENT_INDUNGEON) != 1)
		{
			return;
		}
		GroundObject obj = event.getGroundObject();
		if (obj.getId() == ObjectID.RAIDS_TIGHTROPE_END)
		{
			if ((!this.rope.isEmpty() && this.ropeSpawnDelay == 0) || this.rope.size() >= 2)
			{
				this.rope.clear();
			}
			this.rope.add(obj);
			if (this.ropeSpawnDelay == 0)
			{
				this.ropeSpawnDelay = 2;
			}
		}
	}

	@Subscribe
	public void onNpcSpawned(NpcSpawned event)
	{
		if (this.client.getVarbitValue(VarbitID.RAIDS_CLIENT_INDUNGEON) != 1)
		{
			return;
		}
		NPC npc = event.getNpc();
		int id = npc.getId();
		String name = npc.getName();

		if (id == NpcID.OLM_HAND_LEFT_SPAWNING || id == NpcID.OLM_HAND_LEFT)
		{
			this.meleeHand = npc;
		}
		else if (id == NpcID.OLM_HAND_RIGHT_SPAWNING || id == NpcID.OLM_HAND_RIGHT)
		{
			this.mageHand = npc;
		}
		else if (id == NpcID.RAIDS_VASANISTIRIO_DORMANT || id == NpcID.RAIDS_VASANISTIRIO_WALKING || id == NpcID.RAIDS_VASANISTIRIO_HEALING)
		{
			this.vasa = npc;
			this.vasaAtCrystal = (id == NpcID.RAIDS_VASANISTIRIO_HEALING);
		}
		else if (id == NpcID.RAIDS_VESPULA_FLYING || id == NpcID.RAIDS_VESPULA_ENRAGED || id == NpcID.RAIDS_VESPULA_WALKING || id == NpcID.RAIDS_VESPULA_PORTAL)
		{
			this.vespAlive = true;
		}
		else if (id == NpcID.RAIDS_DOGODILE_MEAT_TREE)
		{
			this.meatTreeAlive = true;
			this.meatTree = npc;
		}
		else if (id == NpcID.RAIDS_VANGUARD_MELEE || id == NpcID.RAIDS_VANGUARD_RANGED || id == NpcID.RAIDS_VANGUARD_MAGIC)
		{
			this.vangsAlive = true;
			this.vangsActive = true;
		}
		else if (id == NpcID.RAIDS_VANGUARD_WALKING || id == NpcID.RAIDS_VANGUARD_DORMANT)
		{
			this.vangsAlive = true;
		}
		else if (name != null)
		{
			if (name.equalsIgnoreCase("lizardman shaman"))
			{
				Actor interacting = npc.getInteracting();
				this.shamanInfoList.add(new ShamanInfo(npc, interacting != null ? interacting.getLocalLocation() : null, false));
			}
			else if (name.equalsIgnoreCase("deathly mage") || name.equalsIgnoreCase("deathly ranger"))
			{
				this.ropeNpcs.add(npc);
			}
		}
	}

	@Subscribe
	public void onNpcDespawned(NpcDespawned event)
	{
		if (this.client.getVarbitValue(VarbitID.RAIDS_CLIENT_INDUNGEON) != 1)
		{
			return;
		}
		NPC npc = event.getNpc();
		int id = npc.getId();
		String name = npc.getName();

		if (id == NpcID.OLM_HAND_LEFT_SPAWNING || id == NpcID.OLM_HAND_LEFT)
		{
			this.meleeHand = null;
			if (npc.isDead())
			{
				this.handCripple = false;
				this.crippleTimer = 45;
			}
		}
		else if (id == NpcID.OLM_HAND_RIGHT_SPAWNING || id == NpcID.OLM_HAND_RIGHT)
		{
			this.mageHand = null;
			if (npc.isDead())
			{
				this.handCripple = false;
				this.crippleTimer = 45;
			}
		}
		else if (id == NpcID.RAIDS_VESPULA_FLYING || id == NpcID.RAIDS_VESPULA_ENRAGED || id == NpcID.RAIDS_VESPULA_WALKING || id == NpcID.RAIDS_VESPULA_PORTAL)
		{
			this.vespAlive = false;
		}
		else if (id == NpcID.RAIDS_DOGODILE_MEAT_TREE)
		{
			this.meatTreeAlive = false;
			this.meatTree = null;
			this.startedChopping = false;
			this.ticksToChop = 5;
		}
		else if (id == NpcID.RAIDS_VANGUARD_MELEE || id == NpcID.RAIDS_VANGUARD_RANGED || id == NpcID.RAIDS_VANGUARD_MAGIC
			|| id == NpcID.RAIDS_VANGUARD_WALKING || id == NpcID.RAIDS_VANGUARD_DORMANT)
		{
			boolean alive = false;
			for (NPC n : this.client.getNpcs())
			{
				int nid = n.getId();
				if (nid == NpcID.RAIDS_VANGUARD_MELEE || nid == NpcID.RAIDS_VANGUARD_RANGED || nid == NpcID.RAIDS_VANGUARD_MAGIC
					|| nid == NpcID.RAIDS_VANGUARD_WALKING || nid == NpcID.RAIDS_VANGUARD_DORMANT)
				{
					alive = true;
					break;
				}
			}
			if (!alive)
			{
				this.vangsAlive = false;
				this.vangsActive = false;
				this.vangsTicks = 1;
				this.vangs4Ticks = 1;
			}
		}
		else if (name != null)
		{
			if (name.equalsIgnoreCase("lizardman shaman"))
			{
				this.shamanInfoList.removeIf(info -> info.shaman == npc);
			}
			else if (name.equalsIgnoreCase("deathly mage") || name.equalsIgnoreCase("deathly ranger"))
			{
				this.ropeNpcs.remove(npc);
			}
		}
	}

	@Subscribe
	public void onNpcChanged(NpcChanged event)
	{
		if (this.client.getVarbitValue(VarbitID.RAIDS_CLIENT_INDUNGEON) != 1)
		{
			return;
		}
		int id = event.getNpc().getId();
		if (id == NpcID.RAIDS_VANGUARD_WALKING)
		{
			this.vangsActive = false;
		}
		else if (id == NpcID.RAIDS_VANGUARD_MELEE || id == NpcID.RAIDS_VANGUARD_RANGED || id == NpcID.RAIDS_VANGUARD_MAGIC)
		{
			this.vangsActive = true;
			this.vangsTicks = 1;
			this.vangs4Ticks = 1;
		}
	}

	@Subscribe
	public void onAnimationChanged(AnimationChanged event)
	{
		if (this.client.getVarbitValue(VarbitID.RAIDS_CLIENT_INDUNGEON) != 1 || !(event.getActor() instanceof NPC))
		{
			return;
		}
		NPC npc = (NPC) event.getActor();
		if (npc.getName() == null || !npc.getName().equalsIgnoreCase("lizardman shaman"))
		{
			return;
		}
		for (ShamanInfo shamanInfo : this.shamanInfoList)
		{
			if (shamanInfo.shaman == npc)
			{
				if (npc.getAnimation() == AnimationID.SHAYZIEN_LIZARD_BOSS_JUMP)
				{
					shamanInfo.jumping = true;
				}
				else if (npc.getAnimation() == AnimationID.SHAYZIEN_LIZARD_BOSS_LAND)
				{
					shamanInfo.jumping = false;
				}
				if (npc.getInteracting() != null)
				{
					shamanInfo.interactingLoc = npc.getInteracting().getLocalLocation();
				}
				break;
			}
		}
	}

	@Subscribe
	public void onActorDeath(ActorDeath event)
	{
		if (this.client.getVarbitValue(VarbitID.RAIDS_CLIENT_INDUNGEON) != 1 || !(event.getActor() instanceof NPC))
		{
			return;
		}
		NPC npc = (NPC) event.getActor();
		if (npc.getName() == null)
		{
			return;
		}
		String name = npc.getName().toLowerCase();
		if (name.contains("great olm (left claw)"))
		{
			this.meleeHand = null;
		}
		else if (name.contains("great olm (right claw)"))
		{
			this.mageHand = null;
		}
		else if (npc.getId() == NpcID.RAIDS_VESPULA_PORTAL)
		{
			this.vespDied = true;
		}
		else if (name.contains("vasa nistirio"))
		{
			this.vasa = null;
			this.vasaCrystalTicks = 0;
			this.vasaAtCrystal = false;
		}
	}

	@Subscribe
	public void onClientTick(ClientTick clientTick)
	{
		if (this.client.getVarbitValue(VarbitID.RAIDS_CLIENT_INDUNGEON) != 1)
		{
			return;
		}
		this.client.setMenuEntries(
			Arrays.stream(this.client.getMenuEntries())
				.filter(this.filterMenuEntries)
				.toArray(MenuEntry[]::new)
		);
	}
}
