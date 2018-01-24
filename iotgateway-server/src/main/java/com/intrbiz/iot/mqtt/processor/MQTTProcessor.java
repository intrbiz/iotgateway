package com.intrbiz.iot.mqtt.processor;

public interface MQTTProcessor
{
    String getPrefix();
    
    String[] getTopics();
    
    void process(MQTTProcessorContext context, String topic, byte[] message);
}
