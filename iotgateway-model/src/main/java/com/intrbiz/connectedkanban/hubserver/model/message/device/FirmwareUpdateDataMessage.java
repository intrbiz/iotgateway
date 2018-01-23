package com.intrbiz.connectedkanban.hubserver.model.message.device;

import java.nio.ByteBuffer;

public class FirmwareUpdateDataMessage implements DeviceMessage
{
    public static final String TOPIC = "/v2/firmware/update/data";

    protected byte[] data;

    public FirmwareUpdateDataMessage()
    {
        super();
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
