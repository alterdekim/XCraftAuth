package com.alterdekim.xcraft.auth.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Map;

@AllArgsConstructor
@Getter
public class MinecraftTexturesPayload {
    private long timestamp;
    private String profileId;
    private String profileName;
    private Map<MinecraftProfileTexture.Type, MinecraftProfileTexture> textures;
}