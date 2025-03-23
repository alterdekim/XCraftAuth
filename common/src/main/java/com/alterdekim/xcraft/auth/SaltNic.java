package com.alterdekim.xcraft.auth;

import com.alterdekim.xcraft.auth.database.User;
import com.alterdekim.xcraft.auth.database.UserStorage;
import com.alterdekim.xcraft.auth.request.JoinMinecraftServerRequest;
import com.alterdekim.xcraft.auth.request.SignUpRequest;
import com.alterdekim.xcraft.auth.response.*;
import com.jsoniter.JsonIterator;
import com.jsoniter.output.JsonStream;
import fi.iki.elonen.NanoHTTPD;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Logger;

public class SaltNic extends NanoHTTPD {

    private final Logger logger;
    private final ConcurrentMap<String, Boolean> sessions;
    private final UserStorage storage;

    private final Response invalidSession = newFixedLengthResponse(Response.Status.FORBIDDEN, "application/json", "{\"error\":\"ForbiddenOperationException\", \"errorMessage\":\"Invalid session token\", \"cause\": \"ForbiddenOperationException\"}");

    private static final String SKIN_DIRECTORY = "plugins/XCraftAuth/skins";
    private static final String CAPE_DIRECTORY = "plugins/XCraftAuth/capes";
    private static final int MAX_FILE_SIZE = 1024 * 1024;

    private final boolean USE_HTTPS;
    private final String PUBLIC_DOMAIN;
    private final int SERVER_PORT;

    public SaltNic(Logger logger, int internal_port, boolean use_https, String public_domain, int server_port) throws IOException {
        super(internal_port);
        this.logger = logger;
        this.USE_HTTPS = use_https;
        this.PUBLIC_DOMAIN = public_domain;
        this.SERVER_PORT = server_port;
        this.storage = new UserStorage();
        this.sessions = new ConcurrentHashMap<>();
        start(NanoHTTPD.SOCKET_READ_TIMEOUT, false);
        logger.info("SaltNic session server started on http://localhost:"+ internal_port);
    }

    @Override
    public Response serve(IHTTPSession session) {
        String uri = session.getUri();
        Method method = session.getMethod();
        logger.info("Attempted to reach url: " + uri + " | method: " + method);

        if ("/api/join".equals(uri) && method == Method.POST) {
            return handleJoinRequest(session);
        } else if ("/api/hasJoined".equals(uri) && method == Method.GET) {
            return handleHasJoinedRequest(session);
        } else if (uri.startsWith("/api/profile/") && method == Method.GET) {
            return handleProfileRequest(session, uri);
        } else if (uri.startsWith("/api/login") && method == Method.POST) {
            return handleLoginRequest(session);
        } else if (uri.startsWith("/api/register") && method == Method.POST) {
            return handleProfileRegistration(session);
        } else if (Method.POST == method && "/api/set_model".equals(uri)) {
            return handleSetModel(session);
        } else if (Method.POST == method && "/api/upload_cape".equals(uri)) {
            return handleCapeUpload(session);
        } else if (Method.POST == method && "/api/upload".equals(uri)) {
            return handleSkinUpload(session);
        } else if (Method.GET == method && uri.startsWith("/api/skin/s")) {
            return serveSkinImage(uri.substring(11));
        } else if (Method.GET == method && uri.startsWith("/api/cape/a")) {
            return serveCapeImage(uri.substring(11));
        }
        return newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", "404 Not Found");
    }

