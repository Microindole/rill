package com.indolyn.rill.core.catalog;

import static org.junit.jupiter.api.Assertions.*;

import com.indolyn.rill.core.model.Column;
import com.indolyn.rill.core.model.DataType;
import com.indolyn.rill.core.model.Schema;
import com.indolyn.rill.core.storage.buffer.BufferPoolManager;
import com.indolyn.rill.core.storage.disk.DiskManager;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CatalogTest {

    private final String TEST_DB_FILE = "test_catalog.db";
    private DiskManager diskManager;
    private BufferPoolManager bufferPoolManager;
    private Catalog catalog;

    @BeforeEach
    void setUp() throws IOException {
        new File(TEST_DB_FILE).delete();
        diskManager = new DiskManager(TEST_DB_FILE);
        diskManager.open();
        bufferPoolManager = new BufferPoolManager(10, diskManager, "LRU");
        catalog = new Catalog(bufferPoolManager);
    }

    @AfterEach
    void tearDown() throws IOException {
        diskManager.close();
        new File(TEST_DB_FILE).delete();
    }

    @Test
    void createTableShouldRegisterAndExposeSchema() throws IOException {
        Schema studentSchema =
            new Schema(
                List.of(new Column("id", DataType.INT), new Column("name", DataType.VARCHAR)));

        String tableName = "student";
        catalog.createTable(tableName, studentSchema);

        TableInfo retrievedTable = catalog.getTable(tableName);
        assertNotNull(retrievedTable);
        assertEquals(tableName, retrievedTable.getTableName());
        assertEquals(2, retrievedTable.getSchema().getColumns().size());
        assertEquals("id", retrievedTable.getSchema().getColumns().get(0).getName());
        assertEquals(DataType.VARCHAR, retrievedTable.getSchema().getColumns().get(1).getType());
    }

    @Test
    void catalogShouldReloadTablesAndPrivilegesFromDisk() throws IOException {
        Schema schema = new Schema(List.of(new Column("data", DataType.VARCHAR)));
        catalog.createTable("my_table", schema);
        catalog.createUser("app_user", "password");
        catalog.grantPrivilege("app_user", "my_table", "SELECT");

        bufferPoolManager.flushAllPages();
        diskManager.close();

        diskManager = new DiskManager(TEST_DB_FILE);
        diskManager.open();
        bufferPoolManager = new BufferPoolManager(10, diskManager, "LRU");
        catalog = new Catalog(bufferPoolManager);

        TableInfo tableInfo = catalog.getTable("my_table");
        assertNotNull(tableInfo);
        assertEquals("my_table", tableInfo.getTableName());
        assertNotNull(catalog.getPasswordHash("app_user"));
        assertTrue(catalog.hasPermission("app_user", "my_table", "SELECT"));
    }
}
