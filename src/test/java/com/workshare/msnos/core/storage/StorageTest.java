package com.workshare.msnos.core.storage;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.workshare.msnos.core.Cloud;
import com.workshare.msnos.core.Iden;

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
    }

    @AfterClass 
    public static void termAll() throws IOException {
        home.delete();
    }

    @Before
    public void init() {
        cloud = mock(Cloud.class);
        when(cloud.getIden()).thenReturn(new Iden(Iden.Type.CLD, UUID.randomUUID()));
    }

    @Test
    public void shouldCreatRootStorageFolder() {
        File root = msnosRootFolder(); 
        delete(root);
        
        new Storage(cloud);

        assertFolderExists(root);
    }

    @Test
    public void shouldCreatCloudStorageFolder() {
        File clouddb = cloudDatabaseFile(); 
        delete(clouddb);

        new Storage(cloud);

        assertFileExists(clouddb);
    }

    @Test
    public void shouldCreateUUIDMap() {
        Storage storage = new Storage(cloud);
        assertNotNull(storage.getUUIDsStore());
    }
    
    @Test
    public void shouldPersistUUIDMap() throws IOException {
        final UUID uuid = cloud.getIden().getUUID();

        Storage storage = new Storage(cloud);
        Set<UUID> uuids = storage.getUUIDsStore();
        uuids.add(uuid);
        storage.close();

        uuids = new Storage(cloud).getUUIDsStore();
        assertTrue(uuids.contains(uuid));
    }
    
    @Test
    public void shouldCreateKeyvalStorage() {
        Storage storage = new Storage(cloud);
        assertNotNull(storage.getKeyvalStore());
    }
    
    @Test
    public void shouldPersistKeyvalStorage() throws IOException {
        Storage storage = new Storage(cloud);
        Map<String, Object> keyval = storage.getKeyvalStore();
        keyval.put("key", "value");
        storage.close();
        
        keyval = new Storage(cloud).getKeyvalStore();
        assertEquals("value",keyval.get("key"));
    }
    
    private File msnosRootFolder() {
        return new File(System.getProperty("user.home"), ".msnos");
    }
    
    private File cloudDatabaseFile() {
        return new File(System.getProperty("user.home"), ".msnos/"+cloud.getIden().getUUID());
    }
    
    private void assertFolderExists(File entry) {
        assertTrue(entry.exists());
        assertTrue(entry.isDirectory());
    }
    
    private void assertFileExists(File entry) {
        assertTrue(entry.exists());
        assertTrue(entry.isFile());
    }
    
    private void delete(final File folder) {
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
