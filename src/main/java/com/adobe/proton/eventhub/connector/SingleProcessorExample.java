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

import akka.actor.ActorSystem;
import akka.stream.ActorMaterializer;
import akka.stream.Materializer;
import akka.stream.javadsl.Keep;
import akka.stream.javadsl.Sink;
import akka.stream.javadsl.Source;
import com.microsoft.azure.eventprocessorhost.EventProcessorHost;
import com.microsoft.azure.eventprocessorhost.EventProcessorOptions;
import com.microsoft.azure.eventprocessorhost.IEventProcessor;
import com.microsoft.azure.servicebus.ConnectionStringBuilder;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import java.util.Date;

public class SingleProcessorExample {

    public static void main(String[] args){

        Config config = ConfigFactory.load();
        String consumerGroupName = config.getString("azure.eventhub.consumerGroupName");
        String namespaceName = config.getString("azure.eventhub.namespaceName");
        String eventHubName = config.getString("azure.eventhub.eventHubName");
        String sasKeyName = config.getString("azure.eventhub.sasKeyName");
        String sasKey = config.getString("azure.eventhub.sasKey");
        String storageAccountName = config.getString("azure.eventhub.storageAccountName");
        String storageAccountKey = config.getString("azure.eventhub.storageAccountKey");
        final String storageConnectionString = "DefaultEndpointsProtocol=https;AccountName=" + storageAccountName
                + ";AccountKey=" + storageAccountKey + ";EndpointSuffix=core.windows.net";
        ConnectionStringBuilder eventHubConnectionString = new ConnectionStringBuilder(namespaceName, eventHubName, sasKeyName, sasKey);
        ActorSystem system = ActorSystem.create("EventHubSystem");
        Materializer materializer = ActorMaterializer.create(system);
        IEventProcessor processor = Source.fromGraph(new EventHubSource()).toMat(Sink.foreach(t -> System.out.println()), Keep.left()).run(materializer);
        EventProcessorOptions options = new EventProcessorOptions();
        options.setInitialOffsetProvider(startAfterTime -> new Date().getTime() - 5000000);
        final EventProcessorHost host = new EventProcessorHost(namespaceName, eventHubName, consumerGroupName,
                eventHubConnectionString.toString(), storageConnectionString, eventHubName);

        try {
            host.registerEventProcessorFactory(new SingleProcessFactory(processor), options);

        } catch (Exception e){

        }
    }
}
