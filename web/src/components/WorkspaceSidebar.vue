<template>
    <aside class="space-y-6">
        <section>
            <div class="mb-3 flex items-center justify-between">
                <h2 class="text-sm font-semibold text-slate-900">会话</h2>
                <span class="text-xs text-slate-500">{{ sessions.length }}</span>
            </div>

            <div v-if="sessions.length" class="space-y-2">
                <input v-model.trim="sessionKeyword" class="app-input" type="search" placeholder="搜索会话或数据库" />
                <select v-model="sessionSort" class="app-input">
                    <option value="active">当前优先</option>
                    <option value="recent">最近使用</option>
                    <option value="created">创建时间</option>
                    <option value="queries">查询次数</option>
                </select>
            </div>

            <div class="mt-3 flex flex-wrap gap-2" v-if="sessions.length">
                <button
                    v-for="option in filterOptions"
                    :key="option.value"
                    type="button"
                    class="text-xs font-semibold"
                    :class="sessionFilter === option.value ? 'text-slate-900' : 'text-slate-500 hover:text-slate-700'"
                    @click="sessionFilter = option.value"
                >
                    {{ option.label }}
                </button>
            </div>

            <div v-if="sessionGroups.length" class="mt-4 space-y-4">
                <section v-for="group in sessionGroups" :key="group.key" class="space-y-2">
                    <div class="flex items-center justify-between text-xs font-semibold uppercase tracking-wide text-slate-500">
                        <span>{{ group.label }}</span>
                        <span>{{ group.items.length }}</span>
                    </div>
                    <div>
                        <div
                            v-for="session in group.items"
                            :key="session.sessionId"
                            class="border-b border-slate-200/70 py-3"
                        >
                            <div class="flex items-center justify-between gap-2">
                                <strong class="text-sm text-slate-900">{{ session.currentDatabase }}</strong>
                                <span class="text-xs text-slate-500">{{ session.recentQueryCount }} 次</span>
                            </div>
                            <p class="mt-1 text-xs text-slate-500">{{ getSessionSummary(session) }}</p>
                            <div class="mt-2 flex gap-2">
                                <AppButton size="sm" :loading="sessionActionLoading" @click="$emit('select-session', session.sessionId)">
                                    进入
                                </AppButton>
                                <AppButton
                                    size="sm"
                                    variant="danger"
                                    subtle
                                    :disabled="sessions.length === 1 || sessionActionLoading"
                                    @click="$emit('delete-session', session.sessionId)"
                                >
                                    删除
                                </AppButton>
                            </div>
                        </div>
                    </div>
                </section>
            </div>
            <AppEmpty v-else :description="sessions.length ? '没有匹配会话' : '暂无工作台会话'" />
        </section>

        <section class="section-divider pt-5">
            <h2 class="text-sm font-semibold text-slate-900">工作区状态</h2>
            <div class="mt-3 space-y-2 text-sm">
                <div class="flex items-center justify-between"><span class="text-slate-500">SQL 片段</span><strong>{{ snippetsCount }}</strong></div>
                <div class="flex items-center justify-between"><span class="text-slate-500">场景脚本</span><strong>{{ scenariosCount }}</strong></div>
                <div class="flex items-center justify-between"><span class="text-slate-500">导出任务</span><strong>{{ exportTasksCount }}</strong></div>
                <div class="h-px bg-slate-200/70"></div>
                <div class="flex items-center justify-between"><span class="text-slate-500">数据库</span><strong>{{ currentDatabase }}</strong></div>
                <div class="flex items-center justify-between"><span class="text-slate-500">身份</span><strong>{{ currentRole }}</strong></div>
                <div class="flex items-center justify-between"><span class="text-slate-500">状态</span><strong>{{ databaseStatus }}</strong></div>
            </div>
            <p v-if="!isAuthenticated" class="mt-3 text-xs text-amber-700">游客使用共享库，登录后切换到个人数据库。</p>
        </section>

        <component
            :is="AdminUserPanelAsync"
            v-if="showAdminPanel"
            :users="adminUsers"
            @provision="$emit('provision-user-db', $event)"
            @drop="$emit('drop-user-db', $event)"
        />
    </aside>
