package com.intrbiz.iot;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import com.intrbiz.iot.engine.impl.authentication.FileDeviceAuthenticationEngine;
import com.intrbiz.iot.engine.impl.queue.RabbitQueueEngine;
import com.intrbiz.iot.mqtt.MQTTGateway;
import com.intrbiz.queue.QueueManager;
import com.intrbiz.queue.rabbit.RabbitPool;

public class ExampleGateway
{
    public static void main(String[] args)
    {
        BasicConfigurator.configure();
        Logger.getRootLogger().setLevel(Level.TRACE);
        QueueManager.getInstance().registerDefaultBroker(new RabbitPool("amqp://127.0.0.1", "mqtt", "mqtt"));
        new MQTTGateway(1884, new FileDeviceAuthenticationEngine(), new RabbitQueueEngine(), null).run();
    }
}
