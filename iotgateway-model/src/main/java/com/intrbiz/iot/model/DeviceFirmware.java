package com.intrbiz.iot.model;

public interface DeviceFirmware
{
    byte[] getDeviceFirmwareId();

    String getDeviceFirmwareInfo();

    byte[] getDeviceFirmwareImage();

    String getDeviceFirmwareMd5();
}
