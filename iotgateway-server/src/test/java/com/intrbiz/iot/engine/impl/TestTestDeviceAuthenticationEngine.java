package com.intrbiz.iot.engine.impl;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.*;

import java.security.SecureRandom;

import org.junit.Before;
import org.junit.Test;

import com.intrbiz.iot.engine.DeviceAuthenticationEngine;
import com.intrbiz.iot.engine.impl.authentication.FileDeviceAuthenticationEngine;

public class TestTestDeviceAuthenticationEngine
{
    private DeviceAuthenticationEngine engine;
    
    @Before
    public void setup()
    {
        this.engine = new FileDeviceAuthenticationEngine();
    }
    
    private byte[] randomArray(int length)
    {
        byte[] buffer = new byte[length];
        new SecureRandom().nextBytes(buffer);
        return buffer;
    }
    
    @Test
    public void testEncryptDecrypte1BMessage() throws Exception
    {
        this.testEncryptDecryptMessage(1);
    }
    
    @Test
    public void testEncryptDecrypte4BMessage() throws Exception
    {
        this.testEncryptDecryptMessage(4);
    }
    
    @Test
    public void testEncryptDecrypte12BMessage() throws Exception
    {
        this.testEncryptDecryptMessage(12);
    }
    
    @Test
    public void testEncryptDecrypte16BMessage() throws Exception
    {
        this.testEncryptDecryptMessage(16);
    }
    
    @Test
    public void testEncryptDecrypte20BMessage() throws Exception
    {
        this.testEncryptDecryptMessage(20);
    }
    
    @Test
    public void testEncryptDecrypte32BMessage() throws Exception
    {
        this.testEncryptDecryptMessage(32);
    }
    
    @Test
    public void testEncryptDecrypte37BMessage() throws Exception
    {
        this.testEncryptDecryptMessage(37);
    }
    
    @Test
    public void testEncryptDecrypte50BMessage() throws Exception
    {
        this.testEncryptDecryptMessage(50);
    }
    
    @Test
    public void testEncryptDecrypte150BMessage() throws Exception
    {
        this.testEncryptDecryptMessage(150);
    }
    
    private void testEncryptDecryptMessage(int messageLength) throws Exception
    {
        byte[] sessionKey = this.randomArray(16);
        byte[] message    = this.randomArray(messageLength);
        // encrypt it
        byte[] encrypted = this.engine.encryptMessage(sessionKey, message);
        assertThat(encrypted, is(notNullValue()));
        // decrypt it
        byte[] decrypted = this.engine.decryptMessage(sessionKey, encrypted);
        assertThat(decrypted, is(notNullValue()));
        assertThat(decrypted, is(equalTo(message)));
    }
}
