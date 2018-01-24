package com.intrbiz.iot.mqtt.processor;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

public class MQTTProcessingChain
{
    private static final Logger logger = Logger.getLogger(MQTTProcessingChain.class);
    
    private MQTTFilter[] filters = new MQTTFilter[0];
    
    private PrefixBucket[] prefixes = new PrefixBucket[0];
    
    private MQTTProcessor defaultProcessor;
    
    public MQTTProcessingChain()
    {
        super();
    }
    
    public synchronized MQTTProcessingChain addFilter(MQTTFilter filter)
    {
        MQTTFilter[] newFilters = new MQTTFilter[this.filters.length + 1];
        System.arraycopy(this.filters, 0, newFilters, 0, this.filters.length);
        newFilters[this.filters.length] = filter;
        Arrays.sort(newFilters, (a, b) -> Integer.compare(a.getOrder(), b.getOrder()));
        this.filters = newFilters;
        return this;
    }
    
    public synchronized MQTTProcessingChain addProcessor(MQTTProcessor processor)
    {
        PrefixBucket bucket = this.getOrCreatePrefix(processor.getPrefix());
        for (String topic : processor.getTopics())
        {
            bucket.routes.put(topic, processor);
        }
        return this;
    }
    
    public MQTTProcessingChain setDefaultProcessor(MQTTProcessor processor)
    {
        this.defaultProcessor = processor;
        return this;
    }
    
    public void process(MQTTProcessorContext context, String topic, byte[] message)
    {
        // filter the message
        for (MQTTFilter filter : this.filters)
        {
            filter.filter(context, topic, message);
        }
        // route the message
        MQTTProcessor processor = null;
        if (topic != null)
        {
            for (PrefixBucket prefix : this.prefixes)
            {
                if (topic.startsWith(prefix.prefix))
                {
                    processor = prefix.routes.get(topic);
                    break;
                }
            }
        }
        // fall back to default processor
        if (processor == null)
        {
            processor = this.defaultProcessor;
        }
        // process the message
        if (processor != null)
        {
            processor.process(context, topic, message);
        }
        else
        {
            logger.warn("Unable to find processor for topic: " + topic);
        }
    }
    
    private synchronized PrefixBucket getOrCreatePrefix(String prefix)
    {
        if (prefix == null) prefix = "";
        for (PrefixBucket bucket : this.prefixes)
        {
            if (prefix.equals(bucket.prefix))
                return bucket;
        }
        PrefixBucket bucket = new PrefixBucket(prefix);
        PrefixBucket[] newPrefixes = new PrefixBucket[this.prefixes.length + 1];
        System.arraycopy(this.prefixes, 0, newPrefixes, 0, this.prefixes.length);
        newPrefixes[this.prefixes.length] = bucket;
        Arrays.sort(newPrefixes, (a, b) -> Integer.compare(b.prefix.length(), a.prefix.length()));
        this.prefixes = newPrefixes;
        return bucket;
    }
    
    private static class PrefixBucket
    {
        private final String prefix;
        
        private final Map<String, MQTTProcessor> routes = new HashMap<String, MQTTProcessor>();
        
        public PrefixBucket(String prefix)
        {
            this.prefix = prefix;
        }
    }
}
