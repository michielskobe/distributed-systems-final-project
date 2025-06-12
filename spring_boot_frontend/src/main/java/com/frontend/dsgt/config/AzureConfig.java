package com.frontend.dsgt.config;

import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.storage.queue.QueueClient;
import com.azure.storage.queue.QueueClientBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AzureConfig {

    @Value("${azure.storage.queue.endpoint}")
    private String queueEndpoint;

    @Value("${azure.storage.queue.name}")
    private String queueName;

    @Bean
    public QueueClient queueClient() {
        String connectStr = System.getenv("AZURE_STORAGE_CONNECTION_STRING");
        return new QueueClientBuilder()
                .connectionString(connectStr)
                .queueName(queueName)
                .buildClient();
    }
}