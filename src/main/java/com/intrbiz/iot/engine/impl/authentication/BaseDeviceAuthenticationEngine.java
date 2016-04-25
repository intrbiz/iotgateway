package com.intrbiz.iot.engine.impl.authentication;

import java.security.SecureRandom;
import java.util.Arrays;
import java.util.UUID;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import com.intrbiz.iot.engine.DeviceAuthenticationEngine;
import com.intrbiz.iot.model.DeviceKey;
import com.intrbiz.iot.model.RegistrationKey;
import com.intrbiz.iot.model.SessionKey;
import com.intrbiz.util.Hash;
import com.intrbiz.util.Hash.BufferSlice;

public abstract class BaseDeviceAuthenticationEngine implements DeviceAuthenticationEngine
{    
    public static final byte[] EMPTY_BUFFER = new byte[0];
    
    private final SecureRandom random;
    
    public BaseDeviceAuthenticationEngine()
    {
        this.random = new SecureRandom();
    }
    

    @Override
    public boolean checkDeviceConnect(UUID clientId)
    {
        return true;
    }

    @Override
    public byte[] generateDeviceAuthenticationChallenge(UUID clientId, byte[] clientNonce)
    {
        // the nonce
        byte[] nonce = new byte[16];
        this.random.nextBytes(nonce);
        // sign the client nonce with the device key
        DeviceKey deviceKey = this.getDeviceKey(clientId);
        if (deviceKey == null) return null;
        byte[] hash = Hash.sha256(clientNonce, deviceKey.getKey());
        // build the message
        byte[] message = new byte[48];
        System.arraycopy(nonce, 0, message, 0, 16);
        System.arraycopy(hash, 0, message, 16, 32);
        return message;
    }

    @Override
    public boolean checkDeviceAuthenticationResponse(UUID clientId, byte[] challenge, byte[] response)
    {
        DeviceKey deviceKey = getDeviceKey(clientId);
        if (deviceKey == null) return false;
        return Arrays.equals(response, Hash.sha256(new BufferSlice(challenge, 0, 16), new BufferSlice(deviceKey.getKey())));
    }
    


    @Override
    public byte[] generateDeviceRegistrationChallenge(UUID clientId, UUID keyId, byte[] clientNonce)
    {
        RegistrationKey key = this.getRegistrationKey(keyId);
        if (key == null) return null;
        // generate a nonce
        byte[] nonce = new byte[16];
        this.random.nextBytes(nonce);
        // sign the client nonce with the requested registration key
        byte[] hash = Hash.sha256(clientNonce, key.getKey());
        // build the message
        byte[] message = new byte[48];
        System.arraycopy(nonce, 0, message, 0, 16);
        System.arraycopy(hash, 0, message, 16, 32);
        return message;
    }

    @Override
    public boolean checkDeviceRegistrationResponse(UUID clientId, UUID keyId, byte[] challenge, byte[] response)
    {
        RegistrationKey key = this.getRegistrationKey(keyId);
        return Arrays.equals(response, Hash.sha256(new BufferSlice(challenge, 0, 16), new BufferSlice(key.getKey())));
    }

