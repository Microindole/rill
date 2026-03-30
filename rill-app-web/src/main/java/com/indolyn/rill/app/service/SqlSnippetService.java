package com.indolyn.rill.app.service;

import com.indolyn.rill.app.dto.SqlSnippetRequest;
import com.indolyn.rill.app.dto.SqlSnippetResponse;
import com.indolyn.rill.app.persistence.entity.SqlSnippetEntity;
import com.indolyn.rill.app.persistence.mapper.SqlSnippetMapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;

import java.time.Instant;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class SqlSnippetService {

    private final SqlSnippetMapper sqlSnippetMapper;
    private final CurrentUserProvider currentUserProvider;

    public SqlSnippetService(SqlSnippetMapper sqlSnippetMapper, CurrentUserProvider currentUserProvider) {
        this.sqlSnippetMapper = sqlSnippetMapper;
        this.currentUserProvider = currentUserProvider;
    }

    public List<SqlSnippetResponse> listSnippets() {
        long ownerId = currentUserProvider.requireCurrentUserId();
        return sqlSnippetMapper
            .selectList(
                new LambdaQueryWrapper<SqlSnippetEntity>()
                    .eq(SqlSnippetEntity::getOwnerId, ownerId)
                    .orderByDesc(SqlSnippetEntity::getUpdatedAt))
            .stream()
            .map(this::toResponse)
            .toList();
    }

    public SqlSnippetResponse getSnippet(long id) {
        return toResponse(requireSnippet(id));
    }

    public SqlSnippetResponse createSnippet(SqlSnippetRequest request) {
        validateRequest(request);
        Instant now = Instant.now();
        SqlSnippetEntity entity = new SqlSnippetEntity();
        entity.setOwnerId(currentUserProvider.requireCurrentUserId());
        entity.setTitle(request.title().trim());
        entity.setDescription(normalizeText(request.description()));
        entity.setSqlText(request.sql().trim());
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        sqlSnippetMapper.insert(entity);
        return toResponse(entity);
    }

    public SqlSnippetResponse updateSnippet(long id, SqlSnippetRequest request) {
        validateRequest(request);
        SqlSnippetEntity entity = requireSnippet(id);
        entity.setTitle(request.title().trim());
        entity.setDescription(normalizeText(request.description()));
        entity.setSqlText(request.sql().trim());
        entity.setUpdatedAt(Instant.now());
        sqlSnippetMapper.updateById(entity);
        return toResponse(entity);
    }

    public void deleteSnippet(long id) {
        requireSnippet(id);
        sqlSnippetMapper.deleteById(id);
    }

    private void validateRequest(SqlSnippetRequest request) {
        if (request == null || request.title() == null || request.title().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Snippet title cannot be empty");
        }
        if (request.sql() == null || request.sql().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Snippet SQL cannot be empty");
        }
    }

    private SqlSnippetEntity requireSnippet(long id) {
        SqlSnippetEntity entity =
            sqlSnippetMapper.selectOne(
                new LambdaQueryWrapper<SqlSnippetEntity>()
                    .eq(SqlSnippetEntity::getId, id)
                    .eq(SqlSnippetEntity::getOwnerId, currentUserProvider.requireCurrentUserId())
                    .last("limit 1"));
        if (entity == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "SQL snippet not found");
        }
        return entity;
    }

    private String normalizeText(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private SqlSnippetResponse toResponse(SqlSnippetEntity entity) {
        return new SqlSnippetResponse(
            entity.getId(),
            entity.getTitle(),
            entity.getDescription(),
            entity.getSqlText(),
            entity.getCreatedAt(),
            entity.getUpdatedAt());
    }
}
