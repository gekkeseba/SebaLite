package net.runelite.client.plugins.spoontob.rooms.Xarpus;

import com.google.common.collect.ImmutableSet;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.GroundObject;
import net.runelite.api.NPC;
import net.runelite.api.Skill;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.GroundObjectSpawned;
import net.runelite.api.events.HitsplatApplied;
import net.runelite.api.events.NpcChanged;
import net.runelite.api.events.NpcDespawned;
import net.runelite.api.events.NpcSpawned;
import net.runelite.api.gameval.NpcID;
import net.runelite.api.gameval.ObjectID;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.game.AlternateSprites;
import net.runelite.client.game.SkillIconManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.spoontob.Room;
import net.runelite.client.plugins.spoontob.SpoonTobConfig;
import net.runelite.client.plugins.spoontob.SpoonTobPlugin;
import net.runelite.client.plugins.spoontob.util.TheatreRegions;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.infobox.Counter;
import net.runelite.client.ui.overlay.infobox.InfoBox;
import net.runelite.client.ui.overlay.infobox.InfoBoxManager;
import net.runelite.client.util.ColorUtil;
import net.runelite.client.util.ImageUtil;
import org.apache.commons.lang3.tuple.Pair;

public class Xarpus extends Room
{
	protected static final Set<Integer> P0_IDS = ImmutableSet.of(NpcID.TOB_XARPUS_STATIC, NpcID.TOB_XARPUS_STATIC_STORY, NpcID.TOB_XARPUS_STATIC_HARD);
	protected static final Set<Integer> P1_IDS = ImmutableSet.of(NpcID.TOB_XARPUS_FEEDING, NpcID.TOB_XARPUS_FEEDING_STORY, NpcID.TOB_XARPUS_FEEDING_HARD);
	protected static final Set<Integer> P2_IDS = ImmutableSet.of(NpcID.TOB_XARPUS_COMBAT, NpcID.TOB_XARPUS_COMBAT_STORY, NpcID.TOB_XARPUS_COMBAT_HARD);
	protected static final Set<Integer> P3_IDS = ImmutableSet.of(NpcID.XARPUS_DEATH, NpcID.XARPUS_DEATH_STORY, NpcID.XARPUS_DEATH_HARD);
	private static final Set<Integer> HM_IDS = ImmutableSet.of(NpcID.TOB_XARPUS_STATIC_HARD, NpcID.TOB_XARPUS_FEEDING_HARD, NpcID.TOB_XARPUS_COMBAT_HARD, NpcID.XARPUS_DEATH_HARD);

	private static BufferedImage EXHUMED_COUNT_ICON;
	private static BufferedImage HEALED_COUNT_ICON;

	@Inject
	private XarpusOverlay xarpusOverlay;

	@Inject
	private XarpusCounterPanel xarpusPanel;

	@Inject
	private InfoBoxManager infoBoxManager;

	@Inject
	private SkillIconManager iconManager;

	@Inject
	private Client client;

	int healCount = 0;

	private ExhumedInfobox exhumedCounter;
	private Counter xarpusHealedCounter;
	private boolean xarpusActive;
	private boolean xarpusStare;
	private boolean postScreech = false;

	private final Map<Long, Pair<GroundObject, Integer>> xarpusExhumeds = new HashMap<>();
	private int exhumedCount;
	private int xarpusTicksUntilAttack;
	private NPC xarpusNPC;
	private boolean exhumedSpawned = false;
	private boolean isHM = false;

	@Inject
	protected Xarpus(SpoonTobPlugin plugin, SpoonTobConfig config)
	{
		super(plugin, config);
	}

	public void init()
	{
		EXHUMED_COUNT_ICON = ImageUtil.resizeCanvas(ImageUtil.loadImageResource(AlternateSprites.class, "1067-POISON.png"), 26, 26);
		HEALED_COUNT_ICON = ImageUtil.resizeCanvas(this.iconManager.getSkillImage(Skill.HITPOINTS, true), 26, 26);
	}

	public void load()
	{
		this.overlayManager.add((Overlay) this.xarpusOverlay);
		this.overlayManager.add((Overlay) this.xarpusPanel);
	}

