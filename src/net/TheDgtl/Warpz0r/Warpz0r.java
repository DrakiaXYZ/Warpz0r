package net.TheDgtl.Warpz0r;


/**
  * Warpz0r - A warp plugin for Bukkit
  * Copyright (C) 2011 Steven "Drakia" Scott <Drakia@Gmail.com>
  * 
  * This program is free software: you can redistribute it and/or modify
  * it under the terms of the GNU General Public License as published by
  * the Free Software Foundation, either version 3 of the License, or
  * (at your option) any later version.
  * 
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  * GNU General Public License for more details.
  *
  * You should have received a copy of the GNU General Public License
  * along with this program.  If not, see <http://www.gnu.org/licenses/>.
  */

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Logger;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.Event.Priority;
import org.bukkit.event.player.PlayerBedEnterEvent;
import org.bukkit.event.player.PlayerListener;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.event.server.PluginEnableEvent;
import org.bukkit.event.server.ServerListener;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.util.config.Configuration;

import com.nijikokun.bukkit.Permissions.Permissions;

public class Warpz0r extends JavaPlugin {
    
	private Permissions permissions = null;
	
    public static Logger log;
    public static Server server;
    private PluginManager pm;
    private Configuration config;
    private HashMap<String, World> worldList = new HashMap<String, World>();
    
    private String warpFile;
    private String homeFile;
    private World defWorld;
    private int warpCost;
    private int homeCost;
    private int setHomeCost;
    private int setWarpCost;
    private int removeWarpCost;
    private static boolean noPrefix = false;
    private boolean bedHome = false;
    
    private final sListener serverListener = new sListener();
    private final pListener playerListener = new pListener();
    
