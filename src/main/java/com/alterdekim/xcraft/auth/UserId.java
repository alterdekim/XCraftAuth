package com.alterdekim.xcraft.auth;

import java.util.UUID;

public class UserId {
    public static String generateUserId(String username) {
        return UUID.nameUUIDFromBytes(username.getBytes()).toString().replace("-", "");
    }
}
