package net.runelite.client.plugins.aoewarnings;

import com.google.common.collect.ImmutableSet;
import net.runelite.client.config.*;

import java.awt.*;
import java.util.Set;

@ConfigGroup("aoe")
public interface AoeWarningConfig extends Config {
    @ConfigSection(name = "Notify", description = "", position = -1)
    public static final String notifyTitle = "Notify";

    @ConfigSection(name = "Overlay", description = "", position = 1)
    public static final String overlayTitle = "Overlay";

    @ConfigSection(position = 7, name = "Text", description = "")
    public static final String textTitle = "Text";

    @ConfigSection(name = "Lizardman Shamans", description = "", position = 12)
    public static final String lizardmanaoeTitle = "Lizardman Shamans";

    @ConfigSection(name = "Crazy Archaeologist", description = "", position = 15)
    public static final String archaeologistaoeTitle = "Crazy Archaeologist";

    @ConfigSection(name = "Ice Demon", description = "", position = 18)
    public static final String icedemonTitle = "Ice Demon";

    @ConfigSection(name = "Vasa", description = "", position = 21)
    public static final String vasaTitle = "Vasa";

    @ConfigSection(name = "Tekton", description = "", position = 24)
    public static final String tektonTitle = "Tekton";

    @ConfigSection(name = "Vorkath", description = "", position = 27)
    public static final String vorkathTitle = "Vorkath";

    @ConfigSection(name = "Galvek", description = "", position = 30)
    public static final String galvekTitle = "Galvek";

    @ConfigSection(name = "Gargoyle Boss", description = "", position = 33)
    public static final String gargbossTitle = "Gargoyle Boss";

    @ConfigSection(name = "Vet'ion", description = "", position = 36)
    public static final String vetionTitle = "Vet'ion";

    @ConfigSection(name = "Chaos Fanatic", description = "", position = 39)
    public static final String chaosfanaticTitle = "Chaos Fanatic";

    @ConfigSection(name = "Olm", description = "", position = 42)
    public static final String olmTitle = "Olm";

    @ConfigSection(name = "Corporeal Beast", description = "", position = 51)
    public static final String corpTitle = "Corporeal Beast";

    @ConfigSection(name = "Wintertodt", description = "", position = 54)
    public static final String wintertodtTitle = "Wintertodt";

    @ConfigSection(name = "Xarpus", description = "", position = 57)
    public static final String xarpusTitle = "Xarpus";

    @ConfigSection(name = "Addy Drags", description = "", position = 60)
    public static final String addyDragsTitle = "Addy Drags";

    @ConfigSection(name = "Drakes", description = "", position = 63)
    public static final String drakeTitle = "Drakes";

    @ConfigSection(name = "Cerberus", description = "", position = 66)
    public static final String cerberusTitle = "Cerberus";

    @ConfigSection(name = "Demonic Gorilla", description = "", position = 69)
    public static final String demonicGorillaTitle = "Demonic Gorilla";

    @ConfigSection(name = "Verzik", description = "", position = 72)
    public static final String verzikTitle = "Verzik";

    public enum FontStyle {
        BOLD("Bold", 1),
        ITALIC("Italic", 2),
        PLAIN("Plain", 0);

        FontStyle(String name, int font) {
            this.name = name;
            this.font = font;
        }

        private String name;

        private int font;

        String getName() {
            return this.name;
        }

        int getFont() {
            return this.font;
        }

        public String toString() {
            return getName();
        }
    }

    public enum VorkathMode {
        BOMBS(AoeProjectileInfo.VORKATH_BOMB),
        POOLS(AoeProjectileInfo.VORKATH_POISON_POOL),
        SPAWN(AoeProjectileInfo.VORKATH_SPAWN),
        FIRES(AoeProjectileInfo.VORKATH_TICK_FIRE);

        private final AoeProjectileInfo info;

        VorkathMode(AoeProjectileInfo info) {
            this.info = info;
        }

        static VorkathMode of(AoeProjectileInfo info) {
            for (VorkathMode m : values()) {
                if (m.info == info)
                    return m;
            }
            throw new EnumConstantNotPresentException(VorkathMode.class, info.toString());
        }
    }

    @ConfigItem(keyName = "aoeNotifyAll", name = "Notify for all AoE warnings", description = "Configures whether or not AoE Projectile Warnings should trigger a notification", position = 0, section = "Notify")
    default boolean aoeNotifyAll() {
        return false;
    }

    @ConfigItem(position = 2, keyName = "overlayColor", name = "Overlay Color", description = "Configures the color of the AoE Projectile Warnings overlay", section = "Overlay")
    default Color overlayColor() {
        return new Color(0, 150, 200);
    }

