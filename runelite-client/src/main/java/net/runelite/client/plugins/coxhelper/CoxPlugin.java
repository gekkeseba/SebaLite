package net.runelite.client.plugins.coxhelper;

import com.google.inject.Provides;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.inject.Inject;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Actor;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.NPC;
import net.runelite.api.Player;
import net.runelite.api.Prayer;
import net.runelite.api.Projectile;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameObjectDespawned;
import net.runelite.api.events.GameObjectSpawned;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.GraphicChanged;
import net.runelite.api.events.NpcDespawned;
import net.runelite.api.events.NpcSpawned;
import net.runelite.api.events.ProjectileMoved;
import net.runelite.api.gameval.AnimationID;
import net.runelite.api.gameval.NpcID;
import net.runelite.api.gameval.SpotanimID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.Text;

@PluginDescriptor(
	name = "CoX Helper",
	enabledByDefault = false,
	description = "All-in-one plugin for Chambers of Xeric",
	tags = {"CoX", "chamber", "xeric", "helper"}
)
@Slf4j
@Getter(AccessLevel.PACKAGE)
public class CoxPlugin extends Plugin
{
	// Guardian's melee attack animation; this id (430) is shared with an unrelated
	// human "spear block" animation in the current gameval AnimationID table, so it's
	// kept as a local, correctly-named constant rather than the misleading gameval name.
	private static final int GUARDIAN_ATTACK_ANIMATION = 430;

	private static final Pattern TP_REGEX = Pattern.compile("You have been paired with <col=ff0000>(.*)</col>! The magical power will enact soon...");

	private final Map<NPC, NPCContainer> npcContainers = new HashMap<>();

	@Inject
	@Getter(AccessLevel.NONE)
	private Client client;
	@Inject
	@Getter(AccessLevel.NONE)
	private CoxOverlay coxOverlay;
	@Inject
	@Getter(AccessLevel.NONE)
	private CoxInfoBox coxInfoBox;
	@Inject
	@Getter(AccessLevel.NONE)
	private CoxDebugBox coxDebugBox;
	@Inject
	@Getter(AccessLevel.NONE)
	private OverlayManager overlayManager;
	@Inject
	private Olm olm;

	private int vanguards;
	private boolean tektonActive;
	private int tektonAttackTicks;

	@Provides
	CoxConfig getConfig(ConfigManager configManager)
	{
		return configManager.getConfig(CoxConfig.class);
	}

	@Override
	protected void startUp()
	{
		this.overlayManager.add(this.coxOverlay);
		this.overlayManager.add(this.coxInfoBox);
		this.overlayManager.add(this.coxDebugBox);
		this.olm.hardRest();
	}

	@Override
	protected void shutDown()
	{
		this.overlayManager.remove(this.coxOverlay);
		this.overlayManager.remove(this.coxInfoBox);
		this.overlayManager.remove(this.coxDebugBox);
	}

	@Subscribe
	private void onChatMessage(ChatMessage event)
	{
		if (!this.inRaid())
		{
			return;
		}

		if (event.getType() == ChatMessageType.GAMEMESSAGE)
		{
			final Matcher tpMatcher = TP_REGEX.matcher(event.getMessage());

			if (tpMatcher.matches())
			{
				for (Player player : this.client.getPlayers())
				{
					final String rawPlayerName = player.getName();

					if (rawPlayerName != null)
					{
						final String fixedPlayerName = Text.sanitize(rawPlayerName);

						if (fixedPlayerName.equals(Text.sanitize(tpMatcher.group(1))))
						{
							this.olm.getVictims().add(new Victim(player, Victim.Type.TELEPORT));
						}
					}
				}
			}

			switch (Text.standardize(event.getMessageNode().getValue()))
			{
				case "the great olm rises with the power of acid.":
					olm.setPhaseType(Olm.PhaseType.ACID);
					break;
				case "the great olm rises with the power of crystal.":
					olm.setPhaseType(Olm.PhaseType.CRYSTAL);
					break;
				case "the great olm rises with the power of flame.":
					olm.setPhaseType(Olm.PhaseType.FLAME);
					break;
				case "the great olm fires a sphere of aggression your way. your prayers have been sapped.":
				case "the great olm fires a sphere of aggression your way.":
					this.olm.setPrayer(Prayer.PROTECT_FROM_MELEE);
					break;
				case "the great olm fires a sphere of magical power your way. your prayers have been sapped.":
				case "the great olm fires a sphere of magical power your way.":
					this.olm.setPrayer(Prayer.PROTECT_FROM_MAGIC);
					break;
				case "the great olm fires a sphere of accuracy and dexterity your way. your prayers have been sapped.":
				case "the great olm fires a sphere of accuracy and dexterity your way.":
					this.olm.setPrayer(Prayer.PROTECT_FROM_MISSILES);
					break;
			}
		}
	}

