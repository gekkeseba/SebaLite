package net.runelite.client.plugins.ballprayer;

import net.runelite.api.Prayer;
import net.runelite.api.Projectile;

/**
 * Holds state for a split orb projectile. Stores the projectile itself,
 * the prayer style to recommend and the projectile end cycle. The
 * end cycle is used to calculate the ticks remaining until impact.
 */
class TrackedProjectile
{
    private final Projectile projectile;
    private final Prayer prayer;
    private final int endCycle;

    TrackedProjectile(final Projectile projectile, final Prayer prayer)
    {
        this.projectile = projectile;
        this.prayer = prayer;
        // snapshot the end cycle on construction. The projectile's end cycle
        // does not normally change for these orbs.
        this.endCycle = projectile.getEndCycle();
    }

    public Projectile getProjectile()
    {
        return projectile;
    }

    public Prayer getPrayer()
    {
        return prayer;
    }

    /**
     * The number of game ticks remaining until this projectile lands.
     * RuneLite exposes projectile cycles in client frames (20 ms each). There are
     * 30 client frames per game tick (0.6 s / 0.02 s)【945392026315716†L216-L223】, so
     * converting cycles into game ticks requires dividing by 30. This method
     * rounds up so that a projectile with <1 tick remaining still returns 1.
     */
    public int getTicksRemaining(final int currentGameCycle)
    {
        int cyclesRemaining = endCycle - currentGameCycle;
        // Each game tick is 30 client cycles. Round up to the next integer.
        return (int) Math.ceil(cyclesRemaining / 30.0);
    }

    /**
     * Returns true if the projectile has already landed or despawned.
     */
    public boolean isExpired(final int currentGameCycle)
    {
        return (endCycle - currentGameCycle) <= 0;
    }
}