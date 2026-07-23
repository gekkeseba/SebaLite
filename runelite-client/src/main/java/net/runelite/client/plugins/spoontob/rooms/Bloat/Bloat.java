package net.runelite.client.plugins.spoontob.rooms.Bloat;

import com.google.common.collect.ImmutableSet;
import java.awt.Color;
import java.awt.Polygon;
import java.io.BufferedInputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import javax.inject.Inject;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.FloatControl;
import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.api.GameState;
import net.runelite.api.GraphicsObject;
import net.runelite.api.NPC;
import net.runelite.api.NPCComposition;
import net.runelite.api.Scene;
import net.runelite.api.Tile;
import net.runelite.api.coords.Angle;
import net.runelite.api.coords.Direction;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldArea;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.AnimationChanged;
import net.runelite.api.events.GameObjectSpawned;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.GraphicsObjectCreated;
import net.runelite.api.events.NpcDespawned;
import net.runelite.api.events.NpcSpawned;
import net.runelite.api.events.VarbitChanged;
import net.runelite.api.gameval.NpcID;
import net.runelite.api.gameval.ObjectID;
import net.runelite.api.gameval.SpotanimID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.spoontob.Room;
import net.runelite.client.plugins.spoontob.RoomOverlay;
import net.runelite.client.plugins.spoontob.SpoonTobConfig;
import net.runelite.client.plugins.spoontob.SpoonTobPlugin;
import net.runelite.client.plugins.spoontob.rooms.Bloat.stomp.BloatDown;
import net.runelite.client.plugins.spoontob.rooms.Bloat.stomp.def.BloatChunk;
import net.runelite.client.plugins.spoontob.util.TheatreRegions;
import net.runelite.client.ui.overlay.Overlay;

public class Bloat extends Room
{
	protected static final Set<Integer> BLOAT_IDS = ImmutableSet.of(NpcID.TOB_BLOAT, NpcID.TOB_BLOAT_STORY, NpcID.TOB_BLOAT_HARD);

	public static final Set<Integer> topOfTankObjectIDs = ImmutableSet.of(ObjectID.TOB_BLOAT_TABLE, ObjectID.TOB_BLOAT_CORPSE2, ObjectID.TOB_BLOAT_CORPSE4, ObjectID.TOB_BLOAT_CORPSE5);
	public static final Set<Integer> tankObjectIDs = ImmutableSet.of(ObjectID.TOB_BLOAT_CHAMBER, ObjectID.TOB_BLOAT_PILLAR, ObjectID.TOB_BLOAT_VIAL_2, ObjectID.TOB_BLOAT_VIAL_1, ObjectID.TOB_BLOAT_CORPSE4, ObjectID.TOB_DUNGEON_BLOAT_DEAD_MERC_MULTI);

	@Inject
	private BloatOverlay bloatOverlay;

	@Inject
	private Client client;

	protected int lastVarp6447 = 0;
	public int bloatVar = 0;

	private boolean bloatActive;
	private NPC bloatNPC;
	private int bloatDownCount = 0;
	private int bloatUpTimer = 0;
	private int bloatState = 0;
	private BloatDown bloatDown = null;
	private final HashMap<WorldPoint, Integer> bloathands = new HashMap<>();

	public int handTicks = 4;
	public boolean handsFalling = false;

	private static Clip clip;
	private LocalPoint bloatPrevLoc = null;
	private String bloatDirection = "";

	@Inject
	protected Bloat(SpoonTobPlugin plugin, SpoonTobConfig config)
	{
		super(plugin, config);
	}

	public void load()
	{
		this.overlayManager.add((Overlay) this.bloatOverlay);
		try
		{
			AudioInputStream stream = AudioSystem.getAudioInputStream(new BufferedInputStream(SpoonTobPlugin.class.getResourceAsStream("reverse.wav")));
			AudioFormat format = stream.getFormat();
			DataLine.Info info = new DataLine.Info(Clip.class, format);
			clip = (Clip) AudioSystem.getLine(info);
			clip.open(stream);
			FloatControl control = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
			if (control != null)
			{
				control.setValue(this.config.reverseVolume() / 2 - 45);
			}
		}
		catch (Exception e)
		{
			clip = null;
		}
	}

	public void unload()
	{
		this.overlayManager.remove((Overlay) this.bloatOverlay);
		this.bloatDownCount = 0;
		this.bloatState = 0;
		this.bloatUpTimer = 0;
		this.bloatDown = null;
		this.handTicks = 4;
		this.handsFalling = false;
		this.bloatPrevLoc = null;
		this.bloatDirection = "";
	}

	@Subscribe
	public void onNpcSpawned(NpcSpawned npcSpawned)
	{
		NPC npc = npcSpawned.getNpc();
		if (BLOAT_IDS.contains(npc.getId()))
		{
			this.bloatActive = true;
			this.bloatNPC = npc;
			this.bloatUpTimer = 0;
		}
	}

