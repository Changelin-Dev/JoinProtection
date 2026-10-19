package me.rockquiet.joinprotection;

import me.rockquiet.joinprotection.configuration.Config;
import me.rockquiet.joinprotection.protection.ProtectionInfo;
import me.rockquiet.joinprotection.protection.ProtectionType;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class ProtectionHandler implements Listener {

    private final Map<UUID, ProtectionInfo> invinciblePlayers = new ConcurrentHashMap<>();
    private final Map<UUID, BossBar> bossBars = new ConcurrentHashMap<>();

    private final JoinProtection plugin;
    private final MessageManager messageManager;

    private double[][] particleCoordinates;

    public ProtectionHandler(JoinProtection joinProtection,
                             MessageManager messageManager) {
        this.plugin = joinProtection;
        this.messageManager = messageManager;

        calculateParticleCoordinates();
    }

    private int getBonusTime(Player player) {
        return player.getEffectivePermissions().stream()
                .filter(permissionAttachmentInfo -> permissionAttachmentInfo.getPermission().startsWith("joinprotection.plus-"))
                .map(permissionAttachmentInfo -> {
                    String permission = permissionAttachmentInfo.getPermission();
                    String[] segments = permission.split("-");
                    return Integer.parseInt(segments[segments.length - 1]);
                }).max(Integer::compareTo)
                .orElse(0);
    }

    public void startJoinProtection(Player player) {
        startProtection(player, plugin.config().plugin.protectionTime, ProtectionType.JOIN);
    }

    public void startWorldProtection(Player player) {
        startProtection(player, plugin.config().plugin.worldChangeProtectionTime, ProtectionType.WORLD);
    }

    public void startCommandProtection(Player player, int protectionTime) {
        startProtection(player, protectionTime, ProtectionType.COMMAND);
    }

    public void startProtection(Player player, int protectionTime, ProtectionType type) {
        if (protectionTime <= 0) return;
        final int finalProtectionTime = protectionTime + getBonusTime(player);

        final UUID uuid = player.getUniqueId();
        if (hasProtection(uuid)) return;

        invinciblePlayers.put(uuid, new ProtectionInfo(player.getLocation(), type));

        final Config config = plugin.config();
        final int totalTime = finalProtectionTime;

        applyInvisibility(player, totalTime);
        sendStartMessage(player, type, totalTime);
        final BossBar bossBar = createBossBar(player, type, totalTime);

        final AtomicInteger timeRemaining = new AtomicInteger(totalTime);
        plugin.getScheduler().runTimerOnEntity(player, task -> {
            if (!hasProtection(uuid)) {
                task.cancel();
                return;
            }

            final int current = timeRemaining.getAndDecrement();

            if (current > 0) {
                updateBossBar(bossBar, type, current, totalTime);

                // the bossbar replaces the action bar countdown
                if (bossBar == null) {
                    messageManager.sendProtectionInfo(player, config.messages.timeRemaining,
                            type.getPlaceholder(config),
                            Placeholder.unparsed("time", formatTime(current)),
                            Placeholder.unparsed("seconds", String.valueOf(current))
                    );
                }
            }

            if (current <= 0) {
                removeProtection(player);
                messageManager.sendProtectionInfo(player, config.messages.protectionEnded, type.getPlaceholder(config));
                task.cancel();
            }
        }, 0, 20);

        spawnParticles(player);
    }

    // ---------------------------------------------------------------- bossbar / invisibility / messages

    private static String formatTime(int totalSeconds) {
        final int hours = totalSeconds / 3600;
        final int minutes = (totalSeconds % 3600) / 60;
        final int seconds = totalSeconds % 60;

        if (hours > 0) return String.format("%dh %02dm %02ds", hours, minutes, seconds);
        if (minutes > 0) return String.format("%dm %02ds", minutes, seconds);
        return seconds + "s";
    }

    private TagResolver[] timeResolvers(ProtectionType type, int remaining) {
        return new TagResolver[]{
                type.getPlaceholder(plugin.config()),
                Placeholder.unparsed("time", formatTime(remaining)),
                Placeholder.unparsed("seconds", String.valueOf(remaining))
        };
    }

    private void sendStartMessage(Player player, ProtectionType type, int totalTime) {
        final String message = plugin.config().messages.protectionStart;
        if (message == null || message.isBlank()) return;

        messageManager.sendMessage(player, message, timeResolvers(type, totalTime));
    }

    private BossBar createBossBar(Player player, ProtectionType type, int totalTime) {
        final Config.BossBarSection section = plugin.config().bossBar;
        if (!section.enabled) return null;

        BossBar.Color color;
        try {
            color = BossBar.Color.valueOf(section.color.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            color = BossBar.Color.GREEN;
        }

        BossBar.Overlay overlay;
        try {
            overlay = BossBar.Overlay.valueOf(section.overlay.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            overlay = BossBar.Overlay.PROGRESS;
        }

        final BossBar bossBar = BossBar.bossBar(
                messageManager.parse(section.title, timeResolvers(type, totalTime)),
                1.0f,
                color,
                overlay
        );
        bossBars.put(player.getUniqueId(), bossBar);
        player.showBossBar(bossBar);
        return bossBar;
    }

    private void updateBossBar(BossBar bossBar, ProtectionType type, int remaining, int totalTime) {
        if (bossBar == null) return;

        bossBar.name(messageManager.parse(plugin.config().bossBar.title, timeResolvers(type, remaining)));
        bossBar.progress(Math.max(0.0f, Math.min(1.0f, remaining / (float) totalTime)));
    }

    private void applyInvisibility(Player player, int totalTime) {
        if (!plugin.config().invisibility.enabled) return;

        // ambient=false, particles=false, icon=false -> fully discreet effect
        player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, (totalTime + 2) * 20, 0, false, false, false));
    }

    /**
     * Single exit point: forgets the protection, hides the bossbar and removes invisibility.
     */
    private void removeProtection(Player player) {
        final UUID uuid = player.getUniqueId();
        invinciblePlayers.remove(uuid);

        final BossBar bossBar = bossBars.remove(uuid);
        if (bossBar != null) {
            player.hideBossBar(bossBar);
        }

        if (plugin.config().invisibility.enabled) {
            player.removePotionEffect(PotionEffectType.INVISIBILITY);
        }
    }

    /**
     * Called on plugin disable: nobody should stay invisible / keep a bossbar.
     */
    public void shutdown() {
        for (UUID uuid : List.copyOf(invinciblePlayers.keySet())) {
            final Player player = plugin.getServer().getPlayer(uuid);
            if (player != null) {
                removeProtection(player);
            }
        }
        invinciblePlayers.clear();
        bossBars.clear();
    }

    public final void calculateParticleCoordinates() {
        final Config config = plugin.config();
        final double[] scale = config.particles.scaleFactor;
        final int circles = config.particles.circles <= 0 ? 4 : config.particles.circles;
        final double increment = Math.PI / circles;

        int arrayLocation = 0;
        particleCoordinates = new double[(circles + 1) * circles * 2][3];

        for (double i = 0; i <= Math.PI; i += increment) {
            double radius = Math.sin(i);
            double y = Math.cos(i);
            for (double a = 0; a < Math.PI * 2; a += increment) {
                double x = Math.cos(a) * radius;
                double z = Math.sin(a) * radius;
                particleCoordinates[arrayLocation++] = new double[]{x * scale[0], y * scale[1], z * scale[2]};

                if (arrayLocation >= particleCoordinates.length) break;
            }
        }
    }

    private void spawnParticles(Player player) {
        final Config config = plugin.config();
        if (!config.particles.enabled) return;

        if (plugin.isPaper() && plugin.getServer().getAverageTickTime() >= config.particles.maximumMspt) {
            return;
        }

        final UUID playerUUID = player.getUniqueId();
        final Particle particle = config.particles.toParticle();
        final int particleAmount = config.particles.amount;

        plugin.getScheduler().runTimerOnEntity(player, task -> {
            if (hasProtection(playerUUID)) {
                final Location location = player.getLocation().add(0, player.getHeight() / 2, 0);

                for (double[] coord : particleCoordinates) {
                    final Location particleLocation = location.clone().add(coord[0], coord[1], coord[2]);
                    location.getWorld().spawnParticle(particle, particleLocation, particleAmount);
                }
            } else {
                task.cancel();
            }
        }, 0, config.particles.refreshRate);
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean isEnabledInWorld(World world) {
        final Config config = plugin.config();
        final Set<String> worldList = config.plugin.worldList;
        final String worldName = world.getName();

        return switch (config.plugin.listType.toLowerCase(Locale.ROOT)) {
            case "whitelist" -> worldList.contains(worldName);
            case "blacklist" -> !worldList.contains(worldName);
            default -> true;
        };
    }

    public boolean hasProtection(UUID playerUUID) {
        return invinciblePlayers.containsKey(playerUUID);
    }

    public ProtectionInfo getProtectionInfo(UUID playerUUID) {
        if (hasProtection(playerUUID)) {
            return invinciblePlayers.get(playerUUID);
        }
        return null;
    }

    public void clearMobTargets(Player player) {
        final Config config = plugin.config();
        final double horizontalRange = config.modules.disableEntityTargeting.horizontalRange;
        final double verticalRange = config.modules.disableEntityTargeting.verticalRange;

        player.getNearbyEntities(horizontalRange, verticalRange, horizontalRange).forEach(entity -> {
            if (entity instanceof Mob mob && Objects.equals(mob.getTarget(), player)) {
                mob.setTarget(null);
            }
        });
    }

    public void cancelProtectionIfEnabled(Player player, String permission, boolean cancelled, String messageOnCancel) {
        if (hasProtection(player.getUniqueId()) && !player.hasPermission(permission) && cancelled) {
            cancelProtection(player, messageOnCancel);
        }
    }

    public void cancelProtection(Player player, String messageOnCancel) {
        messageManager.sendProtectionInfo(player, messageOnCancel, getProtectionInfo(player.getUniqueId()).type().getPlaceholder(plugin.config()));

        removeProtection(player);
    }

    public boolean isEventCancelled(UUID playerUUID, boolean cancelled) {
        return cancelled && hasProtection(playerUUID);
    }

    // clear map
    @EventHandler
    public void onLeave(PlayerQuitEvent event) {
        removeProtection(event.getPlayer());
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        removeProtection(event.getEntity());
    }
}
