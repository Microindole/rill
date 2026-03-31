<template>
    <div class="space-y-4">
        <section class="section-block">
            <div class="flex flex-wrap items-end justify-between gap-4">
                <div>
                    <p class="text-xs font-semibold uppercase tracking-wide text-slate-500">SQL Workspace</p>
                    <h1 class="mt-1 text-2xl font-semibold tracking-tight text-slate-900">控制台</h1>
                    <p class="mt-2 text-sm text-slate-600">
                        {{ auth.user?.displayName }} · {{ auth.user?.role }} · DB {{ platform.currentDatabase }}
                    </p>
                </div>
                <div class="flex flex-wrap gap-2">
                    <AppButton size="sm" :loading="platform.sessionActionLoading" @click="platform.createSession">新建会话</AppButton>
                    <AppButton v-if="auth.isAuthenticated" size="sm" subtle @click="changePasswordDialogVisible = true">修改密码</AppButton>
                    <AppButton v-if="auth.isAuthenticated" size="sm" variant="danger" subtle @click="logout">退出</AppButton>
                    <AppButton v-else size="sm" variant="primary" subtle @click="goLogin">登录</AppButton>
                </div>
            </div>
        </section>

        <section v-if="platform.dashboard" class="section-divider grid gap-3 pt-5 sm:grid-cols-2 xl:grid-cols-4">
            <article><p class="text-xs uppercase tracking-wide text-slate-500">会话</p><p class="mt-2 text-2xl font-semibold text-slate-900">{{ platform.dashboard.totalSessions }}</p></article>
            <article><p class="text-xs uppercase tracking-wide text-slate-500">历史</p><p class="mt-2 text-2xl font-semibold text-slate-900">{{ platform.dashboard.totalQueryHistory }}</p></article>
            <article><p class="text-xs uppercase tracking-wide text-slate-500">片段</p><p class="mt-2 text-2xl font-semibold text-slate-900">{{ platform.dashboard.totalSnippets }}</p></article>
            <article><p class="text-xs uppercase tracking-wide text-slate-500">场景</p><p class="mt-2 text-2xl font-semibold text-slate-900">{{ platform.dashboard.totalScenarios }}</p></article>
        </section>

        <AppNotice v-if="platform.dataError" tone="error" :description="platform.dataError" />

        <section class="section-divider grid gap-6 pt-5 xl:grid-cols-[300px_minmax(0,1fr)]">
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

            <main class="space-y-5">
                <section>
                    <div class="flex flex-wrap items-start justify-between gap-3">
                        <div>
                            <h2 class="text-lg font-semibold text-slate-900">SQL 工作区</h2>
                            <p class="mt-1 text-sm text-slate-500">执行 SQL，查看结果和执行链路。</p>
                        </div>
                        <span class="chip">{{ platform.activeSession?.sessionId ?? "未选择会话" }}</span>
                    </div>

                    <div class="mt-3 flex flex-wrap gap-2">
                        <button class="chip hover:bg-white" @click="fillSql('show databases;')">查看数据库</button>
                        <button class="chip hover:bg-white" @click="fillSql('show tables;')">查看表</button>
                        <button class="chip hover:bg-white" @click="fillSql('select * from users;')">示例查询</button>
                    </div>

                    <div class="mt-3 flex items-center justify-between text-xs text-slate-500">
                        <span v-if="platform.draftRecovered">已恢复该会话草稿</span>
                        <span v-else>支持 Ctrl/Cmd + Enter</span>
                        <button v-if="platform.sql.trim().length > 0" type="button" class="font-semibold text-teal-700" @click="platform.clearSqlDraft()">清空草稿</button>
                    </div>

                    <textarea :value="platform.sql" class="app-textarea mt-2 min-h-[200px]" rows="10" placeholder="输入 SQL，例如：show tables;" @input="updateSqlDraft" @keydown="handleSqlKeydown" />

                    <div class="mt-3 flex flex-wrap gap-2">
                        <AppButton variant="primary" :loading="platform.running" @click="platform.runQuery">执行 SQL</AppButton>
                        <AppButton subtle :disabled="!platform.sql.trim()" @click="openSnippetModal">保存片段</AppButton>
                        <AppButton subtle :disabled="!platform.sql.trim()" @click="openScenarioModal">保存场景</AppButton>
                        <AppButton subtle :disabled="!platform.sql.trim()" @click="openExportModal">导出任务</AppButton>
                    </div>
                </section>

                <section class="section-divider grid gap-5 pt-5 xl:grid-cols-2">
                    <article class="xl:col-span-2">
                        <div class="mb-3 flex items-center justify-between"><h2 class="text-base font-semibold text-slate-900">查询结果</h2><span class="chip">Result</span></div>
                        <component :is="QueryResultPanelAsync" v-if="platform.result" :result="platform.result" @reuse-sql="fillSql" @save-snippet="saveHistoryAsSnippet" @save-scenario="saveHistoryAsScenario" />
                        <AppEmpty v-else description="执行 SQL 后显示结果" />
                    </article>

                    <article>
                        <div class="mb-3 flex items-center justify-between"><h2 class="text-base font-semibold text-slate-900">执行链路</h2><span class="chip">Trace</span></div>
                        <template v-if="platform.result">
                            <div v-if="!tracePanelOpen" class="space-y-3"><p class="text-sm text-slate-500">链路按需加载，减少默认渲染开销。</p><AppButton size="sm" @click="tracePanelOpen = true">展开链路</AppButton></div>
                            <component :is="TraceFlowAsync" v-else :steps="platform.result.traceSteps" />
                        </template>
                        <AppEmpty v-else description="执行 SQL 后展示 trace" />
                    </article>

                    <article>
                        <div class="mb-3 flex items-center justify-between"><h2 class="text-base font-semibold text-slate-900">最近查询</h2><span class="chip">History</span></div>
                        <div class="space-y-2" v-if="platform.activeSession?.recentQueries.length">
                            <article v-for="item in platform.activeSession.recentQueries" :key="item.traceId" class="rounded-xl border border-white/70 bg-white/35 p-3">
                                <div class="flex items-center justify-between gap-2"><strong class="text-sm text-slate-900">{{ item.dbName }}</strong><span class="text-xs text-slate-500">{{ item.elapsedMs }} ms</span></div>
                                <p class="mt-1 text-xs text-slate-500">{{ item.sql }}</p>
                                <div class="mt-2 flex flex-wrap gap-2"><AppButton size="sm" @click="fillSql(item.sql)">填入</AppButton><AppButton size="sm" subtle @click="saveHistoryAsSnippet(item.sql)">存片段</AppButton><AppButton size="sm" subtle @click="saveHistoryAsScenario(item.sql)">存场景</AppButton></div>
                            </article>
                        </div>
                        <AppEmpty v-else description="当前会话暂无查询历史" />
                    </article>

                    <component :is="WorkspaceAssetsPanelAsync" :snippets="platform.snippets" :scenarios="platform.scenarios" :export-tasks="platform.exportTasks" @apply-sql="fillSql" @delete-snippet="platform.removeSnippet" @run-scenario="platform.triggerScenario" @delete-scenario="platform.removeScenario" @run-task="platform.triggerExportTask" @delete-task="platform.removeExportTask" />
                </section>
            </main>
        </section>

        <AppModal v-model="changePasswordDialogVisible" title="修改密码" width="420px">
            <AppNotice v-if="changePasswordSuccess" tone="success" :description="changePasswordSuccess" class="mb-3" />
            <AppNotice v-if="changePasswordError" tone="error" :description="changePasswordError" class="mb-3" />
            <form class="space-y-3" @submit.prevent>
                <label class="block space-y-1"><span class="text-sm font-semibold text-slate-700">当前密码</span><input v-model="currentPassword" class="app-input" type="password" /></label>
                <label class="block space-y-1"><span class="text-sm font-semibold text-slate-700">新密码</span><input v-model="newPassword" class="app-input" type="password" /></label>
                <label class="block space-y-1"><span class="text-sm font-semibold text-slate-700">确认新密码</span><input v-model="confirmNewPassword" class="app-input" type="password" /></label>
            </form>
            <template #footer><AppButton subtle @click="closePasswordDialog">取消</AppButton><AppButton variant="primary" :loading="auth.loading" @click="requestPasswordChange">发送改密邮件</AppButton></template>
        </AppModal>

        <AppModal v-model="exportDialogVisible" title="创建导出任务" width="460px">
            <form class="space-y-3" @submit.prevent>
                <label class="block space-y-1"><span class="text-sm font-semibold text-slate-700">任务标题</span><input v-model="exportTitle" class="app-input" type="text" placeholder="例如：用户表导出" /></label>
                <label class="block space-y-1"><span class="text-sm font-semibold text-slate-700">任务说明</span><textarea v-model="exportDescription" class="app-textarea min-h-[96px]" rows="4"></textarea></label>
                <label class="block space-y-1"><span class="text-sm font-semibold text-slate-700">导出格式</span><select v-model="exportFormat" class="app-input"><option value="csv">CSV</option><option value="json">JSON</option></select></label>
                <label class="flex items-center gap-2 text-sm text-slate-700"><input v-model="runExportImmediately" type="checkbox" /><span>创建后立即执行</span></label>
            </form>
            <template #footer><AppButton subtle @click="closeExportModal">取消</AppButton><AppButton variant="primary" :loading="platform.exportActionLoading" @click="submitExportTask">保存任务</AppButton></template>
        </AppModal>

        <AppModal v-model="snippetDialogVisible" title="保存 SQL 片段" width="460px">
            <form class="space-y-3" @submit.prevent>
                <label class="block space-y-1"><span class="text-sm font-semibold text-slate-700">片段标题</span><input v-model="snippetTitle" class="app-input" type="text" placeholder="例如：查看用户列表" /></label>
                <label class="block space-y-1"><span class="text-sm font-semibold text-slate-700">片段说明</span><textarea v-model="snippetDescription" class="app-textarea min-h-[96px]" rows="4"></textarea></label>
            </form>
            <template #footer><AppButton subtle @click="closeSnippetModal">取消</AppButton><AppButton variant="primary" :loading="platform.snippetActionLoading" @click="submitSnippet">保存片段</AppButton></template>
        </AppModal>

        <AppModal v-model="scenarioDialogVisible" title="保存场景脚本" width="460px">
            <form class="space-y-3" @submit.prevent>
                <label class="block space-y-1"><span class="text-sm font-semibold text-slate-700">场景标题</span><input v-model="scenarioTitle" class="app-input" type="text" placeholder="例如：初始化测试数据" /></label>
                <label class="block space-y-1"><span class="text-sm font-semibold text-slate-700">场景说明</span><textarea v-model="scenarioDescription" class="app-textarea min-h-[96px]" rows="4"></textarea></label>
            </form>
            <template #footer><AppButton subtle @click="closeScenarioModal">取消</AppButton><AppButton variant="primary" :loading="platform.scenarioActionLoading" @click="submitScenario">保存场景</AppButton></template>
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
    if (!window.confirm("删除这个会话后，其历史记录和本地草稿都会一并移除。是否继续？")) {
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
