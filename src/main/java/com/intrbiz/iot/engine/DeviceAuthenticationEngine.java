package com.intrbiz.iot.engine;

import java.util.UUID;

import com.intrbiz.iot.model.DeviceKey;
import com.intrbiz.iot.model.RegistrationKey;
import com.intrbiz.iot.model.SessionKey;

/**
 * Handle registration, authentication and encryption of devices.
 *
 */
public interface DeviceAuthenticationEngine
{
    /* Connect preconditions */
    
    boolean checkDeviceConnect(UUID clientId);
    
    /* Device registration */
    
    byte[] generateDeviceRegistrationChallenge(UUID clientId, UUID keyId, byte[] clientNonce);
    
    boolean checkDeviceRegistrationResponse(UUID clientId, UUID keyId, byte[] challenge, byte[] response);
    
    byte[] registerDevice(UUID clientId, UUID keyId);
    
    /* Device authentication */
    
    byte[] generateDeviceAuthenticationChallenge(UUID clientId, byte[] clientNonce);
    
    boolean checkDeviceAuthenticationResponse(UUID clientId, byte[] challenge, byte[] response);
    
    /* Key handling */
    
    RegistrationKey generateRegistrationKey();
    
    RegistrationKey getRegistrationKey(UUID keyId);
    
    DeviceKey getDeviceKey(UUID clientId);
    
    SessionKey generateSessionKey(UUID clientId);
    
    /* Message ciphering */
    
    byte[] encryptMessage(byte[] sessionKey, byte[] plainText) throws Exception;
    
    byte[] decryptMessage(byte[] sessionKey, byte[] cipherText) throws Exception;
}
