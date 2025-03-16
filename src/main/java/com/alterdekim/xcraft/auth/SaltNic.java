package com.alterdekim.xcraft.auth;

import fi.iki.elonen.NanoHTTPD;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.mindrot.jbcrypt.BCrypt;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import static com.alterdekim.xcraft.auth.XCraft.SERVER_PORT;

public class SaltNic extends NanoHTTPD {

    private final Logger logger;

    private final Map<String, Boolean> sessions;

    public SaltNic(Logger logger) throws IOException {
        super(SERVER_PORT);
        this.logger = logger;
        this.sessions = new HashMap<>();
        start(NanoHTTPD.SOCKET_READ_TIMEOUT, false);
        logger.info("SaltNic session server started on http://localhost:"+SERVER_PORT);
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
        } else if (uri.startsWith("/api/register") && method == Method.POST) {
            return handleProfileRegistration(session);
        }
        return newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", "404 Not Found");
    }

    private Response handleProfileRequest(IHTTPSession session, String uri) {
        if( uri.length() != 45 ) return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "text/plain", "Server error");
        String uuid = uri.substring(13);
        if( UserStorage.getUserPassword(uuid) == null ) return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "text/plain", "Server error");
        return newFixedLengthResponse(Response.Status.OK, "application/json", "{\n" +
                "  \"id\" : \""+uuid+"\",\n" +
                "  \"name\" : \"Notch\",\n" +
                "  \"properties\" : [ {\n" +
                "    \"name\" : \"textures\",\n" +
                "    \"value\" : \"ewogICJ0aW1lc3RhbXAiIDogMTc0MjA1ODQ1MDI1MywKICAicHJvZmlsZUlkIiA6ICJmYzE0MzZmZmQ3MDA0NWFmOWMxODNkZjhjODMwMmU5ZiIsCiAgInByb2ZpbGVOYW1lIiA6ICJEYXJ0SmV2ZGVyIiwKICAidGV4dHVyZXMiIDogewogICAgIlNLSU4iIDogewogICAgICAidXJsIiA6ICJodHRwOi8vdGV4dHVyZXMubWluZWNyYWZ0Lm5ldC90ZXh0dXJlLzVlMTM1Y2ZkYTgwM2U3ZDQ4NTNhN2M5YjQ5N2JhZjM3YWNlNmZkZGYyYjYyNDI1MWY3YjkwNmYyOTAwZWRiMyIsCiAgICAgICJtZXRhZGF0YSIgOiB7CiAgICAgICAgIm1vZGVsIiA6ICJzbGltIgogICAgICB9CiAgICB9LAogICAgIkNBUEUiIDogewogICAgICAidXJsIiA6ICJodHRwOi8vdGV4dHVyZXMubWluZWNyYWZ0Lm5ldC90ZXh0dXJlL2EyZThkOTdlYzc5MTAwZTkwYTc1ZDM2OWQxYjNiYTgxMjczYzRmODJiYzFiNzM3ZTkzNGVlZDRhODU0YmUxYjYiCiAgICB9CiAgfQp9\"\n" +
                "  } ],\n" +
                "  \"profileActions\" : [ ]\n" +
                "}");
    }

    private Response handleHasJoinedRequest(IHTTPSession session) {
        String uuid = UserId.generateUserId(session.getParameters().get("username").get(0));
        if( this.sessions.containsKey(uuid) && this.sessions.get(uuid) ) {
            this.sessions.remove(uuid);
            return newFixedLengthResponse(Response.Status.OK, "application/json", "{\n" +
                    "  \"id\" : \""+uuid+"\",\n" +
                    "  \"name\" : \""+session.getParameters().get("username").get(0)+"\",\n" +
                    "  \"properties\" : [ {\n" +
                    "    \"name\" : \"textures\",\n" +
                    "    \"value\" : \"ewogICJ0aW1lc3RhbXAiIDogMTc0MjA1ODQ1MDI1MywKICAicHJvZmlsZUlkIiA6ICJmYzE0MzZmZmQ3MDA0NWFmOWMxODNkZjhjODMwMmU5ZiIsCiAgInByb2ZpbGVOYW1lIiA6ICJEYXJ0SmV2ZGVyIiwKICAidGV4dHVyZXMiIDogewogICAgIlNLSU4iIDogewogICAgICAidXJsIiA6ICJodHRwOi8vdGV4dHVyZXMubWluZWNyYWZ0Lm5ldC90ZXh0dXJlLzVlMTM1Y2ZkYTgwM2U3ZDQ4NTNhN2M5YjQ5N2JhZjM3YWNlNmZkZGYyYjYyNDI1MWY3YjkwNmYyOTAwZWRiMyIsCiAgICAgICJtZXRhZGF0YSIgOiB7CiAgICAgICAgIm1vZGVsIiA6ICJzbGltIgogICAgICB9CiAgICB9LAogICAgIkNBUEUiIDogewogICAgICAidXJsIiA6ICJodHRwOi8vdGV4dHVyZXMubWluZWNyYWZ0Lm5ldC90ZXh0dXJlL2EyZThkOTdlYzc5MTAwZTkwYTc1ZDM2OWQxYjNiYTgxMjczYzRmODJiYzFiNzM3ZTkzNGVlZDRhODU0YmUxYjYiCiAgICB9CiAgfQp9\"\n" +
                    "  } ],\n" +
                    "  \"profileActions\" : [ ]\n" +
                    "}");
        }
        return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "text/plain", "Server error");
    }

    private Response handleProfileRegistration(IHTTPSession session) {
        try {
            Map<String, String> files = new HashMap<>();
            session.parseBody(files);
            JSONObject json = parseJSON(files.get("postData"));

            if (json == null) {
                return newFixedLengthResponse(Response.Status.BAD_REQUEST, "text/plain", "Invalid JSON format");
            }

            String username = (String) json.get("username");
            String password = (String) json.get("password");

            if (username == null || password == null) {
                return newFixedLengthResponse(Response.Status.BAD_REQUEST, "text/plain", "Missing username or password");
            }

            String uuid = UserId.generateUserId(username);

            if( UserStorage.getUserPassword(uuid) != null ) {
                return newFixedLengthResponse(Response.Status.CONFLICT, "text/plain", "User already exists");
            }

            UserStorage.saveUser(uuid, PasswordHasher.hashPassword(password));

            JSONObject response = new JSONObject();
            response.put("uuid", uuid);
            return newFixedLengthResponse(Response.Status.OK, "text/plain", response.toJSONString());
        } catch (Exception e) {
            logger.warning("Error while processing sign up request from client: " + e.getMessage());
            return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "text/plain", "Server error: " + e.getMessage());
        }
    }

    private Response handleJoinRequest(IHTTPSession session) {
        try {
            Map<String, String> files = new HashMap<>();
            session.parseBody(files);
            JSONObject json = parseJSON(files.get("postData"));

            if (json == null) {
                return newFixedLengthResponse(Response.Status.BAD_REQUEST, "text/plain", "Invalid JSON format");
            }

            String username = (String) json.get("selectedProfile");
            String sessionToken = (String) json.get("accessToken");

            if (username == null || sessionToken == null) {
                return newFixedLengthResponse(Response.Status.BAD_REQUEST, "text/plain", "Missing selectedProfile or accessToken");
            }

            boolean validSession = PasswordHasher.checkPassword(sessionToken, UserStorage.getUserPassword(username));

            if (validSession) {
                this.sessions.put(username, true);
                return newFixedLengthResponse(Response.Status.OK, "application/json", "{}");
            } else {
                this.sessions.put(username, false);
                return newFixedLengthResponse(Response.Status.UNAUTHORIZED, "application/json","{\"status\":\"error\", \"message\":\"Invalid session token\"}");
            }
        } catch (Exception e) {
            logger.warning("Error while processing join request from client: " + e.getMessage());
            return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "text/plain", "Server error: " + e.getMessage());
        }
    }

    private JSONObject parseJSON(String jsonData) {
        try {
            JSONParser parser = new JSONParser();
            return (JSONObject) parser.parse(jsonData);
        } catch (ParseException e) {
            e.printStackTrace();
            return null;
        }
    }
}
