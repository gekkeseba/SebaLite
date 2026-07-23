package net.runelite.client.plugins.volatileearthadvisor;

import java.awt.Color;
import net.runelite.client.config.Alpha;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.Range;

@ConfigGroup("volatileAdvisor")
public interface VolatileEarthAdvisorConfig extends Config
{
    @ConfigItem(
            keyName = "enabled",
            name = "Enable Advisor",
            description = "Show best target/source pair and path",
            position = 0
    )
    default boolean enabled() { return true; }

    @ConfigItem(
            keyName = "showTopAlternatives",
            name = "Show Top Alternatives",
            description = "Draw faint paths for next-best pairs (0–3)",
            position = 1
    )
    @Range(min = 0, max = 3)
    default int showTopAlternatives() { return 1; }

    @ConfigItem(
            keyName = "pathColor",
            name = "Path Color",
            description = "Color used to draw the best path",
            position = 2
    )
    @Alpha
    default Color pathColor() { return Color.WHITE; }

    @ConfigItem(
            keyName = "minPathTiles",
            name = "Minimum Path Tiles",
            description = "Require at least this many tiles (ticks) between source and target (shield timing).",
            position = 3
    )
    @Range(min = 1, max = 40)
    default int minPathTiles() { return 5; }

    @ConfigItem(
            keyName = "maxPathTiles",
            name = "Maximum Path Tiles (0 = no limit)",
            description = "Ignore extremely long paths that are impractical to execute.",
            position = 4
    )
    @Range(min = 0, max = 80)
    default int maxPathTiles() { return 14; }

    @ConfigItem(
            keyName = "drawPathStrokePx",
            name = "Path Stroke (px)",
            description = "Stroke width for best path",
            position = 5
    )
    @Range(min = 1, max = 6)
    default int drawPathStrokePx() { return 3; }
}