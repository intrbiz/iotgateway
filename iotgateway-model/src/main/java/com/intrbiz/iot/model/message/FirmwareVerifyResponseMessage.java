package com.intrbiz.iot.model.message;

import java.nio.ByteBuffer;

import com.intrbiz.iot.util.BinUtil;

public class FirmwareVerifyResponseMessage implements DeviceMessage
{
    public static final String TOPIC = "/v2/firmware/verify/response";

    protected byte[] hash;

    public FirmwareVerifyResponseMessage()
    {
        super();
    }
    
    public FirmwareVerifyResponseMessage(byte[] bytes)
    {
        super();
        this.fromBytes(ByteBuffer.wrap(bytes));
    }

    public byte[] getHash()
    {
        return hash;
    }

    public void setHash(byte[] hash)
    {
        this.hash = hash;
    }

    @Override
    public void fromBytes(ByteBuffer buffer)
    {
        this.hash = BinUtil.parseFixedLenBytes(buffer, 32);
    }

    public byte[] toBytes()
    {
        byte[] msg = new byte[32];
        ByteBuffer buffer = ByteBuffer.wrap(msg);
        BinUtil.writeFixedLenBytes(buffer, 32, this.hash);
        return msg;
    }
}
