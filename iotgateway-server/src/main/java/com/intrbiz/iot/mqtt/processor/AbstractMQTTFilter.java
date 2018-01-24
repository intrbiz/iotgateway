package com.intrbiz.iot.mqtt.processor;

public abstract class AbstractMQTTFilter implements MQTTFilter
{
    private final int order;
    
    protected AbstractMQTTFilter(int order)
    {
        super();
        this.order = order;
    }
    
    public int getOrder()
    {
        return this.order;
    }
}
