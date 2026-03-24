package com.indolyn.rill.core.compiler.planner.plan.query;

import com.indolyn.rill.core.catalog.TableInfo;
import com.indolyn.rill.core.compiler.parser.ast.ExpressionNode;
import com.indolyn.rill.core.compiler.planner.plan.PlanNode;

/**
 * @author hidyouth
 * @description: 顺序扫描执行计划节点
 */
public class SeqScanPlanNode extends PlanNode {
    private final TableInfo tableInfo;
    private final ExpressionNode predicate;

    // 修改构造函数以接受谓词
    public SeqScanPlanNode(TableInfo tableInfo, ExpressionNode predicate) {
        super(tableInfo.getSchema()); // 顺序扫描输出的是整张表的原始 Schema
        this.tableInfo = tableInfo;
        this.predicate = predicate;
    }

    public TableInfo getTableInfo() {
        return tableInfo;
    }

    // 新增 getter
    public ExpressionNode getPredicate() {
        return predicate;
    }
}
