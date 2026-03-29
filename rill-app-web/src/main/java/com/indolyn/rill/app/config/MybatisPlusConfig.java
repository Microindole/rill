package com.indolyn.rill.app.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@MapperScan("com.indolyn.rill.app.persistence.mapper")
public class MybatisPlusConfig {
}
