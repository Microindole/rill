<template>
    <div>
        <section
            ref="heroRef"
            class="home-hero relative flex min-h-[92vh] items-center justify-center overflow-hidden"
            @mousemove="handlePointerMove"
            @mouseleave="handlePointerLeave"
        >
            <canvas ref="canvasRef" class="pointer-events-none absolute inset-0 h-full w-full"></canvas>

            <div class="relative z-10 mx-auto max-w-5xl px-4 text-center sm:px-6">
                <p class="mb-4 text-lg font-semibold text-slate-700">Rill Studio</p>
                <h1 class="mx-auto max-w-4xl text-5xl font-semibold leading-[1.06] tracking-[-0.03em] text-slate-950 sm:text-6xl lg:text-7xl">
                    向下滚动查看项目全景
                </h1>
                <p class="mx-auto mt-5 max-w-2xl text-base leading-7 text-slate-600 sm:text-lg">
                    从内核到前端与命令行，展示真实使用形态。
                </p>
                <p class="mt-8 text-sm font-semibold text-slate-500">向下滚动 ↓</p>
            </div>
        </section>

        <section class="section-divider pt-28 pb-20">
            <div class="grid gap-8 lg:grid-cols-[220px_minmax(0,1fr)]">
                <div>
                    <p class="text-xs font-semibold uppercase tracking-[0.18em] text-slate-500">Kernel</p>
                    <h2 class="mt-2 text-3xl font-semibold tracking-tight text-slate-900">内核代码视角</h2>
                    <p class="mt-3 text-sm leading-7 text-slate-600">来自 rill-core 的代码。</p>
                </div>
                <pre class="type-pane core-pane" v-html="highlightedCore"></pre>
            </div>
        </section>

        <section class="section-divider py-20">
            <div class="grid gap-8 lg:grid-cols-[220px_minmax(0,1fr)]">
                <div>
                    <p class="text-xs font-semibold uppercase tracking-[0.18em] text-slate-500">GUI</p>
                    <h2 class="mt-2 text-3xl font-semibold tracking-tight text-slate-900">图形工作台视角</h2>
                    <p class="mt-3 text-sm leading-7 text-slate-600">按实际控制台布局展示会话、SQL 输入与执行结果。</p>
                </div>
                <div class="gui-shell">
                    <div class="gui-titlebar">
                        <p>rill 高级客户端</p>
                    </div>
                    <div class="gui-toolbar">
                        <span>服务器:</span>
                        <span class="gui-field">localhost ▾</span>
                        <span>端口:</span>
                        <span class="gui-field">8848</span>
                        <span>用户名:</span>
                        <span class="gui-field">root</span>
                        <div class="gui-toolbar-actions">
                            <button type="button">连接</button>
                            <button type="button" class="is-primary" :disabled="guiExecuting" @click="runGuiExecute">
                                {{ guiExecuting ? "执行中..." : "执行(F5)" }}
                            </button>
                            <button type="button">导入SQL</button>
                            <button type="button">查看数据</button>
                        </div>
                    </div>
                    <div class="gui-editor-wrap">
                        <div class="gui-gutter">1</div>
                        <pre class="gui-editor">{{ typed.gui }}<span class="typing-caret"></span></pre>
                    </div>
                    <div class="gui-divider">•••</div>
                    <div class="gui-tabs">
                        <button type="button" :class="{ 'is-active': guiTab === 'table' }" :disabled="!guiExecuted" @click="guiTab = 'table'">表格视图</button>
                        <button type="button" :class="{ 'is-active': guiTab === 'console' }" :disabled="!guiExecuted" @click="guiTab = 'console'">控制台视图</button>
                    </div>
                    <div class="gui-result-wrap">
                        <pre v-if="guiExecuted" class="gui-result">{{ guiTab === "table" ? guiTableView : guiConsoleView }}</pre>
                        <div v-else class="gui-empty">点击“执行(F5)”后显示结果</div>
                    </div>
                </div>
            </div>
        </section>

        <section class="section-divider py-20">
            <div class="grid gap-8 lg:grid-cols-[220px_minmax(0,1fr)]">
                <div>
                    <p class="text-xs font-semibold uppercase tracking-[0.18em] text-slate-500">CLI</p>
                    <h2 class="mt-2 text-3xl font-semibold tracking-tight text-slate-900">命令行使用视角</h2>
                    <p class="mt-3 text-sm leading-7 text-slate-600">按客户端真实交互展示连接、登录、提示符和多行输入。</p>
                </div>
                <div class="cli-shell">
                    <div class="cli-head">Windows Terminal · rill-client</div>
                    <pre class="cli-pane">{{ typed.cli }}<span class="typing-caret"></span></pre>
                </div>
            </div>
        </section>
    </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, reactive, ref } from "vue";

