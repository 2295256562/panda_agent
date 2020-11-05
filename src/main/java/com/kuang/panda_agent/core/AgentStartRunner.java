package com.kuang.panda_agent.core;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;


@Slf4j
@Component
public class AgentStartRunner implements ApplicationRunner {

    @Value("${version}")
    private String version;


    @Override
    public void run(ApplicationArguments args) throws Exception {
        System.setProperty("agent.version", version);
    }
}
