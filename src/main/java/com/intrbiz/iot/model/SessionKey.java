package com.intrbiz.iot.model;

public class SessionKey
{
    private final byte[] sessionKey;
    
    private final byte[] message;

    public SessionKey(byte[] sessionKey, byte[] message)
    {
        super();
        this.sessionKey = sessionKey;
        this.message = message;
    }

    public byte[] getSessionKey()
    {
        return sessionKey;
    }

    public byte[] getMessage()
    {
        return message;
    }
}