    public void onEnable() {
        log = Logger.getLogger("Minecraft");
        pm = getServer().getPluginManager();
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
        
        // Enable Permissions, disable if it's a bridge
        permissions = (Permissions)checkPlugin("Permissions");
        if (permissions != null) {
        	if (permissions.getDescription().getVersion().equals("2.7.7"))
        		permissions = null;
        }
        
        if (iConomyHandler.setupiConomy(pm)) {
        	log.info("[Warpz0r] Register v" + iConomyHandler.register.getDescription().getVersion() + " found");
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
        // Update list of warps
        Locations.updateList();
        
        pm.registerEvent(Event.Type.PLUGIN_ENABLE, serverListener, Priority.Monitor, this);
		pm.registerEvent(Event.Type.PLUGIN_DISABLE, serverListener, Priority.Monitor, this);
		pm.registerEvent(Event.Type.PLAYER_BED_ENTER, playerListener, Priority.Monitor, this);
    }
    
    public void onDisable() {
        Locations.clear();
        worldList.clear();
        log.info( getDescription().getName() + " version " + getDescription().getVersion() + " is disabled" );
    }
    
    public void loadConfig() {
        config.load();
        iConomyHandler.useiConomy = config.getBoolean("useiconomy", false);
        warpCost = config.getInt("warpcost", 5);
        homeCost = config.getInt("homecost", 5);
        setHomeCost = config.getInt("sethomecost", 0);
        setWarpCost = config.getInt("setwarpcost", 0);
        removeWarpCost = config.getInt("removewarpcost", 0);
        Warpz0r.noPrefix = config.getBoolean("noPrefix", false);
        bedHome = config.getBoolean("bedhome", bedHome);
        saveConfig();
    }
    
    public void saveConfig() {
        config.setProperty("useiconomy", iConomyHandler.useiConomy);
        config.setProperty("warpcost", warpCost);
        config.setProperty("homecost", homeCost);
        config.setProperty("sethomecost", setHomeCost);
        config.setProperty("setwarpcost", setWarpCost);
        config.setProperty("removewarpcost", removeWarpCost);
        config.setProperty("noPrefix", Warpz0r.noPrefix);
        config.setProperty("bedhome", bedHome);
        config.save();
    }
    
	/*
	 * Check if a plugin is loaded/enabled already. Returns the plugin if so, null otherwise
	 */
	private Plugin checkPlugin(String p) {
		Plugin plugin = pm.getPlugin(p);
		return checkPlugin(plugin);
	}
	
	private Plugin checkPlugin(Plugin plugin) {
		if (plugin != null && plugin.isEnabled()) {
			log.info("[Warpz0r] Found " + plugin.getDescription().getName() + " (v" + plugin.getDescription().getVersion() + ")");
			return plugin;
		}
		return null;
	}
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String commandLabel, String[] args) {
        if (!(sender instanceof Player)) return false;
        
        Player player = (Player)sender;
        String comName = command.getName().toLowerCase();
        // Command: /warp
        if (comName.equals("warp")) {
            if (!hasPerm(player, "warpz0r.warp")) {
                sendMessage(player, "Permission Denied", true);
                return true;
            }
            if (args.length != 1) {
                return false;
            }
            Location loc = Locations.getWarp(args[0]);
            if (loc != null) {
                if (!loc.getWorld().getName().equals(player.getWorld().getName()) && !hasPerm(player, "warpz0r.worldwarp")) {
                    sendMessage(player, "Not allowed to warp between worlds", true);
                    return true;
                }
                // iConomy check/charge
                if (!hasPerm(player, "warpz0r.free.warp")) {
	                int cost = Locations.getWarpCost(args[0]);
	                if (cost < 0) cost = warpCost;
	                if (!iConomyHandler.chargePlayer(player.getName(), null, cost)) {
	                	sendMessage(player, "Insufficient funds to warp. Cost: " + iConomyHandler.format(cost), true);
	                	return true;
	                }
	                if (cost > 0 && iConomyHandler.useiConomy()) {
	                    sendMessage(player, "Deducted " + iConomyHandler.format(cost) + " for warping", false);
	                }
                }
                // Load the chunk
                loc.getBlock().getChunk().load();
                // Keep the current vertical looking direction
                loc.setPitch(player.getLocation().getPitch());
                player.teleport(loc);
                sendMessage(player, "Teleported to " + args[0], false);
                log.info("[Warpz0r] " + player.getName() + " teleported to " + args[0]);
            } else {
                sendMessage(player, "Warp not found.", true);
                log.info("[Warpz0r] " + player.getName() + " tried to teleport to " + args[0]);
            }
            return true;
        // Command: /setwarp
        } else if (comName.equals("setwarp")) {
            if (!hasPerm(player, "warpz0r.set")) {
                sendMessage(player, "Permission Denied", true);
                return true;
            }
            if (args.length == 0 || args.length > 2) {
                return false;
            }
            
            int cost = -1;
            if (args.length == 2 && hasPerm(player, "warpz0r.set.cost"))
            	cost = Integer.valueOf(args[1]);
            
            if (!hasPerm(player, "warpz0r.free.setwarp")) {
	            if (!iConomyHandler.chargePlayer(player.getName(), null, setWarpCost)) {
	                sendMessage(player, "Insufficient funds to set warp. Cost: " + iConomyHandler.format(setWarpCost), true);
	                return true;
	            }
	            if (setWarpCost > 0 && iConomyHandler.useiConomy()) {
	                sendMessage(player, "Deducted " + iConomyHandler.format(setWarpCost) + " for setting warp", false);
	            }
            }
            Location loc = player.getLocation();
            Locations.addWarp(loc, args[0], cost);
            Locations.saveList(warpFile, Locations.warps);
            sendMessage(player, "Warp Set: " + args[0], false);
            log.info("[Warpz0r] " + player.getName() + " set warp " + args[0]);
            return true;
        // Command: /removewarp
        } else if (comName.equals("removewarp")) {
            if (!hasPerm(player, "warpz0r.remove")) {
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
            if (!hasPerm(player, "warpz0r.free.removewarp")) {
	            if (!iConomyHandler.chargePlayer(player.getName(), null, removeWarpCost)) {
	            	sendMessage(player, "Insufficient funds to remove warp. Cost: " + iConomyHandler.format(removeWarpCost), true);
	            	return true;
	            }
	            if (removeWarpCost > 0 && iConomyHandler.useiConomy()) {
	                sendMessage(player, "Deducted " + iConomyHandler.format(removeWarpCost) + " for removing warp", false);
	            }
            }
            Locations.removeWarp(args[0]);
            Locations.saveList(warpFile, Locations.warps);
            sendMessage(player, "Warp removed: " + args[0], false);
            log.info("[Warpz0r] " + player.getName() + " removed warp " + args[0]);
            return true;
        // Command: /listwarps
        } else if (comName.equals("listwarps")) {
            if (!hasPerm(player, "warpz0r.list")) {
                sendMessage(player, "Permission Denied", true);
                return true;
            }
            
            String[] warpList = Locations.getWarpList();
            if (warpList.length != 0) {
                StringBuilder sb = new StringBuilder();
                sb.append(warpList[0]);
                for (int i = 1; i < warpList.length; i++)
                    sb.append(", ").append(warpList[i]);
                sendMessage(player, "Warps: " + ChatColor.WHITE + sb.toString(), false);
            } else {
                sendMessage(player, "Warp list is empty", true);
            }
            return true;
        // Command: /warpto
        } else if (comName.equals("warpto")) {
            if (!hasPerm(player, "warpz0r.admin.warpto")) {
                sendMessage(player, "Permission Denied", true);
                return true;
            }
            if (args.length != 2) {
                return false;
            }
            Player target = getPlayer(args[0]);
            if (target == null) {
                sendMessage(player, "Target player not found", true);
                return true;
            }
            Location loc = Locations.getWarp(args[1]);
            if (loc == null) {
                sendMessage(player, "Warp not found", true);
                return true;
            }
            target.teleport(loc);
            sendMessage(player, "Teleported " + target.getName() + " to " + args[1], false);
            sendMessage(target, player.getName() + " teleported you to " + args[1], false);
            log.info("[Warpz0r] " + player.getName() + " teleported " + target.getName() + " to " + args[1]);
            return true;
        // Command: /sethome
        } else if (comName.equals("sethome")) {
        	String pName = player.getName();
        	if (args.length > 0) {
        		if (!hasPerm(player, "warpz0r.admin.sethome")) {
        			sendMessage(player, "Permission Denied", true);
        			return true;
        		}
        		pName = args[0];
        		sendMessage(player, "Set home of " + pName, false);
        		log.info("[Warpz0r] " + player.getName() + " set home of " + pName);
        	} else {
	            if (!hasPerm(player, "warpz0r.sethome")) {
	                sendMessage(player, "Permission Denied", true);
	                return true;
	            }
	            if (!hasPerm(player, "warpz0r.free.sethome")) {
		            if (!iConomyHandler.chargePlayer(player.getName(), null, setHomeCost)) {
		                sendMessage(player, "Insufficient funds to set home. Cost: " + iConomyHandler.format(setHomeCost), true);
		                return true;
		            }
		            if (setHomeCost > 0 && iConomyHandler.useiConomy()) {
		                sendMessage(player, "Deducted " + iConomyHandler.format(setHomeCost) + " for setting home", false);
		            }
	            }
	            sendMessage(player, "Home Set", false);
	            log.info("[Warpz0r] " + player.getName() + " set home");
        	}
            Location loc = player.getLocation();
            Locations.addHome(loc, pName);
            Locations.saveList(homeFile, Locations.homes);
            return true;
        // Command: /home
        } else if (comName.equals("home")) {
        	String pName = player.getName();
        	if (args.length > 0) {
        		if (!hasPerm(player, "warpz0r.admin.home")) {
        			sendMessage(player, "Permission Denied", true);
        			return true;
        		}
        		pName = args[0];
        	} else {
	            if (!hasPerm(player, "warpz0r.home")) {
	                sendMessage(player, "Permission Denied", true);
	                return true;
	            }
        	}
        	
            Location loc = Locations.getHome(pName);
            if (loc == null) {
            	sendMessage(player, "Home not set", false);
            	return true;
            }
            
            if (args.length == 0) {
	            if (!loc.getWorld().getName().equals(player.getWorld().getName()) && !hasPerm(player, "warpz0r.worldhome")) {
	                sendMessage(player, "Not allowed to teleport home between worlds", true);
	                return true;
	            }
	            if (!hasPerm(player, "warpz0r.free.home")) {
	                if (!iConomyHandler.chargePlayer(player.getName(), null, homeCost)) {
	                	sendMessage(player, "Insufficient funds to warp home. Cost: " + iConomyHandler.format(homeCost), true);
	                	return true;
	                }
	                if (homeCost > 0 && iConomyHandler.useiConomy()) {
	                    sendMessage(player, "Deducted " + iConomyHandler.format(homeCost) + " for teleporting home", false);
	                }
	            }
	            sendMessage(player, "Teleported to home", false);
	            log.info("[Warpz0r] " + player.getName() + " teleported to home");
            } else {
            	sendMessage(player, "Teleported to home of " + pName, false);
        		log.info("[Warpz0r] " + player.getName() + " teleported to home of " + pName);
            }
            // Keep the current vertical looking direction
            loc.setPitch(player.getLocation().getPitch());
            player.teleport(loc);
            return true;
        // Command: /wz
        } else if (comName.equals("wz")) {
        	if (args.length == 0) return false;
        	if (!args[0].equalsIgnoreCase("compass")) return false;
        	// Command: /wz (Set compass to home)
        	if (args.length == 1) {
        		if (!hasPerm(player, "warpz0r.compasshome")) {
        			sendMessage(player, "Permissions Denied", true);
        			return true;
        		}
        		Location loc = Locations.getHome(player.getName());
        		if (loc != null) {
        			if (!loc.getWorld().getName().equalsIgnoreCase(player.getWorld().getName())) {
        				sendMessage(player, "Home is in a different world", true);
        				return true;
        			}
        			player.setCompassTarget(loc);
        			sendMessage(player, "Compass now pointed to home", false);
        		} else {
        			sendMessage(player, "Home not set", true);
        		}
        	// Command: /wz <warp> (Set compass to warp)
        	} else if (args.length == 2) {
        		// Reset compass to spawn
        		if (args[1].equalsIgnoreCase("reset")) {
            		if (!hasPerm(player, "warpz0r.compassreset")) {
            			sendMessage(player, "Permissions Denied", true);
            			return true;
            		}
        			Location loc = player.getWorld().getSpawnLocation();
        			player.setCompassTarget(loc);
        			sendMessage(player, "Compass now pointed to spawn", false);
        		} else {
            		if (!hasPerm(player, "warpz0r.compasswarp")) {
            			sendMessage(player, "Permissions Denied", true);
            			return true;
            		}
	        		Location loc = Locations.getWarp(args[1]);
	        		if (loc != null) {
	        			if (!loc.getWorld().getName().equalsIgnoreCase(player.getWorld().getName())) {
	        				sendMessage(player, "Warp is in a different world", true);
	        				return true;
	        			}
	        			player.setCompassTarget(loc);
	        			sendMessage(player, "Compass now pointed to " + args[1], false);
	        		} else {
	        			sendMessage(player, "Warp not found", true);
	        		}
        		}
        	} else {
        		return false;
        	}
        	return true;
        // Command: /clearhome
        } else if (comName.equalsIgnoreCase("clearhome")) {
            if (!hasPerm(player, "warpz0r.admin.clearhome")) {
                sendMessage(player, "Permission Denied", true);
                return true;
            }
            if (args.length > 1) {
            	return false;
            }
            
            String pName = player.getName();
            Player target = null;
            if (args.length == 1) {
            	pName = args[0];
            	target = getServer().getPlayer(pName);
            }
            
            Locations.removeHome(pName);
            Locations.saveList(homeFile, Locations.homes);
            sendMessage(player, "Cleared the home of " + pName, false);
            if (target != null) {
            	sendMessage(target, "Your home has been cleared", false);
            }
            log.info("[Warpz0r] " + player.getName() + " reset the home of " + pName);
            return true;
        }

        return false;
    }
    
    private Player getPlayer(String name) {
    	List<Player> players = server.matchPlayer(name);
        if (players.size() < 1) {
        	return null;
        }
        Player target = players.get(0);
        return target;
    }
    
    private static void sendMessage(Player p, String message, boolean error) {
        if (Warpz0r.noPrefix) {
        	p.sendMessage(message);
        } else {
        	ChatColor color = (error) ? ChatColor.RED : ChatColor.GREEN;
        	p.sendMessage(color + "[Warpz0r] " + ChatColor.WHITE + message);
        }
    }
    
	/*
	 * Check whether the player has the given permissions.
	 */
	public boolean hasPerm(Player player, String perm) {
		if (permissions != null) {
			return permissions.getHandler().has(player, perm);
		} else {
			return player.hasPermission(perm);
		}
	}
	
	private class sListener extends ServerListener {
		@Override
		public void onPluginEnable(PluginEnableEvent event) {
			if (iConomyHandler.setupiConomy(event.getPlugin())) {
				log.info("[Warpz0r] Register v" + iConomyHandler.register.getDescription().getVersion() + " found");
			}
			if (permissions == null) {
				if (event.getPlugin().getDescription().getName().equalsIgnoreCase("Permissions")) {
					permissions = (Permissions)checkPlugin(event.getPlugin());
			        if (permissions != null) {
			        	if (permissions.getDescription().getVersion().equals("2.7.7"))
			        		permissions = null;
			        }
				}
			}
		}
		
		@Override
		public void onPluginDisable(PluginDisableEvent event) {
			if (iConomyHandler.checkLost(event.getPlugin())) {
				log.info("[Warpz0r] Register plugin lost.");
			}
			if (event.getPlugin() == permissions) {
				log.info("[Warpz0r] Permissions plugin lost.");
				permissions = null;
			}
		}
	}
	private class pListener extends PlayerListener {
		@Override
		public void onPlayerBedEnter(PlayerBedEnterEvent event) {
			if (!bedHome) return;
			Player player = event.getPlayer();
			Location loc = player.getLocation();
            if (!hasPerm(player, "warpz0r.bedhome")) {
                return;
            }
            if (iConomyHandler.useiConomy() && !hasPerm(player, "warpz0r.free.bedhome")) {
	            if (!iConomyHandler.chargePlayer(player.getName(), null, setHomeCost)) {
	                sendMessage(player, "Insufficient funds to set home. Cost: " + iConomyHandler.format(setHomeCost), true);
	                return;
	            }
	            if (setHomeCost > 0) {
	                sendMessage(player, "Deducted " + iConomyHandler.format(setHomeCost) + " for setting home", false);
	            }
            }
            sendMessage(player, "Home Set", false);
            log.info("[Warpz0r] " + player.getName() + " set home via bed");
        	Locations.addHome(loc, player.getName());
        	Locations.saveList(homeFile, Locations.homes);
		}
	}
}

