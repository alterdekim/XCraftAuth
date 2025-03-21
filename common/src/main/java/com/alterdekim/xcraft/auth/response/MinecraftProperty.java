package com.alterdekim.xcraft.auth.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class MinecraftProperty {
    private String name;
    private String value;
    private String signature;
}