interface Particle {
    x: number;
    y: number;
    vx: number;
    vy: number;
    hue: number;
    seed: number;
}

const heroRef = ref<HTMLElement | null>(null);
const canvasRef = ref<HTMLCanvasElement | null>(null);

const particles: Particle[] = [];
const pointer = { x: 0, y: 0, active: false };
const guiTab = ref<"table" | "console">("table");
const guiExecuted = ref(false);
const guiExecuting = ref(false);

const typed = reactive({ core: "", gui: "", cli: "" });
const guiTableView = `+----+----------+--------+
| id | username | role   |
+----+----------+--------+
| 1  | root     | ADMIN  |
| 2  | visitor  | GUEST  |
+----+----------+--------+
2 rows in set (0.003 sec)`;
const guiConsoleView = `[INFO] connected: localhost:8848
[INFO] session: root@default
[TRACE] parse -> optimize -> execute
[OK] select completed, rows=2, elapsed=3ms`;

const coreSnippets = [
    `public class Planner {\n    public PlanNode createPlan(StatementNode ast) {\n        StatementPlanner<StatementNode> planner = resolvePlanner(ast);\n        if (planner != null) {\n            PlanNode plan = planner.createPlan(ast);\n            TraceCollector.record(\n                \"planner\",\n                component,\n                \"src/main/java/com/indolyn/rill/core/sql/planner/\" + component + \".java\",\n                \"build\",\n                plan == null ? \"当前语句未生成计划节点\" : \"生成计划节点 \" + plan.getClass().getSimpleName());\n            return plan;\n        }\n        throw new UnsupportedOperationException(...);\n    }\n}\n`,
    `public QueryResult executeAndGetResult(String sql, Session session) {\n    List<Token> tokens = lexer.tokenize(sql);\n    StatementNode ast = parser.parse(tokens);\n    LogicalPlan logicalPlan = planner.plan(ast, session);\n    PhysicalPlan physicalPlan = optimizer.optimize(logicalPlan);\n\n    long startAt = System.nanoTime();\n    QueryResult result = executor.execute(physicalPlan, session);\n    long elapsedMs = (System.nanoTime() - startAt) / 1_000_000;\n\n    traceCollector.record(\"executor\", \"query\", sql, \"run\", \"elapsed=\" + elapsedMs + \"ms\");\n    return result;\n}\n`,
    `public void recover() throws IOException {\n    LogCursor cursor = wal.openCursor();\n    while (cursor.hasNext()) {\n        LogEntry entry = cursor.next();\n        if (entry.type() == LogType.BEGIN) {\n            txnManager.markActive(entry.txnId());\n        } else if (entry.type() == LogType.UPDATE) {\n            pageCache.applyRedo(entry.pageId(), entry.afterImage());\n        } else if (entry.type() == LogType.COMMIT) {\n            txnManager.markCommitted(entry.txnId());\n        }\n    }\n    txnManager.rollbackUncommitted(pageCache);\n}\n`
] as const;

