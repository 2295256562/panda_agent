package com.kuang.panda_agent.utils;

import com.android.ddmlib.IDevice;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AndroidMonitor {
    //    static AndroidPamer ad=new AndroidPamer();

    //获取cpu
   public static String[] getcpu(IDevice[] iDevice) throws IDeviceExecuteShellCommandException {
        String[] cpulist=new String[iDevice.length];
        for (int i = 0; i < iDevice.length; i++) {
            String cpu = AndroidUtils.executeShellCommand(iDevice[i], "dumpsys cpuinfo | grep com.daddylab");
            cpulist[i] = cpu;
        }
        return cpulist;
    }
    //获取内存
    public static String[] getmem(IDevice[] iDevice,String paramName) throws IDeviceExecuteShellCommandException {
        String[] mem=new String[iDevice.length];
        for (int i = 0; i < iDevice.length; i++) {
            String meminf = AndroidUtils.executeShellCommand(iDevice[i], "dumpsys meminfo com.daddylab");
            mem[i] = AndroidMonitor.GetParamData(meminf,paramName);
        }
        return mem;
    }

    //获取fps
    public static String[] getFps(IDevice[] iDevice) throws IDeviceExecuteShellCommandException {
        String[] fpslist=new String[iDevice.length];
        for (int i = 0; i < iDevice.length; i++) {
            String fps = AndroidUtils.executeShellCommand(iDevice[i], "dumpsys gfxinfo com.daddylab");
            fpslist[i] = fps;
        }
        return fpslist;
    }
    //获取启动时间
   public static String[] getTime(IDevice[] iDevice,String paramName) throws IDeviceExecuteShellCommandException {
        String[] res = new String[iDevice.length];
        for (int i = 0; i < iDevice.length; i++) {
            String time = AndroidUtils.executeShellCommand(iDevice[i], "am start -W com.daddylab/com.daddylab.MainActivity");
            res[i] = AndroidMonitor.GetParamData(time,paramName);
        }
        return res;
    }
    //匹配字符串
    public static String GetParamData(String src ,String paramName )  {
        String regex=paramName+"[ ]+[0-9]+";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(src);
        if (matcher.find()){
            return matcher.toMatchResult().group(0);
        }else{
            return "";
        }
    }
}