    private Response handleSkinUpload(IHTTPSession session) {
        try {
            // Parse multipart form data
            Map<String, String> files = new java.util.HashMap<>();
            session.parseBody(files);
            String playerUUID = session.getParameters().get("uuid").get(0);
            String password = session.getParameters().get("password").get(0);

            if (playerUUID == null || password == null || !files.containsKey("skin")) {
                return newFixedLengthResponse(Response.Status.OK, "text/plain", "Missing player credentials or skin file");
            }

            if( this.storage.getUserPassword(playerUUID) == null || !PasswordHasher.checkPassword(password, this.storage.getUserPassword(playerUUID)) ) {
                return newFixedLengthResponse(Response.Status.OK, "text/plain", "Invalid credentials");
            }

            new File(SKIN_DIRECTORY).mkdirs();

            File tempFile = new File(files.get("skin"));

            long fileSize = tempFile.length();
            if (fileSize > MAX_FILE_SIZE) {
                return newFixedLengthResponse(Response.Status.OK, "text/plain", "File is too large. Max size: 1MB");
            }

            BufferedImage image = ImageIO.read(tempFile);
            if (image == null) {
                return newFixedLengthResponse(Response.Status.OK, "text/plain", "Invalid image file");
            }

            int width = image.getWidth();
            int height = image.getHeight();

            if (!((width == 64 && height == 64) || (width == 64 && height == 32))) {
                return newFixedLengthResponse(Response.Status.OK, "text/plain", "Invalid dimensions. Only 64x64 or 64x32 allowed");
            }

            File skinFile = new File(SKIN_DIRECTORY, playerUUID + ".png");

            Files.copy(tempFile.toPath(), skinFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            return newFixedLengthResponse(Response.Status.OK, "text/plain", "Skin uploaded successfully for " + playerUUID);
        } catch (Exception e) {
            e.printStackTrace();
            return newFixedLengthResponse(Response.Status.OK, "text/plain", "Upload failed: " + e.getMessage());
        }
    }

    private Response handleCapeUpload(IHTTPSession session) {
        try {
            // Parse multipart form data
            Map<String, String> files = new java.util.HashMap<>();
            session.parseBody(files);
            String playerUUID = session.getParameters().get("uuid").get(0);
            String password = session.getParameters().get("password").get(0);

            if (playerUUID == null || password == null || !files.containsKey("cape")) {
                return newFixedLengthResponse(Response.Status.OK, "text/plain", "Missing player credentials or skin file");
            }

            if( this.storage.getUserPassword(playerUUID) == null || !PasswordHasher.checkPassword(password, this.storage.getUserPassword(playerUUID)) ) {
                return newFixedLengthResponse(Response.Status.OK, "text/plain", "Invalid credentials");
            }

            new File(CAPE_DIRECTORY).mkdirs();

            File tempFile = new File(files.get("cape"));

            long fileSize = tempFile.length();
            if (fileSize > MAX_FILE_SIZE) {
                return newFixedLengthResponse(Response.Status.OK, "text/plain", "File is too large. Max size: 1MB");
            }

            BufferedImage image = ImageIO.read(tempFile);
            if (image == null) {
                return newFixedLengthResponse(Response.Status.OK, "text/plain", "Invalid image file");
            }

            int width = image.getWidth();
            int height = image.getHeight();

            if (!(width == 64 && height == 32)) {
                return newFixedLengthResponse(Response.Status.OK, "text/plain", "Invalid dimensions. Only 64x32 allowed");
            }

            File capeFile = new File(CAPE_DIRECTORY, playerUUID + ".png");

            Files.copy(tempFile.toPath(), capeFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            return newFixedLengthResponse(Response.Status.OK, "text/plain", "Cape uploaded successfully for " + playerUUID);
        } catch (Exception e) {
            e.printStackTrace();
            return newFixedLengthResponse(Response.Status.OK, "text/plain", "Upload failed: " + e.getMessage());
        }
    }

    public boolean getSessionValue(String uuid) {
        if( this.sessions.containsKey(uuid) ) {
            return this.sessions.get(uuid);
        }
        return false;
    }

    public void deleteSessionRequest(String uuid) {
        this.sessions.remove(uuid);
    }

    private Response handleSetModel(IHTTPSession session) {
        try {
            String playerUUID = session.getParameters().get("uuid").get(0);
            String password = session.getParameters().get("password").get(0);
            Boolean model = Boolean.parseBoolean(session.getParameters().get("model").get(0));

            if (playerUUID == null || password == null) {
                return newFixedLengthResponse(Response.Status.OK, "text/plain", "Missing player credentials");
            }

            if( this.storage.getUserPassword(playerUUID) == null || !PasswordHasher.checkPassword(password, this.storage.getUserPassword(playerUUID)) ) {
                return newFixedLengthResponse(Response.Status.OK, "text/plain", "Invalid credentials");
            }

            this.storage.setSkinModel(playerUUID, model ? User.SkinModel.Alex : User.SkinModel.Steve);

            return newFixedLengthResponse(Response.Status.OK, "text/plain", "Skin model was set successfully for " + playerUUID);
        } catch (Exception e) {
            e.printStackTrace();
            return newFixedLengthResponse(Response.Status.OK, "text/plain", "Setting failed: " + e.getMessage());
        }
    }

    private Response serveCapeImage(String playerName) {
        File skinFile = new File(CAPE_DIRECTORY, playerName + ".png");

        if (!skinFile.exists()) {
            return newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", "Cape not found for " + playerName);
        }
        try {
            FileInputStream fis = new FileInputStream(skinFile);
            return newChunkedResponse(Response.Status.OK, "image/png", fis);
        } catch (IOException e) {
            return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "text/plain", "Failed to load skin");
        }
    }

    private Response serveSkinImage(String playerName) {
        File skinFile = new File(SKIN_DIRECTORY, playerName + ".png");

        if (!skinFile.exists()) {
            return newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", "Skin not found for " + playerName);
        }
        try {
            FileInputStream fis = new FileInputStream(skinFile);
            return newChunkedResponse(Response.Status.OK, "image/png", fis);
        } catch (IOException e) {
            return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "text/plain", "Failed to load skin");
        }
    }

