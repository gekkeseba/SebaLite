package net.runelite.client.plugins.spoontob.rooms.Sotetseg;

import java.awt.image.BufferedImage;
import java.util.LinkedHashSet;
import javax.inject.Inject;
import net.runelite.api.Actor;
import net.runelite.api.Client;
import net.runelite.api.GroundObject;
import net.runelite.api.NPC;
import net.runelite.api.Point;
import net.runelite.api.Projectile;
import net.runelite.api.Skill;
import net.runelite.api.Tile;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.AnimationChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.GroundObjectSpawned;
import net.runelite.api.events.NpcDespawned;
import net.runelite.api.events.NpcSpawned;
import net.runelite.api.events.ProjectileMoved;
import net.runelite.api.gameval.AnimationID;
import net.runelite.api.gameval.NpcID;
import net.runelite.api.gameval.ObjectID;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.game.SkillIconManager;
import net.runelite.client.plugins.spoontob.Room;
import net.runelite.client.plugins.spoontob.SpoonTobConfig;
import net.runelite.client.plugins.spoontob.SpoonTobPlugin;
import net.runelite.client.plugins.spoontob.util.TheatreRegions;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.util.ImageUtil;

public class Sotetseg extends Room
{
	@Inject
	private Client client;

	@Inject
	private SotetsegOverlay sotetsegOverlay;

	@Inject
	private DeathBallPanel deathBallPanel;

	@Inject
	private SkillIconManager iconManager;

	static final int SOTETSEG_MAGE_ORB = 1606;
	static final int SOTETSEG_RANGE_ORB = 1607;
	static final int SOTETSEG_BIG_AOE_ORB = 1604;

	private static final Point SW_MAZE_SQUARE_OVERWORLD = new Point(9, 22);
	private static final Point SW_MAZE_SQUARE_UNDERWORLD = new Point(42, 31);

	public static Point getSwMazeSquareOverWorld()
	{
		return SW_MAZE_SQUARE_OVERWORLD;
	}

	public static Point getSwMazeSquareUnderWorld()
	{
		return SW_MAZE_SQUARE_UNDERWORLD;
	}

	static BufferedImage TACTICAL_NUKE_OVERHEAD;

	private boolean sotetsegActive;
	public NPC sotetsegNPC;

	private int overWorldRegionID = -1;
	private boolean wasInUnderWorld = false;

	private final LinkedHashSet<Point> redTiles = new LinkedHashSet<>();

	/** Ticks until Sotetseg's next attack lands, or until the death ball detonates. */
	public int sotetsegTicks = -1;

	public int sotetsegAttacksLeft = 10;

	private boolean bigOrbPresent = false;
	private boolean nukeSpawned = false;

	public BufferedImage mageIcon;
	public BufferedImage rangeIcon;

	@Inject
	protected Sotetseg(SpoonTobPlugin plugin, SpoonTobConfig config)
	{
		super(plugin, config);
	}

	public void init()
	{
		TACTICAL_NUKE_OVERHEAD = ImageUtil.resizeImage(ImageUtil.loadImageResource(SpoonTobPlugin.class, "Tactical_Nuke_Care_Package_Icon_MW2.png"), 32, 32);
	}

	public void load()
	{
		this.overlayManager.add((Overlay) this.sotetsegOverlay);
		this.overlayManager.add((Overlay) this.deathBallPanel);
		loadImages(this.config.soteHatSize());
	}

	public void unload()
	{
		this.overlayManager.remove((Overlay) this.sotetsegOverlay);
		this.overlayManager.remove((Overlay) this.deathBallPanel);
	}

	private void loadImages(int imageSize)
	{
		this.mageIcon = ImageUtil.resizeImage(this.iconManager.getSkillImage(Skill.MAGIC, true), imageSize, imageSize);
		this.rangeIcon = ImageUtil.resizeImage(this.iconManager.getSkillImage(Skill.RANGED, true), imageSize, imageSize);
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged change)
	{
		if (!change.getGroup().equals("tobqol v2"))
		{
			return;
		}
		if (change.getKey().equals("sotHSizz"))
		{
			loadImages(this.config.soteHatSize());
		}
	}

