package com.indolyn.rill.core.catalog;

import com.indolyn.rill.core.storage.database.DatabaseManager;
import com.indolyn.rill.core.model.*;
import com.indolyn.rill.core.storage.buffer.BufferPoolManager;
import com.indolyn.rill.core.storage.disk.DiskManager;
import com.indolyn.rill.core.storage.page.Page;
import com.indolyn.rill.core.storage.page.PageId;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * 系统目录，负责管理数据库的所有元数据（表、列等）。 目录本身也作为特殊的表存储在磁盘上。
 */
public class Catalog {
    private final BufferPoolManager bufferPoolManager;
    // 内存中的缓存
    private final Map<String, TableInfo> tables;
    private final Map<String, Integer> tableIds;
    private final AtomicInteger nextTableId;
    private final PermissionRegistry permissionRegistry;
    private final CatalogMetadataStore metadataStore;
    private final IndexRegistry indexRegistry;
    private final UserDirectoryStore userDirectoryStore;
    // --- 元数据表的特殊定义 ---
    // 存储所有表的信息 (table_id, table_name, first_page_id)
    public static final String CATALOG_TABLES_TABLE_NAME = "_catalog_tables";
    private final Schema tablesTableSchema;
    private PageId tablesTableFirstPageId;

    // 存储所有列的信息 (table_id, column_name, column_type, column_index)
    public static final String CATALOG_COLUMNS_TABLE_NAME = "_catalog_columns";
    private final Schema columnsTableSchema;
    private PageId columnsTableFirstPageId;

    // 用户和权限表的定义
    public static final String CATALOG_USERS_TABLE_NAME = "_catalog_users";
    private final Schema usersTableSchema;
    private PageId usersTableFirstPageId;

    public static final String CATALOG_PRIVILEGES_TABLE_NAME = "_catalog_privileges";
    private final Schema privilegesTableSchema;
    private PageId privilegesTableFirstPageId;

    public Catalog(BufferPoolManager bufferPoolManager) throws IOException {
        this.bufferPoolManager = bufferPoolManager;
        this.tables = new ConcurrentHashMap<>();
        this.tableIds = new ConcurrentHashMap<>();
        this.nextTableId = new AtomicInteger(0);
        this.permissionRegistry = new PermissionRegistry();
        this.metadataStore = new CatalogMetadataStore(bufferPoolManager);
        this.indexRegistry = new IndexRegistry();
        this.userDirectoryStore = new UserDirectoryStore(bufferPoolManager, permissionRegistry);

        // 定义元数据表的 Schema
        this.tablesTableSchema =
            new Schema(
                Arrays.asList(
                    new Column("table_id", DataType.INT),
                    new Column("table_name", DataType.VARCHAR),
                    new Column("first_page_id", DataType.INT)));
        this.columnsTableSchema =
            new Schema(
                Arrays.asList(
                    new Column("table_id", DataType.INT),
                    new Column("column_name", DataType.VARCHAR),
                    new Column("column_type", DataType.VARCHAR), // DataType.toString()
                    new Column("column_index", DataType.INT)));
        this.usersTableSchema =
            new Schema(
                Arrays.asList(
                    new Column("user_id", DataType.INT),
                    new Column("user_name", DataType.VARCHAR),
                    new Column("password_hash", DataType.VARCHAR) // Store hash instead of plain text
                ));
        this.privilegesTableSchema =
            new Schema(
                Arrays.asList(
                    new Column("privilege_id", DataType.INT),
                    new Column("user_id", DataType.INT),
                    new Column("table_name", DataType.VARCHAR),
                    new Column("privilege_type", DataType.VARCHAR) // e.g., "SELECT", "INSERT", "ALL"
                ));

        loadCatalog();
    }

    public List<String> getAllTableNames() {
        return new ArrayList<>(tables.keySet());
    }

    private void loadCatalog() throws IOException {
        tablesTableFirstPageId = new PageId(0);
        Page tablesPage = bufferPoolManager.getPage(tablesTableFirstPageId);

        if (tablesPage.getAllTuples(tablesTableSchema).isEmpty()) {
            bootstrap();
            return;
        }

        List<Tuple> tableMetadata = tablesPage.getAllTuples(tablesTableSchema);
        loadTableCatalogPointers(tableMetadata);
        loadTableDefinitions();
        loadUsers();
        loadPrivileges();
    }

