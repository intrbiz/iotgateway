package com.intrbiz.connectedkanban.hubserver.model.message.device;

import java.nio.ByteBuffer;

import com.intrbiz.connectedkanban.hubserver.util.BinUtil;

public class FirmwareUpdateBeginMessage implements DeviceMessage
{
    public static final String TOPIC = "/v2/firmware/update/begin";
    
    public static final int FLAG_COMPRESSED = 0x01000000; 

    protected int firmwareSize = 0;

    protected String info = "";

    protected int flags = 0;

    protected String md5;

    public FirmwareUpdateBeginMessage()
    {
        super();
    }

    public int getFirmwareSize()
    {
        return firmwareSize;
    }

    public void setFirmwareSize(int firmwareSize)
    {
        this.firmwareSize = firmwareSize;
    }

    public String getInfo()
    {
        return info;
    }

    public void setInfo(String info)
    {
        this.info = info;
    }

    public int getFlags()
    {
        return flags;
    }

    public void setFlags(int flags)
    {
        this.flags = flags;
    }
    
    public boolean isCompressed()
    {
        return (this.flags & FLAG_COMPRESSED) == FLAG_COMPRESSED;
    }
    
    public void setCompressed()
    {
        this.flags |= FLAG_COMPRESSED;
    }

    public String getMd5()
    {
        return md5;
    }

    public void setMd5(String md5)
    {
        this.md5 = md5;
    }

    @Override
    public void fromBytes(ByteBuffer buffer)
    {
        this.firmwareSize = buffer.getInt();
        this.info = BinUtil.parseFixedLenString(buffer, 32);
        this.flags = buffer.getInt();
        this.md5 = BinUtil.parseFixedLenString(buffer, 32);
    }

    public byte[] toBytes()
    {
        byte[] msg = new byte[72];
        ByteBuffer buffer = ByteBuffer.wrap(msg);
        buffer.putInt(this.firmwareSize);
        BinUtil.writeFixedLenString(buffer, 32, this.info);
        buffer.putInt(this.flags);
        BinUtil.writeFixedLenString(buffer, 32, this.md5);
        return msg;
    }
}
