package com.uni.research;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Uni-Research-Assistant 启动类
 * 
 * @author wrench1024
 * @since 2026-01-02
 */
@SpringBootApplication
public class UniResearchApplication {

    public static void main(String[] args) {
        SpringApplication.run(UniResearchApplication.class, args);
        System.out.println("""

                ========================================
                Uni-Research-Assistant 启动成功！
                API 文档地址: http://localhost:8080/api/doc.html
                ========================================
                """);
    }
}
