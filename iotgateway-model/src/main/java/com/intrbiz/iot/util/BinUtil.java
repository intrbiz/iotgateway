package com.intrbiz.iot.util;

import java.nio.ByteBuffer;
import java.util.UUID;

public class BinUtil
{
    public static UUID parseUUID(byte[] uuid)
    {
        return parseUUID(ByteBuffer.wrap(uuid));
    }
    
    public static UUID parseUUID(ByteBuffer buffer)
    {
        long msb = buffer.getLong();
        long lsb = buffer.getLong();
        return msb == 0 && lsb == 0 ? null : new UUID(msb, lsb);
    }
    
    public static byte[] parseFixedLenBytes(ByteBuffer buffer, int len)
    {
        byte[] tmp = new byte[len];
        buffer.get(tmp);
        return tmp;
    }
    
    public static void writeFixedLenBytes(ByteBuffer buffer, int len, byte[] bytes)
    {
        if (bytes == null)
        {
            buffer.position(buffer.position() + len);
        }
        else if (bytes.length < len)
        {
            buffer.put(bytes, 0, bytes.length);
            buffer.position(buffer.position() + (len - bytes.length));
        }
        else
        {
            buffer.put(bytes, 0, len);
        }
        
    }
    
    public static String parseFixedLenString(ByteBuffer buffer, int len)
    {
        byte[] tmp = new byte[len];
        buffer.get(tmp);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < tmp.length; i ++)
        {
            if (tmp[i] == 0) break;
            sb.append((char) tmp[i]);
        }
        return sb.toString();
    }
    
    public static void writeFixedLenString(ByteBuffer buffer, int len, String str)
    {
        if (str == null) str = "";
        byte[] tmp = str.getBytes();
        buffer.put(tmp, 0, tmp.length <= len ? tmp.length : len);
        if (tmp.length < len) buffer.position(buffer.position() + (len - tmp.length));
    }
    
    public static byte[] writeUUID(UUID id)
    {
        byte[] r = new byte[16];
        writeUUID(ByteBuffer.wrap(r), id);
        return r;
    }
    
    public static void writeUUID(ByteBuffer buffer, UUID id)
    {
        if (id == null)
        {
            buffer.putLong(0L);
            buffer.putLong(0L);
        }
        else
        {
            buffer.putLong(id.getMostSignificantBits());
            buffer.putLong(id.getLeastSignificantBits());
        }
    }
}
