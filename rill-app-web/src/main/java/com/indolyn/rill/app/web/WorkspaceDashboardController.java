package com.indolyn.rill.app.web;

import com.indolyn.rill.app.dto.WorkspaceDashboardResponse;
import com.indolyn.rill.app.service.WorkspaceDashboardService;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/workspace")
public class WorkspaceDashboardController {

    private final WorkspaceDashboardService workspaceDashboardService;

    public WorkspaceDashboardController(WorkspaceDashboardService workspaceDashboardService) {
        this.workspaceDashboardService = workspaceDashboardService;
    }

    @GetMapping("/dashboard")
    public WorkspaceDashboardResponse dashboard() {
        return workspaceDashboardService.getDashboard();
    }
}
