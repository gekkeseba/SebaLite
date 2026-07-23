package net.runelite.client.plugins.volatileearthadvisor;

import com.google.inject.Inject;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;

import java.awt.*;

public class VolatileEarthAdvisorPanelOverlay extends OverlayPanel
{
    @Inject private VolatileEarthAdvisorService service;
    @Inject private VolatileEarthAdvisorConfig config;

    public VolatileEarthAdvisorPanelOverlay()
    {
        setPosition(OverlayPosition.TOP_LEFT);
        setLayer(OverlayLayer.ABOVE_WIDGETS);
    }

    @Override
    public Dimension render(Graphics2D graphics)
    {
        if (!config.enabled()) return null;
        final var best = service.getBest();
        if (best == null) return null;

        panelComponent.getChildren().clear();
        panelComponent.getChildren().add(LineComponent.builder()
                .left("Best route")
                .right(best.getScore() + " tiles")
                .build());

        // Optional: add a hint for order
        panelComponent.getChildren().add(LineComponent.builder()
                .left("Order")
                .right("1: Target  2: Source")
                .build());

        return super.render(graphics);
    }
}