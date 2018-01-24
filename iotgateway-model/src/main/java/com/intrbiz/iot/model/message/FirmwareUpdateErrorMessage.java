package com.intrbiz.iot.model.message;

import java.nio.ByteBuffer;

public class FirmwareUpdateErrorMessage implements DeviceMessage
{
    public static final String TOPIC = "/v2/firmware/update/error";

    public static final byte ERROR_too_big = 0x01;

    public static final byte ERROR_already_updating = 0x02;

    public static final byte ERROR_hash_error = 0x03;

    public static final byte ERROR_finish_error = 0x04;

    public static final byte ERROR_write_error = 0x05;

    protected byte errorCode;

    protected byte writeErrorCode;

    public FirmwareUpdateErrorMessage()
    {
        super();
    }
    
    public FirmwareUpdateErrorMessage(byte[] bytes)
    {
        super();
        this.fromBytes(ByteBuffer.wrap(bytes));
    }

    public byte getErrorCode()
    {
        return errorCode;
    }

    public void setErrorCode(byte errorCode)
    {
        this.errorCode = errorCode;
    }

    public byte getWriteErrorCode()
    {
        return writeErrorCode;
    }

    public void setWriteErrorCode(byte writeErrorCode)
    {
        this.writeErrorCode = writeErrorCode;
    }

    @Override
    public void fromBytes(ByteBuffer buffer)
    {
        this.errorCode = buffer.get();
        this.writeErrorCode = buffer.get();
    }

    public byte[] toBytes()
    {
        return new byte[] { this.errorCode, this.writeErrorCode };
    }
}
