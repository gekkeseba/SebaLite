package net.runelite.client.plugins.volatileearthadvisor;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.Value;
import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.api.NPC;
import net.runelite.api.Tile;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.GameObjectDespawned;
import net.runelite.api.events.GameObjectSpawned;
import net.runelite.api.events.NpcDespawned;
import net.runelite.api.events.NpcSpawned;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.api.Perspective;

@Singleton
public class VolatileEarthAdvisorService
{
    public static final int ACID_OBJECT_ID = 57283;
    public static final int VOLATILE_NPC_ID = 14714;

    @Inject private Client client;
    @Inject private ClientThread clientThread;
    @Inject private VolatileEarthAdvisorConfig config;

    private final Set<WorldPoint> acidTiles = new HashSet<>();
    private final Map<Integer, NPC> volatileEarths = new HashMap<>();

    // Cached results
    private Recommendation best;
    private List<Recommendation> top = Collections.emptyList();

    @Value
    public static class Recommendation
    {
        NPC target;              // FIRST hit (orb ends here)
        NPC source;              // SECOND hit (orb spawns here)
        List<WorldPoint> path;   // diagonal-first from source → target
        int score;               // tiles with acid along path
    }

    public void startUp()
    {
        reset();
        clientThread.invoke(this::bootstrapFromScene);
    }

    public void shutDown()
    {
        reset();
    }

    private void reset()
    {
        acidTiles.clear();
        volatileEarths.clear();
        best = null;
        top = Collections.emptyList();
    }

    private void bootstrapFromScene()
    {
        if (client.getTopLevelWorldView() == null) return;

        // Load existing acids
        Tile[][] planeTiles = client.getScene().getTiles()[client.getPlane()];
        if (planeTiles != null)
        {
            for (int x = 0; x < planeTiles.length; x++)
            {
                Tile[] column = planeTiles[x];
                if (column == null) continue;
                for (int y = 0; y < column.length; y++)
                {
                    Tile tile = column[y];
                    if (tile == null) continue;
                    for (GameObject go : tile.getGameObjects())
                    {
                        if (go != null && go.getId() == ACID_OBJECT_ID)
                        {
                            acidTiles.add(go.getWorldLocation());
                        }
                    }
                }
            }
        }

        // Load existing NPCs
        for (NPC npc : client.getNpcs())
        {
            if (npc != null && npc.getId() == VOLATILE_NPC_ID)
            {
                volatileEarths.put(npc.getIndex(), npc);
            }
        }

        recompute();
    }

    // ---------- Events ----------

    @Subscribe
    public void onNpcSpawned(NpcSpawned e)
    {
        final NPC n = e.getNpc();
        if (n.getId() == VOLATILE_NPC_ID)
        {
            volatileEarths.put(n.getIndex(), n);
            recompute();
        }
    }

    @Subscribe
    public void onNpcDespawned(NpcDespawned e)
    {
        final NPC n = e.getNpc();
        if (n.getId() == VOLATILE_NPC_ID)
        {
            volatileEarths.remove(n.getIndex());
            if (volatileEarths.isEmpty())
            {
                best = null;
                top = Collections.emptyList();
            }
            else
            {
                recompute();
            }
        }
    }

    @Subscribe
    public void onGameObjectSpawned(GameObjectSpawned e)
    {
        final GameObject go = e.getGameObject();
        if (go.getId() == ACID_OBJECT_ID)
        {
            acidTiles.add(go.getWorldLocation());
            if (!volatileEarths.isEmpty())
            {
                recompute();
            }
        }
    }

    @Subscribe
    public void onGameObjectDespawned(GameObjectDespawned e)
    {
        final GameObject go = e.getGameObject();
        if (go.getId() == ACID_OBJECT_ID)
        {
            acidTiles.remove(go.getWorldLocation());
            if (!volatileEarths.isEmpty())
            {
                recompute();
            }
        }
    }

    // ---------- Public getters ----------

    public Recommendation getBest() { return best; }

