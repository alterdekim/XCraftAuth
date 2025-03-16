package com.alterdekim.xcraft.auth;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class UserStorage {
    private static final File USER_FILE = new File("plugins/XCraftAuth/users.json");

    public static void saveUser(String username, String hashedPassword) {
        JSONObject users = loadUsers();
        users.put(username, hashedPassword);

        try (FileWriter writer = new FileWriter(USER_FILE)) {
            writer.write(users.toJSONString());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String getUserPassword(String username) {
        JSONObject users = loadUsers();
        return (String) users.get(username);
    }

    private static JSONObject loadUsers() {
        if (!USER_FILE.exists()) {
            return new JSONObject();
        }

        try (FileReader reader = new FileReader(USER_FILE)) {
            JSONParser parser = new JSONParser();
            return (JSONObject) parser.parse(reader);
        } catch (IOException | ParseException e) {
            return new JSONObject();
        }
    }
}
