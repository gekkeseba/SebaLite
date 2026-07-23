package net.runelite.client.plugins.spoontob;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import com.google.inject.Provides;
import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.api.NPC;
import net.runelite.api.Player;
import net.runelite.api.Point;
import net.runelite.api.Renderable;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.AnimationChanged;
import net.runelite.api.events.GameObjectSpawned;
import net.runelite.api.events.GameTick;
import net.runelite.api.gameval.ObjectID;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.callback.Hooks;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.spoontob.rooms.Bloat.Bloat;
import net.runelite.client.plugins.spoontob.rooms.Maiden.Maiden;
import net.runelite.client.plugins.spoontob.rooms.Nylocas.Nylocas;
import net.runelite.client.plugins.spoontob.rooms.Sotetseg.Sotetseg;
import net.runelite.client.plugins.spoontob.rooms.Verzik.Verzik;
import net.runelite.client.plugins.spoontob.rooms.Xarpus.Xarpus;
import net.runelite.client.plugins.spoontob.util.CustomGameObject;
import net.runelite.client.ui.overlay.OverlayManager;

@PluginDescriptor(
	name = "Theatre of Blood",
	description = "All-in-one visual helper for Theatre of Blood",
	tags = {"tob", "theatre", "raids", "maiden", "bloat", "nylocas", "sotetseg", "xarpus", "verzik"},
	enabledByDefault = false
)
public class SpoonTobPlugin extends Plugin
{
	public static int partySize;

	private Room[] rooms;

	@Inject
	private EventBus eventBus;
	@Inject
	private Maiden maiden;
	@Inject
	private Bloat bloat;
	@Inject
	private Nylocas nylocas;
	@Inject
	private Sotetseg sotetseg;
	@Inject
	private Xarpus xarpus;
	@Inject
	private Verzik verzik;
	@Inject
	private Client client;
	@Inject
	private ClientThread clientThread;
	@Inject
	private SpoonTobConfig config;
	@Inject
	private OverlayManager overlayManager;
	@Inject
	private SituationalTickOverlay tickOverlay;
	@Inject
	private Hooks hooks;

	private final Hooks.RenderableDrawListener drawListener = this::shouldDraw;
	private final Set<CustomGameObject> customizedGameObjects = new LinkedHashSet<>();

	public ArrayList<Integer> hiddenIndices;
	public final HashMap<Player, Integer> situationalTicksList = new HashMap<>();
	public int situationalTicks = 0;

	@Provides
	SpoonTobConfig getConfig(ConfigManager configManager)
	{
		return configManager.getConfig(SpoonTobConfig.class);
	}

	@Override
	protected void startUp()
	{
		this.situationalTicksList.clear();
		this.overlayManager.add(this.tickOverlay);
		if (this.rooms == null)
		{
			this.rooms = new Room[]{this.maiden, this.bloat, this.nylocas, this.sotetseg, this.xarpus, this.verzik};
			for (Room room : this.rooms)
			{
				room.init();
			}
		}
		for (Room room : this.rooms)
		{
			room.load();
			this.eventBus.register(room);
		}
		this.hooks.registerRenderableDrawListener(this.drawListener);
		this.hiddenIndices = new ArrayList<>();
	}

	@Override
	protected void shutDown()
	{
		this.situationalTicksList.clear();
		this.overlayManager.remove(this.tickOverlay);
		modifyCustomObjList(true, true);
		for (Room room : this.rooms)
		{
			this.eventBus.unregister(room);
			room.unload();
		}
		this.hooks.unregisterRenderableDrawListener(this.drawListener);
		clearHiddenNpcs();
		this.hiddenIndices = null;
		this.situationalTicks = 0;
	}

	public void refreshScene()
	{
		this.clientThread.invokeLater(() -> this.client.setGameState(net.runelite.api.GameState.LOADING));
	}

