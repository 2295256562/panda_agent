package com.kuang.panda_agent;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = {"com.kuang.panda_agent"})
@EnableScheduling
public class PandaAgentApplication {

    public static void main(String[] args) {
        SpringApplication.run(PandaAgentApplication.class, args);
    }

}
