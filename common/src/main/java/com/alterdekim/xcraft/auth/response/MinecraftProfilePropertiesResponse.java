package com.alterdekim.xcraft.auth.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@AllArgsConstructor
@Getter
public class MinecraftProfilePropertiesResponse {
    private String id;
    private String name;
    private List<MinecraftProperty> properties;
}