	@Subscribe
	public void onGameTick(GameTick event)
	{
		if (enforceRegion())
		{
			partySize = 0;
			for (int i = 330; i < 335; i++)
			{
				String varcStr = this.client.getVarcStrValue(i);
				if (varcStr != null && !varcStr.isEmpty())
				{
					partySize++;
				}
			}

			List<Player> toRemove = new ArrayList<>();
			for (Player p : this.situationalTicksList.keySet())
			{
				int ticks = this.situationalTicksList.get(p);
				if (ticks - 1 == 0)
				{
					toRemove.add(p);
				}
				else
				{
					this.situationalTicksList.put(p, ticks - 1);
				}
			}
			toRemove.forEach(this.situationalTicksList::remove);
		}
		else if (this.config.recolorBarriers() != SpoonTobConfig.barrierMode.OFF && !this.client.getTopLevelWorldView().isInstance())
		{
			Player you = this.client.getLocalPlayer();
			if (you != null && you.getWorldLocation().getRegionID() == 14642)
			{
				modifyCustomObjList(true, true);
			}
		}
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (!event.getGroup().equals("tobqol v2"))
		{
			return;
		}
		if (event.getKey().equals("recolBar") || event.getKey().equals("barrisCol"))
		{
			modifyCustomObjList(true, false);
			modifyCustomObjList(false, false);
		}
	}

	@Subscribe
	private void onGameObjectSpawned(GameObjectSpawned event)
	{
		GameObject obj = event.getGameObject();
		int id = obj.getId();
		if (id == ObjectID.TOB_ARENA_BARRIER || id == ObjectID.TOB_WALKWAY_VERZIK_BARRIER)
		{
			this.customizedGameObjects.add(new CustomGameObject(obj, id));
			modifyCustomObjList(false, false);
		}
	}

	private void modifyCustomObjList(boolean restore, boolean clear)
	{
		if (this.customizedGameObjects.isEmpty())
		{
			return;
		}
		if (restore)
		{
			List<CustomGameObject> objs = new ArrayList<>(this.customizedGameObjects);
			Lists.reverse(objs).forEach(CustomGameObject::restore);
			if (clear)
			{
				this.customizedGameObjects.clear();
			}
		}
		else if (this.config.recolorBarriers() == SpoonTobConfig.barrierMode.COLOR)
		{
			this.customizedGameObjects.forEach(o -> o.setFaceColorValues(this.config.barriersColor()));
		}
	}

	public boolean crossedLine(int region, Point start, Point end, boolean vertical)
	{
		if (!inRegion(region))
		{
			return false;
		}
		for (Player p : this.client.getPlayers())
		{
			WorldPoint wp = p.getWorldLocation();
			if (vertical)
			{
				for (int j = start.getY(); j < end.getY() + 1; j++)
				{
					if (wp.getRegionY() == j && wp.getRegionX() == start.getX())
					{
						return true;
					}
				}
			}
			else
			{
				for (int i = start.getX(); i < end.getX() + 1; i++)
				{
					if (wp.getRegionX() == i && wp.getRegionY() == start.getY())
					{
						return true;
					}
				}
			}
		}
		return false;
	}

	public boolean enforceRegion()
	{
		return inRegion(12611, 12612, 12613, 12687, 13122, 13123, 13125, 13379);
	}

	public boolean inRegion(int... regions)
	{
		int[] mapRegions = this.client.getTopLevelWorldView().getMapRegions();
		if (mapRegions == null)
		{
			return false;
		}
		for (int i : mapRegions)
		{
			for (int j : regions)
			{
				if (i == j)
				{
					return true;
				}
			}
		}
		return false;
	}

	public void setHiddenNpc(NPC npc, boolean hidden)
	{
		if (hidden)
		{
			this.hiddenIndices.add(npc.getIndex());
		}
		else
		{
			this.hiddenIndices.remove(Integer.valueOf(npc.getIndex()));
		}
	}

	public void clearHiddenNpcs()
	{
		this.hiddenIndices.clear();
	}

	@VisibleForTesting
	boolean shouldDraw(Renderable renderable, boolean drawingUI)
	{
		if (renderable instanceof NPC)
		{
			return !this.hiddenIndices.contains(((NPC) renderable).getIndex());
		}
		return true;
	}

