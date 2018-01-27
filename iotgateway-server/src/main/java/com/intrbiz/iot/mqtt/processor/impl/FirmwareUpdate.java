package com.intrbiz.iot.mqtt.processor.impl;

import java.util.Arrays;

import org.apache.log4j.Logger;

import com.intrbiz.iot.model.message.FirmwareUpdateAckMessage;
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
        super(null, HelloV2Message.TOPIC, FirmwareVerifyResponseMessage.TOPIC, FirmwareUpdateErrorMessage.TOPIC, FirmwareUpdateAckMessage.TOPIC);
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
                        }
                    }
                    else
                    {
                        logger.info("No new firmware for device " + context.clientId());
                    }
                }
            }
            else if (FirmwareUpdateAckMessage.TOPIC.equals(topic))
            {
                FirmwareUpdateAckMessage ack = new FirmwareUpdateAckMessage(message);
                logger.debug("Firmware update ack from " + context.clientId() + " " + ack.getAck());
                if (context.firmware().isInProgress() && (ack.isAckBegin() || ack.isAckChunk()))
                {
                    // send the next chunk
                    FirmwareUpdateDataMessage chunk = context.firmware().nextChunk(FirmwareUpdateDataMessage.MAX_LENGTH);
                    if (chunk != null)
                    {
                        logger.debug("Sending firmware chunk to device " + context.clientId());
                        context.publishMessage(FirmwareUpdateDataMessage.TOPIC, chunk.toBytes());
                    }
                    else
                    {
                        // finish the update
                        FirmwareUpdateFinishMessage finish = context.firmware().finish();
                        if (finish != null)
                        {
                            logger.info("Sending finish command to device " + context.clientId());
                            context.publishMessage(FirmwareUpdateFinishMessage.TOPIC, finish.toBytes());
                        }
                    }
                }
                else if (context.firmware().isInProgress() && (ack.isAckFinish()))
                {
                    context.firmware().complete();
                    logger.info("Completed firmware update of device " + context.clientId());
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
