package com.intrbiz.iot.mqtt.processor;

public interface MQTTFilter
{
    int getOrder();
    
    void filter(MQTTProcessorContext context, String topic, byte[] message);
}