    public MinecraftProperty getTextures(String uuid) {
        Map<MinecraftProfileTexture.Type, MinecraftProfileTexture> textures = new HashMap<>();

        if( new File(SKIN_DIRECTORY, uuid + ".png").exists() ) {
            MinecraftProfileTexture texture = new MinecraftProfileTexture("http" + (USE_HTTPS ? "s" : "") + "://"+PUBLIC_DOMAIN+":"+SERVER_PORT+"/api/skin/s" + uuid, new HashMap<>());
            if( storage.getSkinModel(uuid) == User.SkinModel.Alex ) {
                texture.getMetadata().put("model", "slim");
            }
            textures.put(MinecraftProfileTexture.Type.SKIN, texture);
        }

        if( new File(CAPE_DIRECTORY, uuid + ".png").exists() ) {
            textures.put(MinecraftProfileTexture.Type.CAPE, new MinecraftProfileTexture("http"+(USE_HTTPS ? "s" : "")+"://"+PUBLIC_DOMAIN+":"+SERVER_PORT+"/api/cape/a" + uuid, new HashMap<>()));
        }

        MinecraftTexturesPayload minecraftTexturesPayload = new MinecraftTexturesPayload(System.currentTimeMillis(), uuid, this.storage.getUsername(uuid), textures);
        return new MinecraftProperty("textures", Base64.getEncoder().encodeToString(JsonStream.serialize(minecraftTexturesPayload).getBytes()), (Math.random()+""));
    }

