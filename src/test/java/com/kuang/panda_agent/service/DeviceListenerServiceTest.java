package com.kuang.panda_agent.service;

import com.android.ddmlib.AndroidDebugBridge;
import com.android.ddmlib.IDevice;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

@Slf4j
class DeviceListenerServiceTest {

    private AndroidDebugBridge adb = null;

    @Test
    public void takeDevices() {
        //TreeSet<AndroidDevice> devices = AndroidDeviceStore.getInstance()
        //        .getDevices();
        //AndroidDevice device = devices.pollFirst();
        //System.out.println(device.getName());
    }

}