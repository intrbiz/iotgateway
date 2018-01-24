package com.intrbiz.iot.mqtt.handler;

import java.util.Arrays;
import java.util.UUID;

import org.apache.log4j.Logger;

import com.intrbiz.iot.engine.DeviceAuthenticationEngine;
import com.intrbiz.iot.engine.FirmwareEngine;
import com.intrbiz.iot.engine.QueueEngine;
import com.intrbiz.iot.engine.firmware.FirmwareContext;
import com.intrbiz.iot.engine.queue.QueueContext;
import com.intrbiz.iot.model.SessionKey;
import com.intrbiz.iot.mqtt.processor.MQTTProcessingChain;
import com.intrbiz.iot.mqtt.processor.MQTTProcessorContext;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.mqtt.MqttConnAckMessage;
import io.netty.handler.codec.mqtt.MqttConnAckVariableHeader;
import io.netty.handler.codec.mqtt.MqttConnectMessage;
import io.netty.handler.codec.mqtt.MqttConnectReturnCode;
import io.netty.handler.codec.mqtt.MqttFixedHeader;
import io.netty.handler.codec.mqtt.MqttMessage;
import io.netty.handler.codec.mqtt.MqttMessageIdVariableHeader;
import io.netty.handler.codec.mqtt.MqttMessageType;
import io.netty.handler.codec.mqtt.MqttPubAckMessage;
import io.netty.handler.codec.mqtt.MqttPublishMessage;
import io.netty.handler.codec.mqtt.MqttPublishVariableHeader;
import io.netty.handler.codec.mqtt.MqttQoS;
import io.netty.handler.codec.mqtt.MqttSubscribeMessage;
import io.netty.handler.codec.mqtt.MqttTopicSubscription;

/**
 * An MQTT gateway for RabbitMQ which provides authentication of 
 * devices and encryption of message payloads.
 *
 */
public class MQTTHandler extends ChannelInboundHandlerAdapter implements MQTTProcessorContext
{
    private static Logger logger = Logger.getLogger(MQTTHandler.class);
    
    /**
     * Authentication timeout in nanoseconds
     */
    public static final long AUTHENTICATION_TIMEOUT = 30L * 1000_000_000L;
    
    public static final int AUTHENTICATION_ATTEMPTS = 3;
    
    public static final int ENCODED_UUID_LENGTH = 36;
    
    public enum ClientState
    {
        INITIAL,
        CONNECTED,
        REGISTERING,
        AUTHENTICATING,
        AUTHENTICATED,
        DISCONNECTED
    }
    
    // processing chain
    
    private final MQTTProcessingChain processingChain;
    
    // engines
    
    private final DeviceAuthenticationEngine authenticationEngine;
    
    private final QueueEngine queue;
    
    private final FirmwareEngine firmware;
    
    // state
    
    private UUID clientId;
    
    private ClientState state = ClientState.INITIAL;
    
    private byte[] authChallenge = null;
    
    private long authStart = 0;
    
    private UUID regKeyId;
    
    private byte[] regChallenge;
    
    private long regStart;
    
    private byte[] sessionKey;
    
    // context
    
    private QueueContext queueContext;
    
    private FirmwareContext fimrwareContext;
    
    // the connection
    
    private ChannelHandlerContext context;

    public MQTTHandler(DeviceAuthenticationEngine authenticationEngine, QueueEngine queue, FirmwareEngine engine, MQTTProcessingChain processingChain)
    {
        super();
        this.authenticationEngine = authenticationEngine;
        this.queue = queue;
        this.firmware = engine;
        this.processingChain = processingChain;
    }
    
    @Override
    public QueueEngine queueEngine()
    {
        return this.queue;
    }
    
    @Override
    public FirmwareEngine firmwareEngine()
    {
        return this.firmware;
    }
    
    @Override
    public DeviceAuthenticationEngine authenticationEngine()
    {
        return this.authenticationEngine;
    }
    
    @Override
    public QueueContext queue()
    {
        return this.queueContext;
    }
    
    @Override
    public FirmwareContext firmware()
    {
        return this.fimrwareContext;
    }

    @Override
    public UUID clientId()
    {
        return this.clientId;
    }

