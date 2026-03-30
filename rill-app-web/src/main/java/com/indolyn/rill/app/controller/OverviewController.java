package com.indolyn.rill.app.controller;

import com.indolyn.rill.app.dto.SystemOverviewResponse;
import com.indolyn.rill.app.service.OverviewService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class OverviewController {

    private final OverviewService overviewService;

    public OverviewController(OverviewService overviewService) {
        this.overviewService = overviewService;
    }

    @GetMapping("/overview")
    public SystemOverviewResponse overview() {
        return overviewService.getOverview();
    }
}
