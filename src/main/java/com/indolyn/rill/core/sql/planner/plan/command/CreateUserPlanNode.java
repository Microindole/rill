package com.indolyn.rill.core.sql.planner.plan.command;

import com.indolyn.rill.core.sql.ast.expression.IdentifierNode;
import com.indolyn.rill.core.sql.ast.expression.LiteralNode;
import com.indolyn.rill.core.sql.planner.plan.PlanNode;

/**
 * 创建用户的执行计划节点
 */
public class CreateUserPlanNode extends PlanNode {
    private final String username;
    private final String password;

    public CreateUserPlanNode(IdentifierNode username, LiteralNode password) {
        super(null);
        this.username = username.getName();
        this.password = password.literal().lexeme();
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }
}

