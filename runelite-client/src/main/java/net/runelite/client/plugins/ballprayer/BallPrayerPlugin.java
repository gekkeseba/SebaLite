package net.runelite.client.plugins.ballprayer;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.awt.image.BufferedImage;
import java.awt.Color;
import javax.inject.Inject;
import lombok.Getter;
import net.runelite.api.Client;
import net.runelite.api.Prayer;
import net.runelite.api.Projectile;
import net.runelite.api.events.GameTick;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.ui.overlay.infobox.InfoBoxManager;
import net.runelite.client.ui.overlay.infobox.InfoBoxPriority;
import net.runelite.client.ui.overlay.infobox.Counter;
import com.google.inject.Provides;

@PluginDescriptor(
    name        = "Ball Prayer Helper",
    description = "Displays prayer ticks, world overlay, and big-ball infobox",
    tags        = {"prayer", "projectile", "tick"}
)
public class BallPrayerPlugin extends Plugin
{
    // These match the small-orb base colours
    private static final Color RANGE_COLOR = new Color(0x42A832);
    private static final Color MAGE_COLOR  = new Color(0x2978D5);

    @Inject private Client client;
    @Inject private BallPrayerConfig config;
    @Inject private OverlayManager overlayManager;
    @Inject private BallPrayerOverlay panelOverlay;
    @Inject private BallPrayerWorldOverlay worldOverlay;
    @Inject private BallPrayerProjectileOverlay projectileOverlay;
    @Inject private InfoBoxManager infoBoxManager;

    @Getter private Prayer bigPrayer;
    private int bigOrbEndCycle;
    private int bigPrayerActivationCycle;
    private final List<TrackedProjectile> trackedProjectiles = new LinkedList<>();

    // The draggable corner counter
    private Counter bigBallCounter;

    public List<TrackedProjectile> getTrackedProjectiles()
    {
        return trackedProjectiles;
    }

    public int getBigPrayerTicksRemaining(final int currentCycle)
    {
        if (bigPrayer == null)
        {
            return 0;
        }
        int cyclesRemaining = bigPrayerActivationCycle - currentCycle;
        return (int) Math.ceil(cyclesRemaining / 30.0);
    }

    @Provides
    public BallPrayerConfig provideConfig(ConfigManager configManager)
    {
        return configManager.getConfig(BallPrayerConfig.class);
    }

    @Override
    protected void startUp() throws Exception
    {
        if (config.showSmallPrayerOverlay())
        {
            overlayManager.add(panelOverlay);
        }
        if (config.showProjectileOverlay())
        {
            overlayManager.add(projectileOverlay);
        }
        if (config.showWorldOverlay())
        {
            overlayManager.add(worldOverlay);
        }
        bigBallCounter = null;
    }

    @Override
    protected void shutDown() throws Exception
    {
        if (config.showSmallPrayerOverlay())
        {
            overlayManager.remove(panelOverlay);
        }
        if (config.showProjectileOverlay())
        {
            overlayManager.remove(projectileOverlay);
        }
        if (config.showWorldOverlay())
        {
            overlayManager.remove(worldOverlay);
        }
        removeBigBallInfoBox();
        trackedProjectiles.clear();
        bigPrayer = null;
    }

