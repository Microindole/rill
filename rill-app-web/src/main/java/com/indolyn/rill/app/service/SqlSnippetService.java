package com.indolyn.rill.app.service;

import com.indolyn.rill.app.dto.SqlSnippetRequest;
import com.indolyn.rill.app.dto.SqlSnippetResponse;

import java.util.List;

public interface SqlSnippetService {

    List<SqlSnippetResponse> listSnippets();

    SqlSnippetResponse getSnippet(long id);

    SqlSnippetResponse createSnippet(SqlSnippetRequest request);

    SqlSnippetResponse updateSnippet(long id, SqlSnippetRequest request);

    void deleteSnippet(long id);
}
