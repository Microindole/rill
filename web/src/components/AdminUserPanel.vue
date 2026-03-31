<template>
    <section class="section-divider pt-5">
        <div class="mb-3 flex items-center justify-between">
            <h2 class="text-sm font-semibold text-slate-900">管理员</h2>
            <span class="text-xs text-slate-500">{{ users.length }}</span>
        </div>

        <div v-if="users.length">
            <article v-for="item in users" :key="item.userId" class="border-b border-slate-200/70 py-3">
                <h3 class="text-sm font-semibold text-slate-900">{{ item.username }} · {{ item.role }}</h3>
                <p class="mt-1 text-xs text-slate-500">{{ item.email }}</p>
                <p class="mt-1 text-xs text-slate-500">DB: {{ item.kernelDbName }} · {{ item.kernelDbProvisioned ? "已分配" : "未分配" }}</p>
                <div class="mt-2 flex gap-2">
                    <AppButton size="sm" @click="$emit('provision', item.userId)">分配数据库</AppButton>
                    <AppButton
                        size="sm"
                        variant="danger"
                        subtle
                        :disabled="item.role === 'ADMIN' || item.kernelDbName === 'default'"
                        @click="$emit('drop', item.userId)"
                    >
                        删除数据库
                    </AppButton>
                </div>
            </article>
        </div>

        <AppEmpty v-else description="暂无用户数据" />
    </section>
</template>

<script setup lang="ts">
import type { AdminUser } from "@/services/api";
import AppButton from "@/components/ui/AppButton.vue";
import AppEmpty from "@/components/ui/AppEmpty.vue";

defineProps<{ users: AdminUser[] }>();

defineEmits<{
    provision: [userId: number];
    drop: [userId: number];
}>();
</script>
