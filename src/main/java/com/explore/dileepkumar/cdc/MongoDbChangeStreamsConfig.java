package com.explore.dileepkumar.cdc;

import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;
import com.explore.dileepkumar.cdc.customer.Customer;
import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.changestream.ChangeStreamDocument;
import static org.bson.codecs.configuration.CodecRegistries.fromRegistries;
import static org.bson.codecs.configuration.CodecRegistries.fromProviders;

@Component
public class MongoDbChangeStreamsConfig implements ApplicationListener<ApplicationReadyEvent> {
    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        CodecRegistry pojoCodecRegistry =
                fromProviders(PojoCodecProvider.builder().automatic(true).build());
        CodecRegistry codecRegistry =
                fromRegistries(MongoClientSettings.getDefaultCodecRegistry(), pojoCodecRegistry);
        // String mongoUri = "mongodb://root:root123@localhost:27017/customer?authSource=admin";
        String mongoUri = "mongodb://localhost:27017/customer?replicaSet=rs";
        ConnectionString connectionString = new ConnectionString(mongoUri);
        MongoClientSettings clientSettings = MongoClientSettings.builder()
                .applyConnectionString(connectionString).codecRegistry(codecRegistry).build();

        try (MongoClient mongoClient = MongoClients.create(clientSettings)) {
            MongoDatabase db = mongoClient.getDatabase(connectionString.getDatabase());
            MongoCollection<Customer> grades = db.getCollection("customers", Customer.class);
            grades.watch().forEach(printEvent());
        }
    }

    private static java.util.function.Consumer<ChangeStreamDocument<Customer>> printEvent() {
        return System.out::println;
    }
}
