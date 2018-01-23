package com.intrbiz.iot.engine;

import java.util.UUID;

import com.intrbiz.iot.engine.queue.QueueContext;

public interface QueueEngine
{
    QueueContext createContext(UUID clientId);
}
