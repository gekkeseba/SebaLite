package net.runelite.client.plugins.ballprayer;

import net.runelite.api.Client;
import net.runelite.api.Projectile;
import net.runelite.api.Perspective;
import net.runelite.api.Point;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayPriority;
import net.runelite.client.ui.overlay.OverlayUtil;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Font;

@Singleton
public class BallPrayerProjectileOverlay extends Overlay
{
    private static final Color RANGE_COLOR = new Color(0x42A832).brighter().brighter();
    private static final Color MAGE_COLOR  = new Color(0x2978D5).brighter().brighter();
    private static final Color MELEE_COLOR = new Color(0xC03030).brighter().brighter();

    private final Client client;
    private final BallPrayerPlugin plugin;
    private final BallPrayerConfig config;

    @Inject
    public BallPrayerProjectileOverlay(Client client, BallPrayerPlugin plugin, BallPrayerConfig config)
    {
        this.client = client;
        this.plugin = plugin;
        this.config = config;

        setPosition(OverlayPosition.DYNAMIC);
        setLayer(OverlayLayer.ABOVE_SCENE);
        setPriority(OverlayPriority.LOW);
    }

    @Override
    public Dimension render(Graphics2D graphics)
    {
        if (!config.showProjectileOverlay())
        {
            return null;
        }

        int currentCycle = client.getGameCycle();

        // Use bold 18pt for clarity
        graphics.setFont(graphics.getFont().deriveFont(Font.BOLD, 16f));

        for (TrackedProjectile tp : plugin.getTrackedProjectiles())
        {
            Projectile p = tp.getProjectile();
            String text = String.valueOf(tp.getTicksRemaining(currentCycle));

            Point projPoint = Perspective.localToCanvas(
                client,
                (int) p.getX(),
                (int) p.getY(),
                (int) p.getZ()
            );
            if (projPoint == null)
            {
                continue;
            }

            Color color;
            switch (tp.getPrayer())
            {
                case PROTECT_FROM_MISSILES:
                    color = RANGE_COLOR;
                    break;
                case PROTECT_FROM_MAGIC:
                    color = MAGE_COLOR;
                    break;
                case PROTECT_FROM_MELEE:
                    color = MELEE_COLOR;
                    break;
                default:
                    color = Color.WHITE;
            }

            OverlayUtil.renderTextLocation(graphics, projPoint, text, color);
        }

        return null;
    }
}