	@Subscribe
	public void onNpcDespawned(NpcDespawned npcDespawned)
	{
		NPC npc = npcDespawned.getNpc();
		if (BLOAT_IDS.contains(npc.getId()))
		{
			this.bloatActive = false;
			this.bloatNPC = null;
			this.bloatUpTimer = 0;
		}
	}

	@Subscribe
	public void onAnimationChanged(AnimationChanged event)
	{
		if (this.client.getGameState() != GameState.LOGGED_IN || event.getActor() != this.bloatNPC)
		{
			return;
		}
		this.bloatUpTimer = 0;
	}

	@Subscribe
	protected void onGraphicsObjectCreated(GraphicsObjectCreated graphicsObjectC)
	{
		if (!this.bloatActive)
		{
			return;
		}
		GraphicsObject graphicsObject = graphicsObjectC.getGraphicsObject();
		if (graphicsObject.getId() >= SpotanimID.TOB_BLOAT_FLIES_LARGE && graphicsObject.getId() <= SpotanimID.TOB_BLOAT_BLOOD_SPLAT)
		{
			WorldPoint point = WorldPoint.fromLocal(this.client, graphicsObject.getLocation());
			if (!this.bloathands.containsKey(point))
			{
				this.bloathands.put(point, 4);
				if (!this.handsFalling)
				{
					this.handsFalling = true;
				}
			}
		}
	}

	@Subscribe
	protected void onVarbitChanged(VarbitChanged event)
	{
		if (!isInRegion())
		{
			return;
		}
		int varp6447 = this.client.getVarbitValue(this.client.getVarps(), VarbitID.TOB_CLIENT_WAVEPROGRESS_TYPE);
		if (varp6447 != this.lastVarp6447 && varp6447 > 0)
		{
			this.bloatUpTimer = 0;
			this.bloatVar = 1;
		}
		this.lastVarp6447 = varp6447;
		if (this.client.getVarbitValue(VarbitID.TOB_CLIENT_WAVEPROGRESS_TYPE) == 0)
		{
			this.bloatVar = 0;
		}
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (!event.getGroup().equals("tobqol v2"))
		{
			return;
		}
		if (event.getKey().equals("hideAnnoyAssObj"))
		{
			if (TheatreRegions.inRegion(this.client, TheatreRegions.BLOAT))
			{
				this.plugin.refreshScene();
				applyObjectHiding();
			}
		}
		else if (event.getKey().equals("revVol2") && clip != null)
		{
			FloatControl control = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
			if (control != null)
			{
				control.setValue(this.config.reverseVolume() / 2 - 45);
			}
		}
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		if (event.getGameState() == GameState.LOGGED_IN && TheatreRegions.inRegion(this.client, TheatreRegions.BLOAT))
		{
			applyObjectHiding();
		}
	}

	@Subscribe
	public void onGameObjectSpawned(GameObjectSpawned event)
	{
		if (TheatreRegions.inRegion(this.client, TheatreRegions.BLOAT))
		{
			applyObjectHiding();
		}
	}

