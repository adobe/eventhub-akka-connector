# Akka Streams Consumer for Azure Event Hub
Akka streams connector for Azure Event Hub and an Akka streams Source. Can be used as a consumer.
###Documentation
To create a new EventHubSource use the default constructor `new EventHubSource()` 
and to create a usable Akka streams source use `Source.fromGraph(new EventHubSource())`. To materialize it into
a IEventProcessor it has to be materliazed and run, like an Akka streams source.

The SingleProcessorFactory works to create a single source for all the partitions in Event Hub. Creating a new
instance requires that an IEventProcessor be passed in. There is an example in the code as to how to run them both together.

There is an `application.conf` file where a specific Event Hub's keys and consumer groups can be specified.


###Contributions

Contributions are welcome. A Sink still has to be written for the whole flow to be complete.
 Please fork the project and create for your finished feature a pull request.
 
 ###Code of Conduct
 This project abides by the Adobe Code of Conduct. See [CODE OF CONDUCT](CODE_OF_CONDUCT.md).
 
 ###Licensing
 This project is licensed under the Apache V2 License. See [LICENSE](LICENSE) for more information.
 