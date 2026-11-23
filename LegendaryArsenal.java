package com.ronak.legendaryarsenal;

import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.block.Block;
import org.bukkit.command.*;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.*;
import org.bukkit.event.*;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.*;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class LegendaryArsenal extends JavaPlugin implements Listener, CommandExecutor {
    private NamespacedKey itemKey;
    private NamespacedKey arrowKey;
    private final Map<String, Long> cooldowns = new ConcurrentHashMap<>();
    private final Map<UUID, Double> savedSpeeds = new HashMap<>();
    private final Set<UUID> flightActive = new HashSet<>();
    private final Set<UUID> flightAttempted = new HashSet<>();
    private final Map<UUID, Long> lastLeftClick = new HashMap<>();

    @Override public void onEnable() {
        saveDefaultConfig();
        itemKey = new NamespacedKey(this, "legendary_item");
        arrowKey = new NamespacedKey(this, "benro_arrow");
        getServer().getPluginManager().registerEvents(this, this);
        Objects.requireNonNull(getCommand("weapons")).setExecutor(this);
        Bukkit.getScheduler().runTaskTimer(this, this::tickPlayers, 1L, 10L);
        getLogger().info("LegendaryArsenal enabled.");
    }

    @Override public void onDisable() {
        for (Player p : Bukkit.getOnlinePlayers()) restoreSpeed(p);
        flightActive.clear();
    }

    private ItemStack make(String id, Material material, String name, String lore, int model) {
        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        meta.setDisplayName(ChatColor.LIGHT_PURPLE + "✦ " + ChatColor.BOLD + name);
        meta.setLore(List.of(ChatColor.GRAY + lore, ChatColor.DARK_GRAY + "Legendary Arsenal"));
        meta.setCustomModelData(model);
        meta.getPersistentDataContainer().set(itemKey, PersistentDataType.STRING, id);
        stack.setItemMeta(meta);
        return stack;
    }

    private String id(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return "";
        return item.getItemMeta().getPersistentDataContainer().getOrDefault(itemKey, PersistentDataType.STRING, "");
    }
    private boolean hasFullSekue(Player p) {
        return id(p.getInventory().getHelmet()).equals("sekue_helmet")
            && id(p.getInventory().getChestplate()).equals("sekue_chestplate")
            && id(p.getInventory().getLeggings()).equals("sekue_leggings")
            && id(p.getInventory().getBoots()).equals("sekue_boots");
    }
    private boolean ready(Player p, String key, int seconds) {
        String mapKey = p.getUniqueId() + ":" + key;
        long now = System.currentTimeMillis();
        long until = cooldowns.getOrDefault(mapKey, 0L);
        if (until > now) {
            p.sendMessage(ChatColor.RED + "Cooldown: " + ((until - now + 999) / 1000) + "s");
            return false;
        }
        cooldowns.put(mapKey, now + seconds * 1000L);
        return true;
    }
    private void msg(Player p, String s) { p.sendMessage(ChatColor.LIGHT_PURPLE + "[Arsenal] " + ChatColor.WHITE + s); }

    @Override public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length < 3 || !args[0].equalsIgnoreCase("give")) {
            sender.sendMessage(ChatColor.YELLOW + "Use: /weapons give <player> <item>"); return true;
        }
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) { sender.sendMessage(ChatColor.RED + "Player not online."); return true; }
        String item = args[2].toLowerCase(Locale.ROOT);
        List<ItemStack> items = switch (item) {
            case "nightspear" -> List.of(make("nightspear", Material.TRIDENT, "Nightspear", "Sneak: flight | Left-click: charged strike | Right-click: Lunge X", 10001));
            case "hazremace" -> List.of(make("hazre_mace", Material.MACE, "Hazre Mace", "Shift + right-click: teleport | Sneak + left-click: 50-block Blast Mode", 10002));
            case "uryusword" -> List.of(make("uryu_sword", Material.NETHERITE_SWORD, "Uryu Sword", "+70% damage | Freeze targets | Walk on water and fire", 10003));
            case "benrobow" -> List.of(make("benro_bow", Material.BOW, "Benro Bow", "+50% arrow damage | Lightning strikes | 2x movement speed", 10004));
            case "sekuehelmet" -> List.of(make("sekue_helmet", Material.NETHERITE_HELMET, "Sekue Helmet", "Part of the Sekue Full Armour", 10005));
            case "sekuechestplate" -> List.of(make("sekue_chestplate", Material.NETHERITE_CHESTPLATE, "Sekue Chestplate", "Part of the Sekue Full Armour", 10006));
            case "sekueleggings" -> List.of(make("sekue_leggings", Material.NETHERITE_LEGGINGS, "Sekue Leggings", "Part of the Sekue Full Armour", 10007));
            case "sekueboots" -> List.of(make("sekue_boots", Material.NETHERITE_BOOTS, "Sekue Boots", "Part of the Sekue Full Armour", 10008));
            case "sekuefull" -> List.of(make("sekue_helmet", Material.NETHERITE_HELMET, "Sekue Helmet", "Part of the Sekue Full Armour", 10005), make("sekue_chestplate", Material.NETHERITE_CHESTPLATE, "Sekue Chestplate", "Part of the Sekue Full Armour", 10006), make("sekue_leggings", Material.NETHERITE_LEGGINGS, "Sekue Leggings", "Part of the Sekue Full Armour", 10007), make("sekue_boots", Material.NETHERITE_BOOTS, "Sekue Boots", "Part of the Sekue Full Armour", 10008));
            default -> List.of();
        };
        if (items.isEmpty()) { sender.sendMessage(ChatColor.RED + "Unknown item."); return true; }
        for (ItemStack stack : items) target.getInventory().addItem(stack);
        sender.sendMessage(ChatColor.GREEN + "Gave " + item + " to " + target.getName());
        return true;
    }

    @EventHandler public void onInteract(PlayerInteractEvent e) {
        Player p = e.getPlayer(); String id = id(e.getItem());
        Action a = e.getAction();
        if (id.equals("benro_bow") && (a == Action.RIGHT_CLICK_AIR || a == Action.RIGHT_CLICK_BLOCK)) {
            e.setCancelled(true);
            long now = System.currentTimeMillis();
            long next = cooldowns.getOrDefault(p.getUniqueId() + ":benro_quickshot", 0L);
            if (next > now) return;
            cooldowns.put(p.getUniqueId() + ":benro_quickshot", now + 350L);
            boolean creative = p.getGameMode() == GameMode.CREATIVE;
            boolean infinity = e.getItem() != null && e.getItem().containsEnchantment(Enchantment.INFINITY);
            if (!creative && !infinity) {
                int slot = -1;
                for (int i = 0; i < p.getInventory().getStorageContents().length; i++) {
                    ItemStack stack = p.getInventory().getItem(i);
                    if (stack != null && stack.getType() == Material.ARROW) { slot = i; break; }
                }
                if (slot < 0) { msg(p, "You need arrows to fire the Benro Bow."); return; }
                ItemStack arrows = p.getInventory().getItem(slot);
                if (arrows.getAmount() <= 1) p.getInventory().setItem(slot, null); else arrows.setAmount(arrows.getAmount() - 1);
            }
            Arrow quick = p.getWorld().spawnArrow(p.getEyeLocation(), p.getEyeLocation().getDirection(), 3.2f, 0.0f);
            quick.setShooter(p); quick.setDamage(6.0);
            quick.getPersistentDataContainer().set(arrowKey, PersistentDataType.BYTE, (byte) 1);
            p.getWorld().playSound(p.getLocation(), Sound.ENTITY_ARROW_SHOOT, 1f, 1.4f);
        } else if (id.equals("nightspear")) {
            if (a == Action.RIGHT_CLICK_AIR || a == Action.RIGHT_CLICK_BLOCK) {
                e.setCancelled(true);
                Vector dir = p.getLocation().getDirection().normalize();
                p.setVelocity(dir.multiply(2.4).setY(Math.max(0.45, dir.getY() * 1.4)));
                p.getWorld().playSound(p.getLocation(), Sound.ITEM_TRIDENT_RIPTIDE_3, 1f, 1.2f);
                msg(p, "Nightspear Lunge X!");
            }
        } else if (id.equals("hazre_mace") && (a == Action.RIGHT_CLICK_AIR || a == Action.RIGHT_CLICK_BLOCK) && p.isSneaking()) {
            e.setCancelled(true);
            if (!ready(p, "hazre_tp", getConfig().getInt("hazre-teleport-cooldown", 8))) return;
            RayTraceResult hit = p.getWorld().rayTraceBlocks(p.getEyeLocation(), p.getEyeLocation().getDirection(), 16);
            if (hit != null && hit.getHitPosition() != null) {
                Location dest = hit.getHitPosition().toLocation(p.getWorld()).add(0, 1, 0);
                if (dest.getBlock().isPassable() && dest.clone().add(0, 1, 0).getBlock().isPassable()) {
                    p.teleport(dest); p.getWorld().spawnParticle(Particle.PORTAL, dest, 40, .5, .8, .5, .1);
                    p.getWorld().playSound(dest, Sound.ENTITY_ENDERMAN_TELEPORT, 1, 1);
                }
            }
        } else if (id.equals("uryu_sword")) {
            // Passive effects are applied in tickPlayers.
        }
    }

    @EventHandler public void onSwing(PlayerAnimationEvent e) {
        Player p = e.getPlayer(); String id = id(p.getInventory().getItemInMainHand());
        if (id.equals("nightspear")) {
            LivingEntity target = rayTarget(p, 5.0);
            if (target != null) { target.damage(9.0, p); target.getWorld().spawnParticle(Particle.CRIT, target.getLocation().add(0, 1, 0), 18, .3, .4, .3, .2); msg(p, "Charged Nightspear strike!"); }
        } else if (id.equals("hazre_mace") && p.isSneaking()) {
            if (!ready(p, "hazre_blast", getConfig().getInt("hazre-blast-cooldown", 60))) return;
            double radius = getConfig().getDouble("hazre-blast-radius", 50);
            p.getWorld().spawnParticle(Particle.EXPLOSION, p.getLocation(), 8, 2, 1, 2, 0);
            p.getWorld().playSound(p.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 3f, .7f);
            for (Entity entity : p.getNearbyEntities(radius, radius, radius)) {
                if (entity instanceof LivingEntity living && entity != p && living.getLocation().distanceSquared(p.getLocation()) <= radius * radius) {
                    living.damage(18.0, p);
                    Vector knock = living.getLocation().toVector().subtract(p.getLocation().toVector()).normalize().multiply(1.6).setY(.7);
                    living.setVelocity(knock);
                }
            }
            msg(p, "HAZRE BLAST MODE! Radius: " + (int) radius + " blocks.");
        }
    }

    private LivingEntity rayTarget(Player p, double range) {
        RayTraceResult result = p.getWorld().rayTraceEntities(p.getEyeLocation(), p.getEyeLocation().getDirection(), range, .8,
            entity -> entity instanceof LivingEntity && entity != p);
        return result != null && result.getHitEntity() instanceof LivingEntity living ? living : null;
    }

    @EventHandler public void onShoot(EntityShootBowEvent e) {
        if (e.getEntity() instanceof Player p && id(e.getBow()).equals("benro_bow") && e.getProjectile() instanceof Arrow arrow) {
            arrow.getPersistentDataContainer().set(arrowKey, PersistentDataType.BYTE, (byte) 1);
            arrow.setVelocity(arrow.getVelocity().multiply(1.7));
        }
    }

    @EventHandler public void onDamage(EntityDamageByEntityEvent e) {
        if (e.getDamager() instanceof Player p) {
            String held = id(p.getInventory().getItemInMainHand());
            if (held.equals("uryu_sword")) {
                e.setDamage(e.getDamage() * 1.70);
                if (e.getEntity() instanceof LivingEntity victim) {
                    victim.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 100, 10, false, true, true));
                    victim.setFreezeTicks(Math.max(victim.getFreezeTicks(), 100));
                }
            } else if (held.equals("hazre_mace") && p.getFallDistance() > 2.5f) e.setDamage(e.getDamage() * 1.50);
        } else if (e.getDamager() instanceof Arrow arrow) {
            if (arrow.getPersistentDataContainer().has(arrowKey, PersistentDataType.BYTE)) e.setDamage(e.getDamage() * 1.50);
        }
        if (e.getEntity() instanceof Player victim && hasFullSekue(victim)) e.setDamage(e.getDamage() * .50);
    }

    @EventHandler public void onProjectileHit(ProjectileHitEvent e) {
        if (e.getEntity() instanceof Arrow arrow && arrow.getPersistentDataContainer().has(arrowKey, PersistentDataType.BYTE)
            && e.getHitEntity() instanceof LivingEntity target) {
            target.getWorld().strikeLightningEffect(target.getLocation());
            target.getWorld().playSound(target.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, .8f, 1.2f);
        }
    }

    @EventHandler public void onFall(EntityDamageEvent e) {
        if (e.getEntity() instanceof Player p && hasFullSekue(p) && e.getCause() == EntityDamageEvent.DamageCause.FALL) e.setCancelled(true);
    }

    @EventHandler public void onQuit(PlayerQuitEvent e) { restoreSpeed(e.getPlayer()); flightActive.remove(e.getPlayer().getUniqueId()); }

    private void tickPlayers() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            String held = id(p.getInventory().getItemInMainHand());
            if (held.equals("benro_bow")) {
                AttributeInstance attr = p.getAttribute(Attribute.MOVEMENT_SPEED);
                if (attr != null && !savedSpeeds.containsKey(p.getUniqueId())) {
                    savedSpeeds.put(p.getUniqueId(), attr.getBaseValue()); attr.setBaseValue(attr.getBaseValue() * 2.0);
                }
            } else restoreSpeed(p);

            if (held.equals("uryu_sword")) {
                p.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 30, 0, true, false, false));
                Block below = p.getLocation().subtract(0, 1, 0).getBlock();
                if (below.getType() == Material.WATER || below.getType() == Material.BUBBLE_COLUMN) {
                    Location surface = below.getLocation();
                    if (surface.getBlock().getType() == Material.WATER) surface.getBlock().setType(Material.FROSTED_ICE);
                    Bukkit.getScheduler().runTaskLater(this, () -> { if (surface.getBlock().getType() == Material.FROSTED_ICE) surface.getBlock().setType(Material.WATER); }, 40L);
                }
                if (below.getType() == Material.FIRE || below.getType() == Material.SOUL_FIRE || below.getType() == Material.LAVA) {
                    p.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 40, 0, true, false, false));
                    Location path = below.getLocation();
                    Material old = below.getType();
                    path.getBlock().setType(Material.GLASS);
                    Bukkit.getScheduler().runTaskLater(this, () -> { if (path.getBlock().getType() == Material.GLASS) path.getBlock().setType(old); }, 40L);
                }
            }
            if (hasFullSekue(p)) {
                p.addPotionEffect(new PotionEffect(PotionEffectType.HASTE, 30, 1, true, false, false));
                p.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 30, 0, true, false, false));
                p.addPotionEffect(new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 30, 0, true, false, false));
                p.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 30, 1, true, false, false));
            }
            if (held.equals("nightspear") && p.isSneaking() && !flightActive.contains(p.getUniqueId()) && !flightAttempted.contains(p.getUniqueId())) {
                flightAttempted.add(p.getUniqueId());
                if (ready(p, "nightspear_flight", getConfig().getInt("nightspear-flight-cooldown", 30))) {
                    flightActive.add(p.getUniqueId()); p.setAllowFlight(true); p.setFlying(true); msg(p, "Nightspear flight activated!");
                    int duration = getConfig().getInt("nightspear-flight-duration", 8);
                    Bukkit.getScheduler().runTaskLater(this, () -> {
                        if (p.isOnline()) { p.setFlying(false); p.setAllowFlight(false); msg(p, "Flight ended."); }
                        flightActive.remove(p.getUniqueId());
                    }, duration * 20L);
                }
            }
            if (!p.isSneaking() || !held.equals("nightspear")) flightAttempted.remove(p.getUniqueId());
        }
    }
    private void restoreSpeed(Player p) {
        Double old = savedSpeeds.remove(p.getUniqueId());
        if (old != null) { AttributeInstance attr = p.getAttribute(Attribute.MOVEMENT_SPEED); if (attr != null) attr.setBaseValue(old); }
    }
}
