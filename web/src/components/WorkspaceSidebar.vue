<template>
    <aside class="workspace-sidebar">
        <article class="workspace-card">
            <div class="card-header-line">
                <h2>工作台会话</h2>
                <span class="card-caption">{{ sessions.length }} 个</span>
            </div>
            <div v-if="sessions.length" class="session-toolbar">
                <input
                    v-model.trim="sessionKeyword"
                    class="app-input"
                    type="search"
                    placeholder="搜索数据库或会话编号"
                />
                <select v-model="sessionSort" class="app-input">
                    <option value="active">当前会话优先</option>
                    <option value="recent">最近使用优先</option>
                    <option value="created">创建时间优先</option>
                    <option value="queries">查询次数优先</option>
                </select>
            </div>
            <div v-if="sessions.length" class="session-filter-row">
                <button
                    v-for="option in filterOptions"
                    :key="option.value"
                    type="button"
                    class="session-filter-chip"
                    :class="{ active: sessionFilter === option.value }"
                    @click="sessionFilter = option.value"
                >
                    {{ option.label }}
                </button>
            </div>
            <div v-if="sessionGroups.length" class="session-group-stack">
                <section v-for="group in sessionGroups" :key="group.key" class="session-group">
                    <div class="session-group-header">
                        <strong>{{ group.label }}</strong>
                        <span>{{ group.items.length }}</span>
                    </div>
                    <div class="list-block">
                        <div
                            v-for="session in group.items"
                            :key="session.sessionId"
                            class="session-pill"
                            :class="{ active: session.sessionId === activeSessionId }"
                        >
                            <strong>{{ session.currentDatabase }}</strong>
                            <div class="session-pill-tags">
                                <span v-if="session.sessionId === activeSessionId" class="session-tag active">当前</span>
                                <span v-if="isHighActivitySession(session)" class="session-tag">高频</span>
                                <span v-if="isFreshSession(session)" class="session-tag">新建</span>
                                <span class="session-tag neutral">{{ getSessionActivityLabel(session) }}</span>
                            </div>
                            <div class="session-heatline" aria-hidden="true">
                                <span :style="{ width: `${getSessionHeatWidth(session)}%` }"></span>
                            </div>
                            <span>{{ session.recentQueryCount }} 次查询</span>
                            <p class="session-pill-summary">{{ getSessionSummary(session) }}</p>
                            <span class="session-pill-meta">
                                最近使用 {{ formatSessionTime(session.lastUsedAt) }} · 创建于 {{ formatSessionTime(session.createdAt) }}
                            </span>
                            <div class="session-pill-actions">
                                <AppButton size="sm" :loading="sessionActionLoading" @click="$emit('select-session', session.sessionId)">
                                    进入会话
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
            <AppEmpty v-else :description="sessions.length ? '没有匹配的会话' : '还没有工作台会话'" />
        </article>

        <article class="workspace-card">
            <div class="card-header-line">
                <h2>资产概览</h2>
                <span class="card-caption">工作区</span>
            </div>
            <div class="asset-summary">
                <div>
                    <span>SQL 片段</span>
                    <strong>{{ snippetsCount }}</strong>
                </div>
                <div>
                    <span>场景脚本</span>
                    <strong>{{ scenariosCount }}</strong>
                </div>
                <div>
                    <span>导出任务</span>
                    <strong>{{ exportTasksCount }}</strong>
                </div>
            </div>
            <p v-if="!isAuthenticated" class="console-guest-note">
                游客模式下只会操作共享数据库。登录后会自动切换到你的个人数据库，并保留自己的工作台资产。
            </p>
            <div class="workspace-section-stack">
                <div class="workspace-section-line">
                    <span>当前数据库</span>
                    <strong>{{ currentDatabase }}</strong>
                </div>
                <div class="workspace-section-line">
                    <span>当前身份</span>
                    <strong>{{ currentRole }}</strong>
                </div>
                <div class="workspace-section-line">
                    <span>数据库状态</span>
                    <strong>{{ databaseStatus }}</strong>
                </div>
            </div>
        </article>

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

