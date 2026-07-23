package net.runelite.client.plugins.aoewarnings;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.api.Perspective;
import net.runelite.api.Point;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.VarbitID;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

@Singleton
public class AoeWarningOverlay extends Overlay
{
	private final Client client;
	private final AoeWarningPlugin plugin;
	private final AoeWarningConfig config;

	@Inject
	public AoeWarningOverlay(Client client, AoeWarningPlugin plugin, AoeWarningConfig config)
	{
		this.client = client;
		this.plugin = plugin;
		this.config = config;
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.UNDER_WIDGETS);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		WorldPoint lp = this.client.getLocalPlayer().getWorldLocation();
		this.plugin.getLightningTrail().forEach(o -> OverlayUtil.drawTiles(graphics, this.client, o, lp, new Color(0, 150, 200), 2, 150, 50));
		this.plugin.getAcidTrail().forEach(o -> OverlayUtil.drawTiles(graphics, this.client, o.getWorldLocation(), lp, new Color(69, 241, 44), 2, 150, 50));
		this.plugin.getCrystalSpike().forEach(o -> OverlayUtil.drawTiles(graphics, this.client, o.getWorldLocation(), lp, new Color(255, 0, 84), 2, 150, 50));
		this.plugin.getWintertodtSnowFall().forEach(o -> OverlayUtil.drawTiles(graphics, this.client, o.getWorldLocation(), lp, new Color(255, 0, 84), 2, 150, 50));

		Instant now = Instant.now();
		Collection<ProjectileContainer> projectiles = this.plugin.getProjectiles();
		projectiles.forEach(proj ->
		{
			if (proj.getTargetPoint() == null)
			{
				return;
			}
			if (now.isAfter(proj.getStartTime().plus(Duration.ofMillis(proj.getLifetime()))))
			{
				return;
			}
			// Ice Demon's ice barrage and Tekton's meteor AoEs reuse projectile ids that also
			// appear outside their raid rooms; only show the warning while actually in a raid.
			if (proj.getAoeProjectileInfo() == AoeProjectileInfo.ICE_DEMON_ICE_BARRAGE_AOE
				|| proj.getAoeProjectileInfo() == AoeProjectileInfo.TEKTON_METEOR_AOE)
			{
				if (this.client.getVarbitValue(VarbitID.RAIDS_CLIENT_INDUNGEON) == 0)
				{
					return;
				}
			}

			Polygon tilePoly = Perspective.getCanvasTileAreaPoly(this.client, proj.getTargetPoint(), proj.getAoeProjectileInfo().getAoeSize());
			if (tilePoly == null)
			{
				return;
			}

			double progress = (System.currentTimeMillis() - proj.getStartTime().toEpochMilli()) / (double) proj.getLifetime();
			int tickProgress = proj.getFinalTick() - this.client.getTickCount();

			int fillAlpha;
			int outlineAlpha;
			if (this.config.isFadeEnabled())
			{
				fillAlpha = (int) ((1.0 - progress) * 25.0);
				outlineAlpha = (int) ((1.0 - progress) * 255.0);
			}
			else
			{
				fillAlpha = 25;
				outlineAlpha = 255;
			}
			fillAlpha = clamp(fillAlpha);
			outlineAlpha = clamp(outlineAlpha);

			Color color = tickProgress == 0 ? Color.RED : Color.WHITE;

			if (this.config.isOutlineEnabled())
			{
				graphics.setColor(new Color(ColorUtil.setAlphaComponent(this.config.overlayColor().getRGB(), outlineAlpha), true));
				graphics.drawPolygon(tilePoly);
			}
			if (this.config.tickTimers() && tickProgress >= 0)
			{
				OverlayUtil.renderTextLocation(graphics, Integer.toString(tickProgress), this.config.textSize(), this.config.fontStyle().getFont(), color, centerPoint(tilePoly.getBounds()), this.config.shadows(), 0);
			}
			graphics.setColor(new Color(ColorUtil.setAlphaComponent(this.config.overlayColor().getRGB(), fillAlpha), true));
			graphics.fillPolygon(tilePoly);
		});
		projectiles.removeIf(proj -> now.isAfter(proj.getStartTime().plus(Duration.ofMillis(proj.getLifetime()))));
		return null;
	}

	private static int clamp(int alpha)
	{
		return Math.max(0, Math.min(255, alpha));
	}

	private Point centerPoint(Rectangle rect)
	{
		int x = (int) (rect.getX() + rect.getWidth() / 2.0);
		int y = (int) (rect.getY() + rect.getHeight() / 2.0);
		return new Point(x, y);
	}
}
