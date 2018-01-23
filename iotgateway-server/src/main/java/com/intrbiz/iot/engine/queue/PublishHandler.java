package com.intrbiz.iot.engine.queue;

public interface PublishHandler
{
    void publish(byte[] message) throws Exception;
}
