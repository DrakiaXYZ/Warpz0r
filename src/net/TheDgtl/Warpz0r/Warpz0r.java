package net.TheDgtl.Warpz0r;

import java.io.File;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;
import java.util.logging.Logger;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.util.config.Configuration;

import com.nijikokun.bukkit.Permissions.Permissions;
import com.nijikokun.bukkit.iConomy.Misc;
import com.nijikokun.bukkit.iConomy.iConomy;

/**
 * Warpz0r for Bukkit
 *
 * @author Drakia
 */
public class Warpz0r extends JavaPlugin {
    
	private Permissions permissions = null;
	private double permVersion = 0;
    public static Logger log;
    public static Server server;
    private Configuration config;
    private HashMap<String, World> worldList = new HashMap<String, World>();
    private String warpFile;
    private String homeFile;
    private World defWorld;
    
    private boolean useiConomy;
    private int warpCost;
    private int homeCost;
    private int setHomeCost;
    
    public void onEnable() {
        log = Logger.getLogger("Minecraft");
        PluginManager pm = getServer().getPluginManager();
        config = getConfiguration();
        log.info( getDescription().getName() + " version " + getDescription().getVersion() + " is enabled" );
        
        // Create data folder if it doesn't exist.
        if (!getDataFolder().exists()) getDataFolder().mkdirs();
        warpFile = getDataFolder().getPath() + File.separator + "warps.db";
        homeFile = getDataFolder().getPath() + File.separator + "homes.db";
        server = getServer();
        loadConfig();
        
        // Load a list of worlds for warping between worlds.
        for(World w : getServer().getWorlds()) {
            worldList.put(w.getName(), w);
        }
        defWorld = getServer().getWorlds().get(0);
        
        if (setupPermissions()) {
        	if (permissions != null)
        		log.info("[Tombstone] Loaded Permissions v" + permVersion + " for permissions");
        } else {
        	log.info("[Tombstone] No permissions plugin found, using default permission settings");
        }
        
        // Setup iConomy
        if (useiConomy) {
            Plugin icon = pm.getPlugin("iConomy");
            if (icon == null) {
                useiConomy = false;
            } else {
                Warpz0r.log.info("[Warpz0r] Using iConomy. Warp Cost: " + warpCost + " Home Cost: " + homeCost);
            }
        }
        
        // Check if a previous warps.txt exists, import if it does.
        File oldFile = new File(warpFile.substring(0, warpFile.length() - 2) + "txt");
        File newFile = new File(warpFile);
        if (!newFile.exists() && oldFile.exists()) {
            Warpz0r.log.info("[Warpz0r] Migrating existing warps.txt file.");
            Locations.migrateWarps(oldFile.getAbsolutePath(), warpFile, defWorld);
        }
        
        // Check if a previous homes.txt exists, import if it does.
        oldFile = new File(homeFile.substring(0, homeFile.length() - 2) + "txt");
        newFile = new File(homeFile);
        if (!newFile.exists() && oldFile.exists()) {
            Warpz0r.log.info("[Warpz0r] Migrating existing homes.txt file.");
            Locations.migrateWarps(oldFile.getAbsolutePath(), homeFile, defWorld);
        }
        
        // Load warp and home location data
        Locations.loadList(warpFile, Locations.warps, worldList, defWorld);
        Locations.loadList(homeFile, Locations.homes, worldList, defWorld);
    }
    
    public void onDisable() {
        Locations.clear();
        worldList.clear();
        log.info( getDescription().getName() + " version " + getDescription().getVersion() + " is disabled" );
    }
    
    public void loadConfig() {
        config.load();
        useiConomy = config.getBoolean("useiconomy", false);
        warpCost = config.getInt("warpcost", 5);
        homeCost = config.getInt("homecost", 5);
        setHomeCost = config.getInt("sethomecost", 0);
        saveConfig();
    }
    
