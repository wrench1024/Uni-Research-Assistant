package com.uni.research.common.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


/**
 * MyBatis-Plus 配置类
 * 
 * 对应 408 考点：数据库 - 分页查询优化
 * 
 * 设计思路：
 * 1. 配置分页插件，自动处理分页逻辑
 * 2. 指定数据库类型为 MySQL
 * 3. 扫描 Mapper 接口
 * 
 * 面试话术：
 * "我配置了 MyBatis-Plus 的分页插件，它会自动在 SQL 中添加 LIMIT 子句。
 * 分页插件基于 MyBatis 的拦截器机制实现，在 SQL 执行前动态修改 SQL 语句。
 * 这样可以避免手动拼接分页 SQL，提高开发效率，同时保证 SQL 的正确性。"
 * 
 * @author wrench1024
 * @since 2026-01-02
 */
@Configuration
@MapperScan("com.uni.research.module.*.mapper")
public class MyBatisPlusConfig {
    
    /**
     * 分页插件
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        // 添加分页插件
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        return interceptor;
    }
}
