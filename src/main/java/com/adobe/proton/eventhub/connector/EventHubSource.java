/*
 *  Copyright 2017 Adobe.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *          http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.adobe.proton.eventhub.connector;

import akka.Done;
import akka.actor.*;
import akka.japi.function.Procedure;
import akka.stream.*;
import akka.stream.stage.*;
import com.microsoft.azure.eventhubs.EventData;
import com.microsoft.azure.eventprocessorhost.CloseReason;
import com.microsoft.azure.eventprocessorhost.IEventProcessor;
import com.microsoft.azure.eventprocessorhost.PartitionContext;
import scala.Tuple2;
import scala.concurrent.ExecutionContext;
import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.Queue;

public class EventHubSource extends GraphStageWithMaterializedValue<SourceShape<Tuple2<PartitionContext, EventData>>, IEventProcessor>  {

    private Outlet<Tuple2<PartitionContext, EventData>> out;
    private SourceShape<Tuple2<PartitionContext, EventData>> shape;

    private class Logic extends GraphStageLogic implements IEventProcessor{

        private final EventHubSource source = new EventHubSource();
        private Queue<EventData> pendingEvents;
        private PartitionContext currentContext;
        private int partitionCount;
        private int checkpointBatchingCount = 0;

        private AsyncCallback<Done> openCallback = createAsyncCallback(new Procedure<Done>() {
            @Override
            public void apply(Done param) throws Exception{
                partitionCount++;
            }
        });

        private AsyncCallback<Done> closeCallback = createAsyncCallback(new Procedure<Done>() {
            @Override
            public void apply(Done param) throws Exception {
                if(--partitionCount == 0){
                    completeStage();
                }
            }
        });

        private AsyncCallback<Done>  processCallback = createAsyncCallback(new Procedure<Done>() {
            @Override
            public void apply(Done param) throws Exception {
                handler.onPull();
            }
        });

        private ExecutionContext ec;
        private ActorSystem system;
        OutHandler handler = new AbstractOutHandler() {
            @Override
            public void onPull() throws Exception {
                if(pendingEvents == null || pendingEvents.isEmpty()){
                    return;
                }

                if(isAvailable(out)){
                    if(pendingEvents.isEmpty() || pendingEvents == null){
                        return;
                    }
                    EventData current = pendingEvents.poll();
                    push(out, new Tuple2(currentContext, current));
                }
            }
        };

        @Override
        public void preStart(){
            system = ActorSystem.create("dispatcher");
            ec = system.dispatcher();
        }

        @Override
        public void onClose(PartitionContext partitionContext, CloseReason closeReason) throws Exception{
            partitionContext.checkpoint();
            closeCallback.invoke(Done.getInstance());
        }

        @Override
        public void onOpen(PartitionContext partitionContext) throws Exception {
           // System.out.println("Partition " + partitionContext.getPartitionId() + " is opening");
            openCallback.invoke(Done.getInstance());
            pendingEvents =  new ArrayDeque<>();
        }

        @Override
        public void onEvents(PartitionContext partitionContext, Iterable<EventData> iterable) throws Exception {
            Iterator<EventData> toIterate = iterable.iterator();
            currentContext = partitionContext;
            while(toIterate.hasNext()){
                EventData data = toIterate.next();
                checkpointBatchingCount++;
                pendingEvents.offer(data);
                if(checkpointBatchingCount % 100 == 0){
                    partitionContext.checkpoint(data);
                    checkpointBatchingCount = 0;
                }
            }
            processCallback.invoke(Done.getInstance());
        }

        @Override
        public void onError(PartitionContext partitionContext, Throwable throwable) {
           System.out.println("Error in the connector: " + throwable.getMessage());

        }

        public Logic(EventHubSource source) {
            super(source.shape());
            source = this.source;
            setHandler(out, handler);
        }
    }

    @Override
    public SourceShape<Tuple2<PartitionContext, EventData>> shape(){
        return shape;
    }

    public EventHubSource(){
        out = Outlet.create("EventHubSource.out");
        shape = SourceShape.of(out);
    }
    @Override
    public Tuple2<GraphStageLogic, IEventProcessor> createLogicAndMaterializedValue(Attributes attributes){
        GraphStageLogic logic = new Logic(this);
        return new Tuple2(logic, logic);

    }

}
