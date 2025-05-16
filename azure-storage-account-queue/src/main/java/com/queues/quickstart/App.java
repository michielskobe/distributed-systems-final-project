package com.queues.quickstart;

/**
 * Azure Queue Storage client library quickstart
 */
import com.azure.identity.*;
import com.azure.storage.queue.*;
import com.azure.storage.queue.models.*;
import java.io.*;
import java.time.Duration;

public class App {
    public static void main(String[] args) throws IOException
    {
        System.out.println("Azure Queue Storage client library - Java quickstart sample\n");

        // Create a unique name for the queue
        String queueName = "dappqueue-" + java.util.UUID.randomUUID();

        // Instantiate a QueueClient
        // We'll use this client object to create and interact with the queue
        QueueClient queueClient = new QueueClientBuilder()
                .endpoint("https://storageaccountdapp.queue.core.windows.net/")
                .queueName(queueName)
                .credential(new DefaultAzureCredentialBuilder().build())
                .buildClient();

        System.out.println("Creating queue: " + queueName);

        // Create the queue
        queueClient.create();

        System.out.println("\nAdding messages to the queue...");

        // Send several messages to the queue
        queueClient.sendMessage("First message");
        queueClient.sendMessage("Second message");

        // Save the result so we can update this message later
        SendMessageResult result = queueClient.sendMessage("Third message");

        System.out.println("\nPeek at the messages in the queue...");

        // Peek at messages in the queue
        queueClient.peekMessages(10, null, null).forEach(
                peekedMessage -> System.out.println("Message: " + peekedMessage.getMessageText()));

        System.out.println("\nUpdating the third message in the queue...");

        // Update a message using the result that
        // was saved when sending the message
        queueClient.updateMessage(result.getMessageId(),
                result.getPopReceipt(),
                "Third message has been updated",
                Duration.ofSeconds(1));

        QueueProperties properties = queueClient.getProperties();
        long messageCount = properties.getApproximateMessagesCount();

        System.out.printf("Queue length: %d%n", messageCount);

        System.out.println("\nPress Enter key to receive messages and delete them from the queue...");
        System.console().readLine();

        // Get messages from the queue
        queueClient.receiveMessages(10).forEach(
                // "Process" the message
                receivedMessage -> {
                    System.out.println("Message: " + receivedMessage.getMessageText());

                    // Let the service know we're finished with
                    // the message and it can be safely deleted.
                    queueClient.deleteMessage(receivedMessage.getMessageId(), receivedMessage.getPopReceipt());
                }
        );

        System.out.println("\nPress Enter key to delete the queue...");
        System.console().readLine();

        // Clean up
        System.out.println("Deleting queue: " + queueClient.getQueueName());
        queueClient.delete();

        System.out.println("Done");
    }
}
