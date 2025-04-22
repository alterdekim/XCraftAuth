package com.alterdekim.xcraft.auth.database;

import com.alterdekim.xcraft.auth.UserId;
import com.jsoniter.JsonIterator;
import com.jsoniter.output.JsonStream;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.Serializer;

import java.io.File;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.ConcurrentMap;

public class UserStorage {
    private static final File DB_FILE = new File("plugins/XCraftAuth/users.db");
    private final DB db = DBMaker.fileDB(DB_FILE).fileMmapEnableIfSupported().checksumHeaderBypass().make();
    private final ConcurrentMap<String, String> users = db.hashMap("users", Serializer.STRING, Serializer.STRING).createOrOpen();

    public void saveUser(String uuid, User user) {
        this.users.put(uuid, JsonStream.serialize(user));
    }

    public User.SkinModel getSkinModel(String uuid) {
        if( this.users.containsKey(uuid) ) {
            User user = JsonIterator.deserialize(this.users.get(uuid), User.class);
            return user.getModel();
        }
        return User.SkinModel.Steve;
    }

    public String getUsername(String uuid) {
        if( this.users.containsKey(uuid) ) {
            User user = JsonIterator.deserialize(this.users.get(uuid), User.class);
            return user.getUsername();
        }
        return null;
    }

    public String getUserPassword(String uuid) {
        if (this.users.containsKey(uuid)) {
            User user = JsonIterator.deserialize(this.users.get(uuid), User.class);
            return user.getPassword();
        }
        return null;
    }

    public String getUserPasswordByName(String username) throws NoSuchAlgorithmException {
        return getUserPassword(UserId.generateUserId(username));
    }

    public void close() {
        db.close();
    }

    public void setSkinModel(String uuid, User.SkinModel skinModel) {
        if (this.users.containsKey(uuid)) {
            User user = JsonIterator.deserialize(this.users.get(uuid), User.class);
            user.setModel(skinModel);
            this.saveUser(uuid, user);
        }
    }
}
