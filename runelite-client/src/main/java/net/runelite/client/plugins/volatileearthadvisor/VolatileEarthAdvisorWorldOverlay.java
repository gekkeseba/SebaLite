package net.runelite.client.plugins.volatileearthadvisor;

import com.google.inject.Inject;
import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Stroke;
import java.util.List;
import net.runelite.api.Client;
import net.runelite.api.NPC;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayPriority;

public class VolatileEarthAdvisorWorldOverlay extends Overlay
{
    @Inject private Client client;
    @Inject private VolatileEarthAdvisorService service;
    @Inject private VolatileEarthAdvisorConfig config;

    public VolatileEarthAdvisorWorldOverlay()
    {
        setPosition(OverlayPosition.DYNAMIC);
        setLayer(OverlayLayer.ABOVE_SCENE);
        setPriority(OverlayPriority.HIGH);
    }

    @Override
    public Dimension render(Graphics2D g)
    {
        if (!config.enabled() || !service.isActive())
        {
            return null;
        }

        final List<VolatileEarthAdvisorService.Recommendation> toDraw = service.getTop();
        if (toDraw.isEmpty())
        {
            return null;
        }

        // Alternatives (faint)
        for (int i = toDraw.size() - 1; i >= 1; i--)
        {
            drawPath(g, toDraw.get(i).getPath(), 1f, 0.35f);
        }

        // Best path
        final VolatileEarthAdvisorService.Recommendation best = toDraw.get(0);
        drawPath(g, best.getPath(), config.drawPathStrokePx(), 1.0f);

        // Labels: "1" over TARGET, "2" over SOURCE
        drawNpcLabel(g, best.getTarget(), "1");
        drawNpcLabel(g, best.getSource(), "2");

        return null;
    }

    private void drawNpcLabel(Graphics2D g, NPC npc, String text)
    {
        if (npc == null || npc.getLocalLocation() == null) return;
        net.runelite.api.Point p = npc.getCanvasTextLocation(g, text, 0);
        if (p == null) return;

        Font old = g.getFont();
        g.setFont(new Font("Arial", Font.BOLD, 18)); // fixed, crisp, no config toggle

        // 1px outline for contrast
        g.setColor(Color.BLACK);
        g.drawString(text, p.getX() + 1, p.getY() + 1);
        g.drawString(text, p.getX() - 1, p.getY() + 1);
        g.drawString(text, p.getX() + 1, p.getY() - 1);
        g.drawString(text, p.getX() - 1, p.getY() - 1);

        g.setColor(Color.WHITE);
        g.drawString(text, p.getX(), p.getY());

        g.setFont(old);
    }

    private void drawPath(Graphics2D g, List<WorldPoint> path, float strokePx, float alpha)
    {
        if (path == null || path.size() < 2) return;

        final Stroke old = g.getStroke();
        final java.awt.Composite oc = g.getComposite();

        g.setStroke(new BasicStroke(strokePx));
        g.setComposite(AlphaComposite.SrcOver.derive(alpha));
        g.setColor(config.pathColor());

        for (int i = 1; i < path.size(); i++)
        {
            java.awt.Point a = VolatileEarthAdvisorService.tileCenterCanvas(client, path.get(i - 1), g);
            java.awt.Point b = VolatileEarthAdvisorService.tileCenterCanvas(client, path.get(i), g);
            if (a != null && b != null)
            {
                g.drawLine(a.x, a.y, b.x, b.y);
            }
        }

        g.setComposite(oc);
        g.setStroke(old);
    }
}