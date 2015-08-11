package com.workshare.msnos.core.services.storage;

import com.workshare.msnos.core.Iden;
import com.workshare.msnos.core.cloud.Cloud;
import com.workshare.msnos.core.services.storage.Storage;

import org.junit.*;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SuppressWarnings("resource")
public class StorageTest {

    private static File home;

    private Cloud cloud;

    @BeforeClass
    public static void initAll() throws IOException {
        home = File.createTempFile("msnos-", ".tmp");
        home.delete();
        home.mkdirs();
        assertTrue(home.exists());
        assertTrue(home.isDirectory());

        System.setProperty("user.home", home.toString());
        System.out.println(home.toString());
    }

    @AfterClass
    public static void termAll() throws IOException {
        delete(home);
    }

    @Before
    public void init() {
        cloud = mock(Cloud.class);
        when(cloud.getIden()).thenReturn(new Iden(Iden.Type.CLD, UUID.randomUUID()));
    }

    @After
    public void cleanUp() {
        delete(home);
    }

    @Test
    public void shouldCreatRootStorageFolder() {
        File root = msnosRootFolder();
        delete(root);

        new Storage(cloud.getIden().getUUID());

        assertFolderExists(root);
    }

    @Test
    public void shouldCreatCloudStorageFolder() {
        File clouddb = cloudDatabaseFile();
        delete(clouddb);

        new Storage(cloud.getIden().getUUID());

        assertFileExists(clouddb);
    }

    @Test
    public void shouldCreateUUIDMap() {
        Storage storage = new Storage(cloud.getIden().getUUID());
        assertNotNull(storage.getUUIDsStore());
    }

    @Test
    public void shouldPersistUUIDMap() throws IOException {
        final UUID uuid = cloud.getIden().getUUID();

        Storage storage = new Storage(cloud.getIden().getUUID());
        Set<UUID> uuids = storage.getUUIDsStore();
        uuids.add(uuid);
        storage.close();

        uuids = new Storage(cloud.getIden().getUUID()).getUUIDsStore();
        assertTrue(uuids.contains(uuid));
    }

    @Test
    public void shouldCreateKeyvalStorage() {
        Storage storage = new Storage(cloud.getIden().getUUID());
        assertNotNull(storage.getKeyvalStore());
    }

    @Test
    public void shouldPersistKeyvalStorage() throws IOException {
        Storage storage = new Storage(cloud.getIden().getUUID());
        Map<String, Object> keyval = storage.getKeyvalStore();
        keyval.put("key", "value");
        storage.close();

        keyval = new Storage(cloud.getIden().getUUID()).getKeyvalStore();
        assertEquals("value", keyval.get("key"));
    }

    private File msnosRootFolder() {
        return new File(System.getProperty("user.home"), ".msnos");
    }

    private File cloudDatabaseFile() {
        return new File(System.getProperty("user.home"), ".msnos/" + cloud.getIden().getUUID());
    }

    private void assertFolderExists(File entry) {
        assertTrue(entry.exists());
        assertTrue(entry.isDirectory());
    }

    private void assertFileExists(File entry) {
        assertTrue(entry.exists());
        assertTrue(entry.isFile());
    }

    private static void delete(final File folder) {
        if (folder.isDirectory()) {
            File[] files = folder.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        delete(file);
                    }
                    file.delete();
                }
            }

            if (!folder.delete())
                throw new RuntimeException("Cannot delete folder!");
        }
    }
}
