package com.intrbiz.iot.engine.impl.authentication;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.UUID;

import com.intrbiz.iot.model.DeviceKey;
import com.intrbiz.iot.model.RegistrationKey;

public class FileDeviceAuthenticationEngine extends BaseDeviceAuthenticationEngine
{    
    public FileDeviceAuthenticationEngine()
    {
    }
    
    protected void setRegistrationKey(RegistrationKey key)
    {
        try
        {
            try (FileOutputStream fos = new FileOutputStream(new File("keys", key.getId().toString() + ".rkey")))
            {
                fos.write(key.getKey());
                fos.flush();
            }
        }
        catch (Exception e)
        {
            throw new RuntimeException("Failed to store device key", e);
        }
    }
    
    public RegistrationKey getRegistrationKey(UUID keyId)
    {
        File keyFile = new File("keys", keyId.toString() + ".rkey");
        if (keyFile.exists())
        {
            try
            {
                byte[] deviceKey = new byte[16];
                try (FileInputStream fin = new FileInputStream(keyFile))
                {
                    fin.read(deviceKey);
                }
                return new RegistrationKey(keyId, deviceKey);
            }
            catch (Exception e)
            {
                // fail if we can't get the key
                throw new RuntimeException("Failed to get device key", e);
            }
        }
        // fail if we can't get the key
        throw new RuntimeException("Failed to get registration key");
    }
    
    public DeviceKey getDeviceKey(UUID clientId)
    {
        File keyFile = new File("keys", clientId.toString() + ".key");
        if (keyFile.exists())
        {
            try
            {
                byte[] deviceKey = new byte[16];
                try (FileInputStream fin = new FileInputStream(keyFile))
                {
                    fin.read(deviceKey);
                }
                return new DeviceKey(clientId, deviceKey);
            }
            catch (Exception e)
            {
                // fail if we can't get the key
                throw new RuntimeException("Failed to get device key", e);
            }
        }
        // fail if we can't get the key
        throw new RuntimeException("Failed to get device key");
    }
    
    protected void setDeviceKey(UUID registrationKeyId, DeviceKey key)
    {
        try
        {
            try (FileOutputStream fos = new FileOutputStream(new File("keys", key.getId().toString() + ".key")))
            {
                fos.write(key.getKey());
                fos.flush();
            }
        }
        catch (Exception e)
        {
            throw new RuntimeException("Failed to store device key", e);
        }
    }
}