const snippets = {
    gui: `-- 输入 SQL\nshow databases;\nuse default;\nselect id, username\nfrom users\nwhere role = 'ADMIN';`,
    cli: `PS D:\\works\\rill> scripts\\rill.cmd sql --host=127.0.0.1 --port=8848 --user=demo\nAttempting to connect to rill server at 127.0.0.1:8848...\nServer: Welcome! Please enter your username:\nUsing username: demo\nServer: Login successful as demo. You can now execute commands.\nType 'exit;' to quit.\n\ndemo@default> use demo;\nDatabase changed to demo\n\ndemo@demo> select id, username, role\n           -> from users\n           -> where role = 'ADMIN';\n+----+----------+--------+\n| id | username | role   |\n+----+----------+--------+\n| 1  | root     | ADMIN  |\n+----+----------+--------+\n-- || trace: parser > planner > executor\nElapsed: 4 ms\n`
};

const highlightedCore = computed(() => highlightJava(typed.core));

let rafId = 0;
let ctx: CanvasRenderingContext2D | null = null;
let width = 0;
let height = 0;
let typingTimers: Array<number> = [];

function initCanvas() {
    const canvas = canvasRef.value;
    if (!canvas) return;

    const dpr = Math.min(window.devicePixelRatio || 1, 2);
    width = canvas.clientWidth;
    height = canvas.clientHeight;
    canvas.width = Math.floor(width * dpr);
    canvas.height = Math.floor(height * dpr);
    ctx = canvas.getContext("2d");
    if (!ctx) return;
    ctx.setTransform(dpr, 0, 0, dpr, 0, 0);

    particles.length = 0;
    const count = Math.max(260, Math.floor((width * height) / 4200));
    for (let i = 0; i < count; i += 1) {
        particles.push({
            x: Math.random() * width,
            y: Math.random() * height,
            vx: (Math.random() - 0.5) * 0.1,
            vy: (Math.random() - 0.5) * 0.1,
            hue: Math.random() > 0.9 ? 338 : 220,
            seed: Math.random() * 1000
        });
    }
}

function animate(time: number) {
    if (!ctx) return;
    ctx.clearRect(0, 0, width, height);

    const t = time * 0.00035;
    for (const p of particles) {
        // Unified global flow (right-down drift with smooth wave field) to avoid chaotic motion.
        const fx = 0.055 + Math.sin((p.y / 180) + t) * 0.028;
        const fy = 0.018 + Math.cos((p.x / 220) + t * 1.2) * 0.022;
        p.vx += fx;
        p.vy += fy;

        if (pointer.active) {
            const dx = pointer.x - p.x;
            const dy = pointer.y - p.y;
            const near = Math.hypot(dx, dy);
            const influence = 172;
            const ringRadius = 60;

            if (near < influence && near > 0.001) {
                const ux = dx / near;
                const uy = dy / near;
                const falloff = 1 - (near / influence);
                const ringError = near - ringRadius;
                const radial = Math.min(0.125, Math.abs(ringError) / ringRadius * 0.1);
                p.vx += ux * Math.sign(ringError) * radial * (0.24 + falloff * 0.62);
                p.vy += uy * Math.sign(ringError) * radial * (0.24 + falloff * 0.62);
            }
        }

        p.vx *= 0.92;
        p.vy *= 0.92;
        p.vx = Math.max(-1.3, Math.min(1.3, p.vx));
        p.vy = Math.max(-1.1, Math.min(1.1, p.vy));
        p.x += p.vx;
        p.y += p.vy;

        if (p.x < -2) p.x = width + 2;
        if (p.x > width + 2) p.x = -2;
        if (p.y < -2) p.y = height + 2;
        if (p.y > height + 2) p.y = -2;

        const speed = Math.hypot(p.vx, p.vy);
        const len = 1.2 + Math.min(4.2, speed * 18);
        const alpha = 0.2 + Math.min(0.5, speed * 0.9);

        ctx.save();
        ctx.translate(p.x, p.y);
        ctx.rotate(Math.atan2(p.vy || 0.001, p.vx || 0.001));
        ctx.fillStyle = `hsla(${p.hue}, 90%, 55%, ${alpha})`;
        ctx.fillRect(-len * 0.5, -0.7, len, 1.4);
        ctx.restore();
    }

    rafId = requestAnimationFrame(animate);
}