const filterOptions = [
    { value: "all", label: "全部" },
    { value: "busy", label: "高频会话" },
    { value: "fresh", label: "新建会话" }
] as const;

const filteredSessions = computed(() => {
    const keyword = sessionKeyword.value.toLowerCase();
    const ranked = [...props.sessions].filter((session) => {
        if (sessionFilter.value === "busy" && !isHighActivitySession(session)) {
            return false;
        }
        if (sessionFilter.value === "fresh" && !isFreshSession(session)) {
            return false;
        }
        if (!keyword) {
            return true;
        }
        return (
            session.currentDatabase.toLowerCase().includes(keyword) ||
            session.sessionId.toLowerCase().includes(keyword)
        );
    });

    ranked.sort((left, right) => {
        if (sessionSort.value === "active") {
            if (left.sessionId === props.activeSessionId) {
                return -1;
            }
            if (right.sessionId === props.activeSessionId) {
                return 1;
            }
            return Date.parse(right.lastUsedAt) - Date.parse(left.lastUsedAt);
        }
        if (sessionSort.value === "recent") {
            return Date.parse(right.lastUsedAt) - Date.parse(left.lastUsedAt);
        }
        if (sessionSort.value === "created") {
            return Date.parse(right.createdAt) - Date.parse(left.createdAt);
        }
        return right.recentQueryCount - left.recentQueryCount;
    });

    return ranked;
});

const sessionGroups = computed(() => {
    if (!filteredSessions.value.length) {
        return [];
    }

    if (sessionKeyword.value) {
        return [
            {
                key: "matched",
                label: "匹配结果",
                items: filteredSessions.value
            }
        ];
    }

    const current = filteredSessions.value.filter((session) => session.sessionId === props.activeSessionId);
    const remaining = filteredSessions.value.filter((session) => session.sessionId !== props.activeSessionId);
    const recent = remaining.slice(0, 3);
    const others = remaining.slice(3);

    return [
        current.length
            ? {
                  key: "current",
                  label: "当前会话",
                  items: current
              }
            : null,
        recent.length
            ? {
                  key: "recent",
                  label: "最近活跃",
                  items: recent
              }
            : null,
        others.length
            ? {
                  key: "others",
                  label: "其他会话",
                  items: others
              }
            : null
    ].filter((group): group is { key: string; label: string; items: WorkspaceSessionSummary[] } => Boolean(group));
});

function formatSessionTime(value: string) {
    return new Intl.DateTimeFormat("zh-CN", {
        month: "2-digit",
        day: "2-digit",
        hour: "2-digit",
        minute: "2-digit"
    }).format(new Date(value));
}

function isHighActivitySession(session: WorkspaceSessionSummary) {
    return session.recentQueryCount >= 5;
}

function isFreshSession(session: WorkspaceSessionSummary) {
    return Date.now() - Date.parse(session.createdAt) <= 1000 * 60 * 60 * 12;
}

function getSessionActivityLabel(session: WorkspaceSessionSummary) {
    const inactiveMinutes = (Date.now() - Date.parse(session.lastUsedAt)) / (1000 * 60);
    if (inactiveMinutes <= 30) {
        return "活跃";
    }
    if (inactiveMinutes <= 24 * 60) {
        return "处理中";
    }
    return "空闲";
}

function getSessionHeatWidth(session: WorkspaceSessionSummary) {
    return Math.max(18, Math.min(100, session.recentQueryCount * 12));
}

function getSessionSummary(session: WorkspaceSessionSummary) {
    if (session.recentQueryCount === 0) {
        return "当前还没有查询历史，适合作为新的实验会话。";
    }
    if (session.recentQueryCount >= 8) {
        return "查询较密集，适合继续追踪这一组结果和执行路径。";
    }
    if (isFreshSession(session)) {
        return "这是近期新建的会话，适合继续补充初始化查询。";
    }
    return "保留了近期查询痕迹，适合继续回看或补跑相关 SQL。";
}

defineEmits<{
    "select-session": [sessionId: string];
    "delete-session": [sessionId: string];
    "provision-user-db": [userId: number];
    "drop-user-db": [userId: number];
}>();
</script>
