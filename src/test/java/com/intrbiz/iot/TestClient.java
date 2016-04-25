package com.intrbiz.iot;

import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.UUID;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import com.intrbiz.util.Hash;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.mqtt.MqttConnAckMessage;
import io.netty.handler.codec.mqtt.MqttConnectMessage;
import io.netty.handler.codec.mqtt.MqttConnectPayload;
import io.netty.handler.codec.mqtt.MqttConnectReturnCode;
import io.netty.handler.codec.mqtt.MqttConnectVariableHeader;
import io.netty.handler.codec.mqtt.MqttDecoder;
import io.netty.handler.codec.mqtt.MqttEncoder;
import io.netty.handler.codec.mqtt.MqttFixedHeader;
import io.netty.handler.codec.mqtt.MqttMessageType;
import io.netty.handler.codec.mqtt.MqttPublishMessage;
import io.netty.handler.codec.mqtt.MqttPublishVariableHeader;
import io.netty.handler.codec.mqtt.MqttQoS;
import io.netty.handler.timeout.IdleStateHandler;

public class TestClient
{
    private static final Charset UTF8 = Charset.forName("UTF8");
    
    public static void main(String[] args) throws Exception
    {
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        Bootstrap b = new Bootstrap();
        b.group(workerGroup);
        b.channel(NioSocketChannel.class);
        b.option(ChannelOption.SO_KEEPALIVE, true);
        b.handler(new ChannelInitializer<SocketChannel>()
        {
            @Override
            public void initChannel(SocketChannel ch) throws Exception
            {
                ch.pipeline().addFirst("idleStateHandler", new IdleStateHandler(0, 0, 30));
                ch.pipeline().addLast("decoder", new MqttDecoder());
                ch.pipeline().addLast("encoder", new MqttEncoder());
                ch.pipeline().addLast("handler", new MQTTTestClientHandler());
            }
        });
        ChannelFuture f = b.connect("127.0.0.1", 1884).sync();
        f.channel().closeFuture().sync();
    }
    
    private static class MQTTTestClientHandler extends ChannelInboundHandlerAdapter
    {
        private UUID clientId = UUID.randomUUID();
        
        private static final byte[] REG_KEY = { 0xA, 0xB, 0xC, 0xD, 0xE, 0xF, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9 };
        
        private byte[] deviceKey;
        
        @Override
        public void channelActive(ChannelHandlerContext ctx) throws Exception
        {
            System.out.println("Connection active, starting handshake");
            // send a connect message
            ctx.writeAndFlush(new MqttConnectMessage(
                    new MqttFixedHeader(MqttMessageType.CONNECT, false, MqttQoS.AT_MOST_ONCE, false, 0),
                    new MqttConnectVariableHeader("MQTT", 4, false, false, false, 0, false, true, 60),
                    new MqttConnectPayload(clientId.toString(), null, null, null, null)
            ));
        }
        
        private void onConnected(ChannelHandlerContext ctx) throws Exception
        {
            System.out.println("Starting registration");
            UUID keyId = UUID.randomUUID();
            ByteBuf buf = Unpooled.buffer(8);
            buf.writeLong(keyId.getMostSignificantBits());
            buf.writeLong(keyId.getLeastSignificantBits());
            ctx.writeAndFlush(new MqttPublishMessage(
                    new MqttFixedHeader(MqttMessageType.PUBLISH, false, MqttQoS.AT_MOST_ONCE, false, 0),
                    new MqttPublishVariableHeader("/register/challenge", 0),
                    buf
            ));
        }
        
        private void onRegistered(ChannelHandlerContext ctx) throws Exception
        {
            System.out.println("Starting auth");
            ctx.writeAndFlush(new MqttPublishMessage(
                    new MqttFixedHeader(MqttMessageType.PUBLISH, false, MqttQoS.AT_MOST_ONCE, false, 0),
                    new MqttPublishVariableHeader("/auth/challenge", 0),
                    Unpooled.EMPTY_BUFFER
            ));
        }
        
