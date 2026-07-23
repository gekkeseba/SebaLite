package net.runelite.client.plugins.ballprayer;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("ballprayer")
public interface BallPrayerConfig extends Config
{
    @ConfigItem(
            keyName = "showWorldOverlay",
            name    = "World-tile big-ball timer",
            description = "Display the big-ball tick counter at your character’s feet"
    )
    default boolean showWorldOverlay()
    {
        return true;
    }

    @ConfigItem(
            keyName = "showSmallPrayerOverlay",
            name    = "Small prayer orb overlay",
            description = "Display the single-line “p:” prayer tick overlay"
    )
    default boolean showSmallPrayerOverlay()
    {
        return true;
    }

    @ConfigItem(
        keyName = "showProjectileOverlay",
        name    = "Projectile prayer overlay",
        description = "Display tick counters on the prayer orbs as they fly"
    )
    default boolean showProjectileOverlay()
    {
        return true;
    }

    @ConfigItem(
            keyName = "showBigBallInfobox",
            name    = "Big-ball infobox",
            description = "Display the big-ball tick as an InfoBox in the corner"
    )
    default boolean showBigBallInfobox()
    {
        return false;
    }
}