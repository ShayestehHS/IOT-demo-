package org.vosk.demo;

import androidx.annotation.NonNull;

public class DeviceInfoModel {
    private final String deviceName;
    private final String macAddress;

    public DeviceInfoModel(String deviceName, String macAddress) {
        this.deviceName = deviceName;
        this.macAddress = macAddress;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public String getMacAddress() {
        return macAddress;
    }

    @NonNull
    @Override
    public String toString() {
        return this.getDeviceName();
    }
}