	private void applyObjectHiding()
	{
		if (this.config.hideAnnoyingAssObjects() == SpoonTobConfig.annoyingObjectHideMode.TANK)
		{
			removeGameObjectsFromScene(tankObjectIDs, this.client.getPlane());
			removeGameObjectsFromScene(topOfTankObjectIDs, 1);
			nullTopOfTankTiles();
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

	private void nullTopOfTankTiles()
	{
		List<WorldPoint> wpl = new WorldArea(3293, 4445, 6, 6, 1).toWorldPointList();
		wpl.forEach(wp -> WorldPoint.toLocalInstance(this.client, wp).forEach(this::nullThisTile));
	}

	public void nullThisTile(WorldPoint tile)
	{
		int plane = tile.getPlane();
		int sceneX = tile.getX() - this.client.getBaseX();
		int sceneY = tile.getY() - this.client.getBaseY();
		if (plane <= 3 && plane >= 0 && sceneX <= 103 && sceneX >= 0 && sceneY <= 103 && sceneY >= 0)
		{
			this.client.getScene().getTiles()[plane][sceneX][sceneY] = null;
		}
	}

	@Subscribe
	public void onGameTick(GameTick event)
	{
		if (!this.bloatActive)
		{
			return;
		}
		if (this.handsFalling)
		{
			this.handTicks--;
			if (this.handTicks <= 0)
			{
				this.handTicks = 4;
				this.handsFalling = false;
			}
		}
		this.bloatDownCount++;
		this.bloatUpTimer++;
		this.bloathands.values().removeIf(v -> v <= 0);
		this.bloathands.replaceAll((k, v) -> v - 1);

		updateBloatState();
		updateBloatDown();
		updateReverseNotifier();
	}

	private void updateBloatState()
	{
		if (this.bloatNPC.getAnimation() == -1)
		{
			this.bloatDownCount = 0;
			if (this.bloatNPC.getHealthScale() == 0)
			{
				this.bloatState = 2;
			}
			else if (this.bloatUpTimer >= 38)
			{
				this.bloatState = 4;
			}
			else
			{
				this.bloatState = 1;
			}
		}
		else if (this.bloatUpTimer >= 38)
		{
			this.bloatState = 4;
		}
		else if (25 < this.bloatDownCount && this.bloatDownCount < 35)
		{
			this.bloatState = 3;
		}
		else if (this.bloatDownCount < 26)
		{
			this.bloatState = 2;
		}
		else if (this.bloatNPC.getModelHeight() == 568)
		{
			this.bloatState = 2;
		}
		else
		{
			this.bloatState = 1;
		}
	}

	private void updateBloatDown()
	{
		if (this.bloatNPC == null)
		{
			return;
		}
		if (this.bloatNPC.getAnimation() == -1 && this.bloatDown != null)
		{
			this.bloatDown = null;
		}
		else if (this.bloatNPC.getAnimation() != -1 && this.bloatDown == null && !this.bloatNPC.isDead())
		{
			WorldPoint sw = this.bloatNPC.getWorldLocation();
			Direction dir = new Angle(this.bloatNPC.getOrientation()).getNearestDirection();
			Supplier<BloatChunk> chunk = () -> {
				LocalPoint lp = LocalPoint.fromWorld(this.client, sw);
				if (lp != null && this.client.isInInstancedRegion())
				{
					int zone = this.client.getInstanceTemplateChunks()[0][lp.getSceneX() >> 3][lp.getSceneY() >> 3];
					return BloatChunk.getOccupiedChunk(zone);
				}
				return BloatChunk.UNKNOWN;
			};
			this.bloatDown = new BloatDown(this.client, sw, dir, chunk.get());
		}
	}

	private void updateReverseNotifier()
	{
		if (this.bloatNPC == null || this.config.bloatReverseNotifier() == SpoonTobConfig.bloatTurnMode.OFF)
		{
			return;
		}
		LocalPoint lp = LocalPoint.fromWorld(this.client, this.bloatNPC.getWorldLocation());
		if (this.bloatPrevLoc != null && lp != null)
		{
			boolean changed = false;
			if (lp.getX() > this.bloatPrevLoc.getX())
			{
				if (this.bloatDirection.equals("W"))
				{
					changed = true;
				}
				this.bloatDirection = "E";
			}
			else if (lp.getX() < this.bloatPrevLoc.getX())
			{
				if (this.bloatDirection.equals("E"))
				{
					changed = true;
				}
				this.bloatDirection = "W";
			}
			else if (lp.getY() > this.bloatPrevLoc.getY())
			{
				if (this.bloatDirection.equals("S"))
				{
					changed = true;
				}
				this.bloatDirection = "N";
			}
			else if (lp.getY() < this.bloatPrevLoc.getY())
			{
				if (this.bloatDirection.equals("N"))
				{
					changed = true;
				}
				this.bloatDirection = "S";
			}
			if (changed)
			{
				if (this.config.bloatReverseNotifier() == SpoonTobConfig.bloatTurnMode.SOUND)
				{
					this.client.playSoundEffect(98, this.config.reverseVolume());
				}
				else if (this.config.bloatReverseNotifier() == SpoonTobConfig.bloatTurnMode.CHA_CHA && clip != null)
				{
					clip.setFramePosition(0);
					clip.start();
				}
			}
		}
		this.bloatPrevLoc = lp;
	}

	Polygon getBloatTilePoly()
	{
		if (this.bloatNPC == null)
		{
			return null;
		}
		int size = 1;
		NPCComposition composition = this.bloatNPC.getTransformedComposition();
		if (composition != null)
		{
			size = composition.getSize();
		}
		LocalPoint lp;
		switch (this.bloatState)
		{
			case 1:
			case 4:
				lp = this.bloatNPC.getLocalLocation();
				if (lp == null)
				{
					return null;
				}
				return RoomOverlay.getCanvasTileAreaPoly(this.client, lp, size, true);
			case 2:
			case 3:
				lp = LocalPoint.fromWorld(this.client, this.bloatNPC.getWorldLocation());
				if (lp == null)
				{
					return null;
				}
				return RoomOverlay.getCanvasTileAreaPoly(this.client, lp, size, false);
			default:
				return null;
		}
	}

	Color getBloatStateColor()
	{
		switch (this.bloatState)
		{
			case 2:
				return this.config.bloatIndicatorColorDOWN();
			case 3:
				return this.config.bloatIndicatorColorWARN();
			case 4:
				return this.config.bloatIndicatorColorTHRESH();
			default:
				return this.config.bloatIndicatorColorUP();
		}
	}

	private boolean isInRegion()
	{
		return TheatreRegions.inRegion(this.client, TheatreRegions.BLOAT);
	}

	public boolean isBloatActive()
	{
		return this.bloatActive;
	}

	public NPC getBloatNPC()
	{
		return this.bloatNPC;
	}

	public int getBloatDownCount()
	{
		return this.bloatDownCount;
	}

	public int getBloatUpTimer()
	{
		return this.bloatUpTimer;
	}

	public int getBloatState()
	{
		return this.bloatState;
	}

	public BloatDown getBloatDown()
	{
		return this.bloatDown;
	}

	public HashMap<WorldPoint, Integer> getBloathands()
	{
		return this.bloathands;
	}
}
