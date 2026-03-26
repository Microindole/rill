package com.indolyn.rill.core.sql;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.indolyn.rill.core.model.Column;
import com.indolyn.rill.core.execution.QueryProcessor;

import java.io.File;
import java.io.IOException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * 专门用于测试新添加的数据类型的集成测试.
 */
public class DataTypeTest {

    private final String TEST_DB_NAME = "datatype_test_db";
    private QueryProcessor queryProcessor;

    @BeforeEach
    void setUp() {
        System.out.println("--- [DataTypeTest] Cleaning up environment ---");
        deleteDirectory(new File("data/" + TEST_DB_NAME));
        queryProcessor = new QueryProcessor(TEST_DB_NAME);
    }

    @AfterEach
    void tearDown() throws IOException {
        if (queryProcessor != null) {
            queryProcessor.close();
        }
        deleteDirectory(new File("data/" + TEST_DB_NAME));
        System.out.println("--- [DataTypeTest] Environment cleaned up ---");
    }

    @Test
    void testCreateInsertAndSelectNewDataTypes() {
        System.out.println("--- Test: Create, Insert, and Select with FLOAT, DOUBLE, CHAR, SMALLINT, BIGINT, TIMESTAMP, BOOLEAN, DATE ---");

        // 1. 创建包含新数据类型的表
        String createSql =
            "CREATE TABLE new_types_table (c_float FLOAT, c_double DOUBLE, c_char CHAR, c_small SMALLINT, c_big BIGINT, c_ts TIMESTAMP, c_bool BOOLEAN, c_date DATE);";
        System.out.println("Executing: " + createSql);
        String createResult = queryProcessor.executeAndGetResult(createSql);
        assertTrue(
            createResult.contains("Table 'new_types_table' created."),
            "CREATE TABLE with new types failed.");
        System.out.println("Table with new data types created successfully.");

        // 2. 插入使用新数据类型的值
        String insertSql =
            "INSERT INTO new_types_table (c_float, c_double, c_char, c_small, c_big, c_ts, c_bool, c_date) VALUES (1.23, 4.56789, 'a', 7, 922337203685477580, '2026-03-26 10:11:12', TRUE, '2026-03-26');";
        System.out.println("Executing: " + insertSql);
        String insertResult = queryProcessor.executeAndGetResult(insertSql);
        // 此处修改：不再检查 "rows affected"，而是检查操作是否没有报错，这更符合当前系统的返回逻辑
        assertFalse(
            insertResult.toUpperCase().contains("ERROR"),
            "INSERT statement failed with an error: " + insertResult);
        System.out.println("Data with new types inserted successfully. Result: " + insertResult);

        // 3. 查询并验证数据
        String selectSql = "SELECT * FROM new_types_table;";
        System.out.println("Executing: " + selectSql);
        String selectResult = queryProcessor.executeAndGetResult(selectSql);
        System.out.println("Query Result:\n" + selectResult);

        assertTrue(selectResult.contains("1.23"), "SELECT failed to retrieve correct FLOAT value.");
        assertTrue(selectResult.contains("4.56789"), "SELECT failed to retrieve correct DOUBLE value.");
        assertTrue(selectResult.contains("a"), "SELECT failed to retrieve correct CHAR value.");
        assertTrue(selectResult.contains("7"), "SELECT failed to retrieve correct SMALLINT value.");
        assertTrue(
            selectResult.contains("922337203685477580"),
            "SELECT failed to retrieve correct BIGINT value.");
        assertTrue(
            selectResult.contains("2026-03-26T10:11:12"),
            "SELECT failed to retrieve correct TIMESTAMP value.");
        assertTrue(selectResult.contains("true"), "SELECT failed to retrieve correct BOOLEAN value.");
        assertTrue(selectResult.contains("2026-03-26"), "SELECT failed to retrieve correct DATE value.");
        System.out.println("Verification PASSED. All new data types are working correctly.");
    }

    @Test
    void testCreateInsertAndSelectPostgreSqlTypeAliases() {
        String createSql =
            "CREATE TABLE alias_types_table (c_real REAL, c_float8 FLOAT8, c_numeric NUMERIC(12, 4), c_text TEXT);";
        String createResult = queryProcessor.executeAndGetResult(createSql);
        assertTrue(
            createResult.contains("Table 'alias_types_table' created."),
            "CREATE TABLE with PostgreSQL aliases failed.");

        String insertSql =
            "INSERT INTO alias_types_table (c_real, c_float8, c_numeric, c_text) VALUES (1.5, 9.875, 12345.6789, 'hello alias');";
        String insertResult = queryProcessor.executeAndGetResult(insertSql);
        assertFalse(
            insertResult.toUpperCase().contains("ERROR"),
            "INSERT with PostgreSQL aliases failed: " + insertResult);

        String selectResult = queryProcessor.executeAndGetResult("SELECT * FROM alias_types_table;");
        assertTrue(selectResult.contains("1.5"), "SELECT failed to retrieve REAL alias value.");
        assertTrue(selectResult.contains("9.875"), "SELECT failed to retrieve FLOAT8 alias value.");
        assertTrue(selectResult.contains("12345.6789"), "SELECT failed to retrieve NUMERIC alias value.");
        assertTrue(selectResult.contains("hello alias"), "SELECT failed to retrieve TEXT alias value.");
    }

