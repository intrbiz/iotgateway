package com.intrbiz.iot.engine.impl.queue;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeoutException;

import org.apache.log4j.Logger;

import com.intrbiz.iot.engine.QueueEngine;
import com.intrbiz.iot.engine.queue.PublishHandler;
import com.intrbiz.iot.engine.queue.QueueContext;
import com.intrbiz.queue.QueueBrokerPool;
import com.intrbiz.queue.QueueManager;
import com.rabbitmq.client.AMQP.BasicProperties;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;

public class RabbitQueueEngine implements QueueEngine
{
    private QueueBrokerPool<Channel> broker;
    
    private Logger logger = Logger.getLogger(RabbitQueueEngine.class);
    
    @SuppressWarnings("unchecked")
    public RabbitQueueEngine(QueueBrokerPool<?> broker)
    {
        this.broker = (QueueBrokerPool<Channel>) broker;
    }
    
    public RabbitQueueEngine()
    {
        this(QueueManager.getInstance().defaultBroker());
    }
    
    public QueueBrokerPool<Channel> getBroker()
    {
        return this.broker;
    }
    
    public QueueContext createContext(UUID clientId)
    {
        logger.debug("Creating channel for client " + clientId);
        return new RabbitQueueContext(clientId, this.broker.connect());
    }
    
    private class RabbitQueueContext implements QueueContext
    {
        private final UUID clientId;
        
        private final Channel channel;
        
        private RabbitQueueContext(UUID clientId, Channel channel)
        {
            this.clientId = clientId;
            this.channel = channel;
        }

        @Override
        public void subscribe(final String topic, final PublishHandler handler)
        {
            logger.debug("Subscribing client " + this.clientId + " to topic " + topic);
            try
            {
                // ensure the exchange is created
                this.channel.exchangeDeclare(topic, "topic", true);
                // setup a transient queue
                String queue = this.channel.queueDeclare().getQueue();
                // bind our queue to the exchange, route on the device id
                this.channel.queueBind(queue, topic, this.clientId.toString());
                // start consuming
                this.channel.basicConsume(queue, new DefaultConsumer(this.channel)
                {
                    @Override
                    public void handleDelivery(String consumerTag, Envelope envelope, BasicProperties properties, byte[] body) throws IOException
                    {
                        logger.debug("Recieved message for client " + clientId + " on topic " + topic);
                        try
                        {
                            handler.publish(body);
                            channel.basicAck(envelope.getDeliveryTag(), false);
                        }
                        catch (Exception e)
                        {
                            logger.error("Error publishing message to client", e);
                            throw new IOException("Error publishing message to client", e);
                        }
                    }
                });
            }
            catch (IOException e)
            {
                throw new RuntimeException("Failed to subscribe", e);
            }
        }

        @Override
        public void publish(String topic, byte[] message)
        {
            logger.debug("Client " + this.clientId + " publishing message to topic " + topic);
            try
            {
                Map<String, Object> headers = new HashMap<String, Object>();
                headers.put("device-id", this.clientId.toString());
                headers.put("mqtt-client-id", this.clientId.toString());
                headers.put("mqtt-topic", topic);
                BasicProperties props = new BasicProperties.Builder().headers(headers).timestamp(new Date()).build();
                // publish this message
                this.channel.basicPublish(topic, this.clientId.toString(), props, message);
            }
            catch (IOException e)
            {
                throw new RuntimeException("Failed to publish", e);
            }
        }

        @Override
        public void close()
        {
            logger.debug("Closing channel for client " + this.clientId);
            try
            {
                this.channel.close();
            }
            catch (IOException | TimeoutException e)
            {
                // ignore
            }
        }
    }
}
