package com.indolyn.rill.core.execution.operator.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.indolyn.rill.core.catalog.Catalog;
import com.indolyn.rill.core.catalog.IndexInfo;
import com.indolyn.rill.core.catalog.TableInfo;
import com.indolyn.rill.core.model.Column;
import com.indolyn.rill.core.model.DataType;
import com.indolyn.rill.core.model.Schema;
import com.indolyn.rill.core.model.Tuple;
import com.indolyn.rill.core.sql.planner.plan.command.CreateDatabasePlanNode;
import com.indolyn.rill.core.sql.planner.plan.command.DropDatabasePlanNode;
import com.indolyn.rill.core.sql.planner.plan.command.ShowColumnsPlanNode;
import com.indolyn.rill.core.sql.planner.plan.command.ShowCreateTablePlanNode;
import com.indolyn.rill.core.sql.planner.plan.command.ShowDatabasesPlanNode;
import com.indolyn.rill.core.sql.planner.plan.command.ShowTablesPlanNode;
import com.indolyn.rill.core.sql.planner.plan.command.UseDatabasePlanNode;
import com.indolyn.rill.core.storage.database.DatabaseManager;
import com.indolyn.rill.core.storage.page.PageId;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class CommandExecutorBaselineTest {

    @Test
    void createAndDropDatabaseExecutorsShouldDelegateToDatabaseManager() throws Exception {
        DatabaseManager dbManager = Mockito.mock(DatabaseManager.class);

        CreateDatabaseExecutor createExecutor =
            new CreateDatabaseExecutor(new CreateDatabasePlanNode("demo"), dbManager);
        DropDatabaseExecutor dropExecutor =
            new DropDatabaseExecutor(new DropDatabasePlanNode("demo"), dbManager);

        Tuple createResult = createExecutor.next();
        Tuple dropResult = dropExecutor.next();

        verify(dbManager).createDatabase("demo");
        verify(dbManager).dropDatabase("demo");
        assertEquals("Database 'demo' created.", createResult.getValues().get(0).getValue());
        assertEquals("Database 'demo' dropped.", dropResult.getValues().get(0).getValue());
    }

    @Test
    void showExecutorsShouldRenderDatabaseAndTableMetadata() throws Exception {
        DatabaseManager dbManager = Mockito.mock(DatabaseManager.class);
        Catalog catalog = Mockito.mock(Catalog.class);
        Schema schema =
            new Schema(
                List.of(
                    new Column("id", DataType.INT, "INT", List.of(), false, null, true),
                    new Column("name", DataType.VARCHAR, "VARCHAR", List.of(10), false, "'guest'", false)));
        when(dbManager.listDatabases()).thenReturn(List.of("default", "demo"));
        when(catalog.getAllTableNames()).thenReturn(List.of("_catalog_tables", "users", "accounts"));
        when(catalog.getTable("users")).thenReturn(new TableInfo("users", schema, new PageId(4)));
        when(catalog.getIndexesForTable("users"))
            .thenReturn(List.of(new IndexInfo("idx_users_name", "users", "name", 7)));

        ShowDatabasesExecutor showDatabasesExecutor =
            new ShowDatabasesExecutor(new ShowDatabasesPlanNode(), dbManager);
        ShowTablesExecutor showTablesExecutor =
            new ShowTablesExecutor(new ShowTablesPlanNode(), catalog);
        ShowColumnsExecutor showColumnsExecutor =
            new ShowColumnsExecutor(new ShowColumnsPlanNode("users"), catalog);
        ShowCreateTableExecutor showCreateTableExecutor =
            new ShowCreateTableExecutor(new ShowCreateTablePlanNode("users"), catalog);

        assertTrue(showDatabasesExecutor.hasNext());
        assertEquals("default", showDatabasesExecutor.next().getValues().get(0).getValue());
        assertEquals("demo", showDatabasesExecutor.next().getValues().get(0).getValue());
        assertFalse(showDatabasesExecutor.hasNext());

        assertTrue(showTablesExecutor.hasNext());
        assertEquals("accounts", showTablesExecutor.next().getValues().get(0).getValue());
        assertEquals("users", showTablesExecutor.next().getValues().get(0).getValue());
        assertFalse(showTablesExecutor.hasNext());

        assertTrue(showColumnsExecutor.hasNext());
        assertEquals("id", showColumnsExecutor.next().getValues().get(0).getValue());
        assertEquals("name", showColumnsExecutor.next().getValues().get(0).getValue());
        assertFalse(showColumnsExecutor.hasNext());

        assertTrue(showCreateTableExecutor.hasNext());
        String ddl = (String) showCreateTableExecutor.next().getValues().get(1).getValue();
        assertTrue(ddl.contains("CREATE TABLE `users`"));
        assertTrue(ddl.contains("PRIMARY KEY (`id`)"));
        assertTrue(ddl.contains("KEY `idx_users_name` (`name`)"));
    }

    @Test
    void useDatabaseExecutorShouldYieldSingleConfirmationTuple() throws Exception {
        UseDatabaseExecutor executor = new UseDatabaseExecutor(new UseDatabasePlanNode("demo"));

        assertTrue(executor.hasNext());
        assertEquals("Database changed to 'demo'.", executor.next().getValues().get(0).getValue());
        assertFalse(executor.hasNext());
    }
}
