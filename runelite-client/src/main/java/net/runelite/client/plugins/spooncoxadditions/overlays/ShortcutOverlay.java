package net.runelite.client.plugins.spooncoxadditions.overlays;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.image.BufferedImage;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.api.Perspective;
import net.runelite.api.Point;
import net.runelite.api.Skill;
import net.runelite.api.TileObject;
import net.runelite.api.gameval.ObjectID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.client.game.SkillIconManager;
import net.runelite.client.plugins.spooncoxadditions.SpoonCoxAdditionsConfig;
import net.runelite.client.plugins.spooncoxadditions.SpoonCoxAdditionsPlugin;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayPriority;
import net.runelite.client.ui.overlay.OverlayUtil;

@Singleton
public class ShortcutOverlay extends Overlay
{
	private final Client client;
	private final SpoonCoxAdditionsConfig config;
	private final SpoonCoxAdditionsPlugin plugin;
	private final BufferedImage treeIcon;
	private final BufferedImage strengthIcon;
	private final BufferedImage miningIcon;

	@Inject
	ShortcutOverlay(Client client, SpoonCoxAdditionsPlugin plugin, SpoonCoxAdditionsConfig config, SkillIconManager iconManager)
	{
		this.client = client;
		this.plugin = plugin;
		this.config = config;
		setPosition(OverlayPosition.DYNAMIC);
		setPriority(OverlayPriority.LOW);
		setLayer(OverlayLayer.ABOVE_SCENE);
		this.treeIcon = iconManager.getSkillImage(Skill.WOODCUTTING);
		this.strengthIcon = iconManager.getSkillImage(Skill.STRENGTH);
		this.miningIcon = iconManager.getSkillImage(Skill.MINING);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (this.client.getVarbitValue(VarbitID.RAIDS_CLIENT_INDUNGEON) != 1 || !this.plugin.isHighlightShortcuts())
		{
			return null;
		}

		for (TileObject shortcut : this.plugin.getShortcut())
		{
			if (shortcut.getPlane() != this.client.getPlane())
			{
				continue;
			}

			Shape poly = (shortcut instanceof GameObject) ? ((GameObject) shortcut).getConvexHull() : shortcut.getCanvasTilePoly();
			if (poly == null)
			{
				continue;
			}

			BufferedImage icon;
			int id = shortcut.getId();
			if (id == ObjectID.RAIDS_CORRIDOR_ROOTS)
			{
				icon = this.treeIcon;
			}
			else if (id == ObjectID.RAIDS_CORRIDOR_ROCKS)
			{
				icon = this.miningIcon;
			}
			else if (id == ObjectID.RAIDS_CORRIDOR_BOULDER)
			{
				icon = this.strengthIcon;
			}
			else
			{
				continue;
			}

			Point canvasLoc = Perspective.getCanvasImageLocation(this.client, shortcut.getLocalLocation(), icon, 150);
			if (canvasLoc != null)
			{
				graphics.drawImage(icon, canvasLoc.getX(), canvasLoc.getY(), null);
			}

			Shape clickbox = shortcut.getClickbox();
			if (clickbox != null)
			{
				Color fillColor = new Color(this.config.shortcutColor().getRed(), this.config.shortcutColor().getGreen(), this.config.shortcutColor().getBlue(), 20);
				OverlayUtil.renderHoverableArea(graphics, clickbox, this.client.getMouseCanvasPosition(), fillColor, this.config.shortcutColor(), this.config.shortcutColor().darker());
			}
		}
		return null;
	}
}
