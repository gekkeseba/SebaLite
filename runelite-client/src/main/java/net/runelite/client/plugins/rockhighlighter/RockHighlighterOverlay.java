package net.runelite.client.plugins.rockhighlighter;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Shape;
import javax.inject.Inject;
import net.runelite.api.GameObject;
import net.runelite.api.TileObject;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayUtil;

public class RockHighlighterOverlay extends Overlay
{
    private final RockHighlighterPlugin plugin;
    private final RockHighlighterConfig config;

    @Inject
    private RockHighlighterOverlay(RockHighlighterPlugin plugin, RockHighlighterConfig config)
    {
        this.plugin = plugin;
        this.config = config;
        setPosition(OverlayPosition.DYNAMIC);
        setLayer(OverlayLayer.ABOVE_SCENE);
    }

    @Override
    public Dimension render(Graphics2D graphics)
    {
        // Prepare colours: solid border and a semi‑transparent fill
        Color border = config.color();
        Color fill   = new Color(border.getRed(), border.getGreen(), border.getBlue(), 50);
        BasicStroke stroke = new BasicStroke(1.0f);

        for (GameObject obj : plugin.getObjects())
        {
            TileObject tileObj = obj;
            drawHull(graphics, tileObj, border, fill, stroke);
        }
        return null;
    }

    private void drawHull(Graphics2D graphics, TileObject object, Color border, Color fill, BasicStroke stroke)
    {
        // getConvexHull() returns a java.awt.Shape, not a Polygon [oai_citation:4‡static.runelite.net](https://static.runelite.net/runelite-api/apidocs/net/runelite/api/events/ChatMessage.html#:~:text=public%C2%A0String%C2%A0getMessage).
        Shape hull = null;
        if (object instanceof GameObject)
        {
            hull = ((GameObject) object).getConvexHull();
        }
        if (hull != null)
        {
            OverlayUtil.renderPolygon(graphics, hull, border, fill, stroke);
        }
    }
}