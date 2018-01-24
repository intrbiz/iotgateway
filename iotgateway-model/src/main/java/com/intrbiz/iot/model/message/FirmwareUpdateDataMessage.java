package com.intrbiz.iot.model.message;

import java.nio.ByteBuffer;

public class FirmwareUpdateDataMessage implements DeviceMessage
{
    public static final int MAX_LENGTH = 1024;
    
    public static final String TOPIC = "/v2/firmware/update/data";

    protected byte[] data;

    public FirmwareUpdateDataMessage()
    {
        super();
    }

    public FirmwareUpdateDataMessage(byte[] data)
    {
        super();
        this.data = data;
    }

    public byte[] getData()
    {
        return data;
    }

    public void setData(byte[] data)
    {
        this.data = data;
    }

    @Override
    public void fromBytes(ByteBuffer buffer)
    {
        this.data = new byte[buffer.remaining()];
        buffer.get(this.data);
    }

    public byte[] toBytes()
    {
        return this.data;
    }
}
