package com.kuang.panda_agent.service;

import com.android.ddmlib.AndroidDebugBridge;
import com.android.ddmlib.DdmPreferences;
import com.android.ddmlib.IDevice;
import com.kuang.panda_agent.action.moblie.AdbDevice;
import com.kuang.panda_agent.action.moblie.IAdbServerListener;
import com.kuang.panda_agent.common.Constant;
import com.kuang.panda_agent.utils.AndroidUtils;
import com.oracle.tools.packager.Log;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplicationRunListener;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import javax.usb.*;
import javax.usb.event.UsbServicesEvent;
import javax.usb.event.UsbServicesListener;

import static com.kuang.panda_agent.action.moblie.ADB.getPath;

/**
 * 设备监听器服务
 */
@Slf4j
@Component("DeviceListenerService")
public class DeviceListenerService {

    private ExecutorService executor;


    private String adbPath = null;
    private String adbPlatformTools = "platform-tools";
    private AndroidDebugBridge adb = null;
    private boolean success = false;
    private List<AdbDevice> adbDeviceList = new ArrayList<>();
    private List<IAdbServerListener> listeners = null;

    public DeviceListenerService() {
        executor = new ScheduledThreadPoolExecutor(10);
        init();
        listenUSB();
        listenADB();
    }

    /**
     * 初始化
     */
    private void init() {
        log.info("开始初始化...");
        AndroidDebugBridge.initIfNeeded(false);
        if (AndroidDebugBridge.getBridge() == null) { //只有第一次启动服务才做下面操作，刷新页面不做
            adb = AndroidDebugBridge.createBridge(getPath(), true);
            if (adb != null) {
                if (waitForDeviceList()) {
                    success = true;
                }
            }
        }
    }

    /**
     * 等待设备列表
     *
     * @return
     */
    private boolean waitForDeviceList() {
        int maxWaittingTime = 100;
        int interval = 10;
        if (adb != null) {
            while (!adb.hasInitialDeviceList()) {
                try {
                    Thread.sleep(interval);
                } catch (InterruptedException e) {
                    break;
                }
                maxWaittingTime -= 1;
                if (maxWaittingTime == 0) {
                    disconnectAdb();
                    return false;
                }
            }
        } else {
            log.error("adb连接错误，无法获取adb对象！！！");
            return false;
        }
        return true;
    }

    /**
     * 断开adb
     */
    protected void disconnectAdb() {
        if (adb != null) {
            AndroidDebugBridge.disconnectBridge();
            adb = null;
        }
        success = false;
    }

    /**
     * 刷新设备列表
     */
    @SneakyThrows
    private void refreshAdbDeviceList(String id) {
        IDevice[] iDevices = getIDevices();
        for (IDevice iDevice : iDevices) {
            System.out.println("当前设备：" + iDevice);
            HashMap args = new HashMap();
            args.put("deviceid",iDevice.getSerialNumber());
            //args.put("resolution",iDevice.getProperty("SCREEN_SIZE"));
            args.put("resolution", AndroidUtils.getResolution(iDevice));
            args.put("Mem", AndroidUtils.getMemSize(iDevice));
            args.put("sdkVersion",iDevice.getProperty(Constant.PROP_SDK));
            args.put("androidVersion", iDevice.getProperty("ro.build.version.release"));
            args.put("phoneplatform", iDevice.getProperty("ro.board.platform"));
            args.put("cpu",iDevice.getProperty(Constant.PROP_ABI));
            args.put("manufacturer",iDevice.getProperty(Constant.PROP_MANU));
            args.put("model",iDevice.getProperty(Constant.PROP_MODEL));
            args.put("status","1");
            args.put("设备列表", AndroidUtils.getDeviceName(iDevice));
            //args.put("minicapport",minicapport);
            //args.put("adbkitport",adbkitport);
            //args.put("assignroleid","0");
            //devicesDao.insertDevices(args);
            System.out.println(args);
        }
    }

    //public String getADBPath() {
    //    if (adbPath == null) {
    //        adbPath = System.getenv("ANDROID_SDK_ROOT");
    //        if (adbPath != null) {
    //            adbPath += File.separator + adbPlatformTools;
    //        } else {
    //            adbPath = "adb";
    //            return adbPath;
    //        }
    //    }
    //    //adbPath += File.separator + "adb";
    //    return adbPath;
    //}

    /**
     * 监听USB
     */
    //@EventListener(ApplicationEvent.class)
    public void listenUSB() {
        init();
        log.info("========start usb listen!=======");
        Executors.newFixedThreadPool(100);
        usbListenerThread();
    }

