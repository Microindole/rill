<template>
    <div class="page-shell">
        <section class="about-hero">
            <div class="about-hero-copy">
                <p class="hero-eyebrow">项目说明</p>
                <h1>{{ platform.overview.appName }} 的工程边界</h1>
                <p>
                    这一页把模块拆分、能力构成和下一阶段路线放在同一条叙事里，说明它为什么不是单纯的“数据库控制台截图”，而是一套可扩展的系统作品集。
                </p>
            </div>
            <div class="about-hero-note">
                <span>阶段</span>
                <strong>{{ platform.overview.stage }}</strong>
                <p>{{ platform.overview.positioning }}</p>
            </div>
        </section>

        <section class="about-layout">
            <article class="showcase-panel">
                <div class="showcase-header">
                    <div>
                        <p class="section-kicker">发布边界</p>
                        <h2>模块与发布边界</h2>
                    </div>
                </div>
                <div class="module-list">
                    <div v-for="module in platform.overview.modules" :key="module.name" class="module-item">
                        <strong>{{ module.name }}</strong>
                        <span>{{ module.role }}</span>
                        <p>{{ module.details }}</p>
                        <small>{{ module.releaseBoundary }}</small>
                    </div>
                </div>
            </article>

            <article class="showcase-panel accent">
                <div class="showcase-header">
                    <div>
                        <p class="section-kicker">当前亮点</p>
                        <h2>当前系统亮点</h2>
                    </div>
                </div>
                <div class="capability-list">
                    <div v-for="item in platform.overview.highlights" :key="item.label" class="capability-item">
                        <span>{{ item.label }}</span>
                        <strong>{{ item.value }}</strong>
                        <p>{{ item.detail }}</p>
                    </div>
                </div>
            </article>
        </section>

        <section class="showcase-grid">
            <article class="showcase-panel wide">
                <div class="showcase-header">
                    <div>
                        <p class="section-kicker">架构取舍</p>
                        <h2>为什么 Spring Boot 和自研内核要双轨并存</h2>
                    </div>
                </div>
                <div class="showcase-columns">
                    <div>
                        <h3>平台业务数据</h3>
                        <p>用户、JWT 会话、工作台资产、验证码与邮件链路这类平台业务，交给 PostgreSQL 与 Spring Boot 管理，更适合做站点级产品能力。</p>
                    </div>
                    <div>
                        <h3>真实 SQL 数据面</h3>
                        <p>真正被 SQL 操作的数据保留在自研内核里，这样项目的重点仍然落在编译、执行、索引、事务和恢复，而不是把内核替换成现成数据库。</p>
                    </div>
                </div>
            </article>
        </section>

        <section class="showcase-grid">
            <article class="showcase-panel">
                <div class="showcase-header">
                    <div>
                        <p class="section-kicker">能力清单</p>
                        <h2>能力清单</h2>
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

            <article class="showcase-panel">
                <div class="showcase-header">
                    <div>
                        <p class="section-kicker">后续扩展</p>
                        <h2>下一阶段扩展</h2>
                    </div>
                </div>
                <div class="expansion-list">
                    <div v-for="item in platform.overview.expansions" :key="item.area" class="expansion-item">
                        <strong>{{ item.area }}</strong>
                        <span>{{ item.targetModule }}</span>
                        <p>{{ item.approach }}</p>
                        <small>{{ item.why }}</small>
                    </div>
                </div>
            </article>
        </section>
    </div>
</template>

<script setup lang="ts">
import { onMounted } from "vue";
import { usePlatformStore } from "@/stores/platform";

const platform = usePlatformStore();

onMounted(async () => {
    if (platform.overviewUsingMock || platform.overview.appName === "Rill") {
        await platform.loadOverview();
    }
});
</script>
