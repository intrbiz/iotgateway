package com.intrbiz.iot.engine.impl.firmware;

import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.UUID;

import com.intrbiz.iot.engine.FirmwareEngine;
import com.intrbiz.iot.engine.firmware.FirmwareContext;
import com.intrbiz.iot.model.DeviceFirmware;
import com.intrbiz.iot.model.message.FirmwareUpdateBeginMessage;
import com.intrbiz.iot.model.message.FirmwareUpdateDataMessage;
import com.intrbiz.iot.model.message.FirmwareUpdateFinishMessage;
import com.intrbiz.iot.model.message.FirmwareVerifyChallengeMessage;
import com.intrbiz.iot.model.message.FirmwareVerifyResponseMessage;
import com.intrbiz.iot.model.message.HelloV2Message;
import com.intrbiz.util.Hash;

public abstract class BaseFirmwareEngine implements FirmwareEngine
{
    private static enum State {
        INITIAL,
        CHALLENGED,
        VERIFIED,
        IN_PROGRESS,
        COMPLETE;
    }
    
    protected final SecureRandom random = new SecureRandom();
    
    public FirmwareContext createContext(UUID clientId)
    {
        return new AbstractFirmwareContext(clientId);
    }
    
    protected abstract byte[] getFirmwareKey(byte[] firmwareId);
    
    protected abstract DeviceFirmware updateTarget(byte[] firmwareId);
    
    protected boolean needsUpdate(byte[] firmwareId)
    {
        return this.updateTarget(firmwareId) != null;
    }
    
    private class AbstractFirmwareContext implements FirmwareContext
    {
        private State state = State.INITIAL;
        
        private byte[] firmwareId;
        
        private byte[] currentNonce;
        
        private byte[] firmwareKey;
        
        private boolean verified = false;
        
        private DeviceFirmware target;
        
        private ByteBuffer chunkBuffer;
        
        public AbstractFirmwareContext(UUID clientId)
        {
        }

        @Override
        public FirmwareVerifyChallengeMessage startFirmwareVerification(HelloV2Message hello)
        {
            if (this.state == State.INITIAL)
            {
                // stash device firmware info
                this.firmwareId = hello.getFirmwareId();
                // lookup the firmware key
                this.firmwareKey = BaseFirmwareEngine.this.getFirmwareKey(this.firmwareId);
                if (this.firmwareKey != null)
                {
                    // sign the challenge
                    byte[] signature = Hash.sha256HMAC(this.firmwareKey, hello.getNonce());
                    // generate a nonce for the device to sign
                    this.currentNonce = new byte[16];
                    BaseFirmwareEngine.this.random.nextBytes(this.currentNonce);
                    // challenge for the device
                    this.state = State.CHALLENGED;
                    return new FirmwareVerifyChallengeMessage(signature, this.currentNonce);
                }
            }
            return null;
        }
        
        public boolean completeFirmwareVerification(FirmwareVerifyResponseMessage response)
        {
            if (this.state == State.CHALLENGED)
            {
                this.state = State.VERIFIED;
                byte[] expected  = Hash.sha256HMAC(this.firmwareKey, this.currentNonce); 
                this.verified = Arrays.equals(expected, response.getHash());
                return this.verified;
            }
            return false;
        }
        
        public boolean isVerified()
        {
            return this.verified;
        }
        
        public boolean needsUpdate()
        {
            if (this.verified)
            {
                return BaseFirmwareEngine.this.needsUpdate(this.firmwareId);
            }
            return false;
        }

        @Override
        public FirmwareUpdateBeginMessage begin()
        {
            if (this.verified)
            {
                this.target = BaseFirmwareEngine.this.updateTarget(this.firmwareId);
                if (this.target != null)
                {
                    this.chunkBuffer = ByteBuffer.wrap(this.target.getDeviceFirmwareImage());
                    this.state = State.IN_PROGRESS;
                    return new FirmwareUpdateBeginMessage(this.target.getDeviceFirmwareImage().length, this.target.getDeviceFirmwareInfo(), 0x00, this.target.getDeviceFirmwareMd5());
                }
            }
            return null;
        }
        
        @Override
        public boolean isInProgress()
        {
            return this.state == State.IN_PROGRESS;
        }

        @Override
        public FirmwareUpdateDataMessage nextChunk(int maxChunkLength)
        {
            if (this.verified && this.target != null && this.chunkBuffer != null)
            {
                if (this.chunkBuffer.hasRemaining())
                {
                    byte[] chunk = new byte[Math.min(this.chunkBuffer.remaining(), maxChunkLength)];
                    this.chunkBuffer.get(chunk);
                    return new FirmwareUpdateDataMessage(chunk);
                }
            }
            return null;
        }

        @Override
        public FirmwareUpdateFinishMessage finish()
        {
            if (this.verified && this.target != null && this.chunkBuffer != null && (! this.chunkBuffer.hasRemaining()))
            {
                return new FirmwareUpdateFinishMessage(Hash.sha256HMAC(this.firmwareKey, this.target.getDeviceFirmwareImage()));
            }
            return null;
        }
        
        public void complete()
        {
            this.state = State.COMPLETE;
            this.chunkBuffer = null;
        }

        @Override
        public void close()
        {
            if (this.firmwareKey != null)
            {
                for (int i = 0; i < this.firmwareKey.length; i++)
                {
                    this.firmwareKey[i] = 0x00;
                }
            }
        }
    }
}
