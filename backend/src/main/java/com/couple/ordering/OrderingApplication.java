package com.couple.ordering;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 情侣点餐系统后端启动类。
 *
 * <p>使用 {@link MapperScan} 统一扫描 Mapper 接口，避免每个 Mapper 重复标注 {@code @Mapper}。</p>
 */
@SpringBootApplication
@EnableScheduling
@MapperScan("com.couple.ordering.mapper")
public class OrderingApplication {

    public static void main(String[] args) {
        SpringApplication.run(OrderingApplication.class, args);
    }
}