	@Subscribe
	public void onNpcSpawned(NpcSpawned npcSpawned)
	{
		NPC npc = npcSpawned.getNpc();
		switch (npc.getId())
		{
			case NpcID.TOB_SOTETSEG_NONCOMBAT:
			case NpcID.TOB_SOTETSEG_COMBAT:
			case NpcID.TOB_SOTETSEG_NONCOMBAT_STORY:
			case NpcID.TOB_SOTETSEG_COMBAT_STORY:
			case NpcID.TOB_SOTETSEG_NONCOMBAT_HARD:
			case NpcID.TOB_SOTETSEG_COMBAT_HARD:
				this.sotetsegNPC = npc;
				if (!this.sotetsegActive)
				{
					this.sotetsegActive = true;
					this.sotetsegAttacksLeft = 10;
				}
				break;
		}
	}

	@Subscribe
	public void onNpcDespawned(NpcDespawned npcDespawned)
	{
		NPC npc = npcDespawned.getNpc();
		switch (npc.getId())
		{
			case NpcID.TOB_SOTETSEG_NONCOMBAT:
			case NpcID.TOB_SOTETSEG_COMBAT:
			case NpcID.TOB_SOTETSEG_NONCOMBAT_STORY:
			case NpcID.TOB_SOTETSEG_COMBAT_STORY:
			case NpcID.TOB_SOTETSEG_NONCOMBAT_HARD:
			case NpcID.TOB_SOTETSEG_COMBAT_HARD:
				// plane 3 is the shadow-maze underworld: Sotetseg despawns there mid-fight, not dead
				if (this.client.getPlane() != 3)
				{
					this.sotetsegActive = false;
					this.sotetsegNPC = null;
					this.sotetsegTicks = -1;
				}
				if (npc.isDead())
				{
					this.sotetsegAttacksLeft = 10;
				}
				break;
		}
	}

	@Subscribe
	public void onProjectileMoved(ProjectileMoved e)
	{
		if (!this.sotetsegActive)
		{
			return;
		}
		Projectile p = e.getProjectile();
		if (this.client.getGameCycle() >= p.getStartCycle())
		{
			return;
		}
		switch (p.getId())
		{
			case SOTETSEG_BIG_AOE_ORB:
				this.sotetsegTicks = 11;
				this.sotetsegAttacksLeft = 10;
				break;
			case SOTETSEG_MAGE_ORB:
				// the only real Sotetseg attack that counts toward the 10 before the forced death ball -
				// SOTETSEG_RANGE_ORB is just the bounce/splash projectile a mage orb fires toward other
				// players after hitting its target, not a separate attack of Sotetseg's own
				countOrbAttack(p);
				break;
		}
	}

	/**
	 * Decrements the attacks-left counter once per genuine Sotetseg mage orb cast. Only checks that
	 * the projectile's own start position is Sotetseg's tile - split orbs (which ricochet off a
	 * hit player toward other players) originate from the hit player's tile instead, so this
	 * alone is enough to exclude them without needing to also match a specific animation ID.
	 */
	private void countOrbAttack(Projectile p)
	{
		WorldPoint soteWp = WorldPoint.fromLocal(this.client, this.sotetsegNPC.getLocalLocation());
		WorldPoint projWp = WorldPoint.fromLocal(this.client, p.getX1(), p.getY1(), this.client.getPlane());
		if (projWp.equals(soteWp))
		{
			this.sotetsegAttacksLeft--;
		}
	}

	@Subscribe
	public void onAnimationChanged(AnimationChanged event)
	{
		Actor actor = event.getActor();
		if (actor instanceof NPC && actor == this.sotetsegNPC)
		{
			int animation = event.getActor().getAnimation();
			if (animation == AnimationID.TOB_SOTETSEG_ATTACK_MELEE || animation == AnimationID.TOB_SOTETSEG_ATTACK_RANGED)
			{
				this.sotetsegTicks = 6;
			}
		}
	}

