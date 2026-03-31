<template>
    <section class="section-block grid gap-5 lg:grid-cols-[260px_minmax(0,1fr)]">
        <aside class="border-b border-slate-200/70 pb-4 lg:border-b-0 lg:border-r lg:pb-0 lg:pr-5">
            <p class="mb-3 text-xs font-semibold uppercase tracking-wide text-slate-500">开发文档</p>
            <nav class="space-y-1">
                <RouterLink
                    v-for="page in docsPages"
                    :key="page.slug"
                    :to="`/docs/${page.slug}`"
                    class="block border-l-2 px-3 py-2 text-sm transition"
                    :class="page.slug === activePage.slug ? 'border-slate-900 text-slate-900' : 'border-transparent text-slate-600 hover:border-slate-300 hover:text-slate-900'"
                >
                    {{ page.title }}
                </RouterLink>
            </nav>
        </aside>

        <article class="min-w-0">
            <header class="pb-4">
                <h1 class="text-2xl font-semibold tracking-tight text-slate-900">{{ activePage.title }}</h1>
                <p class="mt-2 text-sm leading-7 text-slate-600">{{ activePage.summary }}</p>
            </header>

            <div class="section-divider pt-4">
                <section v-for="section in activePage.sections" :key="section.title" class="py-4">
                    <h2 class="text-lg font-semibold text-slate-900">{{ section.title }}</h2>
                    <div class="mt-3 space-y-3 text-sm leading-7 text-slate-600">
                        <p v-for="line in section.bullets" :key="line" class="m-0">{{ line }}</p>
                    </div>
                </section>
            </div>
        </article>
    </section>
</template>

<script setup lang="ts">
import { computed, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import { defaultDocsSlug, docsPages } from "@/data/docs";

const route = useRoute();
const router = useRouter();

const activeSlug = computed(() => {
    const slug = route.params.slug;
    return typeof slug === "string" ? slug : defaultDocsSlug;
});

const activePage = computed(() =>
    docsPages.find((item) => item.slug === activeSlug.value) ?? docsPages[0]
);

watch(
    () => activeSlug.value,
    async (slug) => {
        if (!docsPages.some((item) => item.slug === slug)) {
            await router.replace(`/docs/${defaultDocsSlug}`);
        }
    },
    { immediate: true }
);
</script>
