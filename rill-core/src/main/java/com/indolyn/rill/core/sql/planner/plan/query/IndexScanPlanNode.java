package com.indolyn.rill.core.sql.planner.plan.query;

import com.indolyn.rill.core.catalog.IndexInfo;
import com.indolyn.rill.core.catalog.TableInfo;
import com.indolyn.rill.core.model.Value;
import com.indolyn.rill.core.sql.planner.plan.PlanNode;

public class IndexScanPlanNode extends PlanNode {
    private final TableInfo tableInfo;
    private final IndexInfo indexInfo;
    private final Value searchKey; // 要在索引中查找的键

    public IndexScanPlanNode(TableInfo tableInfo, IndexInfo indexInfo, Value searchKey) {
        super(tableInfo.getSchema());
        this.tableInfo = tableInfo;
        this.indexInfo = indexInfo;
        this.searchKey = searchKey;
    }

    public TableInfo getTableInfo() {
        return tableInfo;
    }

    public IndexInfo getIndexInfo() {
        return indexInfo;
    }

    public Value getSearchKey() {
        return searchKey;
    }
}
