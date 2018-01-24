package com.intrbiz.iot.model;

public class Firmware
{
    private byte[] id;
    
    private String info;
    
    private byte[] firmware;
    
    private String md5;
    
    public Firmware()
    {
        super();
    }

    public Firmware(byte[] id, String info, byte[] firmware, String md5)
    {
        super();
        this.id = id;
        this.info = info;
        this.firmware = firmware;
        this.md5 = md5;
    }

    public byte[] getId()
    {
        return id;
    }

    public void setId(byte[] id)
    {
        this.id = id;
    }

    public String getInfo()
    {
        return info;
    }

    public void setInfo(String info)
    {
        this.info = info;
    }

    public byte[] getFirmware()
    {
        return firmware;
    }

    public void setFirmware(byte[] firmware)
    {
        this.firmware = firmware;
    }

    public String getMd5()
    {
        return md5;
    }

    public void setMd5(String md5)
    {
        this.md5 = md5;
    }
    
    public String toString()
    {
        return "Firmware[info=" + this.info + ", size=" + this.firmware.length + ", md5=" + this.md5 + "]";
    }
}
