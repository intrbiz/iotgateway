package com.intrbiz.iot.engine.firmware;

import com.intrbiz.iot.model.message.FirmwareUpdateBeginMessage;
import com.intrbiz.iot.model.message.FirmwareUpdateDataMessage;
import com.intrbiz.iot.model.message.FirmwareUpdateFinishMessage;
import com.intrbiz.iot.model.message.FirmwareVerifyChallengeMessage;
import com.intrbiz.iot.model.message.FirmwareVerifyResponseMessage;
import com.intrbiz.iot.model.message.HelloV2Message;

public interface FirmwareContext
{
    FirmwareVerifyChallengeMessage startFirmwareVerification(HelloV2Message hello);
    
    boolean completeFirmwareVerification(FirmwareVerifyResponseMessage response);
    
    boolean isVerified();
    
    boolean needsUpdate();
    
    FirmwareUpdateBeginMessage begin();
    
    boolean isInProgress();
    
    FirmwareUpdateDataMessage nextChunk(int maxChunkLength);
    
    FirmwareUpdateFinishMessage finish();
    
    void complete();
    
    void close();
}
