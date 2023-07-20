package com.explore.dileepkumar.cdc;

import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;
import org.bson.conversions.Bson;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;
import com.explore.dileepkumar.cdc.customer.Customer;
import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.EstimatedDocumentCountOptions;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.changestream.ChangeStreamDocument;
import static org.bson.codecs.configuration.CodecRegistries.fromRegistries;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.bson.BsonDocument;
import org.bson.BsonString;
import org.bson.BsonValue;
import static org.bson.codecs.configuration.CodecRegistries.fromProviders;

@Component
public class MongoDbChangeStreamsConfig implements ApplicationListener<ApplicationReadyEvent> {

    @Value("${mongodb.uri}")
    private String mongoUri;

    @Value("${mongodb.cdc.resumeToken}")
    private String resumeToken;

    @Value("#{'${mongodb.cdc.operationTypes}'.split(',')}") 
    private List<String> operationTypes;

    // @Override
    public void onApplicationEvent1xx(ApplicationReadyEvent event) {
        CodecRegistry pojoCodecRegistry =
                fromProviders(PojoCodecProvider.builder().automatic(true).build());
        CodecRegistry codecRegistry =
                fromRegistries(MongoClientSettings.getDefaultCodecRegistry(), pojoCodecRegistry);
        ConnectionString connectionString = new ConnectionString(mongoUri);
        MongoClientSettings clientSettings = MongoClientSettings.builder()
                .applyConnectionString(connectionString).codecRegistry(codecRegistry).build();

        try (MongoClient mongoClient = MongoClients.create(clientSettings)) {
            MongoDatabase db = mongoClient.getDatabase(connectionString.getDatabase());
            MongoCollection<Customer> grades = db.getCollection("customers", Customer.class);
            EstimatedDocumentCountOptions options = new EstimatedDocumentCountOptions();
            System.out.println(String.format("Estimated docs changed :: %d",
                    grades.estimatedDocumentCount(options)));

            grades.watch().forEach(printEvent());
        }
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        CodecRegistry pojoCodecRegistry =
                fromProviders(PojoCodecProvider.builder().automatic(true).build());
        CodecRegistry codecRegistry =
                fromRegistries(MongoClientSettings.getDefaultCodecRegistry(), pojoCodecRegistry);
        ConnectionString connectionString = new ConnectionString(mongoUri);
        MongoClientSettings clientSettings = MongoClientSettings.builder()
                .applyConnectionString(connectionString).codecRegistry(codecRegistry).build();
        try (MongoClient mongoClient = MongoClients.create(clientSettings)) {
            MongoDatabase db = mongoClient.getDatabase(connectionString.getDatabase());
            MongoCollection<Customer> customers = db.getCollection("customers", Customer.class);
            BsonValue value = new BsonString(resumeToken);
            BsonDocument bsonDocument = new BsonDocument("_data", value);
            List<Bson> pipeline = Collections.singletonList(Aggregates
                    .match(Filters.in("operationType", operationTypes)));
             System.out.println(bsonDocument); 
            customers.watch(pipeline).resumeAfter(bsonDocument).forEach(printEvent());
            // customers.watch().forEach(printEvent());
        }
    }

    private static java.util.function.Consumer<ChangeStreamDocument<Customer>> printEvent() {
        return System.out::println;
    }
}