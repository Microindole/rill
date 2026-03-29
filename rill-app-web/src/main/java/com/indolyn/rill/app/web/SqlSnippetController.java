package com.indolyn.rill.app.web;

import com.indolyn.rill.app.dto.SqlSnippetRequest;
import com.indolyn.rill.app.dto.SqlSnippetResponse;
import com.indolyn.rill.app.service.SqlSnippetService;

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
@RequestMapping("/api/workspace/snippets")
public class SqlSnippetController {

    private final SqlSnippetService sqlSnippetService;

    public SqlSnippetController(SqlSnippetService sqlSnippetService) {
        this.sqlSnippetService = sqlSnippetService;
    }

    @GetMapping
    public List<SqlSnippetResponse> listSnippets() {
        return sqlSnippetService.listSnippets();
    }

    @GetMapping("/{id}")
    public SqlSnippetResponse getSnippet(@PathVariable long id) {
        return sqlSnippetService.getSnippet(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SqlSnippetResponse createSnippet(@RequestBody SqlSnippetRequest request) {
        return sqlSnippetService.createSnippet(request);
    }

    @PutMapping("/{id}")
    public SqlSnippetResponse updateSnippet(@PathVariable long id, @RequestBody SqlSnippetRequest request) {
        return sqlSnippetService.updateSnippet(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteSnippet(@PathVariable long id) {
        sqlSnippetService.deleteSnippet(id);
    }
}
