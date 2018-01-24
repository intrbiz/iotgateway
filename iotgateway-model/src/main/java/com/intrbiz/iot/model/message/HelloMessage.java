package com.intrbiz.iot.model.message;

import java.nio.ByteBuffer;
import java.util.UUID;

import com.intrbiz.iot.util.BinUtil;

public class HelloMessage implements DeviceMessage
{
    public static final String TOPIC = "/v1/hello";
    
    protected UUID deviceId;

    protected byte[] localIp = new byte[4];

    protected String name;
    
    protected byte[] firmwareVersion;

    public HelloMessage()
    {
        super();
    }

    public UUID getDeviceId()
    {
        return deviceId;
    }

    public void setDeviceId(UUID deviceId)
    {
        this.deviceId = deviceId;
    }

    public byte[] getLocalIp()
    {
        return localIp;
    }

    public void setLocalIp(byte[] localIp)
    {
        this.localIp = localIp;
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public byte[] getFirmwareVersion()
    {
        return firmwareVersion;
    }

    public void setFirmwareVersion(byte[] firmwareVersion)
    {
        this.firmwareVersion = firmwareVersion;
    }

    @Override
    public void fromBytes(ByteBuffer buffer)
    {
        this.deviceId = BinUtil.parseUUID(buffer);
        this.localIp = BinUtil.parseFixedLenBytes(buffer, 4);
        this.name = BinUtil.parseFixedLenString(buffer, 128);
        if (buffer.remaining() > 5) this.firmwareVersion = BinUtil.parseFixedLenBytes(buffer, 5);
    }

    public byte[] toBytes()
    {
        byte[] msg = new byte[153];
        ByteBuffer buffer = ByteBuffer.wrap(msg);
        BinUtil.writeUUID(buffer, this.deviceId);
        BinUtil.writeFixedLenBytes(buffer, 4, this.localIp);
        BinUtil.writeFixedLenString(buffer, 128, this.name);
        BinUtil.writeFixedLenBytes(buffer, 5, this.firmwareVersion);
        return msg;
    }
}
