package com.indolyn.rill.app.boot;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.indolyn.rill.app.persistence.entity.AppUserEntity;
import com.indolyn.rill.app.persistence.entity.DemoScenarioEntity;
import com.indolyn.rill.app.persistence.entity.SqlSnippetEntity;
import com.indolyn.rill.app.persistence.mapper.AppUserMapper;
import com.indolyn.rill.app.persistence.mapper.DemoScenarioMapper;
import com.indolyn.rill.app.persistence.mapper.SqlSnippetMapper;

import java.time.Instant;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class AppDemoDataBootstrap implements ApplicationRunner {

    private final AppUserMapper appUserMapper;
    private final SqlSnippetMapper sqlSnippetMapper;
    private final DemoScenarioMapper demoScenarioMapper;

    public AppDemoDataBootstrap(
        AppUserMapper appUserMapper, SqlSnippetMapper sqlSnippetMapper, DemoScenarioMapper demoScenarioMapper) {
        this.appUserMapper = appUserMapper;
        this.sqlSnippetMapper = sqlSnippetMapper;
        this.demoScenarioMapper = demoScenarioMapper;
    }

    @Override
    public void run(ApplicationArguments args) {
        AppUserEntity demoUser =
            appUserMapper.selectOne(new QueryWrapper<AppUserEntity>().eq("username", "demo").last("limit 1"));
        if (demoUser == null) {
            return;
        }
        ensureDefaultSnippet(demoUser.getId());
        ensureDefaultScenario(demoUser.getId());
    }

    private void ensureDefaultSnippet(Long ownerId) {
        SqlSnippetEntity existing =
            sqlSnippetMapper.selectOne(
                new QueryWrapper<SqlSnippetEntity>().eq("owner_id", ownerId).eq("title", "查看用户列表示例").last("limit 1"));
        if (existing != null) {
            return;
        }
        Instant now = Instant.now();
        SqlSnippetEntity entity = new SqlSnippetEntity();
        entity.setOwnerId(ownerId);
        entity.setTitle("查看用户列表示例");
        entity.setDescription("默认演示用 snippet");
        entity.setSqlText("select * from users;");
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        sqlSnippetMapper.insert(entity);
    }

    private void ensureDefaultScenario(Long ownerId) {
        DemoScenarioEntity existing =
            demoScenarioMapper.selectOne(
                new QueryWrapper<DemoScenarioEntity>().eq("owner_id", ownerId).eq("title", "初始化演示用户表").last("limit 1"));
        if (existing != null) {
            return;
        }
        Instant now = Instant.now();
        DemoScenarioEntity entity = new DemoScenarioEntity();
        entity.setOwnerId(ownerId);
        entity.setTitle("初始化演示用户表");
        entity.setDescription("默认演示场景，创建表并写入一条用户数据");
        entity.setSqlScript(
            "create database demo; use demo; create table users (id int primary key, name varchar(32)); insert into users (id, name) values (1, 'alice');");
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        demoScenarioMapper.insert(entity);
    }
}