    private void loadTableCatalogPointers(List<Tuple> tableMetadata) {
        int maxTableId = -1;
        for (Tuple tuple : tableMetadata) {
            int tableId = (int) tuple.getValues().get(0).getValue();
            String tableName = (String) tuple.getValues().get(1).getValue();
            int firstPageId = (int) tuple.getValues().get(2).getValue();

            maxTableId = Math.max(maxTableId, tableId);
            tableIds.put(tableName, tableId);
            switch (tableName) {
                case CATALOG_COLUMNS_TABLE_NAME -> columnsTableFirstPageId = new PageId(firstPageId);
                case CATALOG_USERS_TABLE_NAME -> usersTableFirstPageId = new PageId(firstPageId);
                case CATALOG_PRIVILEGES_TABLE_NAME -> privilegesTableFirstPageId = new PageId(firstPageId);
                default -> {
                }
            }
        }
        nextTableId.set(maxTableId + 1);
    }

    private void loadTableDefinitions() throws IOException {
        for (String tableName : tableIds.keySet()) {
            int tableId = tableIds.get(tableName);
            List<Column> columns =
                metadataStore.readColumnsForTable(tableId, columnsTableFirstPageId, columnsTableSchema);
            int firstPageId =
                (int)
                    metadataStore
                        .getTableTuple(tableName, tableId, tablesTableFirstPageId, tablesTableSchema)
                        .getValues()
                        .get(2)
                        .getValue();
            tables.put(tableName, new TableInfo(tableName, new Schema(columns), new PageId(firstPageId)));
        }
    }

    private void loadUsers() throws IOException {
        userDirectoryStore.loadUsers(usersTableFirstPageId, usersTableSchema);
    }

    private void loadPrivileges() throws IOException {
        userDirectoryStore.loadPrivileges(
            usersTableFirstPageId,
            usersTableSchema,
            privilegesTableFirstPageId,
            privilegesTableSchema);
    }

    private void bootstrap() throws IOException {
        PageId pageZero = bufferPoolManager.newPage().getPageId();
        if (pageZero.getPageNum() != 0) {
            throw new IllegalStateException(
                "Bootstrap failed: Expected to allocate Page 0, but got Page " + pageZero.getPageNum());
        }
        columnsTableFirstPageId = bufferPoolManager.newPage().getPageId();
        if (columnsTableFirstPageId.getPageNum() != 1) {
            throw new IllegalStateException(
                "Catalog bootstrap failed: _catalog_columns was not on Page 1, got "
                    + columnsTableFirstPageId.getPageNum());
        }
        usersTableFirstPageId = bufferPoolManager.newPage().getPageId();
        privilegesTableFirstPageId = bufferPoolManager.newPage().getPageId();

        int tablesTableId = nextTableId.getAndIncrement();
        int columnsTableId = nextTableId.getAndIncrement();
        int usersTableId = nextTableId.getAndIncrement();
        int privilegesTableId = nextTableId.getAndIncrement();

        metadataStore.persistTableEntry(
            tablesTableFirstPageId,
            tablesTableId,
            CATALOG_TABLES_TABLE_NAME,
            tablesTableFirstPageId,
            tablesTableSchema);
        metadataStore.persistTableEntry(
            tablesTableFirstPageId,
            columnsTableId,
            CATALOG_COLUMNS_TABLE_NAME,
            columnsTableFirstPageId,
            tablesTableSchema);
        metadataStore.persistTableEntry(
            tablesTableFirstPageId,
            usersTableId,
            CATALOG_USERS_TABLE_NAME,
            usersTableFirstPageId,
            tablesTableSchema);
        metadataStore.persistTableEntry(
            tablesTableFirstPageId,
            privilegesTableId,
            CATALOG_PRIVILEGES_TABLE_NAME,
            privilegesTableFirstPageId,
            tablesTableSchema);

        metadataStore.writeSchemaToColumnsTable(columnsTableFirstPageId, tablesTableId, tablesTableSchema);
        metadataStore.writeSchemaToColumnsTable(columnsTableFirstPageId, columnsTableId, columnsTableSchema);
        metadataStore.writeSchemaToColumnsTable(columnsTableFirstPageId, usersTableId, usersTableSchema);
        metadataStore.writeSchemaToColumnsTable(
            columnsTableFirstPageId, privilegesTableId, privilegesTableSchema);

        userDirectoryStore.bootstrapDefaultUsers(usersTableFirstPageId, privilegesTableFirstPageId);

        tables.put(
            CATALOG_TABLES_TABLE_NAME,
            new TableInfo(CATALOG_TABLES_TABLE_NAME, tablesTableSchema, tablesTableFirstPageId));
        tables.put(
            CATALOG_COLUMNS_TABLE_NAME,
            new TableInfo(CATALOG_COLUMNS_TABLE_NAME, columnsTableSchema, columnsTableFirstPageId));
        tables.put(
            CATALOG_USERS_TABLE_NAME,
            new TableInfo(CATALOG_USERS_TABLE_NAME, usersTableSchema, usersTableFirstPageId));
        tables.put(
            CATALOG_PRIVILEGES_TABLE_NAME,
            new TableInfo(
                CATALOG_PRIVILEGES_TABLE_NAME, privilegesTableSchema, privilegesTableFirstPageId));

        tableIds.put(CATALOG_TABLES_TABLE_NAME, tablesTableId);
        tableIds.put(CATALOG_COLUMNS_TABLE_NAME, columnsTableId);
        tableIds.put(CATALOG_USERS_TABLE_NAME, usersTableId);
        tableIds.put(CATALOG_PRIVILEGES_TABLE_NAME, privilegesTableId);

        System.out.println("[Bootstrap] Manually created 'testuser' and loaded into memory cache.");
    }