function scheduleTyping(key: keyof typeof typed, content: string, delayMs: number, speedMs: number, loop: boolean, loopPauseMs = 1400) {
    const delayTimer = window.setTimeout(() => {
        typed[key] = "";
        if (key === "gui") {
            guiExecuted.value = false;
            guiExecuting.value = false;
            guiTab.value = "table";
        }
        let index = 0;
        const timer = window.setInterval(() => {
            index += 1;
            typed[key] = content.slice(0, index);
            if (index >= content.length) {
                clearInterval(timer);
                if (loop) {
                    const nextTimer = window.setTimeout(() => {
                        scheduleTyping(key, content, 0, speedMs, true, loopPauseMs);
                    }, loopPauseMs);
                    typingTimers.push(nextTimer);
                }
            }
        }, speedMs);
        typingTimers.push(timer);
    }, delayMs);
    typingTimers.push(delayTimer);
}

function runGuiExecute() {
    if (guiExecuting.value) return;
    guiExecuting.value = true;
    const timer = window.setTimeout(() => {
        guiExecuting.value = false;
        guiExecuted.value = true;
    }, 520);
    typingTimers.push(timer);
}

function getRotatingCoreSnippet() {
    const storageKey = "rill.home.core-snippet-index";
    let index = 0;
    try {
        const raw = window.localStorage.getItem(storageKey);
        const prev = raw ? Number.parseInt(raw, 10) : -1;
        index = Number.isNaN(prev) ? 0 : (prev + 1) % coreSnippets.length;
        window.localStorage.setItem(storageKey, String(index));
    } catch {
        index = Math.floor(Math.random() * coreSnippets.length);
    }
    return coreSnippets[index];
}

function escapeHtml(value: string) {
    return value
        .replace(/&/g, "&amp;")
        .replace(/</g, "&lt;")
        .replace(/>/g, "&gt;");
}

function wrapToken(text: string, cls?: string) {
    const escaped = escapeHtml(text);
    return cls ? `<span class="${cls}">${escaped}</span>` : escaped;
}

function highlightJava(source: string) {
    const keywords = new Set([
        "public", "private", "protected", "class", "interface", "record", "enum",
        "static", "final", "void", "new", "return", "throw", "if", "else", "while",
        "for", "try", "catch", "throws", "extends", "implements", "null", "true",
        "false", "package", "import", "switch", "case", "break", "continue", "this"
    ]);
    const typeWords = new Set([
        "String", "List", "Map", "Set", "QueryResult", "StatementNode", "PlanNode",
        "LogicalPlan", "PhysicalPlan", "Session", "TraceCollector", "UnsupportedOperationException"
    ]);

    let out = "";
    let i = 0;

    while (i < source.length) {
        const ch = source[i];
        const next = source[i + 1] ?? "";

        if (ch === "/" && next === "/") {
            let j = i + 2;
            while (j < source.length && source[j] !== "\n") j += 1;
            out += wrapToken(source.slice(i, j), "tok-comment");
            i = j;
            continue;
        }

        if (ch === "/" && next === "*") {
            let j = i + 2;
            while (j < source.length - 1 && !(source[j] === "*" && source[j + 1] === "/")) j += 1;
            j = Math.min(source.length, j + 2);
            out += wrapToken(source.slice(i, j), "tok-comment");
            i = j;
            continue;
        }

        if (ch === "\"") {
            let j = i + 1;
            while (j < source.length) {
                if (source[j] === "\\" && j + 1 < source.length) {
                    j += 2;
                    continue;
                }
                if (source[j] === "\"") {
                    j += 1;
                    break;
                }
                j += 1;
            }
            out += wrapToken(source.slice(i, j), "tok-string");
            i = j;
            continue;
        }

        if (/[0-9]/.test(ch)) {
            let j = i + 1;
            while (j < source.length && /[0-9_]/.test(source[j])) j += 1;
            out += wrapToken(source.slice(i, j), "tok-number");
            i = j;
            continue;
        }

        if (ch === "@") {
            let j = i + 1;
            while (j < source.length && /[A-Za-z0-9_]/.test(source[j])) j += 1;
            out += wrapToken(source.slice(i, j), "tok-annotation");
            i = j;
            continue;
        }

        if (/[A-Za-z_]/.test(ch)) {
            let j = i + 1;
            while (j < source.length && /[A-Za-z0-9_]/.test(source[j])) j += 1;
            const word = source.slice(i, j);
            let cls: string | undefined;
            if (keywords.has(word)) {
                cls = "tok-keyword";
            } else if (typeWords.has(word) || /^[A-Z][A-Za-z0-9_]*$/.test(word)) {
                cls = "tok-type";
            } else if (source[j] === "(") {
                cls = "tok-func";
            }
            out += wrapToken(word, cls);
            i = j;
            continue;
        }

        if ("{}[]();,.<>:=+-*/!?&|".includes(ch)) {
            out += wrapToken(ch, "tok-punct");
            i += 1;
            continue;
        }

        out += wrapToken(ch);
        i += 1;
    }

    return out;
}

