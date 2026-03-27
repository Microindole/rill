package com.indolyn.rill.core.sql.planner;

import com.indolyn.rill.core.catalog.Catalog;
import com.indolyn.rill.core.execution.trace.TraceCollector;
import com.indolyn.rill.core.sql.ast.StatementNode;
import com.indolyn.rill.core.sql.ast.statement.AlterTableStatementNode;
import com.indolyn.rill.core.sql.ast.statement.CreateDatabaseStatementNode;
import com.indolyn.rill.core.sql.ast.statement.CreateIndexStatementNode;
import com.indolyn.rill.core.sql.ast.statement.CreateTableStatementNode;
import com.indolyn.rill.core.sql.ast.statement.CreateUserStatementNode;
import com.indolyn.rill.core.sql.ast.statement.DeleteStatementNode;
import com.indolyn.rill.core.sql.ast.statement.DropDatabaseStatementNode;
import com.indolyn.rill.core.sql.ast.statement.DropTableStatementNode;
import com.indolyn.rill.core.sql.ast.statement.GrantStatementNode;
import com.indolyn.rill.core.sql.ast.statement.InsertStatementNode;
import com.indolyn.rill.core.sql.ast.statement.SelectStatementNode;
import com.indolyn.rill.core.sql.ast.statement.ShowColumnsStatementNode;
import com.indolyn.rill.core.sql.ast.statement.ShowCreateTableStatementNode;
import com.indolyn.rill.core.sql.ast.statement.ShowDatabasesStatementNode;
import com.indolyn.rill.core.sql.ast.statement.ShowTablesStatementNode;
import com.indolyn.rill.core.sql.ast.statement.UpdateStatementNode;
import com.indolyn.rill.core.sql.ast.statement.UseDatabaseStatementNode;
import com.indolyn.rill.core.sql.planner.plan.PlanNode;
import com.indolyn.rill.core.sql.planner.plan.command.CreateDatabasePlanNode;
import com.indolyn.rill.core.sql.planner.plan.command.DropDatabasePlanNode;
import com.indolyn.rill.core.sql.planner.plan.command.ShowColumnsPlanNode;
import com.indolyn.rill.core.sql.planner.plan.command.ShowCreateTablePlanNode;
import com.indolyn.rill.core.sql.planner.plan.command.ShowDatabasesPlanNode;
import com.indolyn.rill.core.sql.planner.plan.command.ShowTablesPlanNode;
import com.indolyn.rill.core.sql.type.PostgreSqlTypeResolver;
import com.indolyn.rill.core.sql.type.SqlTypeResolver;

import java.util.LinkedHashMap;
import java.util.Map;

public class Planner {

    private final Map<Class<? extends StatementNode>, StatementPlanner<? extends StatementNode>>
        statementPlanners = new LinkedHashMap<>();
    private final SelectPlanBuilder selectPlanBuilder;
    private final InsertPlanBuilder insertPlanBuilder;
    private final DeletePlanBuilder deletePlanBuilder;
    private final UpdatePlanBuilder updatePlanBuilder;
    private final CreateTablePlanBuilder createTablePlanBuilder;
    private final CreateIndexPlanBuilder createIndexPlanBuilder;
    private final AlterTablePlanBuilder alterTablePlanBuilder;
    private final DropTablePlanBuilder dropTablePlanBuilder;
    private final CreateUserPlanBuilder createUserPlanBuilder;
    private final GrantPlanBuilder grantPlanBuilder;

    public Planner(Catalog catalog) {
        SqlTypeResolver sqlTypeResolver = new PostgreSqlTypeResolver();
        this.selectPlanBuilder = new SelectPlanBuilder(catalog);
        this.insertPlanBuilder = new InsertPlanBuilder(catalog);
        this.deletePlanBuilder = new DeletePlanBuilder(catalog);
        this.updatePlanBuilder = new UpdatePlanBuilder(catalog);
        this.createTablePlanBuilder = new CreateTablePlanBuilder(sqlTypeResolver);
        this.createIndexPlanBuilder = new CreateIndexPlanBuilder(catalog);
        this.alterTablePlanBuilder = new AlterTablePlanBuilder(sqlTypeResolver);
        this.dropTablePlanBuilder = new DropTablePlanBuilder();
        this.createUserPlanBuilder = new CreateUserPlanBuilder();
        this.grantPlanBuilder = new GrantPlanBuilder();
        registerStatementPlanners();
    }

