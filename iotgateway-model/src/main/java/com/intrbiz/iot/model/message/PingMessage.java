package com.intrbiz.iot.model.message;

import java.nio.ByteBuffer;

public class PingMessage implements DeviceMessage
{
    public PingMessage()
    {
        super();
    }

    @Override
    public void fromBytes(ByteBuffer buffer)
    {
    }

    public byte[] toBytes()
    {
        return new byte[0];
    }
}
