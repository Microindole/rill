<template>
    <div class="page-shell">
        <section class="about-hero">
            <p class="hero-eyebrow">Project Story</p>
            <h1>项目介绍</h1>
            <p>
                这一页负责把 Rill 讲成一个完整工程：为什么要拆模块，为什么 Spring Boot 和自研内核要双轨并存，以及这个项目未来如何继续延展到网络编程、Redis 和更复杂的系统层。
            </p>
        </section>

        <section class="about-grid">
            <article class="feature-panel">
                <h2>模块分工</h2>
                <ul class="feature-list">
                    <li><strong>rill-core</strong>：数据库内核，承担 SQL 编译、执行、事务、恢复、索引。</li>
                    <li><strong>rill-app-web</strong>：正式后台，承担用户、工作台、资产和演示 API。</li>
                    <li><strong>web</strong>：完整前端站点，承载首页、登录页、控制台和项目介绍。</li>
                </ul>
            </article>
            <article class="feature-panel accent">
                <h2>为什么不是传统管理系统</h2>
                <p>
                    这里的业务不是订单和审批，而是围绕数据库工作台展开：session、snippet、scenario、export task、query history、dashboard。
                </p>
            </article>
            <article class="feature-panel">
                <h2>身份与数据库边界</h2>
                <ul class="feature-list">
                    <li>PostgreSQL 保存用户、JWT 会话、工作台资产和平台业务表。</li>
                    <li>自研数据库内核保存真正被 SQL 操作的数据。</li>
                    <li>游客只能访问 <strong>default</strong>；注册用户自动分配一个个人数据库；管理员管理库生命周期。</li>
                </ul>
            </article>
            <article class="feature-panel">
                <h2>下一阶段</h2>
                <ul class="feature-list">
                    <li>前端继续把 snippet、scenario、export task 做成真正可用的资产区。</li>
                    <li>后端补密码加密、刷新 token、用户 CRUD 和管理员操作台。</li>
                    <li>再往后才是 Redis、网络编程、更多分布式预留。</li>
                </ul>
            </article>
        </section>
    </div>
</template>

<script setup lang="ts">
</script>
