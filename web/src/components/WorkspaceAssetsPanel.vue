<template>
    <article class="workspace-card workspace-card-wide">
        <div class="workspace-card-header">
            <h2>内容库</h2>
            <span class="card-caption">工作区资产</span>
        </div>
        <div class="asset-toolbar">
            <input
                v-model.trim="assetKeyword"
                class="app-input"
                type="search"
                placeholder="搜索标题、说明、数据库或导出路径"
            />
            <div class="asset-filter-row">
                <button
                    v-for="option in assetFilterOptions"
                    :key="option.value"
                    type="button"
                    class="asset-filter-chip"
                    :class="{ active: assetFilter === option.value }"
                    @click="assetFilter = option.value"
                >
                    {{ option.label }}
                </button>
            </div>
        </div>
        <div class="asset-panel-grid">
            <section class="asset-panel">
                <div class="asset-panel-header">
                    <div class="asset-panel-heading">
                        <strong>SQL 片段</strong>
                        <p>沉淀常用查询片段，便于快速回填和复用。</p>
                    </div>
                    <span>{{ filteredSnippets.length }}</span>
                </div>
                <div class="asset-panel-list" v-if="filteredSnippets.length">
                    <div
                        v-for="snippet in filteredSnippets"
                        :key="snippet.id"
                        class="list-item static compact"
                    >
                        <strong>{{ snippet.title }}</strong>
                        <div class="asset-item-tags">
                            <span class="asset-item-tag">片段</span>
                            <span class="asset-item-tag neutral">{{ getAssetFreshness(snippet.updatedAt) }}</span>
                        </div>
                        <p class="asset-item-summary">{{ snippet.description || snippet.sql }}</p>
                        <span class="asset-task-meta">最近更新 {{ formatAssetTime(snippet.updatedAt) }}</span>
                        <div class="console-query-actions">
                            <AppButton size="sm" @click.stop="$emit('apply-sql', snippet.sql)">填入编辑器</AppButton>
                            <AppButton size="sm" variant="danger" subtle @click.stop="$emit('delete-snippet', snippet.id)">
                                删除片段
                            </AppButton>
                        </div>
                    </div>
                </div>
                <AppEmpty v-else :description="snippets.length ? '当前筛选下没有 SQL 片段' : '暂无 SQL 片段'" />
            </section>

            <section class="asset-panel">
                <div class="asset-panel-header">
                    <div class="asset-panel-heading">
                        <strong>场景脚本</strong>
                        <p>保留成组 SQL，用于回放验证或初始化数据。</p>
                    </div>
                    <span>{{ filteredScenarios.length }}</span>
                </div>
                <div class="asset-panel-list" v-if="filteredScenarios.length">
                    <div
                        v-for="scenario in filteredScenarios"
                        :key="scenario.id"
                        class="list-item static compact"
                    >
                        <strong>{{ scenario.title }}</strong>
                        <div class="asset-item-tags">
                            <span class="asset-item-tag">场景</span>
                            <span class="asset-item-tag neutral">{{ getAssetFreshness(scenario.updatedAt) }}</span>
                        </div>
                        <p class="asset-item-summary">{{ scenario.description || scenario.sqlScript }}</p>
                        <span class="asset-task-meta">最近更新 {{ formatAssetTime(scenario.updatedAt) }}</span>
                        <div class="console-query-actions">
                            <AppButton size="sm" @click.stop="$emit('apply-sql', scenario.sqlScript)">填入编辑器</AppButton>
                            <AppButton size="sm" @click.stop="$emit('run-scenario', scenario.id)">运行场景</AppButton>
                            <AppButton size="sm" variant="danger" subtle @click.stop="$emit('delete-scenario', scenario.id)">
                                删除场景
                            </AppButton>
                        </div>
                    </div>
                </div>
                <AppEmpty v-else :description="scenarios.length ? '当前筛选下没有场景脚本' : '暂无演示场景'" />
            </section>

            <section class="asset-panel">
                <div class="asset-panel-header">
                    <div class="asset-panel-heading">
                        <strong>导出任务</strong>
                        <p>统一查看导出状态、输出路径和失败原因。</p>
                    </div>
                    <span>{{ filteredExportTasks.length }}</span>
                </div>
                <div class="asset-panel-list" v-if="filteredExportTasks.length">
                    <div v-for="task in filteredExportTasks" :key="task.id" class="list-item static compact">
                        <strong>{{ task.title }}</strong>
                        <div class="asset-item-tags">
                            <span class="asset-item-tag">{{ task.exportFormat.toUpperCase() }}</span>
                            <span class="asset-item-tag" :class="getTaskTone(task.status)">{{ getTaskLabel(task.status) }}</span>
                            <span class="asset-item-tag neutral">{{ task.dbName }}</span>
                        </div>
                        <p class="asset-item-summary">{{ getTaskSummary(task) }}</p>
                        <span v-if="task.outputPath" class="asset-task-meta">{{ task.outputPath }}</span>
                        <span v-else-if="task.lastError" class="asset-task-meta is-error">{{ task.lastError }}</span>
                        <span v-else class="asset-task-meta">最近更新 {{ formatAssetTime(task.updatedAt) }}</span>
                        <div class="console-query-actions">
                            <AppButton size="sm" :disabled="task.status === 'RUNNING'" @click="$emit('run-task', task.id)">
                                {{ task.status === "COMPLETED" ? "重新执行" : "执行导出" }}
                            </AppButton>
                            <AppButton size="sm" variant="danger" subtle @click="$emit('delete-task', task.id)">
                                删除任务
                            </AppButton>
                        </div>
                    </div>
                </div>
                <AppEmpty v-else :description="exportTasks.length ? '当前筛选下没有导出任务' : '暂无导出任务'" />
            </section>
        </div>
    </article>
