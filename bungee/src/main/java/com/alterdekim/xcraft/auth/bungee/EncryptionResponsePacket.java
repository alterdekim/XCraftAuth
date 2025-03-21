package com.alterdekim.xcraft.auth.bungee;

import com.google.common.base.Preconditions;
import io.netty.buffer.ByteBuf;
import net.md_5.bungee.BungeeCord;
import net.md_5.bungee.EncryptionUtil;
import net.md_5.bungee.Util;
import net.md_5.bungee.api.Callback;
import net.md_5.bungee.connection.InitialHandler;
import net.md_5.bungee.connection.LoginResult;
import net.md_5.bungee.http.HttpClient;
import net.md_5.bungee.jni.cipher.BungeeCipher;
import net.md_5.bungee.netty.ChannelWrapper;
import net.md_5.bungee.netty.cipher.CipherDecoder;
import net.md_5.bungee.netty.cipher.CipherEncoder;
import net.md_5.bungee.protocol.AbstractPacketHandler;
import net.md_5.bungee.protocol.DefinedPacket;
import net.md_5.bungee.protocol.ProtocolConstants;
import net.md_5.bungee.protocol.packet.EncryptionRequest;
import net.md_5.bungee.protocol.packet.EncryptionResponse;

import javax.crypto.SecretKey;
import java.beans.ConstructorProperties;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

import static net.md_5.bungee.BungeeCord.getInstance;

import static com.alterdekim.xcraft.auth.bungee.XCraft.INTERNAL_PORT;


public class EncryptionResponsePacket extends DefinedPacket {

    private byte[] sharedSecret;
    private byte[] verifyToken;

    public void read(ByteBuf buf, ProtocolConstants.Direction direction, int protocolVersion) {
        this.sharedSecret = readArray(buf, 128);
        this.verifyToken = readArray(buf, 128);
    }

    public void write(ByteBuf buf, ProtocolConstants.Direction direction, int protocolVersion) {
        writeArray(this.sharedSecret, buf);
        writeArray(this.verifyToken, buf);
    }

    private EncryptionResponse getDummyResponse() {
        return new EncryptionResponse(this.getSharedSecret(), this.getVerifyToken());
    }

    @Override
    public void handle(AbstractPacketHandler handler) throws Exception {
        getInstance().getLogger().info("Intercepted handle request, returning custom response...");
        InitialHandler initialHandler = (InitialHandler) handler;
        Class<?> initialHandlerClass = InitialHandler.class;
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

        Field thisState = initialHandlerClass.getDeclaredField("thisState");
        thisState.setAccessible(true);

        Preconditions.checkState( thisState.get(handler).toString().equals("ENCRYPT"), "Not expecting ENCRYPT" );

        SecretKey sharedKey = EncryptionUtil.getSecret(this.getDummyResponse(), (EncryptionRequest) request.get(initialHandler));
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
        Callback<String> callback_handler = new Callback<String>(){

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
                            getInstance().getLogger().log(Level.SEVERE, "Error authenticating " + initialHandler.getName() + " with XCraftAuth", e);
                        }
                    }
                    initialHandler.disconnect("You're in offline mode");
                } else {
                    initialHandler.disconnect("XCraftAuth has failed to authenticate you");
                    getInstance().getLogger().log(Level.SEVERE, "Error authenticating " + initialHandler.getName() + " with XCraftAuth", error);
                }
            }
        };
        HttpClient.get(authURL, ((ChannelWrapper)ch.get(initialHandler)).getHandle().eventLoop(), callback_handler);
    }

    public byte[] getSharedSecret() {
        return this.sharedSecret;
    }

    public byte[] getVerifyToken() {
        return this.verifyToken;
    }

    public void setSharedSecret(byte[] sharedSecret) {
        this.sharedSecret = sharedSecret;
    }

    public void setVerifyToken(byte[] verifyToken) {
        this.verifyToken = verifyToken;
    }

    public String toString() {
        return "EncryptionResponsePacket(sharedSecret=" + Arrays.toString(this.getSharedSecret()) + ", verifyToken=" + Arrays.toString(this.getVerifyToken()) + ")";
    }

    public EncryptionResponsePacket() {
    }

    @ConstructorProperties({"sharedSecret", "verifyToken"})
    public EncryptionResponsePacket(byte[] sharedSecret, byte[] verifyToken) {
        this.sharedSecret = sharedSecret;
        this.verifyToken = verifyToken;
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        } else if (!(o instanceof EncryptionResponsePacket)) {
            return false;
        } else {
            EncryptionResponsePacket other = (EncryptionResponsePacket)o;
            if (!other.canEqual(this)) {
                return false;
            } else if (!Arrays.equals(this.getSharedSecret(), other.getSharedSecret())) {
                return false;
            } else {
                return Arrays.equals(this.getVerifyToken(), other.getVerifyToken());
            }
        }
    }

    protected boolean canEqual(Object other) {
        return other instanceof EncryptionResponsePacket;
    }

    public int hashCode() {
        int result = 1;
        result = result * 59 + Arrays.hashCode(this.getSharedSecret());
        result = result * 59 + Arrays.hashCode(this.getVerifyToken());
        return result;
    }
}