    /**
     * 监听ADB
     */
    //@EventListener(ApplicationEvent.class)
    public void listenADB() {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                log.info("refreshAdbDeviceList...");
                while (true) {
                    try {
                        Thread.sleep(new Constant().REFRESHADBINTRAVAL);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    String id = RandomStringUtils.randomAlphanumeric(10);
                    refreshAdbDeviceList(id);
                }
            }
        });
    }

    private void usbListenerThread() {
        try {
            log.info("========start usb listen thread!=======");
            UsbServices services = null;
            //启动USB监听线程
            services = UsbHostManager.getUsbServices();
            services.addUsbServicesListener(new myUsbListener());
            log.info("已开启USB设备监听...");
        } catch (UsbException e) {
            e.printStackTrace();
        }
    }

    class myUsbListener implements UsbServicesListener {

        @Override
        public void usbDeviceAttached(UsbServicesEvent usbServicesEvent) {
            UsbDevice device = usbServicesEvent.getUsbDevice();
            if (!device.isUsbHub()) {
                onUsbDeviceConnected(device);
            }
        }

        @Override
        public void usbDeviceDetached(UsbServicesEvent usbServicesEvent) {
            UsbDevice device = usbServicesEvent.getUsbDevice();
            if (!device.isUsbHub()) {
                onUsbDeviceDisConnected(device);
            }
        }
    }

    private List<AdbDevice> checkAdbDevices(UsbDevice usbDevice) {

        List<AdbDevice> adbDevices = new ArrayList<>();

        UsbDeviceDescriptor deviceDesc = usbDevice.getUsbDeviceDescriptor();

        // Ignore devices from Non-ADB vendors
        // Check interfaces of device
        UsbConfiguration config = usbDevice.getActiveUsbConfiguration();
        for (UsbInterface iface : (List<UsbInterface>) config.getUsbInterfaces()) {
            List<UsbEndpoint> endpoints = iface.getUsbEndpoints();

            // Ignore interface if it does not have two endpoints
            if (endpoints.size() != 2) continue;

            // Ignore interface if it does not match the ADB specs
            if (!AdbDevice.isAdbInterface(iface)) continue;

            UsbEndpointDescriptor ed1 =
                    endpoints.get(0).getUsbEndpointDescriptor();
            UsbEndpointDescriptor ed2 =
                    endpoints.get(1).getUsbEndpointDescriptor();

            // Ignore interface if endpoints are not bulk endpoints
            if (((ed1.bmAttributes() & UsbConst.ENDPOINT_TYPE_BULK) == 0) ||
                    ((ed2.bmAttributes() & UsbConst.ENDPOINT_TYPE_BULK) == 0))
                continue;

            // Determine which endpoint is in and which is out. If both
            // endpoints are in or out then ignore the interface
            byte a1 = ed1.bEndpointAddress();
            byte a2 = ed2.bEndpointAddress();
            byte in, out;
            if (((a1 & UsbConst.ENDPOINT_DIRECTION_IN) != 0) &&
                    ((a2 & UsbConst.ENDPOINT_DIRECTION_IN) == 0)) {
                in = a1;
                out = a2;
            } else if (((a2 & UsbConst.ENDPOINT_DIRECTION_IN) != 0) &&
                    ((a1 & UsbConst.ENDPOINT_DIRECTION_IN) == 0)) {
                out = a1;
                in = a2;
            } else {
                continue;
            }

            adbDevices.add(new AdbDevice(usbDevice, iface, in, out));
        }
        return adbDevices;
    }

    /**
     * USB设备连接时调用
     */
    private void onUsbDeviceConnected(UsbDevice usbDevice) {
        List<AdbDevice> devices = checkAdbDevices(usbDevice);
        devices.forEach(adbDevice -> onAdbDeviceConnected(adbDevice));
    }

    /**
     * USB设备断开时调用
     */
    private void onUsbDeviceDisConnected(UsbDevice usbDevice) {
        List<AdbDevice> devices = checkAdbDevices(usbDevice);
        devices.forEach(adbDevice -> onAdbDeviceDisConnected(adbDevice));
    }

    /**
     * 发现安卓设备时调用
     */
    private void onAdbDeviceConnected(AdbDevice adbDevice) {
        String iii = RandomStringUtils.randomAlphanumeric(15);
        //  refreshAdbDeviceList(iii);
    }

    /**
     * 发现安卓设备断开是调用
     */
    private void onAdbDeviceDisConnected(AdbDevice adbDevice) {
        for (Iterator it = adbDeviceList.iterator(); it.hasNext(); ) {
            AdbDevice device = (AdbDevice) it.next();
            if (adbDevice.getUsbDevice() == device.getUsbDevice()) {
                log.info("Android设备断开：" + adbDevice.getSerialNumber());
                it.remove();
                listeners.forEach(l -> l.onAdbDeviceDisConnected(device));
                // TODO 更新数据库中设备状态，改为0
            }
        }
    }

    /**
     * 获取ADB命令返回的设备列表
     *
     * @return IDevices
     */
    public IDevice[] getIDevices() {
        return adb.getDevices();
    }

    public List<AdbDevice> getAdbDeviceList() {
        return this.adbDeviceList;
    }

}
