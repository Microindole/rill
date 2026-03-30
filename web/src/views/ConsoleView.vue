<template>
    <div class="page-shell">
        <section class="console-shell">
            <header class="console-topbar">
                <div class="console-heading">
                    <p class="hero-eyebrow">工作台总览</p>
                    <h1>SQL 工作台</h1>
                    <p class="console-meta">
                        当前用户：{{ auth.user?.displayName }} · 当前数据库：{{ platform.currentDatabase }}
                    </p>
                    <p class="console-meta">
                        身份：{{ auth.user?.role }} · 个人数据库：{{ auth.user?.kernelDbName }}
                    </p>
                </div>
                <div class="console-actions">
                    <AppButton size="sm" :loading="platform.sessionActionLoading" @click="platform.createSession">新建会话</AppButton>
                    <AppButton v-if="auth.isAuthenticated" size="sm" subtle @click="changePasswordDialogVisible = true">修改密码</AppButton>
                    <AppButton v-if="auth.isAuthenticated" size="sm" variant="danger" subtle @click="logout">退出登录</AppButton>
                    <AppButton v-else size="sm" variant="primary" subtle @click="goLogin">登录解锁个人数据库</AppButton>
                </div>
            </header>

            <section class="console-stats" v-if="platform.dashboard">
                <article class="console-stat-card">
                    <span>会话数</span>
                    <strong>{{ platform.dashboard.totalSessions }}</strong>
                    <p>当前账号下的活跃工作台会话。</p>
                </article>
                <article class="console-stat-card">
                    <span>查询历史</span>
                    <strong>{{ platform.dashboard.totalQueryHistory }}</strong>
                    <p>已经记录下来的 SQL 查询历史。</p>
                </article>
                <article class="console-stat-card">
                    <span>片段资产</span>
                    <strong>{{ platform.dashboard.totalSnippets }}</strong>
                    <p>沉淀到工作台里的 SQL 片段资产。</p>
                </article>
                <article class="console-stat-card accent">
                    <span>场景资产</span>
                    <strong>{{ platform.dashboard.totalScenarios }}</strong>
                    <p>可直接回放的演示场景与示例脚本。</p>
                </article>
            </section>

            <AppNotice v-if="platform.dataError" tone="error" :description="platform.dataError" class="auth-alert" />

            <section class="console-layout">
                <WorkspaceSidebar
                    :sessions="platform.sessions"
                    :active-session-id="platform.activeSession?.sessionId ?? null"
                    :session-action-loading="platform.sessionActionLoading"
                    :snippets-count="platform.snippets.length"
                    :scenarios-count="platform.scenarios.length"
                    :export-tasks-count="platform.exportTasks.length"
                    :is-authenticated="auth.isAuthenticated"
                    :current-database="platform.currentDatabase"
                    :current-role="auth.user?.role ?? '游客'"
                    :database-status="auth.user?.kernelDbName && auth.user.kernelDbName !== 'default' ? '已分配' : '默认库'"
                    :show-admin-panel="auth.user?.role === 'ADMIN'"
                    :admin-users="platform.adminUsers"
                    @select-session="platform.selectSession"
                    @delete-session="confirmDeleteSession"
                    @provision-user-db="platform.provisionUserDatabase"
                    @drop-user-db="platform.dropUserDatabase"
                />

                <main class="workspace-main">
                    <section class="workspace-card workbench-card">
                        <div class="card-header-line">
                            <div>
                                <h2>SQL 工作区</h2>
                                <p class="card-lead">执行当前会话下的 SQL，并查看执行链路、历史记录和结果表格。</p>
                            </div>
                            <span class="mono">{{ platform.activeSession?.sessionId ?? "未选择会话" }}</span>
                        </div>
                        <div class="query-presets">
                            <button class="query-preset-chip" @click="fillSql('show databases;')">查看数据库</button>
                            <button class="query-preset-chip" @click="fillSql('show tables;')">查看表</button>
                            <button class="query-preset-chip" @click="fillSql('select * from users;')">示例查询</button>
                        </div>
                        <div class="query-editor-meta">
                            <span v-if="platform.draftRecovered">已恢复该会话的本地草稿</span>
                            <span v-else>支持 Ctrl / Cmd + Enter 快速执行</span>
                            <button
                                v-if="platform.sql.trim().length > 0"
                                type="button"
                                class="link-button"
                                @click="platform.clearSqlDraft()"
                            >
                                清空草稿
                            </button>
                        </div>
                        <textarea
                            :value="platform.sql"
                            class="app-textarea sql-editor"
                            rows="10"
                            placeholder="输入 SQL，例如：show tables;"
                            @input="updateSqlDraft"
                            @keydown="handleSqlKeydown"
                        />
                        <div class="console-query-actions">
                            <AppButton variant="primary" :loading="platform.running" @click="platform.runQuery">
                                执行 SQL
                            </AppButton>
                            <AppButton subtle :disabled="!platform.sql.trim()" @click="openSnippetModal">
                                保存为片段
                            </AppButton>
                            <AppButton subtle :disabled="!platform.sql.trim()" @click="openScenarioModal">
                                保存为场景
                            </AppButton>
                            <AppButton subtle :disabled="!platform.sql.trim()" @click="openExportModal">
                                创建导出任务
                            </AppButton>
                        </div>
                        <div class="summary-bar" v-if="platform.result">
                            <span class="summary-badge" :class="platform.result.success ? 'is-success' : 'is-error'">
                                {{ platform.result.success ? "执行成功" : "执行失败" }}
                            </span>
                            <span>耗时 {{ platform.result.elapsedMs }} ms</span>
                            <span>链路 {{ platform.result.traceId }}</span>
                            <span>结果 {{ platform.result.rows.length }} 行</span>
                            <span>数据库 {{ platform.result.dbName }}</span>
                        </div>
                    </section>

                    <section class="workspace-grid">
                        <article class="workspace-card workspace-card-wide">
                            <div class="workspace-card-header">
                                <h2>查询结果</h2>
                                <span class="card-caption">结果集</span>
                            </div>
                            <component
                                :is="QueryResultPanelAsync"
                                v-if="platform.result"
                                :result="platform.result"
                                @reuse-sql="fillSql"
                                @save-snippet="saveHistoryAsSnippet"
                                @save-scenario="saveHistoryAsScenario"
                            />
                            <AppEmpty v-else description="执行一条 SQL 后，这里会显示结果表格和原始输出" />
                        </article>

                        <article class="workspace-card">
                            <div class="workspace-card-header">
                                <h2>执行流程</h2>
                                <span class="card-caption">执行链路</span>
                            </div>
                            <template v-if="platform.result">
                                <div v-if="!tracePanelOpen" class="trace-placeholder">
                                    <p>执行链路图默认按需加载，需要时再展开查看。</p>
                                    <AppButton size="sm" @click="tracePanelOpen = true">展开执行链路</AppButton>
                                </div>
                                <component :is="TraceFlowAsync" v-else :steps="platform.result.traceSteps" />
                            </template>
                            <AppEmpty v-else description="执行 SQL 后，这里会展示 trace 执行路径" />
                        </article>

                        <article class="workspace-card">
                            <div class="workspace-card-header">
                                <h2>最近查询</h2>
                                <span class="card-caption">历史记录</span>
                            </div>
                            <div class="list-block" v-if="platform.activeSession?.recentQueries.length">
                                <div
                                    v-for="item in platform.activeSession.recentQueries"
                                    :key="item.traceId"
                                    class="list-item static"
                                >
                                    <strong>{{ item.dbName }}</strong>
                                    <span>{{ item.sql }}</span>
                                    <div class="recent-query-meta">
                                        <span class="summary-badge" :class="item.success ? 'is-success' : 'is-error'">
                                            {{ item.success ? "成功" : "失败" }}
                                        </span>
                                        <span>{{ item.elapsedMs }} ms</span>
                                    </div>
                                    <div class="console-query-actions recent-query-actions">
                                        <AppButton size="sm" @click="fillSql(item.sql)">填入编辑器</AppButton>
                                        <AppButton size="sm" subtle @click="saveHistoryAsSnippet(item.sql)">存为片段</AppButton>
                                        <AppButton size="sm" subtle @click="saveHistoryAsScenario(item.sql)">存为场景</AppButton>
                                    </div>
                                </div>
                            </div>
                            <AppEmpty v-else description="当前会话还没有查询历史" />
                        </article>

                        <component
                            :is="WorkspaceAssetsPanelAsync"
                            :snippets="platform.snippets"
                            :scenarios="platform.scenarios"
                            :export-tasks="platform.exportTasks"
                            @apply-sql="fillSql"
                            @delete-snippet="platform.removeSnippet"
                            @run-scenario="platform.triggerScenario"
                            @delete-scenario="platform.removeScenario"
                            @run-task="platform.triggerExportTask"
                            @delete-task="platform.removeExportTask"
                        />
                    </section>
                </main>
            </section>
        </section>

        <AppModal v-model="changePasswordDialogVisible" title="修改密码" width="420px">
            <AppNotice v-if="changePasswordSuccess" tone="success" :description="changePasswordSuccess" class="auth-alert" />
            <AppNotice v-if="changePasswordError" tone="error" :description="changePasswordError" class="auth-alert" />
            <form class="auth-form" @submit.prevent>
                <label class="field-block">
                    <span class="field-label">当前密码</span>
                    <input v-model="currentPassword" class="app-input" type="password" />
                </label>
                <label class="field-block">
                    <span class="field-label">新密码</span>
                    <input v-model="newPassword" class="app-input" type="password" />
                </label>
                <label class="field-block">
                    <span class="field-label">确认新密码</span>
                    <input v-model="confirmNewPassword" class="app-input" type="password" />
                </label>
            </form>
            <template #footer>
                <AppButton subtle @click="closePasswordDialog">取消</AppButton>
                <AppButton variant="primary" :loading="auth.loading" @click="requestPasswordChange">
                    发送改密确认邮件
                </AppButton>
            </template>
        </AppModal>

        <AppModal v-model="exportDialogVisible" title="创建导出任务" width="460px">
            <form class="auth-form" @submit.prevent>
                <label class="field-block">
                    <span class="field-label">任务标题</span>
                    <input v-model="exportTitle" class="app-input" type="text" placeholder="例如：用户表导出" />
                </label>
                <label class="field-block">
                    <span class="field-label">任务说明</span>
                    <textarea v-model="exportDescription" class="app-textarea compact-textarea" rows="4" placeholder="可选，用于备注导出用途"></textarea>
                </label>
                <label class="field-block">
                    <span class="field-label">导出格式</span>
                    <select v-model="exportFormat" class="app-input">
                        <option value="csv">CSV</option>
                        <option value="json">JSON</option>
                    </select>
                </label>
                <label class="checkbox-line">
                    <input v-model="runExportImmediately" type="checkbox" />
                    <span>创建后立即执行导出</span>
                </label>
            </form>
            <template #footer>
                <AppButton subtle @click="closeExportModal">取消</AppButton>
                <AppButton variant="primary" :loading="platform.exportActionLoading" @click="submitExportTask">
                    保存任务
                </AppButton>
            </template>
        </AppModal>

        <AppModal v-model="snippetDialogVisible" title="保存 SQL 片段" width="460px">
            <form class="auth-form" @submit.prevent>
                <label class="field-block">
                    <span class="field-label">片段标题</span>
                    <input v-model="snippetTitle" class="app-input" type="text" placeholder="例如：查看用户列表" />
                </label>
                <label class="field-block">
                    <span class="field-label">片段说明</span>
                    <textarea
                        v-model="snippetDescription"
                        class="app-textarea compact-textarea"
                        rows="4"
                        placeholder="可选，用于说明这个片段的用途"
                    ></textarea>
                </label>
            </form>
            <template #footer>
                <AppButton subtle @click="closeSnippetModal">取消</AppButton>
                <AppButton variant="primary" :loading="platform.snippetActionLoading" @click="submitSnippet">
                    保存片段
                </AppButton>
            </template>
        </AppModal>

        <AppModal v-model="scenarioDialogVisible" title="保存场景脚本" width="460px">
            <form class="auth-form" @submit.prevent>
                <label class="field-block">
                    <span class="field-label">场景标题</span>
                    <input v-model="scenarioTitle" class="app-input" type="text" placeholder="例如：初始化测试用户表" />
                </label>
                <label class="field-block">
                    <span class="field-label">场景说明</span>
                    <textarea
                        v-model="scenarioDescription"
                        class="app-textarea compact-textarea"
                        rows="4"
                        placeholder="可选，用于说明场景脚本要完成的事情"
                    ></textarea>
                </label>
            </form>
            <template #footer>
                <AppButton subtle @click="closeScenarioModal">取消</AppButton>
                <AppButton variant="primary" :loading="platform.scenarioActionLoading" @click="submitScenario">
                    保存场景
                </AppButton>
            </template>
        </AppModal>
    </div>