</template>

<script setup lang="ts">
import { computed, ref } from "vue";
import AppEmpty from "@/components/ui/AppEmpty.vue";
import type { DemoScenario, ExportTask, SqlSnippet } from "@/services/api";

defineEmits<{
    "apply-sql": [sql: string];
    "delete-snippet": [snippetId: number];
    "run-scenario": [scenarioId: number];
    "delete-scenario": [scenarioId: number];
    "run-task": [taskId: number];
    "delete-task": [taskId: number];
}>();

const props = defineProps<{
    snippets: SqlSnippet[];
    scenarios: DemoScenario[];
    exportTasks: ExportTask[];
}>();

const assetKeyword = ref("");
const assetFilter = ref<"all" | "recent" | "failed">("all");

const assetFilterOptions = [
    { value: "all", label: "全部资产" },
    { value: "recent", label: "近期更新" },
    { value: "failed", label: "失败任务" }
] as const;

const filteredSnippets = computed(() =>
    props.snippets.filter(
        (snippet) =>
            matchesAssetKeyword([snippet.title, snippet.description, snippet.sql]) &&
            (assetFilter.value !== "recent" || isRecentlyUpdated(snippet.updatedAt))
    )
);

const filteredScenarios = computed(() =>
    props.scenarios.filter((scenario) =>
        matchesAssetKeyword([scenario.title, scenario.description, scenario.sqlScript]) &&
        (assetFilter.value !== "recent" || isRecentlyUpdated(scenario.updatedAt))
    )
);

const filteredExportTasks = computed(() =>
    props.exportTasks.filter((task) => {
        if (!matchesAssetKeyword([task.title, task.description, task.dbName, task.outputPath, task.lastError, task.sql])) {
            return false;
        }
        if (assetFilter.value === "recent") {
            return isRecentlyUpdated(task.updatedAt);
        }
        if (assetFilter.value === "failed") {
            return task.status === "FAILED";
        }
        return true;
    })
);

function formatAssetTime(value: string) {
    return new Intl.DateTimeFormat("zh-CN", {
        month: "2-digit",
        day: "2-digit",
        hour: "2-digit",
        minute: "2-digit"
    }).format(new Date(value));
}

function getAssetFreshness(value: string) {
    const diffHours = (Date.now() - Date.parse(value)) / (1000 * 60 * 60);
    if (diffHours <= 12) {
        return "近期更新";
    }
    if (diffHours <= 24 * 7) {
        return "本周活跃";
    }
    return "历史资产";
}

function getTaskLabel(status: string) {
    if (status === "COMPLETED") {
        return "已完成";
    }
    if (status === "RUNNING") {
        return "执行中";
    }
    if (status === "FAILED") {
        return "失败";
    }
    return status;
}

function getTaskTone(status: string) {
    if (status === "COMPLETED") {
        return "success";
    }
    if (status === "FAILED") {
        return "danger";
    }
    if (status === "RUNNING") {
        return "active";
    }
    return "neutral";
}

function getTaskSummary(task: ExportTask) {
    if (task.status === "COMPLETED") {
        return "导出已经生成，可直接查看产物路径或重新执行。";
    }
    if (task.status === "FAILED") {
        return "最近一次执行失败，建议先查看错误原因再重试。";
    }
    if (task.status === "RUNNING") {
        return "任务正在后台执行，稍后会自动刷新状态。";
    }
    return "任务已创建，等待你手动触发导出。";
}

function matchesAssetKeyword(values: Array<string | null | undefined>) {
    if (!assetKeyword.value) {
        return true;
    }
    const keyword = assetKeyword.value.toLowerCase();
    return values.some((value) => (value ?? "").toLowerCase().includes(keyword));
}

function isRecentlyUpdated(value: string) {
    return Date.now() - Date.parse(value) <= 1000 * 60 * 60 * 24 * 7;
}
</script>