	public void unload()
	{
		this.overlayManager.remove((Overlay) this.xarpusOverlay);
		this.overlayManager.remove((Overlay) this.xarpusPanel);
		this.healCount = 0;
		this.infoBoxManager.removeInfoBox(this.exhumedCounter);
		this.exhumedCounter = null;
		this.infoBoxManager.removeInfoBox(this.xarpusHealedCounter);
		this.xarpusHealedCounter = null;
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (!event.getGroup().equals("tobqol v2"))
		{
			return;
		}
		if (event.getKey().equals("exhuIB3"))
		{
			if (this.config.exhumedIB())
			{
				this.infoBoxManager.addInfoBox(this.exhumedCounter);
			}
			else
			{
				this.infoBoxManager.removeInfoBox(this.exhumedCounter);
			}
		}
		if (event.getKey().equals("xarHealC3"))
		{
			if (this.config.xarpusHealingCount())
			{
				this.infoBoxManager.addInfoBox(this.xarpusHealedCounter);
			}
			else
			{
				this.infoBoxManager.removeInfoBox(this.xarpusHealedCounter);
			}
		}
	}

	@Subscribe
	public void onNpcSpawned(NpcSpawned npcSpawned)
	{
		NPC npc = npcSpawned.getNpc();
		if (!P0_IDS.contains(npc.getId()) && !P1_IDS.contains(npc.getId()) && !P2_IDS.contains(npc.getId()) && !P3_IDS.contains(npc.getId()))
		{
			return;
		}
		this.isHM = HM_IDS.contains(npc.getId());
		this.xarpusActive = true;
		this.xarpusNPC = npc;
		this.xarpusStare = false;
		this.xarpusTicksUntilAttack = 9;
		this.healCount = 0;
		this.exhumedSpawned = false;
		this.postScreech = false;
	}

	@Subscribe
	public void onNpcDespawned(NpcDespawned npcDespawned)
	{
		NPC npc = npcDespawned.getNpc();
		if (!P0_IDS.contains(npc.getId()) && !P1_IDS.contains(npc.getId()) && !P2_IDS.contains(npc.getId()) && !P3_IDS.contains(npc.getId()))
		{
			return;
		}
		this.xarpusActive = false;
		this.xarpusNPC = null;
		this.xarpusStare = false;
		this.xarpusTicksUntilAttack = 9;
		this.xarpusExhumeds.clear();
		this.healCount = 0;
		this.exhumedSpawned = false;
		this.postScreech = false;
		this.infoBoxManager.removeInfoBox(this.exhumedCounter);
		this.exhumedCounter = null;
		this.exhumedCount = -1;
		removeCounter();
	}

	@Subscribe
	public void onNpcChanged(NpcChanged event)
	{
		NPC npc = event.getNpc();
		if (this.xarpusActive && (P2_IDS.contains(npc.getId()) || P3_IDS.contains(npc.getId())))
		{
			this.infoBoxManager.removeInfoBox(this.exhumedCounter);
			this.exhumedCounter = null;
		}
	}

	@Subscribe
	public void onGroundObjectSpawned(GroundObjectSpawned event)
	{
		if (!this.xarpusActive)
		{
			return;
		}
		GroundObject o = event.getGroundObject();
		if (o.getId() != ObjectID.TOB_XARPUS_EXHUMED)
		{
			return;
		}
		long hash = o.getHash();
		if (this.xarpusExhumeds.containsKey(hash))
		{
			return;
		}
		this.exhumedSpawned = true;
		if (this.exhumedCounter == null)
		{
			switch (SpoonTobPlugin.partySize)
			{
				case 5:
					this.exhumedCount = this.isHM ? 24 : 18;
					break;
				case 4:
					this.exhumedCount = this.isHM ? 20 : 15;
					break;
				case 3:
					this.exhumedCount = this.isHM ? 16 : 12;
					break;
				case 2:
					this.exhumedCount = this.isHM ? 13 : 9;
					break;
				default:
					this.exhumedCount = this.isHM ? 9 : 7;
					break;
			}
			this.exhumedCounter = new ExhumedInfobox(EXHUMED_COUNT_ICON, (Plugin) this.plugin, this.exhumedCount - 1);
			if (this.config.exhumedIB())
			{
				this.infoBoxManager.addInfoBox(this.exhumedCounter);
				this.exhumedCounter.setTooltip(ColorUtil.wrapWithColorTag((this.exhumedCounter.getCount() > 0) ? ("Exhumeds Left: " + this.exhumedCounter.getCount()) : "NOW", (this.exhumedCounter.getCount() <= 1) ? Color.RED : Color.WHITE));
			}
		}
		else
		{
			this.exhumedCounter.setCount(this.exhumedCounter.getCount() - 1);
			this.exhumedCounter.setTooltip(ColorUtil.wrapWithColorTag((this.exhumedCounter.getCount() > 0) ? ("Exhumeds Left: " + this.exhumedCounter.getCount()) : "NOW", (this.exhumedCounter.getCount() <= 1) ? Color.RED : Color.WHITE));
		}
		this.xarpusExhumeds.put(hash, Pair.of(o, this.isHM ? 9 : 11));
	}

