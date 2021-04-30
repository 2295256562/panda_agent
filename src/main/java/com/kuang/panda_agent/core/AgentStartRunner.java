package com.kuang.panda_agent.core;

import com.kuang.panda_agent.core.mobile.android.ADB;
import com.kuang.panda_agent.core.mobile.android.AndroidDeviceChangeListener;
import com.kuang.panda_agent.core.mobile.appium.AppiumServer;
import com.kuang.panda_agent.core.mobile.ios.IosDeviceChangeListener;
import com.kuang.panda_agent.core.mobile.ios.IosDeviceMonitor;
import com.kuang.panda_agent.server.ServerClient;
import com.kuang.panda_agent.utils.Terminal;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.List;


@Slf4j
@Component
public class AgentStartRunner implements ApplicationRunner {

    @Autowired
    private ServerClient serverClient;

    @Autowired
    private AndroidDeviceChangeListener androidDeviceChangeListener;

    @Autowired
    private IosDeviceChangeListener iosDeviceChangeListener;

    @Value("${version}")
    private String version;


    @Override
    public void run(ApplicationArguments args) throws Exception {
        System.setProperty("agent.version", version);

        // 移动端
        String appiumVersion = AppiumServer.getVersion();

        // 检查appium
        checkAppiumVersion(appiumVersion);
        System.setProperty("appium.version", appiumVersion);
        ADB.killServer();
        Thread.sleep(1000);
        ADB.startServer();
        ADB.addDeviceChangeListener(androidDeviceChangeListener);
        log.info("开始监听AndroidDevice连接/断开");

        // ios设备
        IosDeviceMonitor iosDeviceMonitor = IosDeviceMonitor.getInstance();
        iosDeviceMonitor.start(iosDeviceChangeListener);
        log.info("开始监听IosDevice连接/断开");

        // 是否配置了aapt
        String aaptVersion = Terminal.execute("aapt v");
        if (!StringUtils.isEmpty(aaptVersion) && aaptVersion.startsWith("Android")) {
            System.setProperty("aapt", "true");
        } else {
            System.setProperty("aapt", "false");
        }

        // ffmpeg
        Terminal.execute("ffmpeg -version");

    }

    private void checkAppiumVersion(String appiumVersion) {
        if (StringUtils.isEmpty(appiumVersion) || !appiumVersion.matches("\\d+.\\d+.\\d+")) {
            throw new IllegalArgumentException("非法的appium版本: " + appiumVersion);
        }

        String[] appiumVersionArr = appiumVersion.split("\\.");
        int first = Integer.parseInt(appiumVersionArr[0]);
        int middle = Integer.parseInt(appiumVersionArr[1]);

        if (first < 1 || (first == 1 && middle < 16)) {
            throw new IllegalArgumentException("appium版本不能低于1.16.0");
        }
    }
}
