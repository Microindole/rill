package com.indolyn.rill.core.sql.planner.plan.command;

import com.indolyn.rill.core.sql.ast.expression.IdentifierNode;
import com.indolyn.rill.core.sql.planner.plan.PlanNode;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 授权的执行计划节点
 */
public class GrantPlanNode extends PlanNode {
    private final List<String> privileges;
    private final String tableName;
    private final String username;

    public GrantPlanNode(
        List<IdentifierNode> privileges, IdentifierNode tableName, IdentifierNode username) {
        super(null);
        this.privileges = privileges.stream().map(IdentifierNode::getName).collect(Collectors.toList());
        this.tableName = tableName.getName();
        this.username = username.getName();
    }

    public List<String> getPrivileges() {
        return privileges;
    }

    public String getTableName() {
        return tableName;
    }

    public String getUsername() {
        return username;
    }
}

