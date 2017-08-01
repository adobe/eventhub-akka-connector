import akka.Done;
import akka.Done$;
import akka.NotUsed;
import akka.actor.*;
import akka.japi.function.Procedure;
import akka.stream.*;
import akka.stream.stage.*;
import com.microsoft.azure.eventhubs.EventData;
import com.microsoft.azure.eventprocessorhost.CloseReason;
import com.microsoft.azure.eventprocessorhost.IEventProcessor;
import com.microsoft.azure.eventprocessorhost.PartitionContext;
import scala.Tuple2;
import scala.concurrent.Promise;

import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicBoolean;

public class EventHubSource extends GraphStageWithMaterializedValue<SourceShape<Tuple2<PartitionContext, EventData>>, IEventProcessor>  {

    private class ProcessContext {
        public ProcessContext(CompletionStage<Done> completion, PartitionContext context, Iterable<EventData> events){
        }
    }

    private Outlet<Tuple2<PartitionContext, EventData>> out;
    private SourceShape<Tuple2<PartitionContext, EventData>> shape;


    private class Logic extends GraphStageLogic implements IEventProcessor{

        private final AtomicBoolean started = new AtomicBoolean();
        private final EventHubSource source = new EventHubSource();
        private Queue<EventData> pendingEvents;
        private PartitionContext currentContext;
        private int partitionCount;
        private AsyncCallback<Done> openCallback;
        private AsyncCallback<Done> closeCallback;
        private AsyncCallback<Done> processCallback;

        OutHandler handler = new AbstractOutHandler() {
            @Override
            public void onPull() throws Exception {
                if(pendingEvents == null || pendingEvents.isEmpty()){
                    return;
                }

                if(isAvailable(out)){
                    push(out, new Tuple2(currentContext, pendingEvents.poll()));
                    if(pendingEvents.isEmpty()){

                    }
                }
            }
        };

        @Override
        public void preStart(){
            openCallback = createAsyncCallback(new Procedure<Done>() {
                @Override
                public void apply(Done param) throws Exception{
                    partitionCount++;
                }
            });
            closeCallback = createAsyncCallback(new Procedure<Done>() {
                @Override
                public void apply(Done param) throws Exception {
                    if(--partitionCount == 0){
                        completeStage();
                    }
                }
            });
            processCallback = createAsyncCallback(new Procedure<Done>() {
                @Override
                public void apply(Done param) throws Exception {
                    handler.onPull();
                }
            });
        }


        @Override
        public void onClose(PartitionContext partitionContext, CloseReason closeReason) throws Exception {
            CompletionStage<Done> completion = new CompletableFuture<>();
            completion.thenAccept(closeCallback::invoke);


        }

        @Override
        public void onOpen(PartitionContext partitionContext) throws Exception {
            System.out.println("SAMPLE: Partition " + partitionContext.getPartitionId() + " is opening");
            CompletionStage<Done> completion = new CompletableFuture<>();
            completion.thenAccept(openCallback::invoke);
            pendingEvents =  new ArrayDeque<>();
        }

        @Override
        public void onEvents(PartitionContext partitionContext, Iterable<EventData> iterable) throws Exception {
            Iterator<EventData> toIterate = iterable.iterator();
            while(toIterate.hasNext()){
                EventData data = toIterate.next();
                pendingEvents.offer(data);
            }
            CompletionStage<Done> completion = new CompletableFuture<>();
            completion.thenAccept(processCallback::invoke);
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
