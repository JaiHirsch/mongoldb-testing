package org.driver;


import ch.qos.logback.classic.Logger;
import com.mongodb.Block;
import com.mongodb.MongoClient;
import com.mongodb.MongoWriteConcernException;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.model.BulkWriteOptions;
import com.mongodb.client.model.InsertOneOptions;
import com.mongodb.client.model.WriteModel;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class DriverWrapper {

    private MongoClient client;
    private MongoCollection<Document> collection;
    private Logger logger;

    public DriverWrapper() {

    }

    public DriverWrapper(MongoClient client) {
        logger = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(DriverWrapper.class);
        this.client = client;
    }

    public void init() {
        collection = this.client.getDatabase("clients").getCollection("contacts");
    }

    public List<Document> findByLastName(String lastName) {
        List<Document> people = new ArrayList<>();
        FindIterable<Document> documents = collection.find(new Document("lastName", lastName));
        MongoCursor<Document> iterator = documents.iterator();
        while (iterator.hasNext()) {
            Document document = iterator.next();
            people.add(document);

        }
        return people;
    }


    public void bulkWrite(final List<? extends WriteModel<? extends Document>> requests, final BulkWriteOptions options) {
        try {
            collection.bulkWrite(requests, options);
        } catch (UnsupportedOperationException e) {
            handleMongoErrors(e, "bulkWrite failed with options: " + options.toString() + " array: " + requests);
        }
    }


    public void insertOne(Document document, InsertOneOptions insertOneOptions) {
        try {
            collection.insertOne(document, insertOneOptions);
        } catch (MongoWriteConcernException e) {
            handleMongoErrors(e, "InsertOne failed with options: " + insertOneOptions.toString() + " array: " + document);
        }
    }

    public List<ObjectId> findErrors() {
        List<ObjectId> ids = new ArrayList<>();

        collection.find(new Document("errors", true)).forEach((Block<Document>) document -> {
            ids.add(document.getObjectId("_id"));
        });
        return ids;
    }

    protected void handleMongoErrors(Throwable e, String errorMessage) {

        logger.error(errorMessage, e);

    }

}
