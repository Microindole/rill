package com.indolyn.rill.app.service;

import com.indolyn.rill.app.dto.ExportTaskRequest;
import com.indolyn.rill.app.dto.ExportTaskResponse;

import java.nio.file.Path;
import java.util.List;

public interface ExportTaskService {

    List<ExportTaskResponse> listTasks();

    ExportTaskResponse getTask(long id);

    ExportTaskResponse createTask(ExportTaskRequest request);

    ExportTaskResponse updateTask(long id, ExportTaskRequest request);

    void deleteTask(long id);

    ExportTaskResponse runTask(long id);

    Path resolveDownloadPath(long id);
}