        private void onAuthenticated(ChannelHandlerContext ctx) throws Exception
        {
            ctx.writeAndFlush(new MqttPublishMessage(
                    new MqttFixedHeader(MqttMessageType.PUBLISH, false, MqttQoS.AT_MOST_ONCE, false, 0),
                    new MqttPublishVariableHeader("/hello", 0),
                    Unpooled.copiedBuffer("Hello World", UTF8)
            ));
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) throws Exception
        {
            System.out.println("Disconnected");
            System.exit(0);
        }

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception
        {
            if (msg instanceof MqttConnAckMessage)
            {
                MqttConnAckMessage connAck = (MqttConnAckMessage) msg;
                if (connAck.variableHeader().connectReturnCode() == MqttConnectReturnCode.CONNECTION_ACCEPTED)
                {
                    System.out.println("Successfully connected");
                    this.onConnected(ctx);
                }
            }
            else if (msg instanceof MqttPublishMessage)
            {
                this.processPublish(ctx, (MqttPublishMessage) msg);
            }
        }
        
        private void processPublish(ChannelHandlerContext ctx, MqttPublishMessage message) throws Exception
        {
            // dispatch the message
            String topic = message.variableHeader().topicName();
            if (topic != null && topic.startsWith("/register"))
            {
                System.out.println("Processing registration");
                if ("/register/response".equals(topic))
                {
                    // read the nonce
                    byte[] nonce = new byte[message.content().readableBytes()];
                    message.content().readBytes(nonce);
                    // hash the nonce and our secret key
                    System.out.println("Register Sign: " + this.clientId + " " + Arrays.toString(nonce) + " [" + nonce.length + "] with " + Arrays.toString(REG_KEY));
                    // publish the signed nonce
                    ctx.writeAndFlush(new MqttPublishMessage(
                            new MqttFixedHeader(MqttMessageType.PUBLISH, false, MqttQoS.AT_MOST_ONCE, false, 0),
                            new MqttPublishVariableHeader("/register/complete", 0),
                            Unpooled.wrappedBuffer(Hash.sha256(nonce, REG_KEY))
                    ));
                }
                else if ("/register/ok".equalsIgnoreCase(topic))
                {
                    // decrypt the device key
                    byte[] iv = new byte[16];
                    message.content().readBytes(iv);
                    System.out.println("IV: " + Arrays.toString(iv));
                    byte[] encryptedDeviceKey = new byte[message.content().readableBytes() - 32];
                    System.out.println("Encrypted Device Key Len: " + encryptedDeviceKey.length);
                    message.content().readBytes(encryptedDeviceKey);
                    byte[] check = new byte[32];
                    message.content().readBytes(check);
                    // check the hmac
                    byte[] expectedCheck = Hash.sha256HMAC(REG_KEY, iv, encryptedDeviceKey);
                    System.out.println("HMAC: " + Arrays.equals(expectedCheck, check));
                    //
                    Cipher cipher = Cipher.getInstance("AES/CBC/NoPadding");
                    cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(REG_KEY, "AES"), new IvParameterSpec(iv));
                    this.deviceKey = cipher.doFinal(encryptedDeviceKey);
                    //
                    System.out.println("Successfully registered, device key " + Arrays.toString(this.deviceKey));
                    this.onRegistered(ctx);
                }
                else if ("/register/error".equalsIgnoreCase(topic))
                {
                    System.out.println("Failed to register");
                }
            }
            else if (topic != null && topic.startsWith("/auth"))
            {
                System.out.println("Processing auth");
                if ("/auth/response".equals(topic))
                {
                    // read the nonce
                    byte[] nonce = new byte[message.content().readableBytes()];
                    message.content().readBytes(nonce);
                    // hash the nonce and our secret key
                    System.out.println("Sign: " + this.clientId + " " + Arrays.toString(nonce) + " [" + nonce.length + "] with " + Arrays.toString(this.deviceKey));
                    // publish the signed nonce
                    ctx.writeAndFlush(new MqttPublishMessage(
                            new MqttFixedHeader(MqttMessageType.PUBLISH, false, MqttQoS.AT_MOST_ONCE, false, 0),
                            new MqttPublishVariableHeader("/auth/complete", 0),
                            Unpooled.wrappedBuffer(Hash.sha256(nonce, this.deviceKey))
                    ));
                }
                else if ("/auth/ok".equalsIgnoreCase(topic))
                {
                    System.out.println("Successfully authed");
                    this.onAuthenticated(ctx);
                }
                else if ("/auth/error".equalsIgnoreCase(topic))
                {
                    System.out.println("Failed to auth");
                }
            }
            else
            {
                System.out.println("Got message: " + message);
            }
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
        {
            cause.printStackTrace();
            ctx.close();
        }
    }
}