    @Override
    public byte[] registerDevice(UUID clientId, UUID keyId)
    {
        try
        {
            // the registration key
            RegistrationKey regKey = this.getRegistrationKey(keyId);
            if (regKey == null) return null;
            // generate a random AES key
            byte[] deviceKey = new byte[16];
            this.random.nextBytes(deviceKey);
            this.setDeviceKey(keyId, new DeviceKey(clientId, deviceKey));
            return this.encryptKey(regKey.getKey(), deviceKey);
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }
    
    protected abstract void setDeviceKey(UUID registrationKeyId, DeviceKey key);
    
    public SessionKey generateSessionKey(UUID clientId)
    {
        try
        {
            byte[] sessionKey = new byte[16];
            this.random.nextBytes(sessionKey);
            DeviceKey deviceKey = this.getDeviceKey(clientId);
            return new SessionKey(sessionKey, this.encryptKey(deviceKey.getKey(), sessionKey));
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }
    
    private byte[] encryptKey(byte[] encryptionKey, byte[] theKey) throws Exception
    {
        // generate a random IV
        byte[] iv = new byte[16];
        this.random.nextBytes(iv);
        // encrypt the device key with the reg key
        Cipher cipher = Cipher.getInstance("AES/CBC/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(encryptionKey, "AES"), new IvParameterSpec(iv));
        byte[] encryptedDeviceKey = cipher.doFinal(theKey);
        // compute a hash of the encrypted data and our registration key as an auth tag
        byte[] check = Hash.sha256HMAC(encryptionKey, iv, encryptedDeviceKey);
        // assemble the payload
        byte[] message = new byte[iv.length + encryptedDeviceKey.length + check.length];
        System.arraycopy(iv, 0, message, 0, iv.length);
        System.arraycopy(encryptedDeviceKey, 0, message, iv.length, encryptedDeviceKey.length);
        System.arraycopy(check, 0, message, iv.length + encryptedDeviceKey.length, check.length);
        return message;
    }

    @Override
    public RegistrationKey generateRegistrationKey()
    {
        UUID keyId = UUID.randomUUID();
        byte[] key = new byte[16];
        RegistrationKey regKey = new RegistrationKey(keyId, key);
        this.setRegistrationKey(regKey);
        return regKey;
    }
    
    protected abstract void setRegistrationKey(RegistrationKey key);
    
    private byte[] generateIV()
    {
        byte[] iv = new byte[16];
        this.random.nextBytes(iv);
        return iv;
    }
    
    private void randomFill(byte[] buffer)
    {
        this.random.nextBytes(buffer);
    }
    
    public byte[] encryptMessage(byte[] sessionKey, byte[] plainText) throws Exception
    {
        // message output buffer
        byte[] message = EMPTY_BUFFER;
        // is this not a null length message
        if (plainText.length > 0)
        {
            // compute the cipher text length
            int length          = plainText.length;
            int fullBlocks      = length >> 4;
            int finalBlockBytes = length % 16;
            int paddedLength    = finalBlockBytes > 0 ? (fullBlocks << 4) + 16 : fullBlocks << 4;
            int encryptedMessageLength = 2 + 16 + paddedLength + 32;
            // message output buffer
            message = new byte[encryptedMessageLength];
            // fill the length
            message[0] = (byte) ((length >> 8) & 0xFF);
            message[1] = (byte) (length & 0xFF);
            // generate an IV
            byte[] iv = this.generateIV();
            System.arraycopy(iv, 0, message, 2, 16);
            // encrypt the plain text
            Cipher cipher = Cipher.getInstance("AES/CBC/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(sessionKey, "AES"), new IvParameterSpec(iv));
            // full blocks
            if (fullBlocks > 0)
            {
                byte[] cipherText = cipher.update(plainText, 0, fullBlocks << 4);
                System.arraycopy(cipherText, 0, message, 2 + 16, cipherText.length);
            }
            // final block
            if (finalBlockBytes > 0)
            {
                byte[] finalBlock = new byte[16];
                this.randomFill(finalBlock);
                System.arraycopy(plainText, fullBlocks << 4, finalBlock, 0, finalBlockBytes);
                byte[] finalCipherText = cipher.doFinal(finalBlock);
                System.arraycopy(finalCipherText, 0, message, 2 + 16 + (fullBlocks << 4), finalCipherText.length);
            }
            // compute the HMAC
            byte[] hmac = Hash.sha256HMAC(sessionKey, new BufferSlice(message, 0, 2 + 16 + paddedLength));
            System.arraycopy(hmac, 0, message, 2 + 16 + paddedLength, hmac.length);
        }
        return message;
    }
    
    public byte[] decryptMessage(byte[] sessionKey, byte[] message) throws Exception
    {
        if (message.length == 0) return EMPTY_BUFFER;
        // the plain text length
        int length          = (message[0] & 0xFF) << 8 | (message[1] & 0xFF); 
        int fullBlocks      = length >> 4;
        int finalBlockBytes = length % 16;
        int paddedLength    = finalBlockBytes > 0 ? (fullBlocks << 4) + 16 : fullBlocks << 4;
        // the encryption iv
        byte[] iv = new byte[16];
        System.arraycopy(message, 2, iv, 0, 16);
        // the hmac
        byte[] hmac = new byte[32];
        System.arraycopy(message, 2 + 16 + paddedLength, hmac, 0, hmac.length);
        // verify the hmac
        byte[] expectedHMAC = Hash.sha256HMAC(sessionKey, new BufferSlice(message, 0, 2 + 16 + paddedLength));
        if (Arrays.equals(expectedHMAC, hmac))
        {
            // decrypt
            Cipher cipher = Cipher.getInstance("AES/CBC/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(sessionKey, "AES"), new IvParameterSpec(iv));
            byte[] paddedPlainText = cipher.doFinal(message, 2 + 16, paddedLength);
            byte[] plainText = new byte[length];
            System.arraycopy(paddedPlainText, 0, plainText, 0, length);
            return plainText;
        }
        else
        {
            throw new RuntimeException("Failed to verify message authenticity");
        }
    }
}