    @Subscribe
    public void onGameTick(GameTick event)
    {
        int currentCycle = client.getGameCycle();

        // --- Track projectile events ---
        for (Projectile p : client.getProjectiles())
        {
            int id = p.getId();
            switch (id)
            {
                case 3384:
                {
                    int endCycle = p.getEndCycle();
                    if (bigPrayer == null || Math.abs(endCycle - bigOrbEndCycle) > 10 * 30)
                    {
                        bigPrayer = Prayer.PROTECT_FROM_MISSILES;
                        bigOrbEndCycle = endCycle;
                        bigPrayerActivationCycle = endCycle + (3 * 30);
                    }
                    break;
                }
                case 3385:
                {
                    int endCycle = p.getEndCycle();
                    if (bigPrayer == null || Math.abs(endCycle - bigOrbEndCycle) > 10 * 30)
                    {
                        bigPrayer = Prayer.PROTECT_FROM_MAGIC;
                        bigOrbEndCycle = endCycle;
                        bigPrayerActivationCycle = endCycle + (3 * 30);
                    }
                    break;
                }
                case 3380: case 3379: case 3378:
                {
                    Prayer prayType = (id == 3380)
                        ? Prayer.PROTECT_FROM_MISSILES
                        : (id == 3379)
                            ? Prayer.PROTECT_FROM_MAGIC
                            : Prayer.PROTECT_FROM_MELEE;
                    boolean exists = trackedProjectiles.stream()
                        .anyMatch(tp -> tp.getProjectile() == p);
                    if (!exists)
                    {
                        trackedProjectiles.add(new TrackedProjectile(p, prayType));
                    }
                    break;
                }
                default:
            }
        }

        // Clear bigPrayer after activation
        if (bigPrayer != null && currentCycle >= bigPrayerActivationCycle)
        {
            bigPrayer = null;
        }

        // --- InfoBox logic for big-ball tick counter ---
        if (config.showBigBallInfobox())
        {
            int ticks = getBigPrayerTicksRemaining(currentCycle);
            if (bigPrayer != null && ticks >= 0)
            {
                if (bigBallCounter == null)
                {
                    BufferedImage icon = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
                    Color base = (bigPrayer == Prayer.PROTECT_FROM_MAGIC) ? MAGE_COLOR : RANGE_COLOR;
                    Color bright = base.brighter().brighter();

                    bigBallCounter = new ColoredCounter(icon, this, ticks, bright);
                    bigBallCounter.setPriority(InfoBoxPriority.MED);
                    infoBoxManager.addInfoBox(bigBallCounter);
                }
                bigBallCounter.setCount(ticks);
            }
            else
            {
                removeBigBallInfoBox();
            }
        }

        // Remove expired split orbs
        Iterator<TrackedProjectile> it = trackedProjectiles.iterator();
        while (it.hasNext())
        {
            TrackedProjectile tp = it.next();
            if (tp.isExpired(currentCycle))
            {
                it.remove();
            }
        }
    }

    private void removeBigBallInfoBox()
    {
        if (bigBallCounter != null)
        {
            infoBoxManager.removeInfoBox(bigBallCounter);
            bigBallCounter = null;
        }
    }

    /**
     * A Counter that uses a custom text colour.
     */
    private static class ColoredCounter extends Counter
    {
        private final Color textColor;

        public ColoredCounter(BufferedImage icon, Plugin plugin, int count, Color textColor)
        {
            super(icon, plugin, count);
            this.textColor = textColor;
        }

        @Override
        public Color getTextColor()
        {
            return textColor;
        }
    }

    @Subscribe
    public void onConfigChanged(ConfigChanged event)
    {
        if (!event.getGroup().equals("ballprayer"))
        {
            return;
        }

        switch (event.getKey())
        {
            case "showSmallPrayerOverlay":
                if (config.showSmallPrayerOverlay())
                    overlayManager.add(panelOverlay);
                else
                    overlayManager.remove(panelOverlay);
                break;

            case "showProjectileOverlay":
                if (config.showProjectileOverlay())
                    overlayManager.add(projectileOverlay);
                else
                    overlayManager.remove(projectileOverlay);
                break;

            case "showWorldOverlay":
                if (config.showWorldOverlay())
                    overlayManager.add(worldOverlay);
                else
                    overlayManager.remove(worldOverlay);
                break;

            case "showBigBallInfobox":
                if (!config.showBigBallInfobox())
                    removeBigBallInfoBox();
                // If enabled, infobox will appear on next GameTick
                break;
        }
    }
}