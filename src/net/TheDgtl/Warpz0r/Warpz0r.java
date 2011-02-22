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
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginLoader;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.PluginManager;

import com.nijikokun.bukkit.Permissions.Permissions;
import com.nijikokun.bukkit.iConomy.iConomy;

/**
 * Warpz0r for Bukkit
 *
 * @author Drakia
 */
public class Warpz0r extends JavaPlugin {
	
    public Permissions Permissions = null;
    public static Logger log;
    public static Server server;
    private HashMap<String, World> worldList = new HashMap<String, World>();
    private String warpFile;
    private String homeFile;
    private World defWorld;

    // Constructor for old version
    /*
    public Warpz0r(PluginLoader pluginLoader, Server instance, PluginDescriptionFile desc, File folder, File plugin, ClassLoader cLoader) {
        super(pluginLoader, instance, desc, folder, plugin, cLoader);
    }*/
    
    public void onEnable() {
    	log = Logger.getLogger("Minecraft");
    	PluginManager pm = getServer().getPluginManager();
    	log.info( getDescription().getName() + " version " + getDescription().getVersion() + " is enabled" );
    	
        // Create data folder if it doesn't exist.
        if (!getDataFolder().exists()) getDataFolder().mkdirs();
        warpFile = getDataFolder().getPath() + File.separator + "warps.db";
        homeFile = getDataFolder().getPath() + File.separator + "homes.db";
        server = getServer();
    	
    	// Load a list of worlds for warping between worlds.
		for(World w : getServer().getWorlds()) {
			worldList.put(w.getName(), w);
		}
		defWorld = getServer().getWorlds().get(0);
    	
    	// Setup permissions
    	Plugin perm = pm.getPlugin("Permissions");
	    if(perm != null) Permissions = (Permissions)perm;
	    else log.info("[" + getDescription().getName() + "] Permission system not enabled.");
	    
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
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String commandLabel, String[] args) {
    	if (!(sender instanceof Player)) return false;
    	
		Player player = (Player)sender;
    	String comName = command.getName().toLowerCase();
        if (comName.equals("warp")) {
        	if (!hasPerm(player, "warpz0r.warp", player.isOp())) {
        		player.sendMessage(ChatColor.RED + "[Warpz0r] Permission Denied");
        		return true;
        	}
        	if (args.length != 1) {
        		return false;
        	}
        	
    		Location loc = Locations.getWarp(args[0]);
    		if (loc != null) {
    			if (!loc.getWorld().getName().equals(player.getWorld().getName()) && !hasPerm(player, "warpz0r.worldwarp", player.isOp())) {
    				player.sendMessage(ChatColor.RED + "[Warpz0r] Not allowed to warp between worlds");
    				return true;
    			}
    			// Keep the current vertical looking direction
    			loc.setPitch(player.getLocation().getPitch());
    			player.teleportTo(loc);
    			player.sendMessage(ChatColor.GREEN + "[Warpz0r] Teleported to " + args[0]);
    			log.info("[Warpz0r] " + player.getName() + " teleported to " + args[0]);
    		} else {
    			player.sendMessage(ChatColor.RED + "[Warpz0r] Warp not found.");
    			log.info("[Warpz0r] " + player.getName() + " tried to teleport to " + args[0]);
    		}
    		return true;
        } else if (comName.equals("setwarp")) {
        	if (!hasPerm(player, "warpz0r.set", player.isOp())) {
        		player.sendMessage(ChatColor.RED + "[Warpz0r] Permission Denied");
        		return true;
        	}
        	if (args.length != 1) {
        		return false;
        	}
        	
        	Location loc = player.getLocation();
        	Locations.addWarp(loc, args[0]);
        	Locations.saveList(warpFile, Locations.warps);
        	player.sendMessage(ChatColor.GREEN + "[Warpz0r] Warp Set: " + args[0]);
        	log.info("[Warpz0r] " + player.getName() + " set warp " + args[0]);
        	return true;
        } else if (comName.equals("removewarp")) {
        	if (!hasPerm(player, "warpz0r.remove", player.isOp())) {
        		player.sendMessage(ChatColor.RED + "[Warpz0r] Permission Denied");
        		return true;
        	}
        	if (args.length != 1) {
        		return false;
        	}
        	
        	Location loc = Locations.getWarp(args[0]);
        	if (loc == null) {
        		player.sendMessage(ChatColor.RED + "[Warpz0r] Warp not found.");
        		log.info("[Warpz0r] " + player.getName() + " tried to remove warp " + args[0]);
        		return true;
        	}
        	Locations.removeWarp(args[0]);
        	Locations.saveList(warpFile, Locations.warps);
        	player.sendMessage(ChatColor.GREEN + "[Warpz0r] Warp removed: " + args[0]);
        	log.info("[Warpz0r] " + player.getName() + " removed warp " + args[0]);
        	return true;
        } else if (comName.equals("listwarps")) {
        	if (!hasPerm(player, "warpz0r.list", player.isOp())) {
        		player.sendMessage(ChatColor.RED + "[Warpz0r] Permission Denied");
        		return true;
        	}
        	Set<String> warpList = Locations.getWarpList();
        	if (warpList.size() != 0) {
        		StringBuilder sb = new StringBuilder();
        		Iterator<String> warp = warpList.iterator();
        		sb.append(warp.next());
        		while (warp.hasNext())
        			sb.append(", ").append(warp.next());
        		player.sendMessage(ChatColor.GREEN + "[Warpz0r] Warps: " + ChatColor.WHITE + sb.toString());
        	} else {
        		player.sendMessage(ChatColor.RED + "[Warpz0r] Warp list is empty");
        	}
        	return true;
        } else if (comName.equals("warpto")) {
        	if (!hasPerm(player, "warpz0r.warpto", player.isOp())) {
        		player.sendMessage(ChatColor.RED + "[Warpz0r] Permission Denied");
        		return true;
        	}
        	if (args.length != 2) {
        		return false;
        	}
        	Player target = getServer().getPlayer(args[0]);
        	if (target == null) {
        		player.sendMessage(ChatColor.RED + "[Warpz0r] Target player not found");
        		return true;
        	}
        	Location loc = Locations.getWarp(args[1]);
        	if (loc == null) {
        		player.sendMessage(ChatColor.RED + "[Warpz0r] Warp not found");
        		return true;
        	}
        	target.teleportTo(loc);
			player.sendMessage(ChatColor.GREEN + "[Warpz0r] Teleported " + target.getName() + " to " + args[1]);
			target.sendMessage(ChatColor.GREEN + "[Warpz0r] " + player.getName() + " teleported you to " + args[1]);
			log.info("[Warpz0r] " + player.getName() + " teleported " + target.getName() + " to " + args[1]);
			return true;
        } else if (comName.equals("sethome")) {
        	if (!hasPerm(player, "warpz0r.sethome", player.isOp())) {
        		player.sendMessage(ChatColor.RED + "[Warpz0r] Permission Denied");
        		return true;
        	}
        	Location loc = player.getLocation();
        	Locations.addHome(loc, player.getName());
        	Locations.saveList(homeFile, Locations.homes);
        	player.sendMessage(ChatColor.GREEN + "[Warpz0r] Home Set");
        	log.info("[Warpz0r] " + player.getName() + " set home");
        	return true;
        } else if (comName.equals("home")) {
        	if (!hasPerm(player, "warpz0r.home", player.isOp())) {
        		player.sendMessage(ChatColor.RED + "[Warpz0r] Permission Denied");
        		return true;
        	}
    		Location loc = Locations.getHome(player.getName());
    		if (loc != null) {
    			if (!loc.getWorld().getName().equals(player.getWorld().getName()) && !hasPerm(player, "warpz0r.worldhome", true)) {
    				player.sendMessage(ChatColor.RED + "[Warpz0r] Not allowed to teleport home between worlds");
    				return true;
    			}
    			// Keep the current vertical looking direction
    			loc.setPitch(player.getLocation().getPitch());
    			player.teleportTo(loc);
    			player.sendMessage(ChatColor.GREEN + "[Warpz0r] Teleported to home");
    			log.info("[Warpz0r] " + player.getName() + " teleported to home");
    		} else {
    			player.sendMessage(ChatColor.RED + "[Warpz0r] Home not set");
    		}
    		return true;
        }
        return false;
	}
    
    public Boolean hasPerm(Player player, String perm, Boolean def) {
    	if (Permissions != null)
    		return Permissions.Security.has(player, perm);
    	return def;
    }
}

