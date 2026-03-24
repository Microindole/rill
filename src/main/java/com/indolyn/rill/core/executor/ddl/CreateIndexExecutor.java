package com.indolyn.rill.core.executor.ddl;

import com.indolyn.rill.core.catalog.Catalog;
import com.indolyn.rill.core.catalog.TableInfo;
import com.indolyn.rill.core.common.model.RID;
import com.indolyn.rill.core.common.model.Schema;
import com.indolyn.rill.core.common.model.Tuple;
import com.indolyn.rill.core.common.model.Value;
import com.indolyn.rill.core.compiler.planner.plan.ddl.CreateIndexPlanNode;
import com.indolyn.rill.core.executor.TableHeap;
import com.indolyn.rill.core.executor.TupleIterator;
import com.indolyn.rill.core.executor.dml.SeqScanExecutor;
import com.indolyn.rill.core.storage.buffer.BufferPoolManager;
import com.indolyn.rill.core.storage.index.BPlusTree;
import com.indolyn.rill.core.storage.page.Page;
import com.indolyn.rill.core.transaction.Transaction;

import java.io.IOException;

public class CreateIndexExecutor implements TupleIterator {

    private final CreateIndexPlanNode plan;
    private final TableHeap tableHeap;
    private final Catalog catalog;
    private final BufferPoolManager bufferPoolManager;
    private final Transaction txn;
    private boolean executed = false;

    public CreateIndexExecutor(
        CreateIndexPlanNode plan,
        TableHeap tableHeap,
        Catalog catalog,
        BufferPoolManager bufferPoolManager,
        Transaction txn) {
        this.plan = plan;
        this.tableHeap = tableHeap;
        this.catalog = catalog;
        this.bufferPoolManager = bufferPoolManager;
        this.txn = txn;
    }

    @Override
    public boolean hasNext() throws IOException {
        if (!executed) {
            // 1. 创建一个新的 B+ 树
            Page rootPage = bufferPoolManager.newPage();
            int initialRootPageId = rootPage.getPageId().getPageNum();
            BPlusTree index = new BPlusTree(bufferPoolManager, initialRootPageId);

            // 2. 将索引信息注册到 Catalog (使用初始的RootPageId)
            catalog.createIndex(
                plan.getIndexName(), plan.getTableName(), plan.getColumnName(), initialRootPageId);

            // 3. 遍历表中的所有行，并将其插入到索引中
            TableInfo tableInfo = plan.getTableInfo();
            Schema schema = tableInfo.getSchema();
            int columnIndex = schema.getColumnIndex(plan.getColumnName());

            TupleIterator scan = new SeqScanExecutor(tableHeap, txn, null);
            while (scan.hasNext()) {
                Tuple tuple = scan.next();
                Value key = tuple.getValues().get(columnIndex);
                RID rid = tuple.getRid();
                index.insert(key, rid);
            }

            int finalRootPageId = index.getRootPageId();
            if (initialRootPageId != finalRootPageId) {
                System.out.println(
                    "[CreateIndexExecutor] B+Tree root page changed due to splits. Updating catalog from "
                        + initialRootPageId
                        + " to "
                        + finalRootPageId);
                catalog.updateIndexRootPageId(plan.getIndexName(), finalRootPageId);
            }

            executed = true;
        }
        return false; // DDL 操作不返回元组
    }

    @Override
    public Tuple next() throws IOException {
        return null;
    }

    @Override
    public Schema getOutputSchema() {
        return null;
    }
}
