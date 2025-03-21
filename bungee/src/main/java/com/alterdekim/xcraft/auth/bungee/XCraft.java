package com.alterdekim.xcraft.auth.bungee;

import com.alterdekim.xcraft.auth.SaltNic;
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
import net.md_5.bungee.protocol.packet.EncryptionRequest;
import net.md_5.bungee.protocol.packet.EncryptionResponse;

import javax.crypto.SecretKey;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.logging.Level;

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

            if (!connection.getClass().getName().equals("net.md_5.bungee.connection.InitialHandler")) return;

            Class<?> initialHandlerClass = connection.getClass();


            Method hasJoinedMethod = initialHandlerClass.getDeclaredMethod("handle", EncryptionResponse.class);
            hasJoinedMethod.setAccessible(true);

            Method finish = initialHandlerClass.getDeclaredMethod("finish");
            finish.setAccessible(true);

            Field ch = initialHandlerClass.getDeclaredField("ch");
            ch.setAccessible(true);

            Field request = initialHandlerClass.getDeclaredField("request");
            request.setAccessible(true);

            Field loginProfile = initialHandlerClass.getDeclaredField("loginProfile");
            loginProfile.setAccessible(true);

            Field name = initialHandlerClass.getDeclaredField("name");
            name.setAccessible(true);

            Field uniqueId = initialHandlerClass.getDeclaredField("uniqueId");
            uniqueId.setAccessible(true);

            InitialHandler initialHandler = (InitialHandler) connection;

            Object proxy = Proxy.newProxyInstance(
                    initialHandlerClass.getClassLoader(),
                    new Class[]{initialHandlerClass},
                    (proxy1, method, args) -> {
                        if (method.getName().equals("handle") && args.length == 1 && args[0].getClass() == EncryptionResponse.class) {
                            getLogger().info("Intercepted hasJoined request, returning fake response...");
                            SecretKey sharedKey = EncryptionUtil.getSecret((EncryptionResponse) args[0], (EncryptionRequest) request.get(initialHandler));
                            BungeeCipher decrypt = EncryptionUtil.getCipher(false, sharedKey);
                            ((ChannelWrapper)ch.get(initialHandler)).addBefore("frame-decoder", "decrypt", new CipherDecoder(decrypt));
                            BungeeCipher encrypt = EncryptionUtil.getCipher(true, sharedKey);
                            ((ChannelWrapper)ch.get(initialHandler)).addBefore("frame-prepender", "encrypt", new CipherEncoder(encrypt));
                            String encName = URLEncoder.encode(initialHandler.getName(), "UTF-8");
                            MessageDigest sha = MessageDigest.getInstance("SHA-1");
                            for (byte[] bit : new byte[][]{((EncryptionRequest) request.get(initialHandler)).getServerId().getBytes("ISO_8859_1"), sharedKey.getEncoded(), EncryptionUtil.keys.getPublic().getEncoded()}) {
                                sha.update(bit);
                            }
                            String authURL = "http://localhost:"+INTERNAL_PORT+"/api/hasJoined?username=" + encName;
                            Callback<String> handler = new Callback<String>(){

                                @Override
                                public void done(String result, Throwable error) {
                                    if (error == null) {
                                        LoginResult obj = BungeeCord.getInstance().gson.fromJson(result, LoginResult.class);
                                        if (obj != null && obj.getId() != null) {
                                            try {
                                                loginProfile.set(initialHandler, obj);
                                                name.set(initialHandler, obj.getName());
                                                uniqueId.set(initialHandler, Util.getUUID(obj.getId()));
                                                finish.invoke(initialHandler);
                                                return;
                                            } catch (Exception e) {
                                                getLogger().log(Level.SEVERE, "Error authenticating " + initialHandler.getName() + " with XCraftAuth", e);
                                            }
                                        }
                                        initialHandler.disconnect("You're in offline mode");
                                    } else {
                                        initialHandler.disconnect("XCraftAuth has failed to authenticate you");
                                        getLogger().log(Level.SEVERE, "Error authenticating " + initialHandler.getName() + " with XCraftAuth", error);
                                    }
                                }
                            };
                            HttpClient.get(authURL, ((ChannelWrapper)ch.get(initialHandler)).getHandle().eventLoop(), handler);
                            return new Object();
                        }
                        return method.invoke(connection, args);
                    }
            );

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
