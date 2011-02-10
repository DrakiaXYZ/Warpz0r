package net.TheDgtl.Warpz0r;


import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.logging.Level;

import org.bukkit.Location;
import org.bukkit.World;

public class Locations {
	public static HashMap<String, Warp> warps = new HashMap<String, Warp>();
	public static HashMap<String, Warp> homes = new HashMap<String, Warp>();

	public static void saveList(String locFile, HashMap<String, Warp> List) {
        try {
            BufferedWriter bw = new BufferedWriter(new FileWriter(locFile, false));

    		for (Map.Entry<String, Warp> entry : List.entrySet()) {
    			Location l = entry.getValue().loc;
    			String name = entry.getKey();
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
				if (elements.length != 6) {
					Warpz0r.log.info("[Warpz0r] Invalid warp.");
					continue;
				}
				
				World w = worldList.get(elements[5]);
				
				Location l = new Location(w, Double.parseDouble(elements[1]), 
											 Double.parseDouble(elements[2]), 
											 Double.parseDouble(elements[3]));
				l.setYaw(Float.parseFloat(elements[4]));
				
				List.put(elements[0], new Warp(elements[5], l));
			}
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
				
				list.put(elements[0], new Warp(world.getName(), l));
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
	}
	
	public static void addHome(Location loc, String name) {
		Locations.homes.put(name, new Warp(loc));
	}
	
	public static Location getHome(String name) {
		Warp warp = Locations.homes.get(name);
		if (warp == null) return null;
		if (warp.loc.getWorld() == null) {
			warp.loc.setWorld(Warpz0r.server.getWorld(warp.world));
			if (warp.loc.getWorld() == null) return null;
		}
		return warp.loc;
	}
	
	public static void addWarp(Location loc, String name) {
		Locations.warps.put(name, new Warp(loc));
	}
	
	public static void removeWarp(String name) {
		Locations.warps.remove(name);
	}
	
	public static Location getWarp(String name) {
		Warp warp = Locations.warps.get(name);
		if (warp == null) return null;
		if (warp.loc.getWorld() == null) {
			warp.loc.setWorld(Warpz0r.server.getWorld(warp.world));
			if (warp.loc.getWorld() == null) return null;
		}
		return warp.loc;
	}
	
	public static Set<String> getWarpList() {
		return Locations.warps.keySet();
	}
	
	static private class Warp {
		public String world;
		public Location loc;
		
		Warp(String world, Location loc) {
			this.loc = loc;
			this.world = world;
		}
		
		Warp (Location loc) {
			this.loc = loc;
			this.world = "";
			if (this.loc.getWorld() != null) this.world = this.loc.getWorld().getName();
		}
	}
}
