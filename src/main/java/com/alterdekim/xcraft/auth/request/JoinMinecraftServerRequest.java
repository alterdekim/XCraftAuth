package com.alterdekim.xcraft.auth.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@Getter
@NoArgsConstructor
public class JoinMinecraftServerRequest {
    public String accessToken;
    public String selectedProfile;
    public String serverId;
}
