package net.runelite.client.plugins.ballprayer;

import net.runelite.api.Client;
import net.runelite.api.Prayer;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayPriority;
import net.runelite.client.ui.overlay.components.PanelComponent;
import net.runelite.client.ui.overlay.components.ComponentOrientation;
import net.runelite.client.ui.overlay.components.LineComponent;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Color;
import java.awt.Font;
import java.awt.Point;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

@Singleton
public class BallPrayerOverlay extends OverlayPanel
{
    private static final Color RANGE_COLOR = new Color(0x42A832);
    private static final Color MAGE_COLOR  = new Color(0x2978D5);
    private static final Color MELEE_COLOR = new Color(0xC03030);

    private final Client client;
    private final BallPrayerPlugin plugin;
    private final BallPrayerConfig config;

    @Inject
    public BallPrayerOverlay(Client client, BallPrayerPlugin plugin, BallPrayerConfig config)
    {
        super(plugin);
        this.client = client;
        this.plugin = plugin;
        this.config = config;

        setPosition(OverlayPosition.DYNAMIC);
        setPriority(OverlayPriority.LOW);
        setResizable(false);
        setMovable(true);

        // Fully transparent background
        panelComponent.setBackgroundColor(new Color(0, 0, 0, 0));
    }

    @Override
    public Dimension render(Graphics2D graphics)
    {
        // 18pt bold font for consistency
        graphics.setFont(graphics.getFont().deriveFont(Font.BOLD, 18f));

        // Only render if small-prayer overlay is enabled
        if (!config.showSmallPrayerOverlay())
        {
            return null;
        }

        // Get current game cycle and tracked projectiles
        int currentCycle = client.getGameCycle();
        Collection<TrackedProjectile> orbs = plugin.getTrackedProjectiles();
        List<TrackedProjectile> sorted = new ArrayList<>(orbs);
        sorted.sort(Comparator.comparingInt(o -> o.getTicksRemaining(currentCycle)));

        // Configure panel: vertical layout with 18px gap
        panelComponent.setOrientation(ComponentOrientation.VERTICAL);
        panelComponent.setWrap(false);
        panelComponent.setGap(new Point(0, 18));
        panelComponent.getChildren().clear();

        // Build a single horizontal row, prefixed with "p:"
        PanelComponent row = new PanelComponent();
        row.setBackgroundColor(new Color(0, 0, 0, 0));
        row.setOrientation(ComponentOrientation.HORIZONTAL);
        row.setWrap(false);
        row.setGap(new Point(15, 0));

        row.getChildren().add(LineComponent.builder()
                .left("p:")
                .build());

        // Add each tick with its brightened colour
        for (TrackedProjectile orb : sorted)
        {
            int ticks = orb.getTicksRemaining(currentCycle);
            Color base = getPrayerColor(orb.getPrayer());
            Color color = base.brighter().brighter();
            row.getChildren().add(LineComponent.builder()
                    .left(String.valueOf(ticks))
                    .leftColor(color)
                    .build());
        }

        panelComponent.getChildren().add(row);
        return super.render(graphics);
    }

    private Color getPrayerColor(Prayer prayer)
    {
        switch (prayer)
        {
            case PROTECT_FROM_MISSILES: return RANGE_COLOR;
            case PROTECT_FROM_MAGIC:    return MAGE_COLOR;
            case PROTECT_FROM_MELEE:    return MELEE_COLOR;
            default:                    return Color.WHITE;
        }
    }
}