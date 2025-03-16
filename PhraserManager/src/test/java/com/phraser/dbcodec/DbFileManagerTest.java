package com.phraser.dbcodec;

import com.phraser.db.Block;
import com.phraser.forms.DefaultDBCreator;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DbFileManagerTest {
    static final String PASSWORD = "qwerty";

    @Test
    public void testSaveLoad() throws IOException, InvalidKeySpecException, NoSuchAlgorithmException {
        List<Block> db = DefaultDBCreator.initDefaultBlockConfigWithPhrase("MyDB");

        File f = File.createTempFile("phraser-tmp", ".phr");
        DbFileManager.writeBlocksToFile(db, PASSWORD, f);

        List<Block> db2 = DbFileManager.loadBlocksFromFile(PASSWORD, f);

        assertEquals(db.size(), db2.size());

        for (Block block : db) {
            assertTrue(db2.contains(block));
        }
    }
}
