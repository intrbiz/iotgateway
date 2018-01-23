package com.intrbiz.connectedkanban.hubserver.model.message.device;

import java.nio.ByteBuffer;

import com.intrbiz.connectedkanban.hubserver.util.BinUtil;

public class HelloV2Message extends HelloMessage
{    
    protected String appId;
    
    protected byte[] firmwareId;
    
    protected byte[] nonce;

    public HelloV2Message()
    {
        super();
    }
    
    public String getAppId()
    {
        return this.appId;
    }
    
    public void setAppId(String appId)
    {
        this.appId = appId;
    }

    public byte[] getFirmwareId()
    {
        return firmwareId;
    }

    public void setFirmwareId(byte[] firmwareId)
    {
        this.firmwareId = firmwareId;
    }

    public byte[] getNonce()
    {
        return nonce;
    }

    public void setNonce(byte[] nonce)
    {
        this.nonce = nonce;
    }

    @Override
    public void fromBytes(ByteBuffer buffer)
    {
        this.deviceId = BinUtil.parseUUID(buffer);
        this.localIp = BinUtil.parseFixedLenBytes(buffer, 4);
        this.name = BinUtil.parseFixedLenString(buffer, 128);
        this.firmwareVersion = BinUtil.parseFixedLenBytes(buffer, 5);
        this.appId = BinUtil.parseFixedLenString(buffer, 32);
        this.firmwareId = BinUtil.parseFixedLenBytes(buffer, 16);
        this.nonce = BinUtil.parseFixedLenBytes(buffer, 16);
    }

    public byte[] toBytes()
    {
        byte[] msg = new byte[185];
        ByteBuffer buffer = ByteBuffer.wrap(msg);
        BinUtil.writeUUID(buffer, this.deviceId);
        BinUtil.writeFixedLenBytes(buffer, 4, this.localIp);
        BinUtil.writeFixedLenString(buffer, 128, this.name);
        BinUtil.writeFixedLenBytes(buffer, 5, this.firmwareVersion);
        BinUtil.writeFixedLenString(buffer, 32, this.appId);
        BinUtil.writeFixedLenBytes(buffer, 16, this.firmwareId);
        BinUtil.writeFixedLenBytes(buffer, 16, this.nonce);
        return msg;
    }
}