	// Weapon basic-attack/special animation IDs used to derive "ticks until next attack"
	// for the situational-ticks overlay (Bloat tank swaps, Xarpus stare timing).
	// Named for what they represent here, not always the gameval name (several of these
	// IDs are shared generic animations gameval labels after a single, different weapon
	// example - e.g. dragon claws' basic attack is gameval's "HUMAN_AXE_CHOP" since axes
	// reuse the same generic swing). Verified by numeric ID against current game data.
	private static final int SCYTHE_ATTACK = 8056; // SCYTHE_OF_VITUR_ATTACK
	private static final int CLAW_ATTACK = 393; // HUMAN_AXE_CHOP (shared generic swing)
	private static final int WHIP_ATTACK = 1658; // SLAYER_ABYSSAL_WHIP_ATTACK
	private static final int CLAW_SPECIAL = 7514; // HUMAN_DRAGON_CLAWS_SPEC
	private static final int TRIDENT_ATTACK = 1167; // HUMAN_CASTWAVE_STAFF (trident "cast" swing)
	private static final int SURGE_CAST = 7855; // HUMAN_CAST_SURGE
	private static final int BLADE_ATTACK = 390; // HUMAN_SWORD_SLASH
	private static final int RAPIER_ATTACK = 8145; // GHRAZI_RAPIER_ATTACK
	private static final int HALBERD_SPECIAL = 1203; // DRAGON_HALBERD_SPECIAL_ATTACK
	private static final int HALBERD_ATTACK = 440; // HUMAN_SCYTHE_SWEEP (shared halberd sweep)
	private static final int WARHAMMER_SPECIAL = 1378; // DRAGON_WARHAMMER_SA_PLAYER
	private static final int BLUNT_ATTACK = 401; // HUMAN_BLUNT_POUND
	private static final int BGS_SPECIAL = 7643; // BGS_SPECIAL_ORNATE_PLAYER
	private static final int BGS_SPECIAL_2 = 7642; // BGS_SPECIAL_PLAYER
	private static final int TWO_HAND_ATTACK = 7045; // DH_SWORD_UPDATE_SLASH (shared 2h swing)
	private static final int ELDER_MAUL_ATTACK = 7516; // HUMAN_ELDER_MAUL_ATTACK

	@Subscribe
	public void onAnimationChanged(AnimationChanged event)
	{
		if (!(event.getActor() instanceof Player))
		{
			return;
		}
		Player player = (Player) event.getActor();
		int anim = player.getAnimation();
		int ticks = 0;
		if (anim == SCYTHE_ATTACK)
		{
			ticks = 5;
		}
		else if (anim == CLAW_ATTACK || anim == WHIP_ATTACK || anim == CLAW_SPECIAL || anim == TRIDENT_ATTACK
			|| anim == SURGE_CAST || anim == BLADE_ATTACK || anim == RAPIER_ATTACK)
		{
			ticks = 4;
		}
		else if (anim == HALBERD_SPECIAL || anim == HALBERD_ATTACK)
		{
			ticks = 7;
		}
		else if (anim == WARHAMMER_SPECIAL || anim == BLUNT_ATTACK || anim == BGS_SPECIAL
			|| anim == BGS_SPECIAL_2 || anim == TWO_HAND_ATTACK || anim == ELDER_MAUL_ATTACK)
		{
			ticks = 6;
		}

		if (ticks != 0)
		{
			if (this.client.getLocalPlayer() != null && player == this.client.getLocalPlayer())
			{
				this.situationalTicks = ticks;
			}
			this.situationalTicksList.put(player, ticks + 1);
		}
	}

	public Color calculateHitpointsColor(double hpPercent)
	{
		hpPercent = Math.max(Math.min(100.0, hpPercent), 0.0);
		double rMod = 130.0 * hpPercent / 100.0;
		double gMod = 235.0 * hpPercent / 100.0;
		double bMod = 125.0 * hpPercent / 100.0;
		int r = (int) Math.min(255.0, 255.0 - rMod);
		int g = Math.min(255, (int) gMod);
		int b = Math.min(255, (int) bMod);
		return new Color(r, g, b);
	}

	public Color oldHitpointsColor(double hpPercent)
	{
		hpPercent = Math.max(Math.min(100.0, hpPercent), 0.0);
		double rMod;
		double gMod;
		double bMod;
		if (hpPercent >= 75.0)
		{
			rMod = 0.0;
			gMod = 255.0;
			bMod = 0.0;
		}
		else if (hpPercent >= 50.0)
		{
			rMod = 255.0;
			gMod = 255.0;
			bMod = 0.0;
		}
		else if (hpPercent >= 30.0)
		{
			rMod = 220.0;
			gMod = 200.0;
			bMod = 0.0;
		}
		else
		{
			rMod = 255.0;
			gMod = 102.0;
			bMod = 102.0;
		}
		return new Color((int) rMod, (int) gMod, (int) bMod);
	}
}
