package com.indolyn.rill.app.dto;

import java.util.List;

public record SystemOverviewResponse(
    String appName,
    String stage,
    String positioning,
    List<OverviewHighlightResponse> highlights,
    List<OverviewModuleResponse> modules,
    List<OverviewCapabilityResponse> capabilities,
    List<OverviewExpansionResponse> expansions) {}