    @ConfigItem(keyName = "outline", name = "Display Outline", description = "Configures whether or not AoE Projectile Warnings have an outline", section = "Overlay", position = 3)
    default boolean isOutlineEnabled() {
        return true;
    }

    @ConfigItem(keyName = "delay", name = "Fade Delay", description = "Configures the amount of time in milliseconds that the warning lingers for after the projectile has touched the ground", section = "Overlay", position = 4)
    default int delay() {
        return 300;
    }

    @ConfigItem(keyName = "fade", name = "Fade Warnings", description = "Configures whether or not AoE Projectile Warnings fade over time", section = "Overlay", position = 5)
    default boolean isFadeEnabled() {
        return true;
    }

    @ConfigItem(keyName = "tickTimers", name = "Tick Timers", description = "Configures whether or not AoE Projectile Warnings has tick timers overlaid as well.", section = "Overlay", position = 6)
    default boolean tickTimers() {
        return true;
    }

    @ConfigItem(position = 8, keyName = "fontStyle", name = "Font Style", description = "Bold/Italics/Plain", section = "Text")
    default FontStyle fontStyle() {
        return FontStyle.BOLD;
    }

    @Range(min = 10, max = 40)
    @ConfigItem(position = 9, keyName = "textSize", name = "Text Size", description = "Text Size for Timers.", section = "Text")
    default int textSize() {
        return 32;
    }

    @ConfigItem(position = 10, keyName = "shadows", name = "Shadows", description = "Adds Shadows to text.", section = "Text")
    default boolean shadows() {
        return true;
    }

    @ConfigItem(keyName = "lizardmanaoe", name = "Lizardman Shamans", description = "Configures whether or not AoE Projectile Warnings for Lizardman Shamans is displayed", section = "Lizardman Shamans", position = 13)
    default boolean isShamansEnabled() {
        return true;
    }

    @ConfigItem(keyName = "lizardmanaoenotify", name = "Lizardman Shamans Notify", description = "Configures whether or not AoE Projectile Warnings for Lizardman Shamans should trigger a notification", section = "Lizardman Shamans", position = 14)
    default boolean isShamansNotifyEnabled() {
        return false;
    }

    @ConfigItem(keyName = "archaeologistaoe", name = "Crazy Archaeologist", description = "Configures whether or not AoE Projectile Warnings for Archaeologist is displayed", section = "Crazy Archaeologist", position = 16)
    default boolean isArchaeologistEnabled() {
        return true;
    }

    @ConfigItem(keyName = "archaeologistaoenotify", name = "Crazy Archaeologist Notify", description = "Configures whether or not AoE Projectile Warnings for Crazy Archaeologist should trigger a notification", section = "Crazy Archaeologist", position = 17)
    default boolean isArchaeologistNotifyEnabled() {
        return false;
    }

    @ConfigItem(keyName = "icedemon", name = "Ice Demon", description = "Configures whether or not AoE Projectile Warnings for Ice Demon is displayed", section = "Ice Demon", position = 19)
    default boolean isIceDemonEnabled() {
        return true;
    }

    @ConfigItem(keyName = "icedemonnotify", name = "Ice Demon Notify", description = "Configures whether or not AoE Projectile Warnings for Ice Demon should trigger a notification", section = "Ice Demon", position = 20)
    default boolean isIceDemonNotifyEnabled() {
        return false;
    }

    @ConfigItem(keyName = "vasa", name = "Vasa", description = "Configures whether or not AoE Projectile Warnings for Vasa is displayed", section = "Vasa", position = 22)
    default boolean isVasaEnabled() {
        return true;
    }

    @ConfigItem(keyName = "vasanotify", name = "Vasa Notify", description = "Configures whether or not AoE Projectile Warnings for Vasa should trigger a notification", section = "Vasa", position = 23)
    default boolean isVasaNotifyEnabled() {
        return false;
    }

    @ConfigItem(keyName = "tekton", name = "Tekton", description = "Configures whether or not AoE Projectile Warnings for Tekton is displayed", section = "Tekton", position = 25)
    default boolean isTektonEnabled() {
        return true;
    }

    @ConfigItem(keyName = "tektonnotify", name = "Tekton Notify", description = "Configures whether or not AoE Projectile Warnings for Tekton should trigger a notification", section = "Tekton", position = 26)
    default boolean isTektonNotifyEnabled() {
        return false;
    }

    @ConfigItem(keyName = "vorkathModes", name = "Vorkath", description = "Configure what AoE projectiles you should be warned for at Vorkath", section = "Vorkath", position = 28)
    default Set<VorkathMode> vorkathModes() {
        return (Set<VorkathMode>)ImmutableSet.of(VorkathMode.BOMBS, VorkathMode.FIRES, VorkathMode.POOLS, VorkathMode.SPAWN);
    }

