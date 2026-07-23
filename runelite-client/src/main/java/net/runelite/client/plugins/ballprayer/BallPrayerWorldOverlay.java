package net.runelite.client.plugins.ballprayer;

import net.runelite.api.Client;
import net.runelite.api.Prayer;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.Perspective;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayPriority;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Dimension;

@Singleton
public class BallPrayerWorldOverlay extends Overlay
{
    private final Client client;
    private final BallPrayerConfig config;
    private final BallPrayerPlugin plugin;

    // match the small-orb base colors
    private static final Color RANGE_COLOR = new Color(0x42A832);
    private static final Color MAGE_COLOR  = new Color(0x2978D5);

    @Inject
    public BallPrayerWorldOverlay(Client client, BallPrayerPlugin plugin, BallPrayerConfig config)
    {
        this.client = client;
        this.plugin = plugin;
        this.config = config;

        setPosition(OverlayPosition.DYNAMIC);
        setLayer(OverlayLayer.ABOVE_SCENE);
        setPriority(OverlayPriority.MED);
    }

    @Override
    public Dimension render(Graphics2D graphics)
    {
        // 1) gate on world-overlay toggle
        if (!config.showWorldOverlay())
        {
            return null;
        }

        // 2) fetch bigPrayer & ticks
        Prayer bigPrayer = plugin.getBigPrayer();
        int ticks = plugin.getBigPrayerTicksRemaining(client.getGameCycle());
        if (bigPrayer == null || ticks < 0)
        {
            return null;
        }

        // 3) use the same brighter small-orb colours
        Color base = (bigPrayer == Prayer.PROTECT_FROM_MAGIC) ? MAGE_COLOR : RANGE_COLOR;
        Color color = base.brighter().brighter();
        String text = String.valueOf(ticks);

        // 4) get player tile and draw text there
        LocalPoint lp = client.getLocalPlayer().getLocalLocation();
        net.runelite.api.Point rp = Perspective.localToCanvas(client, lp, client.getPlane());
        if (rp != null)
        {
            java.awt.Point pt = new java.awt.Point(rp.getX(), rp.getY());
            graphics.setColor(color);
            graphics.drawString(text, pt.x, pt.y);
        }

        return null;
    }
}