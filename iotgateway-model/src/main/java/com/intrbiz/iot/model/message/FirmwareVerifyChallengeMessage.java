package com.intrbiz.iot.model.message;

import java.nio.ByteBuffer;

import com.intrbiz.iot.util.BinUtil;

public class FirmwareVerifyChallengeMessage implements DeviceMessage
{
    public static final String TOPIC = "/v2/firmware/verify/challenge";

    protected byte[] hash;

    protected byte[] nonce;

    public FirmwareVerifyChallengeMessage()
    {
        super();
    }
    public FirmwareVerifyChallengeMessage(byte[] hash, byte[] nonce)
    {
        super();
        this.hash = hash;
        this.nonce = nonce;
    }

    public byte[] getHash()
    {
        return hash;
    }

    public void setHash(byte[] hash)
    {
        this.hash = hash;
    }

    public byte[] getNonce()
    {
        return nonce;
    }

    public void setNonce(byte[] nonce)
    {
        this.nonce = nonce;
    }

    @Override
    public void fromBytes(ByteBuffer buffer)
    {
        this.hash = BinUtil.parseFixedLenBytes(buffer, 32);
        this.nonce = BinUtil.parseFixedLenBytes(buffer, 16);
    }

    public byte[] toBytes()
    {
        byte[] msg = new byte[48];
        ByteBuffer buffer = ByteBuffer.wrap(msg);
        BinUtil.writeFixedLenBytes(buffer, 32, this.hash);
        BinUtil.writeFixedLenBytes(buffer, 16, this.nonce);
        return msg;
    }
}
