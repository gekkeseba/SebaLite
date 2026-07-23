package net.runelite.client.plugins.rockhighlighter;

import java.awt.Color;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("rockhighlighter")
public interface RockHighlighterConfig extends Config
{
    enum HighlightMode
    {
        HULL,
        TILE,
        BOTH
    }

    @ConfigItem(
            keyName = "mode",
            name = "Highlight mode",
            description = "Choose whether to highlight the object's hull, its tile or both"
    )
    default HighlightMode mode()
    {
        return HighlightMode.HULL; // default hull only
    }

    @ConfigItem(
            keyName = "color",
            name = "Highlight colour",
            description = "Colour used for the highlight"
    )
    default Color color()
    {
        return new Color(0x00FF00); // default green
    }
}