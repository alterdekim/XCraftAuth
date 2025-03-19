package com.alterdekim.xcraft.auth.response;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Map;


@Getter
@RequiredArgsConstructor
public class MinecraftProfileTexture {
    private final String url;
    private final Map<String, String> metadata;

    public enum Type {
        SKIN,
        CAPE;
    }
}

