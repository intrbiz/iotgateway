package com.intrbiz.connectedkanban.hubserver.model.message.device;

import java.nio.ByteBuffer;

import com.intrbiz.connectedkanban.hubserver.util.BinUtil;

public class FirmwareUpdateFinishMessage implements DeviceMessage
{
    public static final String TOPIC = "/v2/firmware/update/finish";

    protected byte[] hash;

    public FirmwareUpdateFinishMessage()
    {
        super();
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
