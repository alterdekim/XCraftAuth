package com.alterdekim.xcraft.auth.bungee;

import com.alterdekim.xcraft.auth.SaltNic;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.TObjectIntMap;
import net.md_5.bungee.BungeeCord;
import net.md_5.bungee.EncryptionUtil;
import net.md_5.bungee.Util;
import net.md_5.bungee.api.Callback;
import net.md_5.bungee.api.connection.PendingConnection;
import net.md_5.bungee.api.event.PreLoginEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;
import net.md_5.bungee.connection.InitialHandler;
import net.md_5.bungee.connection.LoginResult;
import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.http.HttpClient;
import net.md_5.bungee.jni.cipher.BungeeCipher;
import net.md_5.bungee.netty.ChannelWrapper;
import net.md_5.bungee.netty.cipher.CipherDecoder;
import net.md_5.bungee.netty.cipher.CipherEncoder;
import net.md_5.bungee.protocol.DefinedPacket;
import net.md_5.bungee.protocol.Protocol;
import net.md_5.bungee.protocol.ProtocolConstants;
import net.md_5.bungee.protocol.packet.EncryptionRequest;
import net.md_5.bungee.protocol.packet.EncryptionResponse;

import javax.crypto.SecretKey;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.logging.Level;
import java.util.logging.Logger;

public class XCraft extends Plugin {

    private static SaltNic server = null;

    public static int SERVER_PORT = 8999;
    public static int INTERNAL_PORT = 8999;
    public static String PUBLIC_DOMAIN = "localhost";
    public static Boolean USE_HTTPS = false;

    @Override
    public void onEnable() {
        try {
            injectListener(47);
        } catch(Exception e) {
            getLogger().severe("Error injecting auth packet. " + e.getMessage());
        }
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

    @SuppressWarnings("unchecked")
    private void injectListener(int version) throws Exception {
        Class<?> toServerClass = Protocol.LOGIN.TO_SERVER.getClass();

        Class<?> protocolMappingClass = Class.forName("net.md_5.bungee.protocol.Protocol$ProtocolMapping");
        Constructor<?> pConstructor = protocolMappingClass.getDeclaredConstructor(int.class, int.class);
        pConstructor.setAccessible(true);

        Method registerPacketMethod = toServerClass.getDeclaredMethod("registerPacket",  Class.class, protocolMappingClass );
        registerPacketMethod.setAccessible(true);
        registerPacketMethod.invoke(toServerClass, EncryptionResponsePacket.class, pConstructor.newInstance(version, 1));

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