    @Override
    public ClientState state()
    {
        return this.state;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception
    {
        this.context = ctx;
        logger.info("Got connection, from: " + ctx.channel().remoteAddress());
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception
    {
        // close any queue subscriptions
        if (this.queueContext != null) this.queueContext.close();
        if (this.fimrwareContext != null) this.fimrwareContext.close();
        logger.info("Closed connection, from: " + ctx.channel().remoteAddress());
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception
    {
        logger.debug("Got message: " + msg);
        // process the message
        if (msg instanceof MqttConnectMessage)
        {
            // we got a connect
            this.processConnect(ctx, (MqttConnectMessage) msg);
        }
        else if (msg instanceof MqttPublishMessage)
        {
            // we got a message from the client
            this.processPublish(ctx, (MqttPublishMessage) msg);
        }
        else if (msg instanceof MqttSubscribeMessage)
        {
            // subscribe
            this.processSubscribe(ctx, (MqttSubscribeMessage) msg);
        }
        else if (msg instanceof MqttMessage)
        {
            // generic message
            this.processGenericMessage(ctx, (MqttMessage) msg);
        }
    }
    
    private void processSubscribe(final ChannelHandlerContext ctx, MqttSubscribeMessage message) throws Exception
    {
        // dispatch the message
        if (this.state == ClientState.AUTHENTICATED)
        {
            // setup the subscription
            for (MqttTopicSubscription subscription : message.payload().topicSubscriptions())
            {
                // subscribe to the given topic
                final String topic = subscription.topicName();
                this.queueContext.subscribe(topic, (msg) -> { this.publishMessageToClient(ctx, topic, msg); });
                logger.info("Subscribing client " + this.clientId + " to topic " + topic);
            }
        }
        else
        {
            // reject
            logger.info("Got publish from client whilst in illegal state, closing");
            ctx.close();
        }
    }
    
    private void processGenericMessage(ChannelHandlerContext ctx, MqttMessage message) throws Exception
    {
        if (message.fixedHeader().messageType() == MqttMessageType.DISCONNECT)
        {
            logger.info("Client " + this.clientId + " disconnecting");
            this.state = ClientState.DISCONNECTED;
            ctx.close();
        }
        else if (message.fixedHeader().messageType() == MqttMessageType.PINGREQ)
        {
            logger.debug("Ping Pong");
            ctx.writeAndFlush(new MqttMessage(new MqttFixedHeader(MqttMessageType.PINGRESP, false, MqttQoS.AT_MOST_ONCE, false, 0)));
        }
        else if (message.fixedHeader().messageType() == MqttMessageType.PUBREL)
        {
            ctx.writeAndFlush(new MqttPubAckMessage(new MqttFixedHeader(MqttMessageType.PUBCOMP, false, MqttQoS.AT_MOST_ONCE, false, 0), MqttMessageIdVariableHeader.from(((MqttMessageIdVariableHeader) message.variableHeader()).messageId())));
        }
    }
    
    private void processConnect(ChannelHandlerContext ctx, MqttConnectMessage message) throws Exception
    {
        if (this.state == ClientState.INITIAL)
        {
            logger.info("Got connection: " + message.payload().clientIdentifier() + " " + message.payload().userName() + " " + message.payload().password() + " " + message.fixedHeader().qosLevel());
            // get the client id
            if (message.payload().clientIdentifier() != null && message.payload().clientIdentifier().length() == ENCODED_UUID_LENGTH)
            {
                this.clientId = UUID.fromString(message.payload().clientIdentifier());
                // connect pre-condition
                if (this.authenticationEngine.checkDeviceConnect(this.clientId))
                {
                    // ack the connect
                    logger.info("Connected ok");
                    this.state = ClientState.CONNECTED;
                    ctx.writeAndFlush(new MqttConnAckMessage(new MqttFixedHeader(MqttMessageType.CONNACK, false, MqttQoS.AT_MOST_ONCE, false, 0), new MqttConnAckVariableHeader(MqttConnectReturnCode.CONNECTION_ACCEPTED, true)));
                }
                else
                {
                    // reject the connect
                    logger.info("Terminating connect");
                    this.state = ClientState.DISCONNECTED;
                    ctx.writeAndFlush(new MqttConnAckMessage(new MqttFixedHeader(MqttMessageType.CONNACK, false, MqttQoS.AT_MOST_ONCE, false, 0), new MqttConnAckVariableHeader(MqttConnectReturnCode.CONNECTION_REFUSED_NOT_AUTHORIZED, true)));
                    ctx.close();
                }
            }
            else
            {
                logger.info("Rejecting bad client id: [" + message.payload().clientIdentifier().length() + "] '" + message.payload().clientIdentifier()  + "'");
                // reject the connect
                this.state = ClientState.DISCONNECTED;
                ctx.writeAndFlush(new MqttConnAckMessage(new MqttFixedHeader(MqttMessageType.CONNACK, false, MqttQoS.AT_MOST_ONCE, false, 0), new MqttConnAckVariableHeader(MqttConnectReturnCode.CONNECTION_REFUSED_IDENTIFIER_REJECTED, true)));
                ctx.close();
            }
        }
        else
        {
            logger.warn("Got connect for already connected connection, terminating");
            this.state = ClientState.DISCONNECTED;
            ctx.close();
        }
    }
    
    private void processPublish(ChannelHandlerContext ctx, MqttPublishMessage message) throws Exception
    {
        // dispatch the message
        if (this.state == ClientState.AUTHENTICATED)
        {
            this.processAuthenticatedPublish(ctx, message);
        }
        else
        {
            this.processUnauthenticatedPublish(ctx, message);
        }
    }
    
    @Override
    public void publishMessage(String topic, byte[] plainText)
    {
        try
        {
            this.publishMessageToClient(this.context, topic, plainText);
        }
        catch (Exception e)
        {
            throw new RuntimeException("Failed to publish message to client", e);
        }
    }
    
    private void publishMessageToClient(ChannelHandlerContext ctx, String topic, byte[] plainText) throws Exception
    {
        if (this.state == ClientState.AUTHENTICATED)
        {
            byte[] message = this.authenticationEngine.encryptMessage(this.sessionKey, plainText);
            // publish the message
            logger.debug("Publishing encrypted message to client " + this.clientId + " on topic " + topic);
            ctx.writeAndFlush(new MqttPublishMessage(
                    new MqttFixedHeader(MqttMessageType.PUBLISH, false, MqttQoS.AT_MOST_ONCE, false, 0),
                    new MqttPublishVariableHeader(topic, 0),
                    Unpooled.wrappedBuffer(message)
            ));
        }
        else
        {
            throw new RuntimeException("Cannot publish message, client not yet authenticated");
        }
    }
    
    private void processAuthenticatedPublish(ChannelHandlerContext ctx, MqttPublishMessage message) throws Exception
    {
        // the encrypted message
        byte[] encrypted = new byte[message.content().readableBytes()];
        message.content().readBytes(encrypted);
        // decrypt the message
        byte[] plainText = this.authenticationEngine.decryptMessage(this.sessionKey, encrypted);
        // ACK the message, TODO: handle QoS better, for now just ignore it
        if (message.fixedHeader().qosLevel() == MqttQoS.AT_LEAST_ONCE)
        {
            ctx.writeAndFlush(new MqttPubAckMessage(new MqttFixedHeader(MqttMessageType.PUBACK, false, MqttQoS.AT_MOST_ONCE, false, 0), MqttMessageIdVariableHeader.from(message.variableHeader().messageId())));
        }
        else if (message.fixedHeader().qosLevel() == MqttQoS.EXACTLY_ONCE)
        {
            ctx.writeAndFlush(new MqttPubAckMessage(new MqttFixedHeader(MqttMessageType.PUBREC, false, MqttQoS.AT_MOST_ONCE, false, 0), MqttMessageIdVariableHeader.from(message.variableHeader().messageId())));
        }
        // dispatch the message
        String topic = message.variableHeader().topicName();
        if (logger.isDebugEnabled()) logger.debug("Got message: " + topic + " with " + Arrays.toString(plainText) + " " + new String(plainText) + " " + message.fixedHeader().qosLevel() + " from " + this.clientId + " id " + message.variableHeader().messageId() + " qos " + message.fixedHeader().qosLevel());
        this.processingChain.process(this, topic, plainText);
    }
    
    private void processUnauthenticatedPublish(ChannelHandlerContext ctx, MqttPublishMessage message) throws Exception
    {
        // permit access to our registration and authentication protocol
        String topic = message.variableHeader().topicName();
        if (topic != null && topic.startsWith("/v1/register") && (this.state == ClientState.CONNECTED || this.state == ClientState.REGISTERING))
        {
            this.processRegistration(ctx, message);
        }
        else if (topic != null && topic.startsWith("/v1/auth") && (this.state == ClientState.CONNECTED || this.state == ClientState.AUTHENTICATING))
        {
            this.processAuthentication(ctx, message);
        }
        else
        {
            // reject
            logger.debug("Got publish from client whilst in illegal state, closing");
            ctx.close();
        }
    }
    
    private void processRegistration(ChannelHandlerContext ctx, MqttPublishMessage message) throws Exception
    {
        String topic = message.variableHeader().topicName();
        logger.debug("Processing registration message: " + topic);
        if ("/v1/register/challenge".equals(topic))
        {
            // copy the keyId
            this.regKeyId = new UUID(message.content().readLong(), message.content().readLong());
            logger.info("Starting registration of client " + this.clientId + " with key " + this.regKeyId);
            byte[] clientNonce = new byte[16];
            message.content().readBytes(clientNonce);
            // start of registration process
            this.regChallenge = this.authenticationEngine.generateDeviceRegistrationChallenge(clientId, this.regKeyId, clientNonce);
            this.regStart = System.nanoTime();
            this.state = ClientState.REGISTERING;
            // publish the random token to the client, which it must sign
            ctx.writeAndFlush(new MqttPublishMessage(
                    new MqttFixedHeader(MqttMessageType.PUBLISH, false, MqttQoS.AT_MOST_ONCE, false, 0),
                    new MqttPublishVariableHeader("/v1/register/response", 0),
                    Unpooled.wrappedBuffer(this.regChallenge)
            ));
            logger.debug("Published registration challenge");
        }
        else if ("/v1/register/complete".equalsIgnoreCase(topic) && this.state == ClientState.REGISTERING)
        {
            // copy the response
            byte[] response = new byte[message.content().readableBytes()];
            message.content().readBytes(response);
            logger.debug("Completing registration of client " + this.clientId + " using key " + this.regKeyId + " response = " + Arrays.toString(response));
            // verify
            if (((System.nanoTime() - this.regStart) < AUTHENTICATION_TIMEOUT) && this.authenticationEngine.checkDeviceRegistrationResponse(this.clientId, this.regKeyId, this.regChallenge, response))
            {
                logger.info("Successfully registered client " + this.clientId + " using key " + this.regKeyId);
                // assign the device a key
                byte[] deviceKey = this.authenticationEngine.registerDevice(this.clientId, this.regKeyId);
                // respond
                this.state = ClientState.CONNECTED; // Drop back to connected so that auth can happen
                this.regChallenge = null;
                this.regKeyId = null;
                this.regStart = 0;
                ctx.writeAndFlush(new MqttPublishMessage(
                        new MqttFixedHeader(MqttMessageType.PUBLISH, false, MqttQoS.AT_MOST_ONCE, false, 0),
                        new MqttPublishVariableHeader("/v1/register/ok", 0),
                        Unpooled.wrappedBuffer(deviceKey)
                ));
            }
            else
            {
                logger.warn("Failed to register client " + this.clientId);
                this.state = ClientState.DISCONNECTED;
                this.regChallenge = null;
                this.regKeyId = null;
                this.regStart = 0;
                ctx.writeAndFlush(new MqttPublishMessage(
                        new MqttFixedHeader(MqttMessageType.PUBLISH, false, MqttQoS.AT_MOST_ONCE, false, 0),
                        new MqttPublishVariableHeader("/v1/register/error", 0),
                        Unpooled.EMPTY_BUFFER
                ));
                ctx.close();
            }
        }
    }
    
    private void processAuthentication(ChannelHandlerContext ctx, MqttPublishMessage message) throws Exception
    {
        // process our custom auth protocol
        String topic = message.variableHeader().topicName();
        logger.debug("Processing authentication message: " + topic);
        if ("/v1/auth/challenge".equals(topic))
        {
            byte[] clientNonce = new byte[16];
            message.content().readBytes(clientNonce);
            logger.info("Starting authentication of client " + this.clientId);
            // start of authentication process
            this.authChallenge = this.authenticationEngine.generateDeviceAuthenticationChallenge(this.clientId, clientNonce);
            if (this.authChallenge != null)
            {
                logger.debug("Generated auth challenge: " + Arrays.toString(this.authChallenge));
                this.authStart = System.nanoTime();
                this.state = ClientState.AUTHENTICATING;
                // publish the random token to the client, which it must sign
                ctx.writeAndFlush(new MqttPublishMessage(
                        new MqttFixedHeader(MqttMessageType.PUBLISH, false, MqttQoS.AT_MOST_ONCE, false, 0),
                        new MqttPublishVariableHeader("/v1/auth/response", 0),
                        Unpooled.wrappedBuffer(this.authChallenge)
                ));
            }
            else
            {
                // authentication failed
                logger.debug("Failed to authenticate device " + this.clientId);
                this.state = ClientState.CONNECTED;
                this.authChallenge = null;
                this.authStart = 0;
                ctx.writeAndFlush(new MqttPublishMessage(
                        new MqttFixedHeader(MqttMessageType.PUBLISH, false, MqttQoS.AT_MOST_ONCE, false, 0),
                        new MqttPublishVariableHeader("/v1/auth/error", 0),
                        Unpooled.EMPTY_BUFFER
                ));
                logger.warn("Client " + this.clientId + " has failed autentication too many times, closing");
                this.state = ClientState.DISCONNECTED;
                ctx.close();
            }
        }
        else if ("/v1/auth/complete".equals(topic) && this.state == ClientState.AUTHENTICATING)
        {
            // copy the response
            byte[] response = new byte[message.content().readableBytes()];
            message.content().readBytes(response);
            logger.debug("Verifying client " + this.clientId + " " + Arrays.toString(response) + " [" + response.length + "], auth time: " + (System.nanoTime() - this.authStart));
            // verify
            if (((System.nanoTime() - this.authStart) < AUTHENTICATION_TIMEOUT) && this.authenticationEngine.checkDeviceAuthenticationResponse(this.clientId, this.authChallenge, response))
            {
                // authentication passed
                logger.info("Successfully authenticated device " + this.clientId);
                this.state = ClientState.AUTHENTICATED;
                this.authChallenge = null;
                this.authStart = 0;
                // assign the session key
                SessionKey theSessionKey = this.authenticationEngine.generateSessionKey(this.clientId);
                this.sessionKey = theSessionKey.getSessionKey();
                // setup the our session contexts
                this.queueContext = this.queue.createContext(this.clientId);
                if (this.firmware != null) this.fimrwareContext = this.firmware.createContext(this.clientId);
                // ack the auth
                ctx.writeAndFlush(new MqttPublishMessage(
                        new MqttFixedHeader(MqttMessageType.PUBLISH, false, MqttQoS.AT_MOST_ONCE, false, 0),
                        new MqttPublishVariableHeader("/v1/auth/ok", 0),
                        Unpooled.wrappedBuffer(theSessionKey.getMessage())
                ));
            }
            else
            {
                // authentication failed
                logger.debug("Failed to authenticate device " + this.clientId);
                this.state = ClientState.CONNECTED;
                this.authChallenge = null;
                this.authStart = 0;
                ctx.writeAndFlush(new MqttPublishMessage(
                        new MqttFixedHeader(MqttMessageType.PUBLISH, false, MqttQoS.AT_MOST_ONCE, false, 0),
                        new MqttPublishVariableHeader("/v1/auth/error", 0),
                        Unpooled.EMPTY_BUFFER
                ));
                logger.warn("Client " + this.clientId + " has failed autentication too many times, closing");
                this.state = ClientState.DISCONNECTED;
                ctx.close();
            }
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception
    {
        logger.warn("Error handling client " + this.clientId + " closing connection.", cause);
        this.state = ClientState.DISCONNECTED;
        ctx.close();
    }
}