	@Subscribe
	private void onProjectileMoved(ProjectileMoved event)
	{
		if (!this.inRaid())
		{
			return;
		}

		final Projectile projectile = event.getProjectile();

		switch (projectile.getId())
		{
			case SpotanimID.OLM_FIREBREATH_TRAVEL:
				this.olm.setPrayer(Prayer.PROTECT_FROM_MAGIC);
				break;
			case SpotanimID.OLM_GENERIC_RANGE_PROJ:
				this.olm.setPrayer(Prayer.PROTECT_FROM_MISSILES);
				break;
			case SpotanimID.OLM_ACID_SPIT:
				Actor actor = projectile.getInteracting();
				if (actor instanceof Player)
				{
					this.olm.getVictims().add(new Victim((Player) actor, Victim.Type.ACID));
				}
				break;
		}
	}

	@Subscribe
	private void onGraphicChanged(GraphicChanged event)
	{
		if (!this.inRaid())
		{
			return;
		}

		if (!(event.getActor() instanceof Player))
		{
			return;
		}

		final Player player = (Player) event.getActor();

		if (player.getGraphic() == SpotanimID.OLM_BURNWITHME_PL_SPOT)
		{
			this.olm.getVictims().add(new Victim(player, Victim.Type.BURN));
		}
	}

	@Subscribe
	private void onNpcSpawned(NpcSpawned event)
	{
		if (!this.inRaid())
		{
			return;
		}

		final NPC npc = event.getNpc();

		switch (npc.getId())
		{
			case NpcID.RAIDS_TEKTON_WAITING:
			case NpcID.RAIDS_TEKTON_WALKING_STANDARD:
			case NpcID.RAIDS_TEKTON_FIGHTING_STANDARD:
			case NpcID.RAIDS_TEKTON_HAMMERING:
			case NpcID.RAIDS_TEKTON_WALKING_ENRAGED:
			case NpcID.RAIDS_TEKTON_FIGHTING_ENRAGED:
				this.npcContainers.put(npc, new NPCContainer(npc));
				this.tektonAttackTicks = 27;
				break;
			case NpcID.RAIDS_DOGODILE_SUBMERGED:
			case NpcID.RAIDS_DOGODILE_JUNIOR:
			case NpcID.RAIDS_DOGODILE:
			case NpcID.RAIDS_STONEGUARDIANS_LEFT:
			case NpcID.RAIDS_STONEGUARDIANS_RIGHT:
				this.npcContainers.put(npc, new NPCContainer(npc));
				break;
			case NpcID.RAIDS_VANGUARD_DORMANT:
			case NpcID.RAIDS_VANGUARD_WALKING:
			case NpcID.RAIDS_VANGUARD_MELEE:
			case NpcID.RAIDS_VANGUARD_RANGED:
			case NpcID.RAIDS_VANGUARD_MAGIC:
				this.vanguards++;
				this.npcContainers.put(npc, new NPCContainer(npc));
				break;
		}
	}

	@Subscribe
	private void onNpcDespawned(NpcDespawned event)
	{
		if (!this.inRaid())
		{
			return;
		}

		final NPC npc = event.getNpc();

		switch (npc.getId())
		{
			case NpcID.RAIDS_TEKTON_WAITING:
			case NpcID.RAIDS_TEKTON_WALKING_STANDARD:
			case NpcID.RAIDS_TEKTON_FIGHTING_STANDARD:
			case NpcID.RAIDS_TEKTON_HAMMERING:
			case NpcID.RAIDS_TEKTON_WALKING_ENRAGED:
			case NpcID.RAIDS_TEKTON_FIGHTING_ENRAGED:
			case NpcID.RAIDS_DOGODILE_SUBMERGED:
			case NpcID.RAIDS_DOGODILE_JUNIOR:
			case NpcID.RAIDS_DOGODILE:
			case NpcID.RAIDS_STONEGUARDIANS_LEFT:
			case NpcID.RAIDS_STONEGUARDIANS_RIGHT:
			case NpcID.RAIDS_STONEGUARDIANS_LEFT_DEAD:
			case NpcID.RAIDS_STONEGUARDIANS_RIGHT_DEAD:
				this.npcContainers.remove(npc);
				break;
			case NpcID.RAIDS_VANGUARD_DORMANT:
			case NpcID.RAIDS_VANGUARD_WALKING:
			case NpcID.RAIDS_VANGUARD_MELEE:
			case NpcID.RAIDS_VANGUARD_RANGED:
			case NpcID.RAIDS_VANGUARD_MAGIC:
				this.npcContainers.remove(npc);
				this.vanguards--;
				break;
		}
	}

	@Subscribe
	private void onGameTick(GameTick event)
	{
		if (!this.inRaid())
		{
			this.npcContainers.clear();
			this.vanguards = 0;
			this.tektonActive = false;
			this.tektonAttackTicks = 0;
			this.olm.hardRest();
			return;
		}

		this.handleNpcs();

		if (this.olm.isActive())
		{
			this.olm.update();
		}
	}

