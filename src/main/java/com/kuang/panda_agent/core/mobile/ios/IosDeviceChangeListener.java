package com.kuang.panda_agent.core.mobile.ios;

import com.android.ddmlib.IDevice;


public interface IosDeviceChangeListener {
    void deviceConnected(IDevice iDevice);
    void deviceDisconnected(IDevice iDevice);
}
