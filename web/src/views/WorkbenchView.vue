<template>
    <div class="studio-shell">
        <section class="hero-panel">
            <div class="hero-copy">
                <p class="eyebrow">Rill Demonstration Console</p>
                <h1>{{ store.overview.appName }} {{ store.overview.stage }}</h1>
                <p class="subtitle">
                    {{ store.overview.positioning }}
                </p>
                <div class="hero-actions">
                    <el-button type="primary" size="large" @click="scrollToSection('sql-workbench')">
                        直接执行 SQL
                    </el-button>
                    <el-button size="large" @click="scrollToSection('system-architecture')">
                        查看模块结构
                    </el-button>
                </div>
            </div>
            <div class="hero-status">
                <el-tag :type="store.usingMock ? 'warning' : 'success'" size="large">
                    {{ store.usingMock ? "SQL 使用 Mock 回退" : "SQL 已连接后端" }}
                </el-tag>
                <el-tag :type="store.overviewUsingMock ? 'warning' : 'success'" size="large">
                    {{ store.overviewUsingMock ? "Overview 使用 Mock" : "Overview 来自后端" }}
                </el-tag>
                <el-button text @click="store.loadOverview">刷新概览</el-button>
            </div>
        </section>

        <section class="highlights-grid">
            <article
                v-for="highlight in store.overview.highlights"
                :key="highlight.label"
                class="highlight-card"
            >
                <span class="highlight-label">{{ highlight.label }}</span>
                <strong class="highlight-value">{{ highlight.value }}</strong>
                <p>{{ highlight.detail }}</p>
            </article>
        </section>

        <section id="sql-workbench" class="studio-grid">
            <section class="panel panel-editor">
                <div class="panel-header">
                    <div>
                        <p class="panel-kicker">Core Flow</p>
                        <h2>SQL Workbench</h2>
                    </div>
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
                    :rows="9"
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
                    <span>Trace {{ store.result.traceId }}</span>
                </div>
            </section>

            <section class="panel panel-results">
                <div class="panel-header">
                    <div>
                        <p class="panel-kicker">Result</p>
                        <h2>查询结果</h2>
                    </div>
                    <span class="mono">{{ store.result?.dbName ?? "default" }}</span>
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
                <el-table v-if="store.result" :data="tableRows" height="270">
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
                    <div>
                        <p class="panel-kicker">Trace</p>
                        <h2>执行流程</h2>
                    </div>
                    <span class="mono">{{ store.usingMock ? "fallback" : "runtime-trace" }}</span>
                </div>
                <TraceFlow v-if="store.result" :steps="store.result.traceSteps" />
            </section>

            <section class="panel panel-source">
                <div class="panel-header">
                    <div>
                        <p class="panel-kicker">Source Insight</p>
                        <h2>源码映射</h2>
                    </div>
                    <span class="mono">clickless view</span>
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
        </section>

        <section id="system-architecture" class="section-block">
            <div class="section-header">
                <div>
                    <p class="panel-kicker">Architecture</p>
                    <h2>模块结构与产品边界</h2>
                </div>
                <p>这部分不是又做一个客户端，而是把数据库项目讲成“一个完整系统”。</p>
            </div>
            <div class="module-grid">
                <article v-for="module in store.overview.modules" :key="module.name" class="module-card">
                    <div class="module-top">
                        <h3>{{ module.name }}</h3>
                        <span>{{ module.role }}</span>
                    </div>
                    <p>{{ module.details }}</p>
                    <div class="module-boundary">
                        <strong>发布边界</strong>
                        <span>{{ module.releaseBoundary }}</span>
                    </div>
                </article>
            </div>
        </section>

        <section class="section-block">
            <div class="section-header">
                <div>
                    <p class="panel-kicker">Capability</p>
                    <h2>面试时能展开讲的能力</h2>
                </div>
                <p>执行 SQL 是核心，但它只是切入口，不是全部。</p>
            </div>
            <div class="capability-grid">
                <article
                    v-for="capability in store.overview.capabilities"
                    :key="`${capability.category}-${capability.title}`"
                    class="capability-card"
                >
                    <span class="capability-category">{{ capability.category }}</span>
                    <h3>{{ capability.title }}</h3>
                    <p>{{ capability.details }}</p>
                </article>
            </div>
        </section>

        <section class="section-block">
            <div class="section-header">
                <div>
                    <p class="panel-kicker">Expansion</p>
                    <h2>网络编程与 Redis 要怎么接</h2>
                </div>
                <p>先放在系统外层和控制面，不直接塞进单机内核事务路径。</p>
            </div>
            <div class="expansion-list">
                <article
                    v-for="expansion in store.overview.expansions"
                    :key="`${expansion.area}-${expansion.targetModule}`"
                    class="expansion-card"
                >
                    <div class="expansion-head">
                        <h3>{{ expansion.area }}</h3>
                        <span class="mono">{{ expansion.targetModule }}</span>
                    </div>
                    <p><strong>做法：</strong>{{ expansion.approach }}</p>
                    <p><strong>原因：</strong>{{ expansion.why }}</p>
                </article>
            </div>
        </section>
    </div>
</template>

<script setup lang="ts">
import { computed, onMounted } from "vue";
import TraceFlow from "@/components/TraceFlow.vue";
import { useWorkbenchStore } from "@/stores/workbench";

const store = useWorkbenchStore();

onMounted(() => {
    void store.loadOverview();
});

const tableRows = computed(() => {
    if (!store.result) {
        return [];
    }

    return store.result.rows.map((row) =>
        Object.fromEntries(store.result!.columns.map((column, index) => [column, row[index]]))
    );
});

function scrollToSection(sectionId: string) {
    document.getElementById(sectionId)?.scrollIntoView({
        behavior: "smooth",
        block: "start"
    });
}
</script>
