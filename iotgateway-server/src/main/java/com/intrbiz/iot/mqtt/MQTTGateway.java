package com.intrbiz.iot.mqtt;

import org.apache.log4j.Logger;

import com.intrbiz.iot.engine.DeviceAuthenticationEngine;
import com.intrbiz.iot.engine.FirmwareEngine;
import com.intrbiz.iot.engine.QueueEngine;
import com.intrbiz.iot.mqtt.handler.MQTTHandler;
import com.intrbiz.iot.mqtt.processor.MQTTProcessingChain;
import com.intrbiz.iot.mqtt.processor.impl.FirmwareUpdate;
import com.intrbiz.iot.mqtt.processor.impl.QueueForward;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.mqtt.MqttDecoder;
import io.netty.handler.codec.mqtt.MqttEncoder;
import io.netty.handler.timeout.IdleStateHandler;

public class MQTTGateway implements Runnable
{
    private Logger logger = Logger.getLogger(MQTTGateway.class);
    
    private EventLoopGroup bossGroup;

    private EventLoopGroup workerGroup;
    
    private final int port;
    
    private final DeviceAuthenticationEngine authenticationEngine;
    
    private final QueueEngine queue;
    
    private final FirmwareEngine firmware;
    
    private final MQTTProcessingChain processingChain;
    
    public MQTTGateway(int port, DeviceAuthenticationEngine authenticationEngine, QueueEngine queue, FirmwareEngine firmware)
    {
        super();
        this.port = port;
        this.authenticationEngine = authenticationEngine;
        this.queue = queue;
        this.firmware = firmware;
        this.processingChain = new MQTTProcessingChain();
        // register our default processors and filters
        this.processingChain.addProcessor(new FirmwareUpdate());
        this.processingChain.setDefaultProcessor(new QueueForward());
    }
    
    public MQTTProcessingChain processingChain()
    {
        return this.processingChain;
    }
    
    public QueueEngine queueEngine()
    {
        return this.queue;
    }
    
    public DeviceAuthenticationEngine authenticationEngine()
    {
        return this.authenticationEngine;
    }
    
    public FirmwareEngine firmwareEngine()
    {
        return this.firmware;
    }
    
    public void run()
    {
        // setup the thread groups
        bossGroup = new NioEventLoopGroup(1);
        workerGroup = new NioEventLoopGroup();
        // setup the server
        ServerBootstrap b = new ServerBootstrap();
        b.group(bossGroup, workerGroup).channel(NioServerSocketChannel.class).childHandler(new ChannelInitializer<SocketChannel>()
        {
            @Override
            public void initChannel(SocketChannel ch) throws Exception
            {
                ChannelPipeline pipeline = ch.pipeline();
                pipeline.addFirst("idleStateHandler", new IdleStateHandler(60, 60, 60));
                pipeline.addLast("decoder", new MqttDecoder());
                pipeline.addLast("encoder", new MqttEncoder());
                pipeline.addLast("handler", new MQTTHandler(MQTTGateway.this.authenticationEngine, MQTTGateway.this.queue, MQTTGateway.this.firmware, MQTTGateway.this.processingChain));
            }
        }).option(ChannelOption.SO_BACKLOG, 128).option(ChannelOption.SO_REUSEADDR, true).option(ChannelOption.TCP_NODELAY, true).childOption(ChannelOption.SO_KEEPALIVE, true);
        try
        {
            // Bind and start to accept incoming connections.
            ChannelFuture f = b.bind(this.port);
            logger.info("Server bound to port " + this.port);
            f.sync();
        }
        catch (InterruptedException ex)
        {
            logger.error(null, ex);
        }
    }
}