</template>

<script setup lang="ts">
import { computed, defineAsyncComponent, ref } from "vue";
import type { AdminUser, WorkspaceSessionSummary } from "@/services/api";
import AppButton from "@/components/ui/AppButton.vue";
import AppEmpty from "@/components/ui/AppEmpty.vue";

const AdminUserPanelAsync = defineAsyncComponent(() => import("@/components/AdminUserPanel.vue"));

const props = defineProps<{
    sessions: WorkspaceSessionSummary[];
    activeSessionId: string | null;
    sessionActionLoading: boolean;
    snippetsCount: number;
    scenariosCount: number;
    exportTasksCount: number;
    isAuthenticated: boolean;
    currentDatabase: string;
    currentRole: string;
    databaseStatus: string;
    showAdminPanel: boolean;
    adminUsers: AdminUser[];
}>();

const sessionKeyword = ref("");
const sessionSort = ref<"active" | "recent" | "created" | "queries">("active");
const sessionFilter = ref<"all" | "busy" | "fresh">("all");
const filterOptions = [{ value: "all", label: "全部" }, { value: "busy", label: "高频" }, { value: "fresh", label: "新建" }] as const;

const filteredSessions = computed(() => {
    const keyword = sessionKeyword.value.toLowerCase();
    const ranked = [...props.sessions].filter((session) => {
        if (sessionFilter.value === "busy" && !isHighActivitySession(session)) return false;
        if (sessionFilter.value === "fresh" && !isFreshSession(session)) return false;
        if (!keyword) return true;
        return session.currentDatabase.toLowerCase().includes(keyword) || session.sessionId.toLowerCase().includes(keyword);
    });
    ranked.sort((left, right) => {
        if (sessionSort.value === "active") {
            if (left.sessionId === props.activeSessionId) return -1;
            if (right.sessionId === props.activeSessionId) return 1;
            return Date.parse(right.lastUsedAt) - Date.parse(left.lastUsedAt);
        }
        if (sessionSort.value === "recent") return Date.parse(right.lastUsedAt) - Date.parse(left.lastUsedAt);
        if (sessionSort.value === "created") return Date.parse(right.createdAt) - Date.parse(left.createdAt);
        return right.recentQueryCount - left.recentQueryCount;
    });
    return ranked;
});

const sessionGroups = computed(() => {
    if (!filteredSessions.value.length) return [];
    if (sessionKeyword.value) return [{ key: "matched", label: "匹配结果", items: filteredSessions.value }];
    const current = filteredSessions.value.filter((session) => session.sessionId === props.activeSessionId);
    const remaining = filteredSessions.value.filter((session) => session.sessionId !== props.activeSessionId);
    return [
        current.length ? { key: "current", label: "当前", items: current } : null,
        remaining.length ? { key: "others", label: "其他", items: remaining } : null
    ].filter((group): group is { key: string; label: string; items: WorkspaceSessionSummary[] } => Boolean(group));
});

function isHighActivitySession(session: WorkspaceSessionSummary) {
    return session.recentQueryCount >= 5;
}

function isFreshSession(session: WorkspaceSessionSummary) {
    return Date.now() - Date.parse(session.createdAt) <= 1000 * 60 * 60 * 12;
}

function getSessionSummary(session: WorkspaceSessionSummary) {
    if (session.recentQueryCount === 0) return "空会话，可直接开始实验。";
    if (session.recentQueryCount >= 8) return "高频会话，建议继续沿用。";
    return "保留了可复现的近期查询轨迹。";
}

defineEmits<{
    "select-session": [sessionId: string];
    "delete-session": [sessionId: string];
    "provision-user-db": [userId: number];
    "drop-user-db": [userId: number];
}>();
</script>
