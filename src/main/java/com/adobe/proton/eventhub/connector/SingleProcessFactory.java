package com.adobe.proton.eventhub.connector;

import com.microsoft.azure.eventprocessorhost.IEventProcessor;
import com.microsoft.azure.eventprocessorhost.IEventProcessorFactory;
import com.microsoft.azure.eventprocessorhost.PartitionContext;

public class SingleProcessFactory implements IEventProcessorFactory {

    private final IEventProcessor processor;

    public SingleProcessFactory(IEventProcessor processor){
        this.processor = processor;
    }

    @Override
    public IEventProcessor createEventProcessor(PartitionContext partitionContext) throws Exception {
        return processor;
    }
}
