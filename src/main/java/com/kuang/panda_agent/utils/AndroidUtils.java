package com.kuang.panda_agent.utils;

import com.android.ddmlib.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 安装工具类
 */
@Slf4j
public class AndroidUtils {

    public static String getDeviceName(IDevice iDevice) {
        String brand = iDevice.getProperty("ro.product.brand");
        String model = iDevice.getProperty("ro.product.model");
        return String.format("[%s] %s", brand, model);
    }

    /**
     * @return 屏幕分辨率
     */
    public static String getResolution(IDevice iDevice) throws IDeviceExecuteShellCommandException {
        String wmSize = executeShellCommand(iDevice, "wm size");

        Pattern pattern = Pattern.compile("Physical size: (\\d+x\\d+)");
        Matcher matcher = pattern.matcher(wmSize);
        while (matcher.find()) {
            return matcher.group(1);
        }
        throw new RuntimeException(String.format("[%s]cannot find physical size, wm size: %s", iDevice.getSerialNumber(), wmSize));
    }

    /**
     * 获取内存大小
     * @param iDevice
     * @return
     * @throws IDeviceExecuteShellCommandException
     */
    public static String getMemSize(IDevice iDevice) throws IDeviceExecuteShellCommandException {
        String memInfo = executeShellCommand(iDevice, "cat /proc/meminfo |grep MemTotal"); // MemTotal:        1959700 kB
        if (StringUtils.isEmpty(memInfo) || !memInfo.startsWith("MemTotal")) {
            return null;
        }

        String memKB = Pattern.compile("[^0-9]").matcher(memInfo).replaceAll("").trim();
        return Math.ceil(Long.parseLong(memKB) / (1024.0 * 1024)) + " GB";
    }

    public static void installApk(IDevice iDevice, String apkPath) throws InstallException {
        iDevice.installPackage(apkPath, true);
    }

    public static void uninstallApk(IDevice iDevice, String packageName) throws InstallException {
        iDevice.uninstallPackage(packageName);
    }

    public static List<String> getImeList(IDevice iDevice) throws IDeviceExecuteShellCommandException {
        String imeListString = executeShellCommand(iDevice, "ime list -s");
        if (StringUtils.isEmpty(imeListString)) {
            return new ArrayList<>();
        }

        return Arrays.asList(imeListString.split("\\r?\\n"));
    }

    public  static  List<String> getAppList(IDevice iDevice) throws IDeviceExecuteShellCommandException {
        String imeListString = executeShellCommand(iDevice, "pm list package -3");
        if (StringUtils.isEmpty(imeListString)) {
            return new ArrayList<>();
        }
        return Arrays.asList(imeListString.split("\\r?\\n"));
    }

    /**
     * 执行命令
     *
     * @param cmd
     * @return
     */
    public static String executeShellCommand(IDevice iDevice, String cmd) throws IDeviceExecuteShellCommandException {
        Assert.notNull(iDevice, "iDevice can not be null");
        Assert.hasText(cmd, "cmd can not be empty");
        String mobileId = iDevice.getSerialNumber();

        CollectingOutputReceiver collectingOutputReceiver = new CollectingOutputReceiver();
        try {
            log.info("[{}]execute: {}", mobileId, cmd);
            iDevice.executeShellCommand(cmd, collectingOutputReceiver);
        } catch (TimeoutException | AdbCommandRejectedException | ShellCommandUnresponsiveException | IOException e) {
            throw new IDeviceExecuteShellCommandException(e);
        }

        String response = collectingOutputReceiver.getOutput();
        log.info("[{}]response: {}", mobileId, response);
        return response;
    }
}
