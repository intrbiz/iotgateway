package com.intrbiz.iot.model.message;

import java.nio.ByteBuffer;

public interface DeviceMessage
{   
    void fromBytes(ByteBuffer buffer);
    
    byte[] toBytes();
}
