package com.intrbiz.iot.mqtt.processor;

public abstract class AbstractMQTTProcessor implements MQTTProcessor
{   
    private final String prefix;
    
    private final String[] topics;
    
    protected AbstractMQTTProcessor(String prefix, String... topics)
    {
        this.prefix = prefix;
        this.topics = topics;
    }

    @Override
    public String getPrefix()
    {
        return this.prefix;
    }

    @Override
    public String[] getTopics()
    {
        return this.topics;
    }
}
