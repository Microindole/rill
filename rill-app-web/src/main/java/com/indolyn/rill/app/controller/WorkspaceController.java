package com.indolyn.rill.app.controller;

import com.indolyn.rill.app.dto.QueryExecuteResponse;
import com.indolyn.rill.app.dto.QueryHistoryItemResponse;
import com.indolyn.rill.app.dto.WorkspaceExecuteRequest;
import com.indolyn.rill.app.dto.WorkspaceSessionResponse;
import com.indolyn.rill.app.dto.WorkspaceSessionSummaryResponse;
import com.indolyn.rill.app.service.WorkspaceService;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/workspace")
public class WorkspaceController {

    private final WorkspaceService workspaceService;

    public WorkspaceController(WorkspaceService workspaceService) {
        this.workspaceService = workspaceService;
    }

    @PostMapping("/sessions")
    @ResponseStatus(HttpStatus.CREATED)
    public WorkspaceSessionResponse createSession() {
        return workspaceService.createSession();
    }

    @GetMapping("/sessions")
    public List<WorkspaceSessionSummaryResponse> listSessions() {
        return workspaceService.listSessions();
    }

    @GetMapping("/sessions/{sessionId}")
    public WorkspaceSessionResponse getSession(@PathVariable String sessionId) {
        return workspaceService.getSession(sessionId);
    }

    @GetMapping("/sessions/{sessionId}/history")
    public List<QueryHistoryItemResponse> getSessionHistory(@PathVariable String sessionId) {
        return workspaceService.getSessionHistory(sessionId);
    }

    @PostMapping("/sessions/{sessionId}/execute")
    public QueryExecuteResponse execute(
        @PathVariable String sessionId, @RequestBody WorkspaceExecuteRequest request) {
        if (request == null || request.sql() == null || request.sql().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "SQL cannot be empty");
        }
        return workspaceService.execute(sessionId, request.sql());
    }

    @DeleteMapping("/sessions/{sessionId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteSession(@PathVariable String sessionId) {
        workspaceService.deleteSession(sessionId);
    }
}
