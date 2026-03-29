package com.indolyn.rill.app.web;

import com.indolyn.rill.app.dto.ExportTaskRequest;
import com.indolyn.rill.app.dto.ExportTaskResponse;
import com.indolyn.rill.app.service.ExportTaskService;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/workspace/export-tasks")
public class ExportTaskController {

    private final ExportTaskService exportTaskService;

    public ExportTaskController(ExportTaskService exportTaskService) {
        this.exportTaskService = exportTaskService;
    }

    @GetMapping
    public List<ExportTaskResponse> listTasks() {
        return exportTaskService.listTasks();
    }

    @GetMapping("/{id}")
    public ExportTaskResponse getTask(@PathVariable long id) {
        return exportTaskService.getTask(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ExportTaskResponse createTask(@RequestBody ExportTaskRequest request) {
        return exportTaskService.createTask(request);
    }

    @PutMapping("/{id}")
    public ExportTaskResponse updateTask(@PathVariable long id, @RequestBody ExportTaskRequest request) {
        return exportTaskService.updateTask(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteTask(@PathVariable long id) {
        exportTaskService.deleteTask(id);
    }

    @PostMapping("/{id}/run")
    public ExportTaskResponse runTask(@PathVariable long id) {
        return exportTaskService.runTask(id);
    }
}
