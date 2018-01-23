package com.intrbiz.iot.model;

import java.nio.ByteBuffer;
import java.util.Base64;
import java.util.UUID;

public class RegistrationKey
{
    private final UUID id;
    
    private final byte[] key;
    
    public RegistrationKey(UUID id, byte[] key)
    {
        super();
        this.id = id;
        this.key = key;
    }

    public UUID getId()
    {
        return id;
    }

    public byte[] getKey()
    {
        return key;
    }
    
    public byte[] assemble()
    {
        byte[] assembled = new byte[32]; 
        ByteBuffer buf = ByteBuffer.wrap(assembled);
        buf.putLong(this.id.getMostSignificantBits());
        buf.putLong(this.id.getLeastSignificantBits());
        buf.put(this.key);
        return assembled;
    }
    
    public String encodeBase64()
    {
        return new String(Base64.getEncoder().encode(this.assemble()));
    }
    
    public String encodeHex()
    {
        return this.toHex(this.assemble());
    }
    
    private String toHex(byte[] a)
    {
        StringBuilder sb = new StringBuilder();
        for (byte b : a)
        {
            if (b <= 0xF) sb.append('0');
            sb.append(Integer.toHexString(b & 0xFF));
        }
        return sb.toString();
    }
    
    public String toString()
    {
        return this.encodeBase64();
    }
}