	private void handleNpcs()
	{
		for (NPCContainer npcs : this.getNpcContainers().values())
		{
			switch (npcs.getNpc().getId())
			{
				case NpcID.RAIDS_TEKTON_WAITING:
				case NpcID.RAIDS_TEKTON_WALKING_STANDARD:
				case NpcID.RAIDS_TEKTON_FIGHTING_STANDARD:
				case NpcID.RAIDS_TEKTON_HAMMERING:
				case NpcID.RAIDS_TEKTON_WALKING_ENRAGED:
				case NpcID.RAIDS_TEKTON_FIGHTING_ENRAGED:
					npcs.setTicksUntilAttack(npcs.getTicksUntilAttack() - 1);
					npcs.setAttackStyle(NPCContainer.Attackstyle.MELEE);
					switch (npcs.getNpc().getAnimation())
					{
						case AnimationID.TEKTON_ATTACK_STAB:
						case AnimationID.TEKTON_SLASH:
						case AnimationID.TEKTON_HAMMER_CRUSH:
						case AnimationID.TEKTON_ATTACK_STAB_ENRAGED:
						case AnimationID.TEKTON_HAMMER_CRUSH_ENRAGED:
						case AnimationID.TEKTON_SLASH_ENRAGED:
							this.tektonActive = true;
							if (npcs.getTicksUntilAttack() < 1)
							{
								npcs.setTicksUntilAttack(4);
							}
							break;
						case AnimationID.TEKTON_IDLE_TRANSITION_TO_READY:
						case AnimationID.TEKTON_IDLE_TRANSITION_TO_READY_ENRAGED:
							this.tektonActive = true;
							if (npcs.getTicksUntilAttack() < 1)
							{
								npcs.setTicksUntilAttack(3);
							}
							break;
						case AnimationID.TEKTON_IDLE_TRANSITION_TO_ANVIL:
							this.tektonActive = false;
							this.tektonAttackTicks = 47;
							if (npcs.getTicksUntilAttack() < 1)
							{
								npcs.setTicksUntilAttack(15);
							}
							break;
					}
					break;
				case NpcID.RAIDS_STONEGUARDIANS_LEFT:
				case NpcID.RAIDS_STONEGUARDIANS_RIGHT:
				case NpcID.RAIDS_STONEGUARDIANS_LEFT_DEAD:
				case NpcID.RAIDS_STONEGUARDIANS_RIGHT_DEAD:
					npcs.setTicksUntilAttack(npcs.getTicksUntilAttack() - 1);
					npcs.setAttackStyle(NPCContainer.Attackstyle.MELEE);
					if (npcs.getNpc().getAnimation() == GUARDIAN_ATTACK_ANIMATION &&
						npcs.getTicksUntilAttack() < 1)
					{
						npcs.setTicksUntilAttack(5);
					}
					break;
				case NpcID.RAIDS_VANGUARD_MAGIC:
					if (npcs.getAttackStyle() == NPCContainer.Attackstyle.UNKNOWN)
					{
						npcs.setAttackStyle(NPCContainer.Attackstyle.MAGE);
					}
					break;
				case NpcID.RAIDS_VANGUARD_RANGED:
					if (npcs.getAttackStyle() == NPCContainer.Attackstyle.UNKNOWN)
					{
						npcs.setAttackStyle(NPCContainer.Attackstyle.RANGE);
					}
					break;
				case NpcID.RAIDS_VANGUARD_MELEE:
					if (npcs.getAttackStyle() == NPCContainer.Attackstyle.UNKNOWN)
					{
						npcs.setAttackStyle(NPCContainer.Attackstyle.MELEE);
					}
					break;
			}
		}
		if (this.tektonActive && this.tektonAttackTicks > 0)
		{
			this.tektonAttackTicks--;
		}
	}

	boolean inRaid()
	{
		return this.client.getVarbitValue(VarbitID.RAIDS_CLIENT_INDUNGEON) == 1;
	}

	@Subscribe
	public void onGameObjectSpawned(GameObjectSpawned event)
	{
		if (event.getGameObject() == null)
		{
			return;
		}

		int id = event.getGameObject().getId();
		switch (id)
		{
			case Olm.HEAD_GAMEOBJECT_RISING:
			case Olm.HEAD_GAMEOBJECT_READY:
				if (this.olm.getHead() == null)
				{
					this.olm.startPhase();
				}
				this.olm.setHead(event.getGameObject());
				break;
			case Olm.LEFT_HAND_GAMEOBJECT_RISING:
			case Olm.LEFT_HAND_GAMEOBJECT_READY:
				this.olm.setHand(event.getGameObject());
				break;
		}
	}

	@Subscribe
	public void onGameObjectDespawned(GameObjectDespawned event)
	{
		if (event.getGameObject() == null)
		{
			return;
		}

		int id = event.getGameObject().getId();
		if (id == Olm.HEAD_GAMEOBJECT_READY)
		{
			this.olm.setHead(null);
		}
	}
}
