package com.my.family.paxl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@Slf4j
public class PaxlApplication {

    public static void main(String[] args) {
        SpringApplication.run(PaxlApplication.class, args);
        log.info("[PaxlApplication] 启动成功");
    }
}