    @ConfigItem(keyName = "vorkathotify", name = "Vorkath Notify", description = "Configures whether or not AoE Projectile Warnings for Vorkath should trigger a notification", section = "Vorkath", position = 29)
    default boolean isVorkathNotifyEnabled() {
        return false;
    }

    @ConfigItem(keyName = "galvek", name = "Galvek", description = "Configures whether or not AoE Projectile Warnings for Galvek are displayed", section = "Galvek", position = 31)
    default boolean isGalvekEnabled() {
        return true;
    }

    @ConfigItem(keyName = "galveknotify", name = "Galvek Notify", description = "Configures whether or not AoE Projectile Warnings for Galvek should trigger a notification", section = "Galvek", position = 32)
    default boolean isGalvekNotifyEnabled() {
        return false;
    }

    @ConfigItem(keyName = "gargboss", name = "Gargoyle Boss", description = "Configs whether or not AoE Projectile Warnings for Dawn/Dusk are displayed", section = "Gargoyle Boss", position = 34)
    default boolean isGargBossEnabled() {
        return true;
    }

    @ConfigItem(keyName = "gargbossnotify", name = "Gargoyle Boss Notify", description = "Configures whether or not AoE Projectile Warnings for Gargoyle Bosses should trigger a notification", section = "Gargoyle Boss", position = 35)
    default boolean isGargBossNotifyEnabled() {
        return false;
    }

    @ConfigItem(keyName = "vetion", name = "Vet'ion", description = "Configures whether or not AoE Projectile Warnings for Vet'ion are displayed", section = "Vet'ion", position = 37)
    default boolean isVetionEnabled() {
        return true;
    }

    @ConfigItem(keyName = "vetionnotify", name = "Vet'ion Notify", description = "Configures whether or not AoE Projectile Warnings for Vet'ion should trigger a notification", section = "Vet'ion", position = 38)
    default boolean isVetionNotifyEnabled() {
        return false;
    }

    @ConfigItem(keyName = "chaosfanatic", name = "Chaos Fanatic", description = "Configures whether or not AoE Projectile Warnings for Chaos Fanatic are displayed", section = "Chaos Fanatic", position = 40)
    default boolean isChaosFanaticEnabled() {
        return true;
    }

    @ConfigItem(keyName = "chaosfanaticnotify", name = "Chaos Fanatic Notify", description = "Configures whether or not AoE Projectile Warnings for Chaos Fanatic should trigger a notification", section = "Chaos Fanatic", position = 41)
    default boolean isChaosFanaticNotifyEnabled() {
        return false;
    }

    @ConfigItem(keyName = "olm", name = "Olm", description = "Configures whether or not AoE Projectile Warnings for The Great Olm are displayed", section = "Olm", position = 43)
    default boolean isOlmEnabled() {
        return true;
    }

    @ConfigItem(keyName = "olmnotify", name = "Olm Notify", description = "Configures whether or not AoE Projectile Warnings for Olm should trigger a notification", section = "Olm", position = 44)
    default boolean isOlmNotifyEnabled() {
        return false;
    }

    @ConfigItem(keyName = "bombDisplay", name = "Olm Bombs", description = "Display a timer and colour-coded AoE for Olm's crystal-phase bombs.", section = "Olm", position = 46)
    default boolean bombDisplay() {
        return true;
    }

    @ConfigItem(keyName = "bombHeatmap", name = "Bomb heatmap", description = "Display a heatmap based on bomb tile severity.", section = "Olm", position = 47)
    default boolean bombHeatmap() {
        return false;
    }

    @Range(max = 100)
    @ConfigItem(keyName = "bombHeatmapOpacity", name = "Bomb opacity", description = "Heatmap color opacity.", section = "Olm", position = 48)
    default int bombHeatmapOpacity() {
        return 50;
    }

    @ConfigItem(keyName = "bombDisplaynotify", name = "Olm Bombs Notify", description = "Configures whether or not AoE Projectile Warnings for Olm Bombs should trigger a notification", section = "Olm", position = 49)
    default boolean bombDisplayNotifyEnabled() {
        return false;
    }

    @ConfigItem(keyName = "lightning", name = "Olm Lightning Trails", description = "Show Lightning Trails", section = "Olm", position = 50)
    default boolean LightningTrail() {
        return true;
    }