function handlePointerMove(event: MouseEvent) {
    const el = heroRef.value;
    if (!el) return;
    const rect = el.getBoundingClientRect();
    pointer.x = event.clientX - rect.left;
    pointer.y = event.clientY - rect.top;
    pointer.active = true;
}

function handlePointerLeave() {
    pointer.active = false;
}

function handleResize() {
    initCanvas();
}

onMounted(() => {
    initCanvas();
    rafId = requestAnimationFrame(animate);
    window.addEventListener("resize", handleResize, { passive: true });

    const activeCoreSnippet = getRotatingCoreSnippet();
    scheduleTyping("core", activeCoreSnippet, 250, 18, false);
    scheduleTyping("gui", snippets.gui, 700, 28, true, 2200);
    scheduleTyping("cli", snippets.cli, 1100, 26, true, 4200);
});

onBeforeUnmount(() => {
    cancelAnimationFrame(rafId);
    window.removeEventListener("resize", handleResize);
    typingTimers.forEach((timer) => {
        clearTimeout(timer);
        clearInterval(timer);
    });
});
</script>

<style scoped>
.type-pane {
    margin: 0;
    min-height: 260px;
    padding: 0;
    background: transparent;
    color: #0f172a;
    font-family: "Consolas", "SFMono-Regular", monospace;
    font-size: 13px;
    line-height: 1.75;
    white-space: pre-wrap;
}

.core-pane {
    min-height: 300px;
    border: 1px solid #2d323d;
    background: linear-gradient(180deg, #171a21 0%, #11141b 100%);
    box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.03), 0 10px 30px rgba(15, 23, 42, 0.18);
    border-radius: 6px;
    padding: 16px 18px;
    color: #d6dbe7;
    line-height: 1.8;
}

.core-pane :deep(.tok-keyword) {
    color: #6ca8ff;
    font-weight: 650;
}

.core-pane :deep(.tok-string) {
    color: #e8b673;
}

.core-pane :deep(.tok-comment) {
    color: #7f8897;
    font-style: italic;
}

.core-pane :deep(.tok-number) {
    color: #be95ff;
}

.core-pane :deep(.tok-type) {
    color: #61d6c4;
}

.core-pane :deep(.tok-func) {
    color: #86b7ff;
}

.core-pane :deep(.tok-annotation) {
    color: #ff9f7a;
}

.core-pane :deep(.tok-punct) {
    color: #8c97ab;
}

.gui-shell,
.cli-shell {
    border: 1px solid #d7dce5;
    background: #fff;
}

.gui-titlebar {
    display: flex;
    align-items: center;
    height: 32px;
    border-bottom: 1px solid #2f3135;
    padding: 0 12px;
    background: #181a1f;
}

.gui-titlebar p {
    margin: 0;
    font-size: 12px;
    color: #d4d4d8;
}

.gui-shell {
    border-color: #2f3135;
    background: #23252b;
}

.gui-toolbar {
    display: flex;
    align-items: center;
    flex-wrap: wrap;
    gap: 8px;
    min-height: 34px;
    border-bottom: 1px solid #30333a;
    padding: 6px 10px;
    color: #c6c8cf;
    font-size: 12px;
}

