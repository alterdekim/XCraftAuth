package com.alterdekim.xcraft.auth;


import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.URL;

public class XCraft extends JavaPlugin {

    private static SaltNic server = null;

    public static int SERVER_PORT = 8999;
    public static String PUBLIC_DOMAIN = "localhost";
    public static Boolean USE_HTTPS = false;

    @Override
    public void onEnable() {
        this.saveDefaultConfig();
        if( server == null ) {
            try {
                getLogger().info("Starting SaltNic server...");
                SERVER_PORT = getConfig().getInt("public_port");
                PUBLIC_DOMAIN = getConfig().getString("public_domain");
                USE_HTTPS = getConfig().getBoolean("use_https");
                server = new SaltNic(getLogger());
            } catch (IOException e) {
                getLogger().severe("Failed to start SaltNic server: " + e.getMessage());
            }
        }
        getLogger().info("Patching AuthLib URLs...");
        while(true) {
            try {
                patchAuthLib();
                getLogger().info("AuthLib URLs patched successfully!");
                return;
            } catch (Exception e) {
                e.printStackTrace();
                getLogger().severe("Failed to patch AuthLib: " + e.getMessage());
            }
        }
    }

    private void patchAuthLib() throws Exception {
        Class<?> clazz = Class.forName("com.mojang.authlib.yggdrasil.YggdrasilMinecraftSessionService");
        modifyFinalField(clazz, "BASE_URL", "http://localhost:"+SERVER_PORT+"/api/");
        modifyFinalField(clazz, "JOIN_URL", new URL("http://localhost:"+SERVER_PORT+"/api/join"));
        modifyFinalField(clazz, "CHECK_URL", new URL("http://localhost:"+SERVER_PORT+"/api/hasJoined"));
    }

    private void modifyFinalField(Class<?> clazz, String fieldName, Object newValue) throws Exception {
        Field field = clazz.getDeclaredField(fieldName);
        field.setAccessible(true);

        Field modifiersField = Field.class.getDeclaredField("modifiers");
        modifiersField.setAccessible(true);
        modifiersField.setInt(field, field.getModifiers() & ~Modifier.FINAL);

        field.set(null, newValue);
        getLogger().info(fieldName + " patched to: " + newValue);
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
