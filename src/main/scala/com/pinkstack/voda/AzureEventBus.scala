package com.pinkstack.voda

import java.util.function.Consumer

import com.azure.messaging.eventhubs._
import com.azure.messaging.eventhubs.checkpointstore.blob.BlobCheckpointStore
import com.azure.messaging.eventhubs.models.{ErrorContext, EventContext, EventPosition}
import com.azure.storage.blob.BlobContainerClientBuilder

object AzureEventBus {

  import scala.jdk.CollectionConverters._

  def currentMeasurementsProducer(implicit config: Configuration.Config): EventHubProducerClient =
    new EventHubClientBuilder()
      .connectionString(config.collecting.currentMeasurements.connectionString)
      .buildProducerClient()

  def historicalMeasurementsProducer(implicit config: Configuration.Config): EventHubProducerClient =
    new EventHubClientBuilder()
      .connectionString(config.collecting.historicalMeasurements.connectionString)
      .buildProducerClient()

  // Experimental part for consumption of Events
  def historicalMeasurementsConsumer(eventProcessor: Consumer[EventContext])
                                    (errorProcessor: Consumer[ErrorContext])
                                    (implicit config: Configuration.Config): EventProcessorClient = {
    val (blobConnectionString, blobContainerName) = (
      "DefaultEndpointsProtocol=..... (deleted)",
      "env-1e827db3-5a1c-4e6e-a725-0c6c5eecb82f"
    )

    val blobContainerAsyncClient = new BlobContainerClientBuilder()
      .connectionString(blobConnectionString)
      .containerName(blobContainerName)
      .buildAsyncClient

    new EventProcessorClientBuilder()
      .connectionString(config.collecting.currentMeasurements.connectionString)
      .consumerGroup(EventHubClientBuilder.DEFAULT_CONSUMER_GROUP_NAME)
      .initialPartitionEventPosition(Map("x" -> EventPosition.earliest()).asJava)
      .processEvent(eventProcessor)
      .processError(errorProcessor)
      .checkpointStore(new BlobCheckpointStore(blobContainerAsyncClient))
      .buildEventProcessorClient()
  }
}
