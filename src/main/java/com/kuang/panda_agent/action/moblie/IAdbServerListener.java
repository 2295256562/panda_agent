package com.kuang.panda_agent.action.moblie;


public interface IAdbServerListener {
    void onAdbDeviceConnected(AdbDevice device);
    void onAdbDeviceDisConnected(AdbDevice device);
}