</template>

<script setup lang="ts">
import { defineAsyncComponent, onMounted, ref, watch } from "vue";
import { useRouter } from "vue-router";
import WorkspaceSidebar from "@/components/WorkspaceSidebar.vue";
import { useAuthStore } from "@/stores/auth";
import { usePlatformStore } from "@/stores/platform";
import AppButton from "@/components/ui/AppButton.vue";
import AppEmpty from "@/components/ui/AppEmpty.vue";
import AppModal from "@/components/ui/AppModal.vue";
import AppNotice from "@/components/ui/AppNotice.vue";

const TraceFlowAsync = defineAsyncComponent(() => import("@/components/TraceFlow.vue"));
const QueryResultPanelAsync = defineAsyncComponent(() => import("@/components/QueryResultPanel.vue"));
const WorkspaceAssetsPanelAsync = defineAsyncComponent(() => import("@/components/WorkspaceAssetsPanel.vue"));

const auth = useAuthStore();
const platform = usePlatformStore();
const router = useRouter();
const changePasswordDialogVisible = ref(false);
const tracePanelOpen = ref(false);
const currentPassword = ref("");
const newPassword = ref("");
const confirmNewPassword = ref("");
const changePasswordError = ref("");
const changePasswordSuccess = ref("");
const exportDialogVisible = ref(false);
const exportTitle = ref("");
const exportDescription = ref("");
const exportFormat = ref<"csv" | "json">("csv");
const runExportImmediately = ref(true);
const snippetDialogVisible = ref(false);
const snippetTitle = ref("");
const snippetDescription = ref("");
const scenarioDialogVisible = ref(false);
const scenarioTitle = ref("");
const scenarioDescription = ref("");

