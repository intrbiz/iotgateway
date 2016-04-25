package com.intrbiz.iot.engine.queue;

public interface QueueContext
{
    void subscribe(String topic, PublishHandler handler);
    
    void publish(String topic, byte[] message);
    
    void close();
}