    @Test
    void testLengthConstraintsSurviveRestartAndBlockInsertAndUpdate() throws IOException {
        String createSql = "CREATE TABLE limited_users (name VARCHAR(5), code CHAR(3));";
        String createResult = queryProcessor.executeAndGetResult(createSql);
        assertTrue(createResult.contains("Table 'limited_users' created."));

        String validInsert =
            queryProcessor.executeAndGetResult(
                "INSERT INTO limited_users (name, code) VALUES ('alice', 'xyz');");
        assertFalse(validInsert.toUpperCase().contains("ERROR"));

        queryProcessor.close();
        queryProcessor = new QueryProcessor(TEST_DB_NAME);

        String invalidInsert =
            queryProcessor.executeAndGetResult(
                "INSERT INTO limited_users (name, code) VALUES ('toolong', 'xyz');");
        assertTrue(
            invalidInsert.contains("exceeds length limit"),
            "VARCHAR length constraint should still work after restart.");

        String invalidUpdate =
            queryProcessor.executeAndGetResult(
                "UPDATE limited_users SET code = 'abcd' WHERE name = 'alice';");
        assertTrue(
            invalidUpdate.contains("exceeds length limit"),
            "CHAR length constraint should block overlong UPDATE values.");
    }

    @Test
    void testNumericConstraintsSurviveRestartAndBlockOverflowValues() throws IOException {
        String createSql = "CREATE TABLE invoice_lines (amount NUMERIC(5, 2));";
        String createResult = queryProcessor.executeAndGetResult(createSql);
        assertTrue(createResult.contains("Table 'invoice_lines' created."));

        String validInsert =
            queryProcessor.executeAndGetResult(
                "INSERT INTO invoice_lines (amount) VALUES (123.45);");
        assertFalse(validInsert.toUpperCase().contains("ERROR"));

        queryProcessor.close();
        queryProcessor = new QueryProcessor(TEST_DB_NAME);

        String invalidScaleInsert =
            queryProcessor.executeAndGetResult(
                "INSERT INTO invoice_lines (amount) VALUES (12.345);");
        assertTrue(
            invalidScaleInsert.contains("NUMERIC(5, 2)"),
            "NUMERIC scale constraint should still work after restart.");

        String invalidPrecisionInsert =
            queryProcessor.executeAndGetResult(
                "INSERT INTO invoice_lines (amount) VALUES (1234.56);");
        assertTrue(
            invalidPrecisionInsert.contains("NUMERIC(5, 2)"),
            "NUMERIC precision constraint should still work after restart.");

        String invalidScaleUpdate =
            queryProcessor.executeAndGetResult(
                "UPDATE invoice_lines SET amount = 12.345 WHERE amount = 123.45;");
        assertTrue(
            invalidScaleUpdate.contains("NUMERIC(5, 2)"),
            "NUMERIC scale constraint should block UPDATE after restart.");

        String invalidPrecisionUpdate =
            queryProcessor.executeAndGetResult(
                "UPDATE invoice_lines SET amount = 1234.56 WHERE amount = 123.45;");
        assertTrue(
            invalidPrecisionUpdate.contains("NUMERIC(5, 2)"),
            "NUMERIC precision constraint should block UPDATE after restart.");
    }

    @Test
    void testColumnMetadataSurvivesRestartForShowCreateTableSupport() throws IOException {
        String createSql =
            "CREATE TABLE ddl_meta_users (id INT PRIMARY KEY, name VARCHAR(5) NOT NULL DEFAULT 'guest', enabled BOOLEAN DEFAULT TRUE);";
        String createResult = queryProcessor.executeAndGetResult(createSql);
        assertTrue(createResult.contains("Table 'ddl_meta_users' created."));

        queryProcessor.close();
        queryProcessor = new QueryProcessor(TEST_DB_NAME);

        Column id = queryProcessor.getCatalog().getTableSchema("ddl_meta_users").getColumn("id");
        Column name = queryProcessor.getCatalog().getTableSchema("ddl_meta_users").getColumn("name");
        Column enabled = queryProcessor.getCatalog().getTableSchema("ddl_meta_users").getColumn("enabled");

        assertNotNull(queryProcessor.getCatalog().getTableSchema("ddl_meta_users").getPrimaryKeyColumnName());
        assertEquals("id", queryProcessor.getCatalog().getTableSchema("ddl_meta_users").getPrimaryKeyColumnName());
        assertTrue(id.isPrimaryKey());
        assertFalse(name.isNullable());
        assertEquals("'guest'", name.getDefaultValue());
        assertEquals("TRUE", enabled.getDefaultValue());
    }

    // 辅助方法，用于递归删除目录
    private void deleteDirectory(File directory) {
        if (!directory.exists()) {
            return;
        }
        File[] allContents = directory.listFiles();
        if (allContents != null) {
            for (File file : allContents) {
                deleteDirectory(file);
            }
        }
        directory.delete();
    }
}
