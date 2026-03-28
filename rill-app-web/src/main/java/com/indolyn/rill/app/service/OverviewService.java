package com.indolyn.rill.app.service;

import com.indolyn.rill.app.dto.OverviewCapabilityResponse;
import com.indolyn.rill.app.dto.OverviewExpansionResponse;
import com.indolyn.rill.app.dto.OverviewHighlightResponse;
import com.indolyn.rill.app.dto.OverviewModuleResponse;
import com.indolyn.rill.app.dto.SystemOverviewResponse;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class OverviewService {

    private final DatabaseService databaseService;

    public OverviewService(DatabaseService databaseService) {
        this.databaseService = databaseService;
    }

    public SystemOverviewResponse getOverview() {
        return new SystemOverviewResponse(
            "Rill",
            "数据库内核演示台",
            "不是另一个客户端，而是面向面试和演示的数据库可观测性控制台。",
            buildHighlights(),
            buildModules(),
            buildCapabilities(),
            buildExpansions());
    }

    private List<OverviewHighlightResponse> buildHighlights() {
        return List.of(
            new OverviewHighlightResponse("已加载数据库", String.valueOf(databaseService.getLoadedDatabases().size()),
                "来自 Spring Boot 当前持有的 QueryProcessorRegistry 状态。"),
            new OverviewHighlightResponse("发布形态", "5 类",
                "Windows 安装包、Linux 归档、macOS 归档、纯 Web jar、带 UI Web jar。"),
            new OverviewHighlightResponse("核心方向", "SQL + Storage + Txn",
                "编译链路、执行链路、存储索引、事务恢复已经打通主路径。"),
            new OverviewHighlightResponse("Web 定位", "演示台",
                "用于讲清内核能力、执行路径和扩展路线，而不是替代 GUI 客户端。"));
    }

    private List<OverviewModuleResponse> buildModules() {
        return List.of(
            new OverviewModuleResponse("rill-core", "数据库内核",
                "不直接暴露为 Web 或 GUI 入口",
                "承载 SQL 编译、执行、catalog、表堆、索引、事务、恢复。"),
            new OverviewModuleResponse("rill-server", "服务端外壳",
                "数据库主体安装包的一部分",
                "提供原生服务端与 MySQL/Navicat 兼容服务端。"),
            new OverviewModuleResponse("rill-client", "本地客户端",
                "可选组件",
                "提供 CLI、GUI 与本地工具入口。"),
            new OverviewModuleResponse("rill-app-web", "Spring Boot 演示台后端",
                "独立 Web jar",
                "把内核能力变成浏览器里可讲解、可观察、可展示的演示界面。"),
            new OverviewModuleResponse("web", "Vue 演示台前端",
                "独立前端或嵌入式 UI",
                "围绕 SQL、trace、架构、能力摘要和扩展路线组织信息。"));
    }

    private List<OverviewCapabilityResponse> buildCapabilities() {
        return List.of(
            new OverviewCapabilityResponse("SQL", "Workbench 执行 SQL",
                "可以直接执行 SQL，展示结果、错误、耗时和 trace。"),
            new OverviewCapabilityResponse("Compiler", "编译链路拆分",
                "词法、语法、语义、规划已拆成协作者结构，便于讲开闭原则和方言收口。"),
            new OverviewCapabilityResponse("Execution", "执行计划与运行时",
                "能展示执行阶段命中的组件，而不是只有最终结果表。"),
            new OverviewCapabilityResponse("Storage", "类型、约束、catalog、索引",
                "已经打通真实数据类型与部分约束落地，不是纯 parser demo。"),
            new OverviewCapabilityResponse("Ops", "多平台构建与发布",
                "CI、Release、安装包和独立 Web jar 已具备基础工程化形态。"));
    }

    private List<OverviewExpansionResponse> buildExpansions() {
        return List.of(
            new OverviewExpansionResponse("网络编程", "rill-server / future gateway",
                "继续强化原生协议与 MySQL 协议服务端，并预留统一网关层。",
                "这样面试里可以讲数据库不是库内嵌玩具，而是可通过网络对外服务的系统。"),
            new OverviewExpansionResponse("Redis", "app 层缓存与 future cluster metadata",
                "先作为外层缓存和元数据/会话加速组件接入，不直接塞进内核事务路径。",
                "避免 Redis 把单机内核设计污染成耦合实现，同时保留后续分布式控制面的落点。"),
            new OverviewExpansionResponse("分布式预留", "future cluster-* modules",
                "把 coordinator、gateway、node 做成后续模块，而不是现在把分布式逻辑写死进 core。",
                "这样 Web 和 Spring Boot 后端只需要替换 DatabaseService 的实现，不需要推倒重来。"));
    }
}
