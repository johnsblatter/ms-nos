package com.workshare.msnos.core.services.storage;

import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.HTreeMap;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class Storage implements Closeable {

    private final DB dbase;
    private final Set<UUID> uuids;
    private final Map<String, Object> keyval;

    public Storage(UUID uuid) {
        dbase = ensureDatabasePresent(uuid);
        uuids = ensureUuidsSetPresent(dbase);
        keyval = createKeyval(dbase);
    }

    private Map<String, Object> createKeyval(DB db) {
        HTreeMap<String, Object> res = db.getHashMap("kval");
        if (res == null)
            res = db.createHashMap("kval").expireMaxSize(999).make();
        return res;
    }

    private Set<UUID> ensureUuidsSetPresent(DB db) {
        Set<UUID> res = db.getHashSet("uuids");
        if (res == null)
            res = db.createHashSet("uuids").expireMaxSize(999).make();

        return res;
    }

    private DB ensureDatabasePresent(UUID uuid) {
        final File root = msnosRoot();
        synchronized (Storage.class) {
            if (!root.exists()) {
                root.mkdirs();
            }

            File dbFile = new File(root, uuid.toString());
            DB db = DBMaker.newFileDB(dbFile)
                    .mmapFileEnableIfSupported()
                    .asyncWriteEnable()
                    .closeOnJvmShutdown()
                    .transactionDisable()
                    .make();

            db.compact();
            return db;
        }

    }

    public Set<UUID> getUUIDsStore() {
        return uuids;
    }

    public Map<String, Object> getKeyvalStore() {
        return keyval;
    }

    @Override
    public void close() throws IOException {
        try {
            dbase.commit();
            dbase.compact();
        } finally {
            dbase.close();
        }
    }

    private static File msnosRoot() {
        return new File(System.getProperty("user.home"), ".msnos");
    }

}
