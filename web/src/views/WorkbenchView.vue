<template>
    <div class="studio-shell">
        <header class="studio-header">
            <div>
                <p class="eyebrow">Rill Studio</p>
                <h1>数据库内核可视化控制台</h1>
                <p class="subtitle">
                    执行 SQL，观察词法、语法、语义、规划与执行链路，并映射到核心源码组件。
                </p>
            </div>
            <el-tag type="warning" size="large">WebUI 第一阶段骨架</el-tag>
        </header>

        <main class="studio-grid">
            <section class="panel panel-editor">
                <div class="panel-header">
                    <h2>SQL Workbench</h2>
                    <el-button type="primary" :loading="store.running" @click="store.executeQuery">
                        执行查询
                    </el-button>
                </div>
                <div class="editor-toolbar">
                    <el-input v-model="store.dbName" placeholder="数据库名" class="db-input" />
                    <el-tag :type="store.usingMock ? 'info' : 'success'">
                        {{ store.usingMock ? "Mock 数据" : "后端接口" }}
                    </el-tag>
                </div>
                <el-input
                    v-model="store.sql"
                    type="textarea"
                    :rows="8"
                    resize="none"
                    placeholder="输入 SQL"
                />
                <el-alert
                    v-if="store.lastError"
                    class="status-alert"
                    title="后端接口不可用，当前展示 mock 结果"
                    type="warning"
                    :description="store.lastError"
                    show-icon
                    :closable="false"
                />
                <div class="summary-bar" v-if="store.result">
                    <span>耗时 {{ store.result.elapsedMs }} ms</span>
                    <span>阶段数 {{ store.result.traceSteps.length }}</span>
                    <span>结果行数 {{ store.result.rows.length }}</span>
                </div>
            </section>

            <section class="panel panel-results">
                <div class="panel-header">
                    <h2>查询结果</h2>
                    <span class="mono">{{ store.result?.traceId ?? "N/A" }}</span>
                </div>
                <el-alert
                    v-if="store.result && !store.result.success"
                    title="查询执行失败"
                    type="error"
                    :description="store.result.rawResult"
                    show-icon
                    :closable="false"
                    class="status-alert"
                />
                <el-table v-if="store.result" :data="tableRows" height="260">
                    <el-table-column
                        v-for="column in store.result.columns"
                        :key="column"
                        :prop="column"
                        :label="column"
                    />
                </el-table>
                <el-empty
                    v-if="store.result && store.result.columns.length === 0 && store.result.success"
                    description="当前语句没有表格结果"
                />
                <pre v-if="store.result" class="raw-result">{{ store.result.rawResult }}</pre>
            </section>

            <section class="panel panel-flow">
                <div class="panel-header">
                    <h2>执行流程</h2>
                    <span class="mono">{{ store.usingMock ? "fallback" : "trace" }}</span>
                </div>
                <TraceFlow v-if="store.result" :steps="store.result.traceSteps" />
            </section>

            <section class="panel panel-source">
                <div class="panel-header">
                    <h2>源码映射</h2>
                    <span class="mono">Source Insight</span>
                </div>
                <div class="source-list" v-if="store.result">
                    <article
                        v-for="step in store.result.traceSteps"
                        :key="step.id"
                        class="source-card"
                    >
                        <h3>{{ step.title }}</h3>
                        <p>{{ step.detail }}</p>
                        <dl>
                            <div>
                                <dt>组件</dt>
                                <dd>{{ step.component }}</dd>
                            </div>
                            <div>
                                <dt>类</dt>
                                <dd>{{ step.sourceClass }}</dd>
                            </div>
                            <div>
                                <dt>方法</dt>
                                <dd>{{ step.sourceMethod }}</dd>
                            </div>
                            <div>
                                <dt>文件</dt>
                                <dd class="mono">{{ step.sourceFile }}</dd>
                            </div>
                        </dl>
                    </article>
                </div>
            </section>
        </main>
    </div>
</template>

<script setup lang="ts">
import { computed } from "vue";
import TraceFlow from "@/components/TraceFlow.vue";
import { useWorkbenchStore } from "@/stores/workbench";

const store = useWorkbenchStore();

const tableRows = computed(() => {
    if (!store.result) {
        return [];
    }

    return store.result.rows.map((row) =>
        Object.fromEntries(store.result!.columns.map((column, index) => [column, row[index]]))
    );
});
</script>
