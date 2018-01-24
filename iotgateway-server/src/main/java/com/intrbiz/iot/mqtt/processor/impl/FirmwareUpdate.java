package com.intrbiz.iot.mqtt.processor.impl;

import java.util.Arrays;

import org.apache.log4j.Logger;

import com.intrbiz.iot.model.message.FirmwareUpdateBeginMessage;
import com.intrbiz.iot.model.message.FirmwareUpdateDataMessage;
import com.intrbiz.iot.model.message.FirmwareUpdateErrorMessage;
import com.intrbiz.iot.model.message.FirmwareUpdateFinishMessage;
import com.intrbiz.iot.model.message.FirmwareVerifyChallengeMessage;
import com.intrbiz.iot.model.message.FirmwareVerifyResponseMessage;
import com.intrbiz.iot.model.message.HelloV2Message;
import com.intrbiz.iot.mqtt.processor.AbstractMQTTProcessor;
import com.intrbiz.iot.mqtt.processor.MQTTProcessorContext;

public class FirmwareUpdate extends AbstractMQTTProcessor
{
    private static final Logger logger = Logger.getLogger(FirmwareUpdate.class);
    
    public FirmwareUpdate()
    {
        super(null, HelloV2Message.TOPIC, FirmwareVerifyResponseMessage.TOPIC, FirmwareUpdateErrorMessage.TOPIC);
    }

    @Override
    public void process(MQTTProcessorContext context, String topic, byte[] message)
    {
        if (context.firmware() != null)
        {
            if (HelloV2Message.TOPIC.equals(topic))
            {
                HelloV2Message hello = new HelloV2Message(message);
                logger.info("Requesting firmware verification for device " + context.clientId() + " appId=" + hello.getAppId() + " version=" + Arrays.toString(hello.getFirmwareVersion()) + " id=" + Arrays.toString(hello.getFirmwareId()));
                FirmwareVerifyChallengeMessage verificationChallenge = context.firmware().startFirmwareVerification(hello);
                if (verificationChallenge != null) context.publishMessage(FirmwareVerifyChallengeMessage.TOPIC, verificationChallenge.toBytes());
            }
            else if (FirmwareVerifyResponseMessage.TOPIC.equals(topic))
            {
                // verify the device response
                if (context.firmware().completeFirmwareVerification(new FirmwareVerifyResponseMessage(message)))
                {
                    logger.info("Firmware verification successful for device " + context.clientId());
                    // do we have an upgrade target
                    if (context.firmware().needsUpdate())
                    {
                        // begin a firmware update
                        FirmwareUpdateBeginMessage begin = context.firmware().begin();
                        if (begin != null)
                        {
                            logger.info("Begining firmware update for device " + context.clientId() + " to: " + begin.getInfo() + " (" + begin.getMd5() + ")");
                            context.publishMessage(FirmwareUpdateBeginMessage.TOPIC, begin.toBytes());
                            // send the chunks
                            FirmwareUpdateDataMessage chunk;
                            while ((chunk = context.firmware().nextChunk(FirmwareUpdateDataMessage.MAX_LENGTH)) != null)
                            {
                                context.publishMessage(FirmwareUpdateDataMessage.TOPIC, chunk.toBytes());
                            }
                            // finish the update
                            FirmwareUpdateFinishMessage finish = context.firmware().finish();
                            if (finish != null) context.publishMessage(FirmwareUpdateFinishMessage.TOPIC, finish.toBytes());
                            logger.info("Finished sending firmware to device " + context.clientId());
                        }
                    }
                }
            }
            else if (FirmwareUpdateErrorMessage.TOPIC.equals(topic))
            {
                FirmwareUpdateErrorMessage error = new FirmwareUpdateErrorMessage(message);
                logger.warn("Error updating firmware for device " + context.clientId() + " error code: " + error.getErrorCode() + "/" + error.getWriteErrorCode());
            }
        }
        // forward hellos
        if (HelloV2Message.TOPIC.equals(topic))
        {
            context.queue().publish(topic, message);
        }
    }
}
