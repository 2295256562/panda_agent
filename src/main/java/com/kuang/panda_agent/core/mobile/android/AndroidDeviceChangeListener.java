package com.kuang.panda_agent.core.mobile.android;

import com.android.ddmlib.AndroidDebugBridge;
import com.android.ddmlib.IDevice;
import com.kuang.panda_agent.core.Device;
import com.kuang.panda_agent.core.mobile.Mobile;
import com.kuang.panda_agent.core.mobile.MobileChangeHandler;
import com.kuang.panda_agent.core.mobile.MobileDevice;
import com.kuang.panda_agent.core.mobile.android.scrcpy.Scrcpy;
import com.kuang.panda_agent.core.mobile.android.stf.*;
import com.kuang.panda_agent.core.mobile.appium.AppiumServer;
import com.kuang.panda_agent.model.UploadFile;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.Date;


@Component
@Slf4j
public class AndroidDeviceChangeListener extends MobileChangeHandler implements AndroidDebugBridge.IDeviceChangeListener {

    private static final String APIDEMOS_APK = "vendor/apk/ApiDemos-debug.apk";

    @Override
    public void deviceConnected(IDevice device) {
        new Thread(() -> androidDeviceConnected(device)).start();
    }

    private void androidDeviceConnected(IDevice iDevice) {
        String mobileId = iDevice.getSerialNumber();
        log.info("[{}]已连接", mobileId);

        log.info("[{}]等待上线", mobileId);
        AndroidUtil.waitForMobileOnline(iDevice, 5 * 60);
        log.info("[{}]已上线", mobileId);

        mobileConnected(iDevice);
    }

    @Override
    public void deviceDisconnected(IDevice device) {
        new Thread(() -> mobileDisconnected(device.getSerialNumber())).start();
    }

    @Override
    public void deviceChanged(IDevice iDevice, int changeMask) {
    }

    @Override
    protected Device newMobile(IDevice iDevice, Mobile mobile, AppiumServer appiumServer) {
        return new AndroidDevice(mobile, iDevice, appiumServer);
    }

    @Override
    protected MobileDevice initMobile(IDevice iDevice, AppiumServer appiumServer) throws Exception {
        String mobileId = iDevice.getSerialNumber();

        Mobile mobile = new Mobile();

        mobile.setPlatform(AndroidDevice.PLATFORM);
        mobile.setCreateTime(new Date());
        mobile.setId(mobileId);
        mobile.setSystemVersion(AndroidUtil.getAndroidVersion(AndroidUtil.getSdkVersion(iDevice)));
        mobile.setName(AndroidUtil.getDeviceName(iDevice));
        mobile.setCpuInfo(AndroidUtil.getCpuInfo(iDevice));
        mobile.setMemSize(AndroidUtil.getMemSize(iDevice));
        mobile.setEmulator(iDevice.isEmulator() ? Mobile.EMULATOR : Mobile.REAL_MOBILE);

        String resolution = AndroidUtil.getResolution(iDevice); // 720x1280
        String[] res = resolution.split("x");
        mobile.setScreenWidth(Integer.parseInt(res[0]));
        mobile.setScreenHeight(Integer.parseInt(res[1]));

        AndroidDevice androidDevice = new AndroidDevice(mobile, iDevice, appiumServer);

        // 小于android5.0使用stf远程真机方案，否则使用scrcpy方案
        // 小于android5.0初始化driver需要指定app
        if (!androidDevice.greaterOrEqualsToAndroid5()) {
            log.info("[{}]开始安装minicap", mobileId);
            MinicapInstaller minicapInstaller = new MinicapInstaller(iDevice);
            minicapInstaller.install();
            log.info("[{}]安装minicap成功", mobileId);

            log.info("[{}]开始安装minitouch", mobileId);
            MinitouchInstaller minitouchInstaller = new MinitouchInstaller(iDevice);
            minitouchInstaller.install();
            log.info("[{}]安装minitouch成功", mobileId);

            // 安装一个测试apk，用于初始化appium driver
            log.info("[{}]开始安装{}", mobileId, APIDEMOS_APK);
            androidDevice.installApp(new File(APIDEMOS_APK).getAbsolutePath());
            log.info("[{}]安装{}完成", mobileId, APIDEMOS_APK);
        }

        DesiredCapabilities caps = new DesiredCapabilities();
        caps.setCapability("skipServerInstallation", false);
        caps.setCapability("skipDeviceInitialization", false);
        caps.setCapability("skipUnlock", false);
        caps.setCapability("skipLogcatCapture", false);

        log.info("[{}]开始初始化appium", mobileId);
        RemoteWebDriver driver = androidDevice.freshDriver(caps, true);
        log.info("[{}]初始化appium完成", mobileId);

        // 截图并上传到服务器
        UploadFile uploadFile = androidDevice.screenshotAndUploadToServer();
        mobile.setImgPath(uploadFile.getFilePath());

        driver.quit();

        return androidDevice;
    }

    @Override
    protected void reconnectToAgent(Device device, IDevice iDevice) {
        // 重连到agent 更新IDevice
        ((AndroidDevice) device).setIDevice(iDevice);
    }

    @Override
    protected void beforePutDeviceToHolder(Device device) {
        AndroidDevice androidDevice = (AndroidDevice) device;
        IDevice iDevice = androidDevice.getIDevice();

        androidDevice.setMinicap(new Minicap(iDevice));
        androidDevice.setMinitouch(new Minitouch(iDevice));
        androidDevice.setScrcpy(new Scrcpy(iDevice));
        androidDevice.setAdbKit(new AdbKit(androidDevice.getId()));
    }
}
