package com.kuang.panda_agent.action.moblie;

import org.junit.jupiter.api.Test;

import java.io.IOException;

import static com.kuang.panda_agent.action.moblie.ADB.*;

class ADBTest {
    @Test
    void killServer1() throws IOException {
        killServer();
    }

    @Test
    void startServer1() throws IOException {
        startServer();
    }

    @Test
    void getPath1() {
        getPath();
    }
}