package com.intrbiz.iot.mqtt.processor;

import java.util.UUID;

import com.intrbiz.iot.engine.DeviceAuthenticationEngine;
import com.intrbiz.iot.engine.FirmwareEngine;
import com.intrbiz.iot.engine.QueueEngine;
import com.intrbiz.iot.engine.firmware.FirmwareContext;
import com.intrbiz.iot.engine.queue.QueueContext;
import com.intrbiz.iot.mqtt.handler.MQTTHandler.ClientState;

public interface MQTTProcessorContext
{
    // engines
    
    DeviceAuthenticationEngine authenticationEngine();
    
    QueueEngine queueEngine(); 
    
    FirmwareEngine firmwareEngine();
    
    // state
    
    UUID clientId();
    
    ClientState state();
    
    // contexts
    
    QueueContext queue();
    
    FirmwareContext firmware();
    
    // publish to client
    
    void publishMessage(String topic, byte[] plainText);
}