    /** Returns up to (1 + showTopAlternatives) recommendations, best first. */
    public List<Recommendation> getTop()
    {
        if (best == null) return Collections.emptyList();
        int k = Math.min(1 + Math.max(0, config.showTopAlternatives()), top.size());
        return top.subList(0, k);
    }

    public boolean isActive()
    {
        return config.enabled() && !volatileEarths.isEmpty();
    }

    // ---------- Core logic ----------

    private void recompute()
    {
        if (!config.enabled()) return;

        if (volatileEarths.size() < 2)
        {
            best = null;
            top = Collections.emptyList();
            return;
        }

        final List<NPC> trees = new ArrayList<>(volatileEarths.values());
        final List<Recommendation> all = new ArrayList<>(trees.size() * (trees.size() - 1));

        for (int i = 0; i < trees.size(); i++)
        {
            for (int j = 0; j < trees.size(); j++)
            {
                if (i == j) continue;

                NPC target = trees.get(i); // FIRST hit
                NPC source = trees.get(j); // SECOND hit (spawns orb)

                WorldPoint src = source.getWorldLocation();
                WorldPoint dst = target.getWorldLocation();
                if (src == null || dst == null || src.getPlane() != dst.getPlane()) continue;

                List<WorldPoint> path = diagonalFirstLine(src, dst);

                // Feasibility constraints (tiles == ticks)
                int len = path.size();
                int min = Math.max(1, config.minPathTiles());
                if (len < min) continue;

                int max = Math.max(0, config.maxPathTiles());
                if (max > 0 && len > max) continue;

                int score = 0;
                if (!acidTiles.isEmpty())
                {
                    for (WorldPoint p : path)
                    {
                        if (acidTiles.contains(p)) score++;
                    }
                }

                all.add(new Recommendation(target, source, path, score));
            }
        }

        if (all.isEmpty())
        {
            best = null;
            top = Collections.emptyList();
            return;
        }

        // Sort: highest score; then shorter feasible path; then shorter Manhattan
        all.sort(
                Comparator.<Recommendation>comparingInt(Recommendation::getScore).reversed()
                        .thenComparingInt(r -> r.getPath().size())
                        .thenComparingInt(r -> manhattan(
                                r.getSource().getWorldLocation(),
                                r.getTarget().getWorldLocation()))
        );

        top = all;
        best = all.get(0);
    }

    private static int manhattan(WorldPoint a, WorldPoint b)
    {
        return Math.abs(a.getX() - b.getX()) + Math.abs(a.getY() - b.getY());
    }

    /** Diagonal-first from source → target (opposite of player pathing priority) */
    static List<WorldPoint> diagonalFirstLine(WorldPoint src, WorldPoint dst)
    {
        ArrayList<WorldPoint> line = new ArrayList<>(32);
        int x = src.getX(), y = src.getY(), z = src.getPlane();
        int dx = dst.getX() - x;
        int dy = dst.getY() - y;

        int stepX = Integer.compare(dx, 0);
        int stepY = Integer.compare(dy, 0);

        int absDx = Math.abs(dx);
        int absDy = Math.abs(dy);

        int diagSteps = Math.min(absDx, absDy);
        for (int i = 0; i < diagSteps; i++)
        {
            x += stepX;
            y += stepY;
            line.add(new WorldPoint(x, y, z));
        }

        int remX = absDx - diagSteps;
        for (int i = 0; i < remX; i++)
        {
            x += stepX;
            line.add(new WorldPoint(x, y, z));
        }

        int remY = absDy - diagSteps;
        for (int i = 0; i < remY; i++)
        {
            y += stepY;
            line.add(new WorldPoint(x, y, z));
        }
        return line;
    }

    /** For drawing a line between tile centers; handles nulls defensively. */
    public static java.awt.Point tileCenterCanvas(Client client, WorldPoint wp, java.awt.Graphics2D g)
    {
        if (wp == null) return null;
        LocalPoint lp = LocalPoint.fromWorld(client, wp);
        if (lp == null) return null;
        net.runelite.api.Point p = Perspective.localToCanvas(client, lp, wp.getPlane());
        if (p == null) return null;
        return new java.awt.Point(p.getX(), p.getY());
    }
}