    public TableInfo createTable(String tableName, Schema schema) throws IOException {
        if (tables.containsKey(tableName)) {
            throw new IllegalArgumentException("Table " + tableName + " already exists.");
        }
        PageId firstPageId = bufferPoolManager.newPage().getPageId();
        int newTableId = nextTableId.getAndIncrement();
        metadataStore.persistNewTable(
            tablesTableFirstPageId,
            columnsTableFirstPageId,
            tablesTableSchema,
            newTableId,
            tableName,
            firstPageId,
            schema);

        TableInfo tableInfo = new TableInfo(tableName, schema, firstPageId);
        tables.put(tableName, tableInfo);
        tableIds.put(tableName, newTableId);

        return tableInfo;
    }

    /**
     * 从目录中删除一个表
     *
     * @param tableName 要删除的表名
     */
    public void dropTable(String tableName) throws IOException {
        Integer tableId = tableIds.get(tableName);
        if (tableId == null) {
            throw new IllegalArgumentException("Table " + tableName + " does not exist.");
        }

        tables.remove(tableName);
        tableIds.remove(tableName);

        metadataStore.deleteMatchingTuples(
            tablesTableFirstPageId, tablesTableSchema, 0, new Value(tableId));
        metadataStore.deleteMatchingTuples(
            columnsTableFirstPageId, columnsTableSchema, 0, new Value(tableId));
    }

    /**
     * 向现有表添加一个新列
     *
     * @param tableName 要修改的表名
     * @param newColumn 要追加到表末尾的列
     */
    public void addColumn(String tableName, Column newColumn) throws IOException {
        TableInfo tableInfo = getTable(tableName);
        if (tableInfo == null) {
            throw new IllegalArgumentException("Table " + tableName + " does not exist.");
        }
        int tableId = tableIds.get(tableName);

        List<Column> updatedColumns = new ArrayList<>(tableInfo.getSchema().getColumns());
        updatedColumns.add(newColumn);
        Schema newSchema = new Schema(updatedColumns);

        metadataStore.persistAddedColumn(
            columnsTableFirstPageId,
            tableId,
            tableInfo.getSchema().getColumns().size(),
            newColumn);

        TableInfo newTableInfo = new TableInfo(tableName, newSchema, tableInfo.getFirstPageId());
        tables.put(tableName, newTableInfo);
    }

    public TableInfo getTable(String tableName) {
        return tables.get(tableName);
    }

    /**
     * Retrieves a list of all user-defined table names.
     *
     * @return A sorted list of table names.
     */
    public List<String> getTableNames() {
        return tables.keySet().stream()
            .filter(name -> !name.startsWith("_catalog"))
            .sorted()
            .collect(Collectors.toList());
    }

    public TableInfo getTableByTuple(Tuple tuple) {
        if (tuple == null) return null;
        for (TableInfo tableInfo : tables.values()) {
            Schema schema = tableInfo.getSchema();
            if (schema.getColumns().size() == tuple.getValues().size()) {
                boolean typesMatch = true;
                for (int i = 0; i < schema.getColumns().size(); i++) {
                    if (schema.getColumns().get(i).getType() != tuple.getValues().get(i).getType()) {
                        typesMatch = false;
                        break;
                    }
                }
                if (typesMatch) {
                    return tableInfo;
                }
            }
        }
        return null;
    }

    /**
     * 创建并注册一个新的索引。
     */
    public void createIndex(String indexName, String tableName, String columnName, int rootPageId) {
        indexRegistry.createIndex(indexName, tableName, columnName, rootPageId);
    }

    /**
     * 更新索引根页号。
     */
    public void updateIndexRootPageId(String indexName, int newRootPageId) {
        indexRegistry.updateRootPageId(indexName, newRootPageId);
        System.out.println(
            "[Catalog] Updated root page ID for index '" + indexName + "' to " + newRootPageId);
    }

