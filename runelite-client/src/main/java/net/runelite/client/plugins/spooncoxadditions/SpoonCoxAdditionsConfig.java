package net.runelite.client.plugins.spooncoxadditions;

import java.awt.Color;
import net.runelite.client.config.Alpha;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.config.Range;

@ConfigGroup("CoxAdditions")
public interface SpoonCoxAdditionsConfig extends Config
{
	@ConfigSection(name = "Olm", description = "Olm Plugins", position = 0, closedByDefault = true)
	String olmSection = "olm";
	@ConfigSection(name = "Rooms", description = "Cox Room Plugins", position = 1, closedByDefault = true)
	String roomSection = "rooms";

	@ConfigItem(keyName = "olmCrippleTimer", name = "Olm Cripple Timer", description = "Adds a timer over olms right hand when crippled", position = 4, section = olmSection)
	default boolean olmCrippleTimer()
	{
		return true;
	}

	@Range(min = 1, max = 32)
	@ConfigItem(name = "Olm Cripple Text Size", keyName = "olmCrippleTextSize", description = "Increase or decreases the size of the text for the Olm cripple timer timer", position = 5, section = olmSection)
	default int olmCrippleTextSize()
	{
		return 20;
	}

	@Alpha
	@ConfigItem(keyName = "olmCrippleText", name = "Olm Cripple Text", description = "Configures the color of the timer for olm hand cripple", position = 6, section = olmSection)
	default Color olmCrippleText()
	{
		return Color.YELLOW;
	}

	@ConfigItem(keyName = "hideAttackHead", name = "Hide Attack Head", description = "Removes the attack option on Olms Head before head phase", position = 7, section = olmSection)
	default boolean hideAttackHead()
	{
		return true;
	}

	@ConfigItem(name = "Crystal Mark Timer", keyName = "crystalMark", description = "Warns you when Olm has marked you for a crystal spike, and counts down until it lands", position = 8, section = olmSection)
	default crystalMarkMode crystalMark()
	{
		return crystalMarkMode.TEXT;
	}

	@Alpha
	@ConfigItem(name = "Crystal Mark Color", keyName = "crystalMarkColor", description = "Color of the crystal-mark warning", position = 9, section = olmSection)
	default Color crystalMarkColor()
	{
		return Color.MAGENTA;
	}

	@ConfigItem(keyName = "removeCastCoX", name = "Remove Cast CoX", description = "Removes cast on players in Chambers of Xeric", position = 2, section = roomSection)
	default boolean removeCastCoX()
	{
		return false;
	}

	@ConfigItem(name = "Remove Chop", keyName = "removeChop", description = "Removes chop option on trees at Ice Demon when no axe in inventory", position = 5, section = roomSection)
	default boolean removeChop()
	{
		return false;
	}

	@ConfigItem(name = "Mutta Chop Cycle", keyName = "meatTreeChopCycle", description = "Displays a timer till next chop cycle when cutting the Meat Tree", position = 8, section = roomSection)
	default meatTreeChopCycleMode meatTreeChopCycle()
	{
		return meatTreeChopCycleMode.OFF;
	}

	@ConfigItem(name = "Vanguard Tick Cycle", keyName = "vangsCycle", description = "Shows the ticks that the Vanguards have been up for. Resets everytime they go down", position = 9, section = roomSection)
	default VangsTicksMode vangsCycle()
	{
		return VangsTicksMode.OFF;
	}

	@ConfigItem(name = "Remove Feed", keyName = "removeFeed", description = "Removes feed option on lux grubs in vesp when no herbs in inventory", position = 10, section = roomSection)
	default boolean removeFeed()
	{
		return false;
	}

	@ConfigItem(name = "Remove Pick Root", keyName = "removePickRoot", description = "Removes pick on roots in Vespula after it dies", position = 11, section = roomSection)
	default boolean removePickRoot()
	{
		return false;
	}

	@ConfigItem(name = "Hide Attack Vespula", keyName = "hideVesp", description = "Hides attack option on Vespula", position = 12, section = roomSection)
	default boolean hideVesp()
	{
		return true;
	}

	@ConfigItem(keyName = "highlightShortcuts", name = "Highlight shortcuts", description = "Displays which shortcut it is", position = 14, section = roomSection)
	default boolean highlightShortcuts()
	{
		return true;
	}

