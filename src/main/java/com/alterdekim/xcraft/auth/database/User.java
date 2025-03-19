package com.alterdekim.xcraft.auth.database;

import lombok.*;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class User {
    private String username;
    private String password;
    private SkinModel model;

    @Getter
    @RequiredArgsConstructor
    public enum SkinModel {
        Steve,
        Alex
    }
}
