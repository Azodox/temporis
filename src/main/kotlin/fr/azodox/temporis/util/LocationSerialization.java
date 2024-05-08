package fr.azodox.temporis.util;

import org.bukkit.Bukkit;
import org.bukkit.Location;

public class LocationSerialization {

    // Convert string to location.
    public static Location deserialize(String str){
        String[] str2loc = str.split(":");
        Location loc = new Location(Bukkit.getServer().getWorld(str2loc[0]),0,0,0);
        loc.setX(Double.parseDouble(str2loc[1]));
        loc.setY(Double.parseDouble(str2loc[2]));
        loc.setZ(Double.parseDouble(str2loc[3]));
        loc.setYaw(Float.parseFloat(str2loc[4]));
        loc.setPitch(Float.parseFloat(str2loc[5]));
        return loc;
    }

    // Convert Location to string
    public static String serialize(Location loc){
        return loc.getWorld().getName()+":"+loc.getBlockX()+":"+loc.getBlockY()+":"+loc.getBlockZ() + ":" + loc.getYaw() + ":" + loc.getPitch();
    }
}