	@ConfigItem(name = "Shortcut Color", keyName = "shortcutColor", description = "Highlight color for shortcuts", position = 15, section = roomSection)
	default Color shortcutColor()
	{
		return Color.YELLOW;
	}

	@ConfigItem(name = "Rope Tag Helper", keyName = "chinRope", description = "Highlights rangers/magers when multiple are next to each other", position = 19, section = roomSection)
	default chinRopeMode chinRope()
	{
		return chinRopeMode.OFF;
	}

	@Range(min = 1, max = 5)
	@ConfigItem(name = "Rope NPC Width", keyName = "chinRopeThiCC", description = "Width for the Rope NPC highlights", position = 20, section = roomSection)
	default int chinRopeThiCC()
	{
		return 2;
	}

	@ConfigItem(name = "Rope Tag Helper Color", keyName = "chinRopeColor", description = "Highlight color for rangers/magers chin helper at rope", position = 21, section = roomSection)
	default Color chinRopeColor()
	{
		return Color.MAGENTA;
	}

	@ConfigItem(name = "Rope Cross", keyName = "ropeCross", description = "Highlights the rope green during safe ticks, orange during questionable, and red during certain death", position = 22, section = roomSection)
	default ropeCrossMode ropeCross()
	{
		return ropeCrossMode.OFF;
	}

	@ConfigItem(name = "Rope Cross Ticks", keyName = "ropeCrossTicks", description = "Puts the ticks since activating rapid heal on the local player, on the rope, or both", position = 23, section = roomSection)
	default ropeCrossTicksMode ropeCrossTicks()
	{
		return ropeCrossTicksMode.ROPE;
	}

	@ConfigItem(name = "Rope Cross Ticks Countdown", keyName = "ropeTicksDown", description = "Counts down the ticks instead of up", position = 24, section = roomSection)
	default boolean ropeTicksDown()
	{
		return true;
	}

	@ConfigItem(name = "Shaman Slam", keyName = "shamanSlam", description = "Predicts where the Lizardman Shaman will rain down from the sky.", position = 25, section = roomSection)
	default boolean shamanSlam()
	{
		return false;
	}

	@Alpha
	@ConfigItem(name = "Shaman Slam Color", keyName = "shamanSlamColor", description = "Configures the color of the Shaman slam overlay", position = 26, section = roomSection)
	default Color shamanSlamColor()
	{
		return Color.RED;
	}

	@ConfigItem(name = "Vasa Crystal Timer", keyName = "vasaCrystalTimer", description = "Puts a timer on active crystal until Vasa teleports again", position = 27, section = roomSection)
	default crystalTimerMode vasaCrystalTimer()
	{
		return crystalTimerMode.BOLD;
	}

	@Range(min = 1, max = 32)
	@ConfigItem(name = "Vasa Crystal Timer Text", keyName = "vasaCrystalTextSize", description = "Increase or decreases the size of the text for the Vasa crystal timer", position = 28, section = roomSection)
	default int vasaCrystalTextSize()
	{
		return 20;
	}

	@ConfigItem(name = "Vasa Crystal Timer Color", keyName = "vasaCrystalTimerColor", description = "Color picker for vasa crystal timer", position = 29, section = roomSection)
	default Color vasaCrystalTimerColor()
	{
		return Color.WHITE;
	}

	enum VangsTicksMode
	{
		OFF,
		TOTAL_TICKS,
		FOUR_TICK_CYCLE,
		BOTH
	}

	enum chinRopeMode
	{
		OFF,
		HULL,
		OUTLINE
	}

	enum crystalTimerMode
	{
		OFF,
		BOLD,
		REGULAR,
		SMALL,
		CUSTOM
	}

	enum meatTreeChopCycleMode
	{
		OFF,
		OVERLAY,
		INFOBOX
	}

	enum crystalMarkMode
	{
		OFF,
		TEXT,
		AREA,
		BOTH
	}

	enum ropeCrossMode
	{
		OFF,
		TICKS,
		HIGHLIGHT,
		BOTH
	}

	enum ropeCrossTicksMode
	{
		ROPE,
		PLAYER,
		BOTH
	}
}
