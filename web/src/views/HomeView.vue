<template>
    <div class="page-shell">
        <section class="hero-banner">
            <div class="hero-copy">
                <p class="hero-eyebrow">{{ platform.overview.appName }} · {{ platform.overview.stage }}</p>
                <h1>{{ platform.overview.positioning }}</h1>
                <p class="hero-text">
                    Rill 把自研数据库内核、Spring Boot 业务壳和浏览器工作台收进同一套演示路径里，让“能运行”和“能讲清楚”同时成立。
                </p>
                <div class="hero-cta">
                    <RouterLink class="primary-link" to="/console">进入工作台</RouterLink>
                    <RouterLink class="secondary-link" to="/about">查看项目结构</RouterLink>
                </div>
                <div class="hero-context-strip">
                    <div class="hero-context-item">
                        <span>模块数</span>
                        <strong>{{ platform.overview.modules.length }}</strong>
                    </div>
                    <div class="hero-context-item">
                        <span>能力项</span>
                        <strong>{{ platform.overview.capabilities.length }}</strong>
                    </div>
                    <div class="hero-context-item">
                        <span>体验入口</span>
                        <strong>游客 / 用户 / 管理员</strong>
                    </div>
                </div>
            </div>
            <div class="hero-card-stack">
                <article v-for="item in topHighlights" :key="item.label" class="hero-stat-card">
                    <span>{{ item.label }}</span>
                    <strong>{{ item.value }}</strong>
                    <p>{{ item.detail }}</p>
                </article>
            </div>
        </section>

        <section class="showcase-grid">
            <article class="showcase-panel wide">
                <div class="showcase-header">
                    <div>
                        <p class="section-kicker">核心叙事</p>
                        <h2>一套能从内核讲到产品界面的数据库作品集</h2>
                    </div>
                    <span class="card-caption">产品叙事</span>
                </div>
                <div class="showcase-columns">
                    <div>
                        <h3>面向展示</h3>
                        <p>首页负责解释项目价值，工作台负责呈现 SQL 执行，项目介绍页负责讲清楚模块边界和工程取舍。</p>
                    </div>
                    <div>
                        <h3>面向实现</h3>
                        <p>浏览器里看到的不只是 UI，而是经过 Spring Boot、认证、工作台业务层和自研数据库内核串起来的一整条主路径。</p>
                    </div>
                </div>
            </article>

            <article class="showcase-panel accent">
                <p class="section-kicker">Identity</p>
                <h2>三种身份，三种数据库边界</h2>
                <ul class="feature-list">
                    <li>游客直接进入共享 <code>default</code> 数据库，适合快速体验。</li>
                    <li>普通用户注册后拥有个人数据库与自己的工作台资产。</li>
                    <li>管理员负责数据库生命周期、用户分配和平台治理操作。</li>
                </ul>
            </article>
        </section>

        <section class="showcase-grid">
            <article class="showcase-panel">
                <div class="showcase-header">
                    <div>
                        <p class="section-kicker">Modules</p>
                        <h2>当前模块结构</h2>
                    </div>
                </div>
                <div class="module-list">
                    <div v-for="module in platform.overview.modules" :key="module.name" class="module-item">
                        <strong>{{ module.name }}</strong>
                        <span>{{ module.role }}</span>
                        <p>{{ module.details }}</p>
                    </div>
                </div>
            </article>

            <article class="showcase-panel">
                <div class="showcase-header">
                    <div>
                        <p class="section-kicker">Capabilities</p>
                        <h2>已经能演示的能力</h2>
                    </div>
                </div>
                <div class="capability-list">
                    <div v-for="item in platform.overview.capabilities" :key="item.title" class="capability-item">
                        <span>{{ item.category }}</span>
                        <strong>{{ item.title }}</strong>
                        <p>{{ item.details }}</p>
                    </div>
                </div>
            </article>
        </section>
    </div>
</template>

<script setup lang="ts">
import { computed, onMounted } from "vue";
import { usePlatformStore } from "@/stores/platform";

const platform = usePlatformStore();

const topHighlights = computed(() => platform.overview.highlights.slice(0, 2));

onMounted(async () => {
    if (platform.overviewUsingMock || platform.overview.appName === "Rill") {
        await platform.loadOverview();
    }
});
</script>
