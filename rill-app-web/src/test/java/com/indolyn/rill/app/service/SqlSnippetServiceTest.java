package com.indolyn.rill.app.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.indolyn.rill.app.dto.SqlSnippetRequest;
import com.indolyn.rill.app.dto.SqlSnippetResponse;
import com.indolyn.rill.app.persistence.entity.SqlSnippetEntity;
import com.indolyn.rill.app.persistence.mapper.SqlSnippetMapper;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.web.server.ResponseStatusException;

class SqlSnippetServiceTest {

    @Test
    void listSnippetsShouldReturnOrderedResponses() {
        SqlSnippetMapper sqlSnippetMapper = Mockito.mock(SqlSnippetMapper.class);
        CurrentUserProvider currentUserProvider = Mockito.mock(CurrentUserProvider.class);
        when(currentUserProvider.requireCurrentUserId()).thenReturn(1L);
        SqlSnippetEntity entity = snippet(1L, "Users", "List users", "select * from users;");
        when(sqlSnippetMapper.selectList(any())).thenReturn(List.of(entity));
        SqlSnippetService service = new SqlSnippetService(sqlSnippetMapper, currentUserProvider);

        List<SqlSnippetResponse> responses = service.listSnippets();

        assertEquals(1, responses.size());
        assertEquals("Users", responses.get(0).title());
        assertEquals("select * from users;", responses.get(0).sql());
    }

    @Test
    void createSnippetShouldValidateAndInsert() {
        SqlSnippetMapper sqlSnippetMapper = Mockito.mock(SqlSnippetMapper.class);
        CurrentUserProvider currentUserProvider = Mockito.mock(CurrentUserProvider.class);
        when(currentUserProvider.requireCurrentUserId()).thenReturn(1L);
        SqlSnippetService service = new SqlSnippetService(sqlSnippetMapper, currentUserProvider);

        SqlSnippetResponse response =
            service.createSnippet(new SqlSnippetRequest("Users", "List users", "select * from users;"));

        assertEquals("Users", response.title());
        verify(sqlSnippetMapper).insert(any(SqlSnippetEntity.class));
    }

    @Test
    void updateMissingSnippetShouldReturnNotFound() {
        SqlSnippetMapper sqlSnippetMapper = Mockito.mock(SqlSnippetMapper.class);
        CurrentUserProvider currentUserProvider = Mockito.mock(CurrentUserProvider.class);
        when(currentUserProvider.requireCurrentUserId()).thenReturn(1L);
        SqlSnippetService service = new SqlSnippetService(sqlSnippetMapper, currentUserProvider);

        ResponseStatusException exception =
            assertThrows(
                ResponseStatusException.class,
                () -> service.updateSnippet(99L, new SqlSnippetRequest("Users", "", "select 1;")));

        assertEquals(404, exception.getStatusCode().value());
    }

    private SqlSnippetEntity snippet(Long id, String title, String description, String sql) {
        SqlSnippetEntity entity = new SqlSnippetEntity();
        entity.setId(id);
        entity.setOwnerId(1L);
        entity.setTitle(title);
        entity.setDescription(description);
        entity.setSqlText(sql);
        entity.setCreatedAt(Instant.parse("2026-03-29T00:00:00Z"));
        entity.setUpdatedAt(Instant.parse("2026-03-29T00:00:00Z"));
        return entity;
    }
}
