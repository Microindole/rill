package com.indolyn.rill.core.executor.ddl;

import com.indolyn.rill.core.catalog.Catalog;
import com.indolyn.rill.core.catalog.TableInfo;
import com.indolyn.rill.core.common.model.*;
import com.indolyn.rill.core.compiler.planner.plan.ddl.CreateTablePlanNode;
import com.indolyn.rill.core.executor.TableHeap;
import com.indolyn.rill.core.executor.TupleIterator;
import com.indolyn.rill.core.executor.dml.SeqScanExecutor;
import com.indolyn.rill.core.storage.buffer.BufferPoolManager;
import com.indolyn.rill.core.storage.index.BPlusTree;
import com.indolyn.rill.core.storage.page.Page;
import com.indolyn.rill.core.transaction.LockManager;
import com.indolyn.rill.core.transaction.Transaction;
import com.indolyn.rill.core.transaction.log.LogManager;
import com.indolyn.rill.core.transaction.log.LogRecord;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

/**
 * 创建表的执行器.
 */
public class CreateTableExecutor implements TupleIterator {

    private final CreateTablePlanNode plan;
    private final Catalog catalog;
    private boolean done = false;
    private static final Schema RESULT_SCHEMA =
        new Schema(List.of(new Column("message", DataType.VARCHAR)));
    private final Transaction txn;
    private final LogManager logManager;
    private final BufferPoolManager bufferPoolManager;
    private final LockManager lockManager;

    public CreateTableExecutor(
        CreateTablePlanNode plan,
        Catalog catalog,
        Transaction txn,
        LogManager logManager,
        BufferPoolManager bufferPoolManager,
        LockManager lockManager) {
        this.plan = plan;
        this.catalog = catalog;
        this.txn = txn;
        this.logManager = logManager;
        this.bufferPoolManager = bufferPoolManager;
        this.lockManager = lockManager;
    }

    @Override
    public Tuple next() throws IOException {
        if (done) {
            return null;
        }
        executeCreateTableWithIndex();

        done = true;
        // DDL 语句通常返回一个表示成功的元组，或不返回
        return new Tuple(
            Collections.singletonList(new Value("Table '" + plan.getTableName() + "' created.")));
    }

    @Override
    public boolean hasNext() throws IOException {
        return !done;
    }

    @Override
    public Schema getOutputSchema() {
        return RESULT_SCHEMA;
    }

    private void executeCreateTableWithIndex() throws IOException {
        // 在物理操作前，先写日志
        LogRecord logRecord =
            new LogRecord(
                txn.getTransactionId(),
                txn.getPrevLSN(),
                LogRecord.LogType.CREATE_TABLE,
                plan.getTableName(),
                plan.getOutputSchema());
        long lsn = logManager.appendLogRecord(logRecord);
        txn.setPrevLSN(lsn);

        // 日志写入成功后，再执行物理操作
        catalog.createTable(plan.getTableName(), plan.getOutputSchema());

        // 自动创建主键索引
        String primaryKeyColumnName = plan.getOutputSchema().getPrimaryKeyColumnName();
        if (primaryKeyColumnName != null) {
            // 自动生成索引名称
            String indexName = "pk_" + plan.getTableName() + "_" + primaryKeyColumnName;
            TableInfo tableInfo = catalog.getTable(plan.getTableName());
            TableHeap tableHeap = new TableHeap(bufferPoolManager, tableInfo, logManager, lockManager);

            // 1. 创建一个新的 B+树
            Page rootPage = bufferPoolManager.newPage();
            int initialRootPageId = rootPage.getPageId().getPageNum();
            BPlusTree index = new BPlusTree(bufferPoolManager, initialRootPageId);

            // 2. 将索引信息注册到 Catalog (使用初始的RootPageId)
            catalog.createIndex(indexName, plan.getTableName(), primaryKeyColumnName, initialRootPageId);

            // 3. 遍历表中的所有行，并将其插入到索引中
            Schema schema = tableInfo.getSchema();
            int columnIndex = schema.getColumnIndex(primaryKeyColumnName);
            TupleIterator scan = new SeqScanExecutor(tableHeap, txn, null);
            while (scan.hasNext()) {
                Tuple tuple = scan.next();
                Value key = tuple.getValues().get(columnIndex);
                RID rid = tuple.getRid();
                index.insert(key, rid);
            }

            int finalRootPageId = index.getRootPageId();
            if (initialRootPageId != finalRootPageId) {
                catalog.updateIndexRootPageId(indexName, finalRootPageId);
            }
        }
    }
}