    @ConfigItem(keyName = "lightningnotify", name = "Olm Lightning Trails Notify", description = "Configures whether or not AoE Projectile Warnings for Olm Lightning Trails should trigger a notification", section = "Olm", position = 51)
    default boolean LightningTrailNotifyEnabled() {
        return false;
    }

    @ConfigItem(keyName = "corp", name = "Corporeal Beast", description = "Configures whether or not AoE Projectile Warnings for the Corporeal Beast are displayed", section = "Corporeal Beast", position = 53)
    default boolean isCorpEnabled() {
        return true;
    }

    @ConfigItem(keyName = "corpnotify", name = "Corporeal Beast Notify", description = "Configures whether or not AoE Projectile Warnings for Corporeal Beast should trigger a notification", section = "Corporeal Beast", position = 53)
    default boolean isCorpNotifyEnabled() {
        return false;
    }

    @ConfigItem(keyName = "wintertodt", name = "Wintertodt Snow Fall", description = "Configures whether or not AOE Projectile Warnings for the Wintertodt snow fall are displayed", section = "Wintertodt", position = 55)
    default boolean isWintertodtEnabled() {
        return true;
    }

    @ConfigItem(keyName = "wintertodtnotify", name = "Wintertodt Snow Fall Notify", description = "Configures whether or not AoE Projectile Warnings for Wintertodt Snow Fall Notify should trigger a notification", section = "Wintertodt", position = 56)
    default boolean isWintertodtNotifyEnabled() {
        return false;
    }

    @ConfigItem(keyName = "isXarpusEnabled", name = "Xarpus", description = "Configures whether or not AOE Projectile Warnings for Xarpus are displayed", section = "Xarpus", position = 58)
    default boolean isXarpusEnabled() {
        return true;
    }

    @ConfigItem(keyName = "isXarpusEnablednotify", name = "Xarpus Notify", description = "Configures whether or not AoE Projectile Warnings for Xarpus should trigger a notification", section = "Xarpus", position = 59)
    default boolean isXarpusNotifyEnabled() {
        return false;
    }

    @ConfigItem(keyName = "addyDrags", name = "Addy Drags", description = "Show Bad Areas", section = "Addy Drags", position = 61)
    default boolean addyDrags() {
        return true;
    }

    @ConfigItem(keyName = "addyDragsnotify", name = "Addy Drags Notify", description = "Configures whether or not AoE Projectile Warnings for Addy Dragons should trigger a notification", section = "Addy Drags", position = 62)
    default boolean addyDragsNotifyEnabled() {
        return false;
    }

    @ConfigItem(keyName = "drake", name = "Drakes Breath", description = "Configures if Drakes Breath tile markers are displayed", section = "Drakes", position = 64)
    default boolean isDrakeEnabled() {
        return true;
    }

    @ConfigItem(keyName = "drakenotify", name = "Drakes Breath Notify", description = "Configures whether or not AoE Projectile Warnings for Drakes Breath should trigger a notification", section = "Drakes", position = 65)
    default boolean isDrakeNotifyEnabled() {
        return false;
    }

    @ConfigItem(keyName = "cerbFire", name = "Cerberus Fire", description = "Configures if Cerberus fire tile markers are displayed", section = "Cerberus", position = 67)
    default boolean isCerbFireEnabled() {
        return true;
    }

    @ConfigItem(keyName = "cerbFirenotify", name = "Cerberus Fire Notify", description = "Configures whether or not AoE Projectile Warnings for Cerberus his fire should trigger a notification", section = "Cerberus", position = 68)
    default boolean isCerbFireNotifyEnabled() {
        return false;
    }

    @ConfigItem(keyName = "demonicGorilla", name = "Demonic Gorilla", description = "Configures if Demonic Gorilla boulder tile markers are displayed", section = "Demonic Gorilla", position = 70)
    default boolean isDemonicGorillaEnabled() {
        return true;
    }

    @ConfigItem(keyName = "demonicGorillaNotify", name = "Demonic Gorilla Notify", description = "Configures whether or not AoE Projectile Warnings for Demonic Gorilla boulders should trigger a notification", section = "Demonic Gorilla", position = 71)
    default boolean isDemonicGorillaNotifyEnabled() {
        return false;
    }

    @ConfigItem(keyName = "verzik", name = "Verzik", description = "Configures if Verzik purple Nylo/falling rock AOE is shown", section = "Verzik", position = 73)
    default boolean isVerzikEnabled() {
        return true;
    }

    @ConfigItem(keyName = "verzikNotify", name = "Verzik Notify", description = "Configures whether or not AoE Projectile Warnings for Verzik boulders/purple nylo should trigger a notification", section = "Verzik", position = 74)
    default boolean isVerzikNotifyEnabled() {
        return false;
    }
}
