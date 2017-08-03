import akka.Done;
import akka.actor.ActorSystem;
import akka.japi.function.Procedure;
import akka.stream.ActorMaterializer;
import akka.stream.Materializer;
import akka.stream.SourceShape;
import akka.stream.javadsl.Keep;
import akka.stream.javadsl.Sink;
import akka.stream.javadsl.Source;
import com.microsoft.azure.eventprocessorhost.EventProcessorHost;
import com.microsoft.azure.eventprocessorhost.EventProcessorOptions;
import com.microsoft.azure.eventprocessorhost.IEventProcessor;
import com.microsoft.azure.eventprocessorhost.IEventProcessorFactory;
import com.microsoft.azure.servicebus.ConnectionStringBuilder;
import scala.concurrent.duration.FiniteDuration;

public class SingleProcessorExample {





    public static void main(String[] args){
        final String consumerGroupName = "akka-consumer";
        final String namespaceName = "proton-druid-test";
        final String eventHubName = "eventhub-test-1";
        final String sasKeyName = "TestPolicy";
        final String sasKey = "sFNzyenwCuMF/68z2nl2tL2myjxnpWG+YbOFlz3zrxc=";


        final String storageAccountName = "protonstorage";
        final String storageAccountKey = "hW0zhEVr4NX/D2ihNc5QZ7Rn6mZiH6+d1ddBG7tzLq0o8LT/M58xoStPyBuxSDL2bP40fa4tHzqV/70dok82ow==";
        final String storageConnectionString = "DefaultEndpointsProtocol=https;AccountName=" + storageAccountName
                + ";AccountKey=" + storageAccountKey + ";EndpointSuffix=core.windows.net";


        ConnectionStringBuilder eventHubConnectionString = new ConnectionStringBuilder(namespaceName, eventHubName, sasKeyName, sasKey);
        ActorSystem system = ActorSystem.create("EventHubSystem");
        Materializer materializer = ActorMaterializer.create(system);
        IEventProcessor processor = Source.fromGraph(new EventHubSource()).toMat(Sink.foreach(t -> System.out.println(t)), Keep.left()).run(materializer);
        EventProcessorOptions options = new EventProcessorOptions();
        final EventProcessorHost host = new EventProcessorHost(namespaceName, eventHubName, consumerGroupName,
                eventHubConnectionString.toString(), storageConnectionString, eventHubName);

        try {
            host.registerEventProcessorFactory(new SingleProcessFactory(processor), options);

        } catch (Exception e){

        }





    }
}
