package dev.zain.trimeffect;

import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ArmorMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.*;

public class TrimEffectPlugin extends JavaPlugin implements Listener {
    private Map<String, List<PotionEffect>> trimEffects = new HashMap<>();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadTrimEffects();
        getServer().getPluginManager().registerEvents(this, this);
    }

    private void loadTrimEffects() {
        trimEffects.clear();
        FileConfiguration config = getConfig();
        if (!config.isConfigurationSection("trims")) return;
        for (String trim : config.getConfigurationSection("trims").getKeys(false)) {
            List<Map<?, ?>> effectList = config.getMapList("trims." + trim + ".effects");
            List<PotionEffect> potionEffects = new ArrayList<>();
            for (Map<?, ?> rawMap : effectList) {
                Map<String, Object> effectData = new HashMap<>();
                for (Map.Entry<?, ?> entry : rawMap.entrySet()) {
                    effectData.put(entry.getKey().toString(), entry.getValue());
                }
                Object typeObj = effectData.get("effect");
                if (typeObj == null) continue;
                PotionEffectType type = PotionEffectType.getByName(typeObj.toString().toUpperCase());
                if (type == null) continue;
                Object ampObj = effectData.getOrDefault("amplifier", 0);
                int amplifier = ampObj instanceof Number ? ((Number) ampObj).intValue() : Integer.parseInt(ampObj.toString());
                Object ambientObj = effectData.getOrDefault("ambient", false);
                boolean ambient = ambientObj instanceof Boolean ? (Boolean) ambientObj : Boolean.parseBoolean(ambientObj.toString());
                Object particlesObj = effectData.getOrDefault("particles", true);
                boolean particles = particlesObj instanceof Boolean ? (Boolean) particlesObj : Boolean.parseBoolean(particlesObj.toString());
                potionEffects.add(new PotionEffect(type, Integer.MAX_VALUE, amplifier, ambient, particles));
            }
            trimEffects.put(trim.toLowerCase(), potionEffects);
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        applyTrimEffects(event.getPlayer());
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        applyTrimEffects(event.getPlayer());
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getWhoClicked() instanceof Player player) {
            getServer().getScheduler().runTaskLater(this, () -> applyTrimEffects(player), 1L);
        }
    }

    private void applyTrimEffects(Player player) {
        for (PotionEffect effect : player.getActivePotionEffects()) {
            player.removePotionEffect(effect.getType());
        }
        Map<PotionEffectType, PotionEffect> toApply = new HashMap<>();
        for (ItemStack item : getArmor(player)) {
            if (item == null || item.getType() == Material.AIR) continue;
            if (!(item.getItemMeta() instanceof ArmorMeta meta)) continue;
            if (!meta.hasTrim()) continue;
            String trimName = meta.getTrim().getPattern().getKey().getKey().toLowerCase();
            List<PotionEffect> effects = trimEffects.get(trimName);
            if (effects != null) {
                for (PotionEffect effect : effects) {
                    if (!toApply.containsKey(effect.getType())) {
                        toApply.put(effect.getType(), new PotionEffect(effect.getType(), Integer.MAX_VALUE, effect.getAmplifier(), effect.isAmbient(), effect.hasParticles()));
                    }
                }
            }
        }
        for (PotionEffect effect : toApply.values()) {
            player.addPotionEffect(effect);
        }
    }

    private List<ItemStack> getArmor(Player player) {
        return Arrays.asList(
                player.getInventory().getHelmet(),
                player.getInventory().getChestplate(),
                player.getInventory().getLeggings(),
                player.getInventory().getBoots()
        );
    }
}
