import type { SystemOverview } from "@/types/overview";

export const mockOverview: SystemOverview = {
    appName: "Rill",
    stage: "数据库内核演示台",
    positioning: "不是另一个客户端，而是面向面试和演示的数据库可观测性控制台。",
    highlights: [
        {
            label: "核心方向",
            value: "SQL + Storage + Txn",
            detail: "编译链路、执行链路、存储索引、事务恢复已经打通主路径。"
        },
        {
            label: "发布形态",
            value: "5 类",
            detail: "Windows 安装包、Linux 归档、macOS 归档、纯 Web jar、带 UI Web jar。"
        },
        {
            label: "Web 定位",
            value: "演示台",
            detail: "用于讲清内核能力、执行路径和扩展路线，而不是替代 GUI 客户端。"
        },
        {
            label: "下一阶段",
            value: "Network + Redis",
            detail: "通过服务端、网关和应用层缓存把数据库项目扩到更完整的系统维度。"
        }
    ],
    modules: [
        {
            name: "rill-core",
            role: "数据库内核",
            releaseBoundary: "不直接暴露为 Web 或 GUI 入口",
            details: "承载 SQL 编译、执行、catalog、表堆、索引、事务、恢复。"
        },
        {
            name: "rill-server",
            role: "服务端外壳",
            releaseBoundary: "数据库主体安装包的一部分",
            details: "提供原生服务端与 MySQL/Navicat 兼容服务端。"
        },
        {
            name: "rill-client",
            role: "本地客户端",
            releaseBoundary: "可选组件",
            details: "提供 CLI、GUI 与本地工具入口。"
        },
        {
            name: "rill-app-web",
            role: "Spring Boot 演示台后端",
            releaseBoundary: "独立 Web jar",
            details: "把内核能力变成浏览器里可讲解、可观察、可展示的演示界面。"
        }
    ],
    capabilities: [
        {
            category: "SQL",
            title: "Workbench 执行 SQL",
            details: "直接执行 SQL，展示结果、错误、耗时和 trace。"
        },
        {
            category: "Compiler",
            title: "编译链路拆分",
            details: "词法、语法、语义、规划已拆成协作者结构，能讲开闭原则和方言收口。"
        },
        {
            category: "Execution",
            title: "执行计划与运行时",
            details: "不仅展示结果，还展示执行阶段命中的组件。"
        },
        {
            category: "Ops",
            title: "多平台发布",
            details: "CI、Release、安装包和独立 Web jar 已具备基础工程化形态。"
        }
    ],
    expansions: [
        {
            area: "网络编程",
            targetModule: "rill-server / future gateway",
            approach: "继续强化原生协议与 MySQL 协议服务端，并预留统一网关层。",
            why: "这样可以讲数据库是通过网络对外服务的系统，不只是内嵌库。"
        },
        {
            area: "Redis",
            targetModule: "app 层缓存与 future cluster metadata",
            approach: "先作为外层缓存和元数据/会话加速组件接入，不直接塞进内核事务路径。",
            why: "避免 Redis 把单机内核设计污染成耦合实现，同时保留后续分布式控制面的落点。"
        }
    ]
};
