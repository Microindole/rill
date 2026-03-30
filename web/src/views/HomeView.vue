<template>
    <div class="page-shell">
        <section class="hero-banner">
            <div class="hero-copy">
                <p class="hero-eyebrow">Database Kernel Showcase</p>
                <h1>Rill 不是一个演示控制台，而是一套可讲清楚的数据库系统作品集。</h1>
                <p class="hero-text">
                    首页负责讲价值，控制台负责演示 SQL，项目介绍负责讲架构与取舍。这样整站才像一个完整产品，而不是一张截图式页面。
                </p>
                <div class="hero-cta">
                    <RouterLink class="primary-link" to="/console">进入控制台</RouterLink>
                    <RouterLink class="secondary-link" to="/about">查看项目介绍</RouterLink>
                </div>
            </div>
            <div class="hero-card-stack">
                <article class="hero-stat-card">
                    <span>Backend</span>
                    <strong>Spring Boot + PostgreSQL</strong>
                    <p>承载用户、工作台、片段、场景、导出任务和演示资产。</p>
                </article>
                <article class="hero-stat-card alt">
                    <span>Kernel</span>
                    <strong>Self-built SQL Engine</strong>
                    <p>负责解析、计划、执行、索引、事务与恢复等数据库内核能力。</p>
                </article>
            </div>
        </section>

        <section class="home-grid">
            <article class="feature-panel">
                <h2>首页放什么内容</h2>
                <p>首页应该回答“这个项目到底做了什么、为什么值得看”，而不是直接塞一个 SQL 输入框。</p>
                <ul class="feature-list">
                    <li>项目定位：自研数据库内核 + 正式工作台后台 + Web 演示站点。</li>
                    <li>核心卖点：编译链路、执行链路、索引、恢复、协议层、多模块发布。</li>
                    <li>面试叙事：从工程架构、系统设计到 UI 展示都能展开。</li>
                </ul>
            </article>
            <article class="feature-panel accent">
                <h2>控制台页放什么内容</h2>
                <p>控制台不是整个网站本体，它应该是“已登录用户”的工作区。</p>
                <ul class="feature-list">
                    <li>工作台 session</li>
                    <li>SQL 执行与 trace</li>
                    <li>Snippet / Scenario / Export Task 资产</li>
                    <li>用户自己的历史与当前数据库上下文</li>
                </ul>
            </article>
            <article class="feature-panel">
                <h2>项目介绍页放什么内容</h2>
                <p>这里用来讲模块结构、发布物、重构方向，以及为什么 Spring Boot 和内核要双轨并存。</p>
            </article>
        </section>

        <section class="home-grid">
            <article class="feature-panel">
                <h2>三种身份怎么分工</h2>
                <ul class="feature-list">
                    <li>游客：只进入共享 <code>default</code> 数据库，适合快速体验。</li>
                    <li>普通用户：注册后自动拥有一个同名内核数据库，并继续能访问 <code>default</code>。</li>
                    <li>管理员：拥有站点管理能力，也是唯一能创建和删除内核数据库的角色。</li>
                </ul>
            </article>
            <article class="feature-panel accent">
                <h2>为什么首页要讲这些</h2>
                <p>
                    因为这不是一个简单的 SQL 演示页面。首页要先把“平台业务数据在 PostgreSQL、真实数据面在自研内核、前端通过 JWT 调后台”的结构讲清楚。
                </p>
            </article>
        </section>
    </div>
</template>

<script setup lang="ts">
</script>