onMounted(async () => {
    await platform.loadOverview();
    await platform.loadWorkspaceData();
});

async function logout() {
    await auth.logoutCurrentUser();
    platform.resetWorkspaceState();
    await router.push("/login");
}

async function goLogin() {
    await router.push("/login?redirect=/console");
}

async function confirmDeleteSession(sessionId: string) {
    if (platform.sessions.length <= 1) {
        platform.dataError = "至少保留一个工作台会话。";
        return;
    }
    if (!window.confirm("删除这个工作台会话后，其历史记录和本地草稿都会一并移除。是否继续？")) {
        return;
    }
    await platform.removeSession(sessionId);
}

function fillSql(nextSql: string) {
    platform.setSql(nextSql);
}

watch(
    () => platform.result?.traceId,
    () => {
        tracePanelOpen.value = false;
    }
);

function updateSqlDraft(event: Event) {
    platform.setSql((event.target as HTMLTextAreaElement).value);
}

function handleSqlKeydown(event: KeyboardEvent) {
    if ((event.ctrlKey || event.metaKey) && event.key === "Enter") {
        event.preventDefault();
        if (!platform.running) {
            void platform.runQuery();
        }
    }
}

async function requestPasswordChange() {
    changePasswordError.value = "";
    changePasswordSuccess.value = "";
    if (!currentPassword.value || !newPassword.value || !confirmNewPassword.value) {
        changePasswordError.value = "当前密码、新密码和确认密码不能为空。";
        return;
    }
    if (newPassword.value !== confirmNewPassword.value) {
        changePasswordError.value = "两次输入的新密码不一致。";
        return;
    }
    try {
        const response = await auth.requestPasswordChangeEmail({
            currentPassword: currentPassword.value,
            newPassword: newPassword.value
        });
        changePasswordSuccess.value = response.message;
        currentPassword.value = "";
        newPassword.value = "";
        confirmNewPassword.value = "";
    } catch {
        changePasswordError.value = auth.error || "改密请求失败。";
    }
}

