package com.alterdekim.xcraft.auth.bungee;

import com.alterdekim.xcraft.auth.SaltNic;
import net.md_5.bungee.api.connection.PendingConnection;
import net.md_5.bungee.api.event.PreLoginEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;
import net.md_5.bungee.event.EventHandler;
import sun.misc.Unsafe;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.util.UUID;

import static com.alterdekim.xcraft.auth.lib.Patcher.patchAuthLib;

public class XCraft extends Plugin implements Listener {

    private static SaltNic server = null;

    public static int SERVER_PORT = 8999;
    public static int INTERNAL_PORT = 8999;
    public static String PUBLIC_DOMAIN = "localhost";
    public static Boolean USE_HTTPS = false;

    @Override
    public void onEnable() {
        getProxy().getPluginManager().registerListener(this, this);
        try {
            Configuration config = ConfigurationProvider.getProvider(YamlConfiguration.class).load(new File(getDataFolder(), "config.yml"));
            SERVER_PORT = config.getInt("public_port");
            INTERNAL_PORT = config.getInt("internal_port");
            PUBLIC_DOMAIN = config.getString("public_domain");
            USE_HTTPS = config.getBoolean("use_https");
        } catch (IOException e) {
            getLogger().info("No config file present");
            makeConfig();
        }
        if( server == null ) {
            try {
                getLogger().info("Starting SaltNic server...");
                server = new SaltNic(getLogger(), INTERNAL_PORT, USE_HTTPS, PUBLIC_DOMAIN, SERVER_PORT);
            } catch (IOException e) {
                getLogger().severe("Failed to start SaltNic server: " + e.getMessage());
            }
        }
    }

    @EventHandler
    public void onPreLogin(PreLoginEvent event) {
        try {
            PendingConnection connection = event.getConnection();

            // Ensure we are dealing with InitialHandler
            if (!connection.getClass().getName().equals("net.md_5.bungee.connection.InitialHandler")) {
                return;
            }

            Class<?> initialHandlerClass = connection.getClass();

            Field onlineModeField = initialHandlerClass.getDeclaredField("onlineMode");
            onlineModeField.setAccessible(true);
            onlineModeField.set(connection, false); // Disable Mojang auth

            getLogger().info("Bypassed Mojang authentication for " + connection.getName());
        } catch (Exception e) {
            e.printStackTrace();
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

    public void makeConfig() {
        if (!getDataFolder().exists()) {
            getDataFolder().mkdir();
        }

        File file = new File(getDataFolder(), "config.yml");


        if (!file.exists()) {
            try (InputStream in = getResourceAsStream("config.yml")) {
                Files.copy(in, file.toPath());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
