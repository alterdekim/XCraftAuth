package com.alterdekim.xcraft.auth.lib;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.util.logging.Logger;

public class Patcher {

    public static void patchAuthLib(Logger logger, int internal_port) throws Exception {
        Class<?> clazz = Class.forName("com.mojang.authlib.yggdrasil.YggdrasilMinecraftSessionService");
        modifyFinalField(clazz, "BASE_URL", "http://localhost:"+internal_port+"/api/", logger);
        modifyFinalField(clazz, "JOIN_URL", new URL("http://localhost:"+internal_port+"/api/join"), logger);
        modifyFinalField(clazz, "CHECK_URL", new URL("http://localhost:"+internal_port+"/api/hasJoined"), logger);
    }

    private static void modifyFinalField(Class<?> clazz, String fieldName, Object newValue, Logger logger) throws Exception {
        Field field = clazz.getDeclaredField(fieldName);
        field.setAccessible(true);

        Field modifiersField = Field.class.getDeclaredField("modifiers");
        modifiersField.setAccessible(true);
        modifiersField.setInt(field, field.getModifiers() & ~Modifier.FINAL);

        field.set(null, newValue);
        logger.info(fieldName + " patched to: " + newValue);
    }
}
