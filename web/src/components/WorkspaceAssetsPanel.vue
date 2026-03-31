<template>
    <section class="section-divider pt-5 xl:col-span-2">
        <div class="mb-4 flex flex-wrap items-center justify-between gap-3">
            <h2 class="text-base font-semibold text-slate-900">内容库</h2>
            <div class="flex flex-wrap gap-3">
                <button
                    v-for="option in assetFilterOptions"
                    :key="option.value"
                    type="button"
                    class="text-xs font-semibold"
                    :class="assetFilter === option.value ? 'text-slate-900' : 'text-slate-500 hover:text-slate-700'"
                    @click="assetFilter = option.value"
                >
                    {{ option.label }}
                </button>
            </div>
        </div>

        <input v-model.trim="assetKeyword" class="app-input mb-4" type="search" placeholder="搜索标题、说明、SQL、输出路径" />

        <div class="grid gap-5 lg:grid-cols-3">
            <section>
                <div class="mb-3 flex items-center justify-between">
                    <strong class="text-sm text-slate-900">SQL 片段</strong>
                    <span class="text-xs text-slate-500">{{ filteredSnippets.length }}</span>
                </div>
                <div v-if="filteredSnippets.length">
                    <article v-for="snippet in filteredSnippets" :key="snippet.id" class="border-b border-slate-200/70 py-3">
                        <h3 class="text-sm font-semibold text-slate-900">{{ snippet.title }}</h3>
                        <p class="mt-1 line-clamp-2 text-xs text-slate-500">{{ snippet.description || snippet.sql }}</p>
                        <div class="mt-2 flex gap-2">
                            <AppButton size="sm" @click.stop="$emit('apply-sql', snippet.sql)">填入</AppButton>
                            <AppButton size="sm" variant="danger" subtle @click.stop="$emit('delete-snippet', snippet.id)">删除</AppButton>
                        </div>
                    </article>
                </div>
                <AppEmpty v-else :description="snippets.length ? '当前筛选下无片段' : '暂无 SQL 片段'" />
            </section>

            <section>
                <div class="mb-3 flex items-center justify-between">
                    <strong class="text-sm text-slate-900">场景脚本</strong>
                    <span class="text-xs text-slate-500">{{ filteredScenarios.length }}</span>
                </div>
                <div v-if="filteredScenarios.length">
                    <article v-for="scenario in filteredScenarios" :key="scenario.id" class="border-b border-slate-200/70 py-3">
                        <h3 class="text-sm font-semibold text-slate-900">{{ scenario.title }}</h3>
                        <p class="mt-1 line-clamp-2 text-xs text-slate-500">{{ scenario.description || scenario.sqlScript }}</p>
                        <div class="mt-2 flex gap-2">
                            <AppButton size="sm" @click.stop="$emit('apply-sql', scenario.sqlScript)">填入</AppButton>
                            <AppButton size="sm" @click.stop="$emit('run-scenario', scenario.id)">运行</AppButton>
                            <AppButton size="sm" variant="danger" subtle @click.stop="$emit('delete-scenario', scenario.id)">删除</AppButton>
                        </div>
                    </article>
                </div>
                <AppEmpty v-else :description="scenarios.length ? '当前筛选下无场景' : '暂无场景脚本'" />
            </section>

            <section>
                <div class="mb-3 flex items-center justify-between">
                    <strong class="text-sm text-slate-900">导出任务</strong>
                    <span class="text-xs text-slate-500">{{ filteredExportTasks.length }}</span>
                </div>
                <div v-if="filteredExportTasks.length">
                    <article v-for="task in filteredExportTasks" :key="task.id" class="border-b border-slate-200/70 py-3">
                        <div class="flex items-center justify-between gap-2">
                            <h3 class="text-sm font-semibold text-slate-900">{{ task.title }}</h3>
                            <span class="text-xs text-slate-500">{{ task.exportFormat.toUpperCase() }}</span>
                        </div>
                        <p class="mt-1 text-xs text-slate-500">{{ getTaskLabel(task.status) }} · {{ task.dbName }}</p>
                        <p class="mt-1 line-clamp-2 text-xs text-slate-500">{{ getTaskSummary(task) }}</p>
                        <div class="mt-2 flex gap-2">
                            <AppButton size="sm" :disabled="task.status === 'RUNNING'" @click="$emit('run-task', task.id)">
                                {{ task.status === "COMPLETED" ? "重跑" : "执行" }}
                            </AppButton>
                            <AppButton
                                v-if="task.status === 'COMPLETED'"
                                size="sm"
                                subtle
                                @click="$emit('download-task', task.id)"
                            >
                                下载
                            </AppButton>
                            <AppButton size="sm" variant="danger" subtle @click="$emit('delete-task', task.id)">删除</AppButton>
                        </div>
                    </article>
                </div>
                <AppEmpty v-else :description="exportTasks.length ? '当前筛选下无任务' : '暂无导出任务'" />
            </section>
        </div>
    </section>
</template>

<script setup lang="ts">
import { computed, ref } from "vue";
import AppButton from "@/components/ui/AppButton.vue";
import AppEmpty from "@/components/ui/AppEmpty.vue";
import type { DemoScenario, ExportTask, SqlSnippet } from "@/services/api";

defineEmits<{
    "apply-sql": [sql: string];
    "delete-snippet": [snippetId: number];
    "run-scenario": [scenarioId: number];
    "delete-scenario": [scenarioId: number];
    "run-task": [taskId: number];
    "download-task": [taskId: number];
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
    { value: "all", label: "全部" },
    { value: "recent", label: "近 7 天" },
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
    props.scenarios.filter(
        (scenario) =>
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

function getTaskLabel(status: string) {
    if (status === "COMPLETED") return "已完成";
    if (status === "RUNNING") return "执行中";
    if (status === "FAILED") return "失败";
    return "待执行";
}

function getTaskSummary(task: ExportTask) {
    if (task.status === "COMPLETED") return task.outputPath || "已生成输出文件";
    if (task.status === "FAILED") return task.lastError || "任务执行失败";
    if (task.status === "RUNNING") return "任务正在后台运行";
    return "任务已创建，等待执行";
}

function matchesAssetKeyword(values: Array<string | null | undefined>) {
    if (!assetKeyword.value) return true;
    const keyword = assetKeyword.value.toLowerCase();
    return values.some((value) => (value ?? "").toLowerCase().includes(keyword));
}

function isRecentlyUpdated(value: string) {
    return Date.now() - Date.parse(value) <= 1000 * 60 * 60 * 24 * 7;
}
</script>