    public PlanNode createPlan(StatementNode ast) {
        StatementPlanner<StatementNode> planner = resolvePlanner(ast);
        if (planner != null) {
            PlanNode plan = planner.createPlan(ast);
            String component = plannerComponentName(ast);
            TraceCollector.record(
                "planner",
                component,
                "src/main/java/com/indolyn/rill/core/sql/planner/" + component + ".java",
                "build",
                plan == null
                    ? "当前语句未生成计划节点"
                    : "生成计划节点 " + plan.getClass().getSimpleName());
            return plan;
        }

        throw new UnsupportedOperationException(
            "Unsupported statement type for planning: " + ast.getClass().getSimpleName());
    }

    private String plannerComponentName(StatementNode ast) {
        if (ast instanceof CreateTableStatementNode) {
            return "CreateTablePlanBuilder";
        }
        if (ast instanceof CreateIndexStatementNode) {
            return "CreateIndexPlanBuilder";
        }
        if (ast instanceof InsertStatementNode) {
            return "InsertPlanBuilder";
        }
        if (ast instanceof SelectStatementNode) {
            return "SelectPlanBuilder";
        }
        if (ast instanceof DeleteStatementNode) {
            return "DeletePlanBuilder";
        }
        if (ast instanceof UpdateStatementNode) {
            return "UpdatePlanBuilder";
        }
        if (ast instanceof DropTableStatementNode) {
            return "DropTablePlanBuilder";
        }
        if (ast instanceof AlterTableStatementNode) {
            return "AlterTablePlanBuilder";
        }
        if (ast instanceof CreateUserStatementNode) {
            return "CreateUserPlanBuilder";
        }
        if (ast instanceof GrantStatementNode) {
            return "GrantPlanBuilder";
        }
        return "Planner";
    }

    private void registerStatementPlanners() {
        registerPlanner(CreateTableStatementNode.class, createTablePlanBuilder::build);
        registerPlanner(
            CreateDatabaseStatementNode.class,
            stmt -> new CreateDatabasePlanNode(stmt.databaseName().getName()));
        registerPlanner(InsertStatementNode.class, insertPlanBuilder::build);
        registerPlanner(SelectStatementNode.class, selectPlanBuilder::build);
        registerPlanner(DeleteStatementNode.class, deletePlanBuilder::build);
        registerPlanner(UpdateStatementNode.class, updatePlanBuilder::build);
        registerPlanner(DropTableStatementNode.class, dropTablePlanBuilder::build);
        registerPlanner(
            DropDatabaseStatementNode.class,
            stmt -> new DropDatabasePlanNode(stmt.databaseName().getName()));
        registerPlanner(AlterTableStatementNode.class, alterTablePlanBuilder::build);
        registerPlanner(ShowTablesStatementNode.class, stmt -> new ShowTablesPlanNode());
        registerPlanner(
            ShowColumnsStatementNode.class,
            stmt -> new ShowColumnsPlanNode(stmt.tableName().getName()));
        registerPlanner(
            ShowCreateTableStatementNode.class,
            stmt -> new ShowCreateTablePlanNode(stmt.tableName().getName()));
        registerPlanner(ShowDatabasesStatementNode.class, stmt -> new ShowDatabasesPlanNode());
        registerPlanner(CreateIndexStatementNode.class, createIndexPlanBuilder::build);
        registerPlanner(CreateUserStatementNode.class, createUserPlanBuilder::build);
        registerPlanner(GrantStatementNode.class, grantPlanBuilder::build);
        registerPlanner(UseDatabaseStatementNode.class, stmt -> null);
    }

    private <T extends StatementNode> void registerPlanner(
        Class<T> statementType, StatementPlanner<T> planner) {
        statementPlanners.put(statementType, planner);
    }

    @SuppressWarnings("unchecked")
    private StatementPlanner<StatementNode> resolvePlanner(StatementNode ast) {
        StatementPlanner<? extends StatementNode> exactPlanner = statementPlanners.get(ast.getClass());
        if (exactPlanner != null) {
            return (StatementPlanner<StatementNode>) exactPlanner;
        }

        for (Map.Entry<Class<? extends StatementNode>, StatementPlanner<? extends StatementNode>>
            entry : statementPlanners.entrySet()) {
            if (entry.getKey().isInstance(ast)) {
                return (StatementPlanner<StatementNode>) entry.getValue();
            }
        }
        return null;
    }

    @FunctionalInterface
    private interface StatementPlanner<T extends StatementNode> {
        PlanNode createPlan(T statement);
    }
}