	@Subscribe
	public void onGameTick(GameTick event)
	{
		if (!this.sotetsegActive)
		{
			return;
		}

		if (this.sotetsegTicks >= 0)
		{
			this.sotetsegTicks--;
		}

		if (this.sotetsegNPC != null && (this.sotetsegNPC.getId() == NpcID.TOB_SOTETSEG_COMBAT || this.sotetsegNPC.getId() == NpcID.TOB_SOTETSEG_COMBAT_STORY || this.sotetsegNPC.getId() == NpcID.TOB_SOTETSEG_COMBAT_HARD))
		{
			this.redTiles.clear();
			if (isInOverWorld())
			{
				this.wasInUnderWorld = false;
				if (this.client.getLocalPlayer() != null && this.client.getLocalPlayer().getWorldLocation() != null)
				{
					this.overWorldRegionID = this.client.getLocalPlayer().getWorldLocation().getRegionID();
				}
			}
		}

		boolean foundBigOrb = false;
		for (Projectile p : this.client.getProjectiles())
		{
			if (p.getId() == SOTETSEG_BIG_AOE_ORB)
			{
				foundBigOrb = true;
				break;
			}
		}
		this.bigOrbPresent = foundBigOrb;

		if (!this.bigOrbPresent)
		{
			this.nukeSpawned = false;
		}
		else if (!this.nukeSpawned)
		{
			this.sotetsegTicks = 10;
			this.nukeSpawned = true;
		}
	}

	@Subscribe
	public void onGroundObjectSpawned(GroundObjectSpawned event)
	{
		if (!this.sotetsegActive)
		{
			return;
		}
		GroundObject o = event.getGroundObject();
		if (o.getId() == ObjectID.TOB_SOTETSEG_LIGHTTILE)
		{
			Tile t = event.getTile();
			WorldPoint p = WorldPoint.fromLocal(this.client, t.getLocalLocation());
			Point point = new Point(p.getRegionX(), p.getRegionY());
			if (isInOverWorld())
			{
				this.redTiles.add(new Point(point.getX() - SW_MAZE_SQUARE_OVERWORLD.getX(), point.getY() - SW_MAZE_SQUARE_OVERWORLD.getY()));
			}
			if (isInUnderWorld())
			{
				this.redTiles.add(new Point(point.getX() - SW_MAZE_SQUARE_UNDERWORLD.getX(), point.getY() - SW_MAZE_SQUARE_UNDERWORLD.getY()));
				this.wasInUnderWorld = true;
			}
		}
	}

	WorldPoint worldPointFromMazePoint(Point mazePoint)
	{
		if (this.overWorldRegionID == -1 && this.client.getLocalPlayer() != null)
		{
			return WorldPoint.fromRegion(this.client.getLocalPlayer().getWorldLocation().getRegionID(), mazePoint.getX() + SW_MAZE_SQUARE_OVERWORLD.getX(), mazePoint.getY() + SW_MAZE_SQUARE_OVERWORLD.getY(), 0);
		}
		return WorldPoint.fromRegion(this.overWorldRegionID, mazePoint.getX() + SW_MAZE_SQUARE_OVERWORLD.getX(), mazePoint.getY() + SW_MAZE_SQUARE_OVERWORLD.getY(), 0);
	}

	public boolean isSotetsegActive()
	{
		return this.sotetsegActive;
	}

	public boolean isWasInUnderWorld()
	{
		return this.wasInUnderWorld;
	}

	public LinkedHashSet<Point> getRedTiles()
	{
		return this.redTiles;
	}

	private boolean isInOverWorld()
	{
		return TheatreRegions.inRegion(this.client, TheatreRegions.SOTETSEG);
	}

	private boolean isInUnderWorld()
	{
		return TheatreRegions.inRegion(this.client, TheatreRegions.SOTETSEG_MAZE);
	}
}
