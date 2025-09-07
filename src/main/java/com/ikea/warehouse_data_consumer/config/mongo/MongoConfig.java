package com.ikea.warehouse_data_consumer.config.mongo;

import com.mongodb.MongoClientSettings;
import com.mongodb.ReadConcern;
import com.mongodb.ReadPreference;
import com.mongodb.WriteConcern;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.MongoTransactionManager;
import org.springframework.data.mongodb.SessionSynchronization;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.SimpleMongoClientDatabaseFactory;

@Configuration
@Profile("mongo")
public class MongoConfig {

    @Value("${spring.data.mongodb.database}")
    private String databaseName;

    @Value("${spring.data.mongodb.uri:}")
    private String mongoUri;

    @Bean
    public MongoClient mongoClient() {
        MongoClientSettings.Builder builder = MongoClientSettings.builder()
            .readConcern(ReadConcern.MAJORITY)
            .readPreference(ReadPreference.secondary())
            .writeConcern(WriteConcern.MAJORITY.withJournal(true));

        assert StringUtils.isNotBlank(mongoUri);
        return MongoClients.create(builder.applyConnectionString(new com.mongodb.ConnectionString(mongoUri)).build());
    }

    @Bean
    public MongoDatabaseFactory mongoDbFactory(MongoClient client) {
        return new SimpleMongoClientDatabaseFactory(client, databaseName);
    }

    @Bean
    public MongoTemplate mongoTemplate(MongoDatabaseFactory factory) {
        MongoTemplate template = new MongoTemplate(factory);
        // Bind sessions to the thread for causal consistency outside transactions.
        template.setSessionSynchronization(SessionSynchronization.ALWAYS);
        return template;
    }

    // Enable transactions (requires replica set)
    @Bean
    public MongoTransactionManager transactionManager(MongoDatabaseFactory factory) {
        return new MongoTransactionManager(factory);
    }
}
