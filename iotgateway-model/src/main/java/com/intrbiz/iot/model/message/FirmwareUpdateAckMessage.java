package com.intrbiz.iot.model.message;

import java.nio.ByteBuffer;

public class FirmwareUpdateAckMessage implements DeviceMessage
{
    public static final String TOPIC = "/v2/firmware/update/ack";

    public static final byte ACK_BEGIN = 0x01;
    public static final byte ACK_CHUNK = 0x02;
    public static final byte ACK_FINISH = 0x03;

    protected byte ack;

    public FirmwareUpdateAckMessage()
    {
        super();
    }
    
    public FirmwareUpdateAckMessage(byte[] bytes)
    {
        super();
        this.fromBytes(ByteBuffer.wrap(bytes));
    }

    public byte getAck()
    {
        return ack;
    }

    public void setAck(byte ack)
    {
        this.ack = ack;
    }
    
    public boolean isAckBegin()
    {
        return this.ack == ACK_BEGIN;
    }
    
    public boolean isAckChunk()
    {
        return this.ack == ACK_CHUNK;
    }
    
    public boolean isAckFinish()
    {
        return this.ack == ACK_FINISH;
    }

    @Override
    public void fromBytes(ByteBuffer buffer)
    {
        this.ack = buffer.get();
    }

    public byte[] toBytes()
    {
        return new byte[] { this.ack };
    }
}
