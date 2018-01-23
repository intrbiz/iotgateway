package com.intrbiz.connectedkanban.hubserver.model.message.device;

import java.nio.ByteBuffer;

public interface DeviceMessage
{   
    void fromBytes(ByteBuffer buffer);
    
    byte[] toBytes();
}
