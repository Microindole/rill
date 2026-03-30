<template>
    <article class="workspace-card">
        <div class="card-header-line">
            <h2>管理员面板</h2>
            <span class="card-caption">管理权限</span>
        </div>
        <div class="list-block" v-if="users.length">
            <div v-for="item in users" :key="item.userId" class="list-item static">
                <strong>{{ item.username }} · {{ item.role }}</strong>
                <span>{{ item.email }} · DB {{ item.kernelDbName }} · {{ item.kernelDbProvisioned ? "已分配" : "未分配" }}</span>
                <div class="console-query-actions">
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
            </div>
        </div>
        <AppEmpty v-else description="暂无用户数据" />
    </article>
</template>

<script setup lang="ts">
import type { AdminUser } from "@/services/api";
import AppButton from "@/components/ui/AppButton.vue";
import AppEmpty from "@/components/ui/AppEmpty.vue";

defineProps<{
    users: AdminUser[];
}>();

defineEmits<{
    provision: [userId: number];
    drop: [userId: number];
}>();
</script>
