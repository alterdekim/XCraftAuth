package com.alterdekim.xcraft.auth.spigot;


import com.alterdekim.xcraft.auth.SaltNic;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.URL;

import static com.alterdekim.xcraft.auth.lib.Patcher.patchAuthLib;

public class XCraft extends JavaPlugin {

    private static SaltNic server = null;

    public static int SERVER_PORT = 8999;
    public static int INTERNAL_PORT = 8999;
    public static String PUBLIC_DOMAIN = "localhost";
    public static Boolean USE_HTTPS = false;

    @Override
    public void onEnable() {
        this.saveDefaultConfig();
        if( server == null ) {
            try {
                getLogger().info("Starting SaltNic server...");
                SERVER_PORT = getConfig().getInt("public_port");
                INTERNAL_PORT = getConfig().getInt("internal_port");
                PUBLIC_DOMAIN = getConfig().getString("public_domain");
                USE_HTTPS = getConfig().getBoolean("use_https");
                server = new SaltNic(getLogger(), INTERNAL_PORT, USE_HTTPS, PUBLIC_DOMAIN, SERVER_PORT);
            } catch (IOException e) {
                getLogger().severe("Failed to start SaltNic server: " + e.getMessage());
            }
        }
        getLogger().info("Patching AuthLib URLs...");
        while(true) {
            try {
                patchAuthLib(getLogger(), INTERNAL_PORT);
                getLogger().info("AuthLib URLs patched successfully!");
                return;
            } catch (Exception e) {
                e.printStackTrace();
                getLogger().severe("Failed to patch AuthLib: " + e.getMessage());
            }
        }
    }

    @Override
    public void onDisable() {
        if (server != null) {
            server.stop();
            getLogger().info("SaltNic session server stopped.");
            server = null;
        }
    }
}