	@Subscribe
	public void onGameTick(GameTick event)
	{
		if (this.xarpusActive)
		{
			if (!this.xarpusExhumeds.isEmpty())
			{
				this.xarpusExhumeds.replaceAll((k, v) -> Pair.of(v.getLeft(), v.getRight() - 1));
				this.xarpusExhumeds.values().removeIf(p -> p.getRight() <= 0);
			}
			if (this.xarpusNPC.getOverheadText() != null && !this.xarpusStare)
			{
				this.xarpusStare = true;
				this.xarpusTicksUntilAttack = 9;
			}
			if (this.xarpusStare)
			{
				this.xarpusTicksUntilAttack--;
				if (this.xarpusTicksUntilAttack <= 0)
				{
					this.xarpusTicksUntilAttack = 8;
					this.postScreech = true;
				}
				this.infoBoxManager.removeInfoBox(this.exhumedCounter);
			}
			else if (P2_IDS.contains(this.xarpusNPC.getId()))
			{
				this.xarpusTicksUntilAttack--;
				if (this.xarpusTicksUntilAttack <= 0)
				{
					this.xarpusTicksUntilAttack = 4;
				}
			}
		}
	}

	@Subscribe
	public void onHitsplatApplied(HitsplatApplied event)
	{
		if (event.getActor() != null && event.getActor().getName() != null && event.getActor().getName().toLowerCase().contains("xarpus") && event.getHitsplat().getHitsplatType() == 6)
		{
			this.healCount += event.getHitsplat().getAmount();
			addCounter();
			updateCounter();
		}
	}

	private void updateCounter()
	{
		if (this.xarpusHealedCounter != null)
		{
			this.xarpusHealedCounter.setCount(this.healCount);
		}
	}

	private void addCounter()
	{
		if (this.config.xarpusHealingCount() && this.xarpusHealedCounter == null)
		{
			this.xarpusHealedCounter = new Counter(HEALED_COUNT_ICON, (Plugin) this.plugin, this.healCount);
			this.xarpusHealedCounter.setTooltip("Xarpus Heals");
			this.infoBoxManager.addInfoBox(this.xarpusHealedCounter);
		}
	}

	private void removeCounter()
	{
		if (this.xarpusHealedCounter != null)
		{
			this.infoBoxManager.removeInfoBox(this.xarpusHealedCounter);
			this.healCount = 0;
			this.xarpusHealedCounter = null;
		}
	}

	public boolean isInXarpusRegion()
	{
		return TheatreRegions.inRegion(this.client, TheatreRegions.XARPUS);
	}

	public ExhumedInfobox getExhumedCounter()
	{
		return this.exhumedCounter;
	}

	public Counter getXarpusHealedCounter()
	{
		return this.xarpusHealedCounter;
	}

	public boolean isXarpusActive()
	{
		return this.xarpusActive;
	}

	public boolean isXarpusStare()
	{
		return this.xarpusStare;
	}

	public Map<Long, Pair<GroundObject, Integer>> getXarpusExhumeds()
	{
		return this.xarpusExhumeds;
	}

	public int getXarpusTicksUntilAttack()
	{
		return this.xarpusTicksUntilAttack;
	}

	public NPC getXarpusNPC()
	{
		return this.xarpusNPC;
	}

	public boolean isExhumedSpawned()
	{
		return this.exhumedSpawned;
	}

	public boolean isHM()
	{
		return this.isHM;
	}

	public boolean isPostScreech()
	{
		return this.postScreech;
	}

	public int getHealCount()
	{
		return this.healCount;
	}

	public static class ExhumedInfobox extends InfoBox
	{
		private int count;

		public ExhumedInfobox(BufferedImage image, Plugin plugin, int count)
		{
			super(image, plugin);
			this.count = count;
		}

		public int getCount()
		{
			return this.count;
		}

		public void setCount(int count)
		{
			this.count = count;
		}

		public String getText()
		{
			return Integer.toString(getCount());
		}

		public Color getTextColor()
		{
			return (this.count <= 1) ? Color.RED : Color.WHITE;
		}
	}
}
