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

/**
 * This file is part of mongodb-testing.
 * <p>
 * mongodb-testing is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p>
 * Foobar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with Foobar.  If not, see <http://www.gnu.org/licenses/>.
 */

public class DriverWrapper {

    private MongoClient client;
    private MongoCollection<Document> collection;
    private Logger logger;


    public DriverWrapper(MongoClient client) {
        logger = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(DriverWrapper.class);
        this.client = client;
    }

    public void init() {
        this.collection = this.client.getDatabase("clients").getCollection("contacts");
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

    public List<Document> findByLastNameUsingInto(String lastName) {
        return collection.find(new Document("lastName", lastName)).into(new ArrayList<>());
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