function closePasswordDialog() {
    changePasswordDialogVisible.value = false;
    changePasswordError.value = "";
    changePasswordSuccess.value = "";
    currentPassword.value = "";
    newPassword.value = "";
    confirmNewPassword.value = "";
}

function openExportModal() {
    exportTitle.value = `${platform.currentDatabase} 导出任务`;
    exportDescription.value = "";
    exportFormat.value = "csv";
    runExportImmediately.value = true;
    exportDialogVisible.value = true;
}

function closeExportModal() {
    exportDialogVisible.value = false;
}

async function submitExportTask() {
    if (!exportTitle.value.trim()) {
        platform.dataError = "导出任务标题不能为空";
        return;
    }
    await platform.createTaskFromCurrentSql({
        title: exportTitle.value,
        description: exportDescription.value,
        exportFormat: exportFormat.value,
        runImmediately: runExportImmediately.value
    });
    if (!platform.dataError) {
        exportDialogVisible.value = false;
    }
}

function openSnippetModal() {
    snippetTitle.value = "新的 SQL 片段";
    snippetDescription.value = "";
    snippetDialogVisible.value = true;
}

function saveHistoryAsSnippet(sqlText: string) {
    platform.setSql(sqlText);
    openSnippetModal();
}

function closeSnippetModal() {
    snippetDialogVisible.value = false;
}

async function submitSnippet() {
    if (!snippetTitle.value.trim()) {
        platform.dataError = "SQL 片段标题不能为空";
        return;
    }
    await platform.createSnippetFromCurrentSql({
        title: snippetTitle.value,
        description: snippetDescription.value
    });
    if (!platform.dataError) {
        snippetDialogVisible.value = false;
    }
}

function openScenarioModal() {
    scenarioTitle.value = "新的场景脚本";
    scenarioDescription.value = "";
    scenarioDialogVisible.value = true;
}

function saveHistoryAsScenario(sqlText: string) {
    platform.setSql(sqlText);
    openScenarioModal();
}

function closeScenarioModal() {
    scenarioDialogVisible.value = false;
}

async function submitScenario() {
    if (!scenarioTitle.value.trim()) {
        platform.dataError = "场景脚本标题不能为空";
        return;
    }
    await platform.createScenarioFromCurrentSql({
        title: scenarioTitle.value,
        description: scenarioDescription.value
    });
    if (!platform.dataError) {
        scenarioDialogVisible.value = false;
    }
}
</script>
