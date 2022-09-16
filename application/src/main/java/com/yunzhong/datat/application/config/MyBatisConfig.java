package com.yunzhong.datat.application.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * MyBatis相关配置
 */
@Configuration
@EnableTransactionManagement
@MapperScan({"com.yunzhong.datat.application.mapper","com.yunzhong.datat.application.dao"})
public class MyBatisConfig {
}
