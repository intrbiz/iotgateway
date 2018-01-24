package com.intrbiz.iot.mqtt.processor.impl;

import com.intrbiz.iot.mqtt.processor.AbstractMQTTProcessor;
import com.intrbiz.iot.mqtt.processor.MQTTProcessorContext;

public class QueueForward extends AbstractMQTTProcessor
{    
    public QueueForward()
    {
        super(null);
    }

    @Override
    public void process(MQTTProcessorContext context, String topic, byte[] message)
    {
        context.queue().publish(topic, message);
    }
}
