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

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.logging.Level;

import org.bukkit.Location;
import org.bukkit.World;

public class Locations {
    public static HashMap<String, Warp> warps = new HashMap<String, Warp>();
    public static HashMap<String, Warp> homes = new HashMap<String, Warp>();
    public static String warpList[];

    public static void saveList(String locFile, HashMap<String, Warp> List) {
        try {
            BufferedWriter bw = new BufferedWriter(new FileWriter(locFile, false));

            for (Map.Entry<String, Warp> entry : List.entrySet()) {
                Location l = entry.getValue().loc;
                String name = entry.getValue().fullName;
                StringBuilder builder = new StringBuilder();
                builder.append(name);
                builder.append(':');
                builder.append(l.getX());
                builder.append(':');
                builder.append(l.getY());
                builder.append(':');
                builder.append(l.getZ());
                builder.append(':');
                builder.append(l.getYaw());
                builder.append(':');
                builder.append(entry.getValue().world);
                builder.append(':');
                builder.append(entry.getValue().cost);
                bw.append(builder.toString());
                bw.newLine();
            }
            bw.close();
        } catch (Exception e) {
            Warpz0r.log.log(Level.SEVERE, "Exception while writing locations to " + locFile);
            e.printStackTrace();
        }
    }

    public static void loadList(String locFile, HashMap<String, Warp> List, HashMap<String, World> worldList, World defWorld) {
        Scanner scanner = null;
        try {
            scanner = new Scanner(new FileReader(locFile));
            while (scanner.hasNextLine()) {
                
                String[] elements = scanner.nextLine().split(":");
                if (elements.length < 6) {
                    Warpz0r.log.info("[Warpz0r] Invalid warp.");
                    continue;
                }
                
                World w = worldList.get(elements[5]);
                
                Location l = new Location(w, Double.parseDouble(elements[1]), 
                                             Double.parseDouble(elements[2]), 
                                             Double.parseDouble(elements[3]));
                l.setYaw(Float.parseFloat(elements[4]));
                int cost = -1;
                if (elements.length > 6)
                	cost = Integer.parseInt(elements[6]);
                
                List.put(elements[0].toLowerCase(), new Warp(elements[0], elements[5], l, cost));
            }
        } catch (FileNotFoundException e) {
        } catch (Exception e) {
            Warpz0r.log.info("[Warpz0r] Could not load locations from " + locFile);
        } finally {
            if (scanner != null) scanner.close();
        }
    }
    
    public static void migrateWarps(String oldFile, String locFile, World world) {
        Scanner scanner = null;
        HashMap<String, Warp> list = new HashMap<String, Warp>();
        try {
            scanner = new Scanner(new FileReader(oldFile));
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                if (line.equals("")) continue;
                String[] elements = line.split(":");
                if (elements.length < 5) {
                    Warpz0r.log.info("[Warpz0r] Invalid Location Data");
                    continue;
                }
                
                Location l = new Location(world, Double.parseDouble(elements[1]), 
                                             Double.parseDouble(elements[2]), 
                                             Double.parseDouble(elements[3]));
                l.setYaw(Float.parseFloat(elements[4]));
                
                list.put(elements[0].toLowerCase(), new Warp(elements[0], world.getName(), l));
            }
            Locations.saveList(locFile, list);
        } catch (Exception e) {
            Warpz0r.log.info("[Warpz0r] Could not migrate " + oldFile);
        } finally {
            if (scanner != null) scanner.close();
        }
    }
    
    public static void clear() {
        Locations.warps.clear();
        Locations.warpList = new String[0];
    }
    
    public static void addHome(Location loc, String name) {
        Locations.homes.put(name.toLowerCase(), new Warp(name, loc, -1));
    }
    
    public static boolean removeHome(String name) {
    	return (Locations.homes.remove(name.toLowerCase()) != null);
    }
    
    public static Location getHome(String name) {
        Warp warp = Locations.homes.get(name.toLowerCase());
        if (warp == null) return null;
        if (warp.loc.getWorld() == null) {
            warp.loc.setWorld(Warpz0r.server.getWorld(warp.world));
            if (warp.loc.getWorld() == null) return null;
        }
        return warp.loc;
    }
    
    public static void addWarp(Location loc, String name, int cost) {
        Locations.warps.put(name.toLowerCase(), new Warp(name, loc, cost));
        Locations.updateList();
    }
    
    public static void removeWarp(String name) {
        Locations.warps.remove(name.toLowerCase());
        Locations.updateList();
    }
    
    public static void updateList() {
    	Collection<Warp> list = Locations.warps.values();
    	Locations.warpList = new String[list.size()];
    	int i = 0;
    	for (Warp w : list) { 
    		Locations.warpList[i++] = w.fullName;
    	}
    	Arrays.sort(Locations.warpList);
    }
    
    public static Location getWarp(String name) {
        Warp warp = Locations.warps.get(name.toLowerCase());
        if (warp == null) return null;
        if (warp.loc.getWorld() == null) {
            warp.loc.setWorld(Warpz0r.server.getWorld(warp.world));
            if (warp.loc.getWorld() == null) return null;
        }
        return warp.loc;
    }
    
    public static int getWarpCost(String name) {
    	Warp warp = Locations.warps.get(name.toLowerCase());
    	if (warp == null) return -1;
    	return warp.cost;
    }
    
    public static String[] getWarpList() {
        return Locations.warpList;
    }
    
    public static class Warp {
    	public String fullName;
        public String world;
        public Location loc;
        public int cost;
        
        Warp (String fullName, String world, Location loc) {
        	this(fullName, world, loc, -1);
        }
        
        Warp(String fullName, String world, Location loc, int cost) {
        	this.fullName = fullName;
            this.loc = loc;
            this.world = world;
            this.cost = cost;
        }
        
        Warp (String fullName, Location loc, int cost) {
        	this.fullName = fullName;
            this.loc = loc;
            this.world = "";
            if (this.loc.getWorld() != null) this.world = this.loc.getWorld().getName();
            this.cost = cost;
        }
    }
}
