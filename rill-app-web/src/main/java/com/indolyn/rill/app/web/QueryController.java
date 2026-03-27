package com.indolyn.rill.app.web;

import com.indolyn.rill.app.dto.QueryExecuteRequest;
import com.indolyn.rill.app.dto.QueryExecuteResponse;
import com.indolyn.rill.app.dto.QueryHistoryItemResponse;
import com.indolyn.rill.app.service.QueryTraceService;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/query")
public class QueryController {

    private final QueryTraceService queryTraceService;

    public QueryController(QueryTraceService queryTraceService) {
        this.queryTraceService = queryTraceService;
    }

    @PostMapping("/execute")
    public QueryExecuteResponse execute(@RequestBody QueryExecuteRequest request) {
        if (request == null || request.sql() == null || request.sql().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "SQL cannot be empty");
        }
        return queryTraceService.execute(request.dbName(), request.sql());
    }

    @GetMapping("/history")
    public List<QueryHistoryItemResponse> history() {
        return queryTraceService.getHistory();
    }

    @GetMapping("/trace/{traceId}")
    @ResponseStatus(HttpStatus.OK)
    public QueryExecuteResponse trace(@PathVariable String traceId) {
        QueryExecuteResponse response = queryTraceService.getTrace(traceId);
        if (response == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Trace not found");
        }
        return response;
    }
}
