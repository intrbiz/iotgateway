package com.intrbiz.iot.engine;

import java.util.UUID;

import com.intrbiz.iot.engine.firmware.FirmwareContext;

public interface FirmwareEngine
{
    FirmwareContext createContext(UUID clientId);
}