    /**
     * 根据表名和列名查找索引。
     */
    public IndexInfo getIndex(String tableName, String columnName) {
        return indexRegistry.getIndex(tableName, columnName);
    }

    /**
     * 根据索引名称获取索引信息。
     */
    public IndexInfo getIndex(String indexName) {
        return indexRegistry.getIndex(indexName);
    }

    public List<IndexInfo> getIndexesForTable(String tableName) {
        return indexRegistry.getIndexesForTable(tableName);
    }

    /**
     * 删除一个表上的所有索引元数据。
     *
     * @param tableName 表名
     */
    public void dropIndexesForTable(String tableName) {
        List<IndexInfo> indexes = indexRegistry.getIndexesForTable(tableName);
        indexRegistry.dropIndexesForTable(tableName);
        for (IndexInfo indexInfo : indexes) {
            System.out.println(
                "[Catalog] Dropped index metadata '"
                    + indexInfo.getIndexName()
                    + "' for table '"
                    + tableName
                    + "'.");
        }
    }

    /**
     * @param username 用户名
     * @return 密码的哈希字节数组，如果用户不存在则返回 null。
     */
    public byte[] getPasswordHash(String username) {
        return permissionRegistry.getPasswordHash(username);
    }

    /**
     * @param username      用户名
     * @param tableName     要操作的表名
     * @param privilegeType 请求的权限类型 (e.g., "SELECT", "INSERT")
     * @return 如果有权限则返回 true，否则返回 false。
     */
    public boolean hasPermission(String username, String tableName, String privilegeType) {
        if (permissionRegistry.hasPermission(username, tableName, privilegeType)) {
            return true;
        }

        System.out.println("[Catalog] 本地权限缓存检查失败，为 '" + username + "' 强制从全局 'default' 目录重新加载权限...");
        try {
            forceReloadPermissions();
        } catch (IOException e) {
            System.err.println("致命错误: 在权限检查期间无法重新加载权限: " + e.getMessage());
            return false;
        }

        return permissionRegistry.hasPermission(username, tableName, privilegeType);
    }

    public void createUser(String username, String password) throws IOException {
        if (permissionRegistry.containsUser(username)) {
            throw new IllegalStateException("User '" + username + "' already exists.");
        }
        userDirectoryStore.createUser(usersTableFirstPageId, username, password);

        System.out.println("[Catalog] User '" + username + "' created successfully.");
    }

    public void grantPrivilege(String username, String tableName, String privilegeType)
        throws IOException {
        userDirectoryStore.grantPrivilege(
            privilegesTableFirstPageId, username, tableName, privilegeType);

        System.out.println(
            "[Catalog] Granted " + privilegeType + " on " + tableName + " to '" + username + "'.");
    }

    public Schema getTableSchema(String tableName) {
        TableInfo tableInfo = tables.get(tableName);
        if (tableInfo != null) {
            return tableInfo.getSchema();
        }
        return null;
    }

    private void forceReloadPermissions() throws IOException {
        permissionRegistry.clear();

        DiskManager defaultDiskManager = null;
        try {
            String defaultDbPath = DatabaseManager.getDbFilePath("default");
            defaultDiskManager = new DiskManager(defaultDbPath);
            defaultDiskManager.open();
            BufferPoolManager defaultBPM = new BufferPoolManager(10, defaultDiskManager, "LRU");

            Page defaultTablesTablePage = defaultBPM.getPage(new PageId(0));
            List<Tuple> catalogTuples = defaultTablesTablePage.getAllTuples(this.tablesTableSchema);

            PageId usersPageId = null;
            PageId privsPageId = null;

            for (Tuple t : catalogTuples) {
                String tableName = (String) t.getValues().get(1).getValue();
                int pageId = (int) t.getValues().get(2).getValue();
                if (CATALOG_USERS_TABLE_NAME.equals(tableName)) {
                    usersPageId = new PageId(pageId);
                }
                if (CATALOG_PRIVILEGES_TABLE_NAME.equals(tableName)) {
                    privsPageId = new PageId(pageId);
                }
            }

            if (usersPageId == null || privsPageId == null) {
                System.err.println("[Catalog.forceReload] 警告: 在 'default' 数据库中找不到用户或权限系统表。");
                return;
            }

            UserDirectoryStore reloadStore = new UserDirectoryStore(defaultBPM, permissionRegistry);
            reloadStore.loadUsers(usersPageId, usersTableSchema);
            reloadStore.loadPrivileges(usersPageId, usersTableSchema, privsPageId, privilegesTableSchema);
        } finally {
            if (defaultDiskManager != null) {
                defaultDiskManager.close();
            }
        }
    }
}