.gui-field {
    border: 1px solid #3b3f48;
    background: #1d2026;
    padding: 1px 8px;
    color: #e4e7ef;
}

.gui-toolbar-actions {
    margin-left: auto;
    display: flex;
    gap: 6px;
    flex-wrap: wrap;
}

.gui-toolbar-actions button {
    border: 1px solid #3b3f48;
    background: #1d2026;
    color: #d2d4dc;
    padding: 1px 8px;
    font-size: 12px;
    cursor: pointer;
}

.gui-toolbar-actions button:hover {
    border-color: #4b5261;
}

.gui-toolbar-actions button.is-primary {
    border-color: #1766cc;
    background: #0f5fc8;
    color: #f5f9ff;
}

.gui-toolbar-actions button:disabled {
    opacity: 0.65;
    cursor: default;
}

.gui-editor-wrap {
    display: grid;
    grid-template-columns: 32px minmax(0, 1fr);
    min-height: 230px;
    border-bottom: 1px solid #30333a;
}

.gui-gutter {
    border-right: 1px solid #30333a;
    padding: 8px 8px;
    color: #737985;
    font-family: "Consolas", "SFMono-Regular", monospace;
    font-size: 12px;
}

.gui-editor {
    margin: 0;
    padding: 8px 12px;
    background: #2a2d34;
    color: #dde2ef;
    font-family: "Consolas", "SFMono-Regular", monospace;
    font-size: 13px;
    line-height: 1.62;
    white-space: pre-wrap;
}

.gui-divider {
    display: flex;
    align-items: center;
    justify-content: center;
    height: 18px;
    border-bottom: 1px solid #30333a;
    background: #1e2025;
    color: #a3a7b2;
    font-size: 12px;
}

.gui-tabs {
    display: flex;
    gap: 14px;
    height: 28px;
    align-items: center;
    border-bottom: 1px solid #30333a;
    padding: 0 10px;
    color: #bac0cd;
    font-size: 12px;
}

.gui-tabs button {
    border: none;
    background: transparent;
    padding: 0;
    color: #bac0cd;
    font-size: 12px;
    cursor: pointer;
}

.gui-tabs button:disabled {
    opacity: 0.45;
    cursor: default;
}

.gui-tabs .is-active {
    color: #f0f4ff;
    text-decoration: underline;
    text-underline-offset: 5px;
}

.gui-result-wrap {
    min-height: 128px;
    background: #24272d;
}

.gui-result {
    margin: 0;
    min-height: 128px;
    padding: 10px 12px;
    color: #c8cedc;
    font-size: 12px;
    white-space: pre-wrap;
}

.gui-empty {
    display: flex;
    min-height: 128px;
    align-items: center;
    justify-content: center;
    color: #9aa1ae;
    font-size: 12px;
}

.cli-head {
    height: 34px;
    display: flex;
    align-items: center;
    padding: 0 12px;
    border-bottom: 1px solid #1f2937;
    background: #0f172a;
    color: #cbd5e1;
    font-size: 12px;
    font-family: "Consolas", "SFMono-Regular", monospace;
}

.cli-pane {
    margin: 0;
    min-height: 250px;
    padding: 14px 16px;
    background: #0b1220;
    color: #dbeafe;
    font-family: "Consolas", "SFMono-Regular", monospace;
    font-size: 13px;
    line-height: 1.7;
    white-space: pre-wrap;
}

.typing-caret {
    display: inline-block;
    width: 7px;
    height: 1em;
    margin-left: 2px;
    translate: 0 2px;
    background: currentColor;
    opacity: 0.9;
    animation: blink 1s steps(1, end) infinite;
}

@keyframes blink {
    0%,
    45% {
        opacity: 0.9;
    }
    50%,
    100% {
        opacity: 0;
    }
}

@media (max-width: 900px) {
    .gui-toolbar-actions {
        width: 100%;
        margin-left: 0;
    }
}
</style>