    private Response handleProfileRequest(IHTTPSession session, String uri) {
        if( uri.length() != 45 ) return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "text/plain", "Server error");
        String uuid = uri.substring(13);
        if( this.storage.getUserPassword(uuid) == null ) return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "text/plain", "Server error");
        List<MinecraftProperty> properties = Collections.singletonList(getTextures(uuid));
        String username = this.storage.getUsername(uuid);
        MinecraftProfilePropertiesResponse profile = new MinecraftProfilePropertiesResponse(uuid, username, properties);
        return newFixedLengthResponse(Response.Status.OK, "application/json", JsonStream.serialize(profile));
    }

    private Response handleHasJoinedRequest(IHTTPSession session) {
        try {
            String uuid = UserId.generateUserId(session.getParameters().get("username").get(0));
            if (this.sessions.containsKey(uuid) && this.sessions.get(uuid)) {
                this.sessions.remove(uuid);
                return handleProfileRequest(session, "/api/profile/"+uuid);
            }
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return invalidSession;
    }

    private Response handleProfileRegistration(IHTTPSession session) {
        try {
            Map<String, String> files = new HashMap<>();
            session.parseBody(files);
            SignUpRequest request = JsonIterator.deserialize(files.get("postData"), SignUpRequest.class);

            if (request == null) {
                return newFixedLengthResponse(Response.Status.BAD_REQUEST, "text/plain", "Invalid JSON format");
            }

            String username = request.getUsername();
            String password = request.getPassword();

            if (username == null || password == null || password.length() < 3 || username.length() < 3) {
                return newFixedLengthResponse(Response.Status.BAD_REQUEST, "text/plain", "Missing username or password");
            }

            String uuid = UserId.generateUserId(username);

            if( this.storage.getUserPassword(uuid) != null ) {
                return newFixedLengthResponse(Response.Status.CONFLICT, "text/plain", "User already exists");
            }

            this.storage.saveUser(uuid, new User(username, PasswordHasher.hashPassword(password), User.SkinModel.Steve));

            return newFixedLengthResponse(Response.Status.OK, "text/plain", JsonStream.serialize(new SignUpResponse(uuid)));
        } catch (Exception e) {
            logger.info("Error while processing sign up request from client: " + e.getMessage());
            return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "text/plain", "Server error: " + e.getMessage());
        }
    }

    private Response handleLoginRequest(IHTTPSession session) {
        try {
            Map<String, String> files = new HashMap<>();
            session.parseBody(files);
            SignUpRequest loginRequest = JsonIterator.deserialize(files.get("postData"), SignUpRequest.class);

            if (loginRequest == null) {
                return newFixedLengthResponse(Response.Status.BAD_REQUEST, "text/plain", "Invalid JSON format");
            }

            String username = loginRequest.getUsername();
            String password = loginRequest.getPassword();

            if (username == null || password == null) {
                return newFixedLengthResponse(Response.Status.BAD_REQUEST, "text/plain", "Missing username or password");
            }

            if( this.storage.getUserPasswordByName(username) == null ) {
                return newFixedLengthResponse(Response.Status.CONFLICT, "text/plain", "User doesn't exist");
            }

            boolean validSession = PasswordHasher.checkPassword(password, this.storage.getUserPasswordByName(username));

            if (validSession) {
                return newFixedLengthResponse(Response.Status.OK, "application/json", "{}");
            } else {
                return invalidSession;
            }
        } catch (Exception e) {
            logger.info("Error while processing join request from client: " + e.getMessage());
            return invalidSession;
        }
    }

    private Response handleJoinRequest(IHTTPSession session) {
        try {
            Map<String, String> files = new HashMap<>();
            session.parseBody(files);
            JoinMinecraftServerRequest joinRequest = JsonIterator.deserialize(files.get("postData"), JoinMinecraftServerRequest.class);

            if (joinRequest == null) {
                return invalidSession;
            }

            String username = joinRequest.getSelectedProfile();
            String sessionToken = joinRequest.getAccessToken();

            if (username == null || sessionToken == null) {
                return invalidSession;
            }

            boolean validSession = PasswordHasher.checkPassword(sessionToken, this.storage.getUserPassword(username));

            if (validSession) {
                this.sessions.put(username, true);
                return newFixedLengthResponse(Response.Status.OK, "application/json", "{}");
            } else {
                this.sessions.put(username, false);
                return invalidSession;
            }
        } catch (Exception e) {
            logger.info("Error while processing join request from client: " + e.getMessage());
            return invalidSession;
        }
    }

    @Override
    public void stop() {
        super.stop();
        this.storage.close();
    }
}