    public void saveConfig() {
        config.setProperty("useiconomy", useiConomy);
        config.setProperty("warpcost", warpCost);
        config.setProperty("homecost", homeCost);
        config.setProperty("sethomecost", setHomeCost);
        config.save();
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String commandLabel, String[] args) {
        if (!(sender instanceof Player)) return false;
        
        Player player = (Player)sender;
        String comName = command.getName().toLowerCase();
        // Command: /warp
        if (comName.equals("warp")) {
            if (!hasPerm(player, "warpz0r.warp", player.isOp())) {
                sendMessage(player, "Permission Denied", true);
                return true;
            }
            if (args.length != 1) {
                return false;
            }
            if (useiConomy && iConomy.db.get_balance(player.getName()) < warpCost) {
                sendMessage(player, "Insufficient funds to warp. Cost: " + Misc.formatCurrency(warpCost, iConomy.currency), true);
                return true;
            }
            Location loc = Locations.getWarp(args[0]);
            if (loc != null) {
                if (!loc.getWorld().getName().equals(player.getWorld().getName()) && !hasPerm(player, "warpz0r.worldwarp", player.isOp())) {
                    sendMessage(player, "Not allowed to warp between worlds", true);
                    return true;
                }
                // Subtract iConomy
                if (useiConomy && warpCost > 0) {
                    int bal = iConomy.db.get_balance(player.getName());
                    iConomy.db.set_balance(player.getName(), bal - warpCost);
                    sendMessage(player, "Deducted " + Misc.formatCurrency(warpCost, iConomy.currency) + " for warping", false);
                }
                // Keep the current vertical looking direction
                loc.setPitch(player.getLocation().getPitch());
                player.teleportTo(loc);
                sendMessage(player, "Teleported to " + args[0], false);
                log.info("[Warpz0r] " + player.getName() + " teleported to " + args[0]);
            } else {
                sendMessage(player, "Warp not found.", true);
                log.info("[Warpz0r] " + player.getName() + " tried to teleport to " + args[0]);
            }
            return true;
        // Command: /setwarp
        } else if (comName.equals("setwarp")) {
            if (!hasPerm(player, "warpz0r.set", player.isOp())) {
                sendMessage(player, "Permission Denied", true);
                return true;
            }
            if (args.length != 1) {
                return false;
            }
            
            Location loc = player.getLocation();
            Locations.addWarp(loc, args[0]);
            Locations.saveList(warpFile, Locations.warps);
            sendMessage(player, "Warp Set: " + args[0], false);
            log.info("[Warpz0r] " + player.getName() + " set warp " + args[0]);
            return true;
        // Command: /removewarp
        } else if (comName.equals("removewarp")) {
            if (!hasPerm(player, "warpz0r.remove", player.isOp())) {
                sendMessage(player, "Permission Denied", true);
                return true;
            }
            if (args.length != 1) {
                return false;
            }
            
            Location loc = Locations.getWarp(args[0]);
            if (loc == null) {
                sendMessage(player, "Warp not found.", true);
                log.info("[Warpz0r] " + player.getName() + " tried to remove warp " + args[0]);
                return true;
            }
            Locations.removeWarp(args[0]);
            Locations.saveList(warpFile, Locations.warps);
            sendMessage(player, "Warp removed: " + args[0], false);
            log.info("[Warpz0r] " + player.getName() + " removed warp " + args[0]);
            return true;
        // Command: /listwarps
        } else if (comName.equals("listwarps")) {
            if (!hasPerm(player, "warpz0r.list", player.isOp())) {
                sendMessage(player, "Permission Denied", true);
                return true;
            }
            Set<String> warpList = Locations.getWarpList();
            if (warpList.size() != 0) {
                StringBuilder sb = new StringBuilder();
                Iterator<String> warp = warpList.iterator();
                sb.append(warp.next());
                while (warp.hasNext())
                    sb.append(", ").append(warp.next());
                sendMessage(player, "Warps: " + ChatColor.WHITE + sb.toString(), false);
            } else {
                sendMessage(player, "Warp list is empty", true);
            }
            return true;
        // Command: /warpto
        } else if (comName.equals("warpto")) {
            if (!hasPerm(player, "warpz0r.warpto", player.isOp())) {
                sendMessage(player, "Permission Denied", true);
                return true;
            }
            if (args.length != 2) {
                return false;
            }
            Player target = getServer().getPlayer(args[0]);
            if (target == null) {
                sendMessage(player, "Target player not found", true);
                return true;
            }
            Location loc = Locations.getWarp(args[1]);
            if (loc == null) {
                sendMessage(player, "Warp not found", true);
                return true;
            }
            target.teleportTo(loc);
            sendMessage(player, "Teleported " + target.getName() + " to " + args[1], false);
            sendMessage(target, player.getName() + " teleported you to " + args[1], false);
            log.info("[Warpz0r] " + player.getName() + " teleported " + target.getName() + " to " + args[1]);
            return true;
        // Command: /sethome
        } else if (comName.equals("sethome")) {
            if (!hasPerm(player, "warpz0r.sethome", player.isOp())) {
                sendMessage(player, "Permission Denied", true);
                return true;
            }
            if (useiConomy && setHomeCost > 0) {
                if (iConomy.db.get_balance(player.getName()) < setHomeCost) {
                    sendMessage(player, "Insufficient funds to set home. Cost: " + Misc.formatCurrency(setHomeCost, iConomy.currency), true);
                    return true;
                } else {
                    int bal = iConomy.db.get_balance(player.getName());
                    iConomy.db.set_balance(player.getName(), bal - setHomeCost);
                    sendMessage(player, "Deducted " + Misc.formatCurrency(setHomeCost, iConomy.currency) + " for setting home", false);
                }
            }
            Location loc = player.getLocation();
            Locations.addHome(loc, player.getName());
            Locations.saveList(homeFile, Locations.homes);
            sendMessage(player, "Home Set", false);
            log.info("[Warpz0r] " + player.getName() + " set home");
            return true;
        // Command: /home
        } else if (comName.equals("home")) {
            if (!hasPerm(player, "warpz0r.home", player.isOp())) {
                sendMessage(player, "Permission Denied", true);
                return true;
            }
            if (useiConomy && iConomy.db.get_balance(player.getName()) < homeCost) {
                sendMessage(player, "Insufficient funds to warp home. Cost: " + Misc.formatCurrency(homeCost, iConomy.currency), true);
                return true;
            }
            Location loc = Locations.getHome(player.getName());
            if (loc != null) {
                if (!loc.getWorld().getName().equals(player.getWorld().getName()) && !hasPerm(player, "warpz0r.worldhome", true)) {
                    sendMessage(player, "Not allowed to teleport home between worlds", true);
                    return true;
                }
                // Subtract iConomy
                if (useiConomy && homeCost > 0) {
                    int bal = iConomy.db.get_balance(player.getName());
                    iConomy.db.set_balance(player.getName(), bal - homeCost);
                    sendMessage(player, "Deducted " + Misc.formatCurrency(homeCost, iConomy.currency) + " for teleporting home", false);
                }
                // Keep the current vertical looking direction
                loc.setPitch(player.getLocation().getPitch());
                player.teleportTo(loc);
                sendMessage(player, "Teleported to home", false);
                log.info("[Warpz0r] " + player.getName() + " teleported to home");
            } else {
                sendMessage(player, "Home not set", false);
            }
            return true;
        }

        return false;
    }
    
    private static void sendMessage(Player p, String message, boolean error) {
        ChatColor color = (error) ? ChatColor.RED : ChatColor.GREEN;
        p.sendMessage(color + "[Warpz0r] " + ChatColor.WHITE + message);
    }
    
	/*
	 * Find what Permissions plugin we're using and enable it.
	 */
	private boolean setupPermissions() {
		Plugin perm;
		PluginManager pm = getServer().getPluginManager();
		
		perm = pm.getPlugin("Permissions");
		// We're running Permissions
		if (perm != null) {
			if (!perm.isEnabled()) {
				pm.enablePlugin(perm);
			}
			permissions = (Permissions)perm;
			try {
				permVersion = Double.parseDouble(Permissions.version);
			} catch (Exception e) {
				log.info("Could not determine Permissions version: " + Permissions.version);
				return false;
			}
			return true;
		}
		// Permissions not loaded
		return false;
	}
    
	/*
	 * Check whether the player has the given permissions.
	 */
	public boolean hasPerm(Player player, String perm, boolean def) {
		if (permissions != null) {
			return permissions.getHandler().has(player, perm);
		} else {
			return def;
		}
	}
}

