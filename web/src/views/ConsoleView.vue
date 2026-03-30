<template>
    <div class="page-shell">
        <section class="console-topbar">
            <div>
                <p class="hero-eyebrow">Workspace Console</p>
                <h1>SQL 工作台</h1>
                <p class="console-meta">
                    当前用户：{{ auth.user?.displayName }} · 当前数据库：{{ platform.currentDatabase }}
                </p>
                <p class="console-meta">
                    身份：{{ auth.user?.role }} · 个人数据库：{{ auth.user?.kernelDbName }}
                </p>
            </div>
            <div class="console-actions">
                <el-button @click="platform.createSession">新建会话</el-button>
                <el-button v-if="auth.isAuthenticated" type="danger" plain @click="logout">退出登录</el-button>
                <el-button v-else type="primary" plain @click="goLogin">登录解锁个人数据库</el-button>
            </div>
        </section>

        <el-alert
            v-if="platform.dataError"
            type="error"
            :description="platform.dataError"
            show-icon
            :closable="false"
            class="auth-alert"
        />

        <section class="console-layout">
            <aside class="workspace-sidebar">
                <article class="workspace-card">
                    <h2>仪表盘</h2>
                    <div class="metric-list" v-if="platform.dashboard">
                        <div>
                            <span>Sessions</span>
                            <strong>{{ platform.dashboard.totalSessions }}</strong>
                        </div>
                        <div>
                            <span>History</span>
                            <strong>{{ platform.dashboard.totalQueryHistory }}</strong>
                        </div>
                        <div>
                            <span>Snippets</span>
                            <strong>{{ platform.dashboard.totalSnippets }}</strong>
                        </div>
                        <div>
                            <span>Scenarios</span>
                            <strong>{{ platform.dashboard.totalScenarios }}</strong>
                        </div>
                    </div>
                </article>

                <article class="workspace-card">
                    <div class="card-header-line">
                        <h2>工作台会话</h2>
                    </div>
                    <button
                        v-for="session in platform.sessions"
                        :key="session.sessionId"
                        class="session-pill"
                        :class="{ active: session.sessionId === platform.activeSession?.sessionId }"
                        @click="platform.selectSession(session.sessionId)"
                    >
                        <strong>{{ session.currentDatabase }}</strong>
                        <span>{{ session.recentQueryCount }} queries</span>
                    </button>
                </article>

                <article class="workspace-card">
                    <h2>资产概览</h2>
                    <p>{{ platform.snippets.length }} 个 SQL Snippet</p>
                    <p>{{ platform.scenarios.length }} 个 Demo Scenario</p>
                    <p>{{ platform.exportTasks.length }} 个 Export Task</p>
                    <p v-if="!auth.isAuthenticated" class="console-guest-note">
                        游客模式下只会操作共享 default 数据库。登录后会自动切换到你的个人数据库并保留自己的工作台资产。
                    </p>
                </article>
            </aside>

            <main class="workspace-main">
                <section class="workspace-card workbench-card">
                    <div class="card-header-line">
                        <h2>SQL Workbench</h2>
                        <span class="mono">{{ platform.activeSession?.sessionId ?? "未选择会话" }}</span>
                    </div>
                    <el-input
                        v-model="platform.sql"
                        type="textarea"
                        :rows="9"
                        resize="none"
                        placeholder="输入 SQL"
                    />
                    <div class="console-query-actions">
                        <el-button type="primary" :loading="platform.running" @click="platform.runQuery">
                            执行 SQL
                        </el-button>
                    </div>
                    <div class="summary-bar" v-if="platform.result">
                        <span>耗时 {{ platform.result.elapsedMs }} ms</span>
                        <span>Trace {{ platform.result.traceId }}</span>
                        <span>结果 {{ platform.result.rows.length }} 行</span>
                    </div>
                </section>

                <section class="workspace-grid">
                    <article class="workspace-card">
                        <h2>查询结果</h2>
                        <el-table v-if="platform.result" :data="tableRows" height="260">
                            <el-table-column
                                v-for="column in platform.result.columns"
                                :key="column"
                                :prop="column"
                                :label="column"
                            />
                        </el-table>
                        <el-empty
                            v-if="platform.result && platform.result.columns.length === 0 && platform.result.success"
                            description="当前语句没有表格结果"
                        />
                        <pre v-if="platform.result" class="raw-result">{{ platform.result.rawResult }}</pre>
                    </article>

                    <article class="workspace-card">
                        <h2>执行流程</h2>
                        <TraceFlow v-if="platform.result" :steps="platform.result.traceSteps" />
                    </article>

                    <article class="workspace-card">
                        <h2>最近查询</h2>
                        <div class="list-block" v-if="platform.activeSession?.recentQueries.length">
                            <button
                                v-for="item in platform.activeSession.recentQueries"
                                :key="item.traceId"
                                class="list-item"
                                @click="platform.sql = item.sql"
                            >
                                <strong>{{ item.dbName }}</strong>
                                <span>{{ item.sql }}</span>
                            </button>
                        </div>
                        <el-empty v-else description="当前会话还没有查询历史" />
                    </article>

                    <article class="workspace-card">
                        <h2>SQL Snippets</h2>
                        <div class="list-block" v-if="platform.snippets.length">
                            <button
                                v-for="snippet in platform.snippets"
                                :key="snippet.id"
                                class="list-item"
                                @click="platform.sql = snippet.sql"
                            >
                                <strong>{{ snippet.title }}</strong>
                                <span>{{ snippet.description || snippet.sql }}</span>
                            </button>
                        </div>
                        <el-empty v-else description="暂无 SQL 片段" />
                    </article>

                    <article class="workspace-card">
                        <h2>Demo Scenarios</h2>
                        <div class="list-block" v-if="platform.scenarios.length">
                            <button
                                v-for="scenario in platform.scenarios"
                                :key="scenario.id"
                                class="list-item"
                                @click="platform.sql = scenario.sqlScript"
                            >
                                <strong>{{ scenario.title }}</strong>
                                <span>{{ scenario.description || scenario.sqlScript }}</span>
                            </button>
                        </div>
                        <el-empty v-else description="暂无演示场景" />
                    </article>

                    <article class="workspace-card">
                        <h2>Export Tasks</h2>
                        <div class="list-block" v-if="platform.exportTasks.length">
                            <div v-for="task in platform.exportTasks" :key="task.id" class="list-item static">
                                <strong>{{ task.title }}</strong>
                                <span>{{ task.status }} · {{ task.exportFormat }} · {{ task.dbName }}</span>
                            </div>
                        </div>
                        <el-empty v-else description="暂无导出任务" />
                    </article>
                </section>
            </main>
        </section>
    </div>
</template>

<script setup lang="ts">
import { computed, onMounted } from "vue";
import { useRouter } from "vue-router";
import TraceFlow from "@/components/TraceFlow.vue";
import { useAuthStore } from "@/stores/auth";
import { usePlatformStore } from "@/stores/platform";

const auth = useAuthStore();
const platform = usePlatformStore();
const router = useRouter();

onMounted(async () => {
    await platform.loadOverview();
    await platform.loadWorkspaceData();
});

const tableRows = computed(() => {
    if (!platform.result) {
        return [];
    }
    return platform.result.rows.map((row) =>
        Object.fromEntries(platform.result!.columns.map((column, index) => [column, row[index]]))
    );
});

async function logout() {
    await auth.logoutCurrentUser();
    platform.resetWorkspaceState();
    await router.push("/login");
}

async function goLogin() {
    await router.push("/login?redirect=/console");
}
</script>
