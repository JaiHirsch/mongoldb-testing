package org.driver;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.core.Appender;
import com.mongodb.MongoClient;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.BulkWriteOptions;
import com.mongodb.client.model.InsertOneModel;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.mockito.runners.MockitoJUnitRunner;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Created by jaihirsch on 7/3/16.
 */

@RunWith(MockitoJUnitRunner.class)
public class DriverMockingTest {


    private static Logger logger = (Logger) LoggerFactory.getLogger(MongoCollection.class);

    @Mock
    private MongoClient mockClient;
    @Mock
    private MongoCollection mockCollection;
    @Mock
    private MongoDatabase mockDB;
    @Mock
    private Appender mockAppender;


    @InjectMocks
    private DriverWrapper wrapper;

    @Before
    public void initMocks() {
        when(mockClient.getDatabase(anyString())).thenReturn(mockDB);
        when(mockDB.getCollection(anyString())).thenReturn(mockCollection);
        wrapper.init();
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testErrorLoggingUnsupportedOperationExceptionForBulkWrite() {

        when(mockCollection.bulkWrite(any(List.class), any(BulkWriteOptions.class))).thenThrow(new UnsupportedOperationException("WriteModel of type MOCKMODEL is not supported"));

        List<InsertOneModel<Document>> insertOneModels = Arrays.asList(new InsertOneModel<>(new Document("_id", 4)),
                new InsertOneModel<>(new Document("_id", 5)));
        BulkWriteOptions opts = new BulkWriteOptions().ordered(false);

        initMockLogger();
        wrapper.bulkWrite(insertOneModels, opts);
        Mockito.verify(mockCollection).bulkWrite(insertOneModels, opts);
        verifyLogging("bulkWrite failed with options: ");
    }

    @Test
    public void findBob() {

        FindIterable iterable = mock(FindIterable.class);
        MongoCursor cursor = mock(MongoCursor.class);
        Document bob = new Document("firstName", "Bob").append("lastName", "Bobberson");

        when(mockCollection.find(new Document("lastName", "Bobberson"))).thenReturn(iterable);

        when(iterable.iterator()).thenReturn(cursor);

        when(cursor.hasNext()).thenReturn(true).thenReturn(false);

        when(cursor.next()).thenReturn(bob);

        List<Document> found = wrapper.findByLastName("Bobberson");

        assertEquals(bob, found.get(0));
    }

    @Test
    public void loggerIsCalledOnFindErrors() {
        FindIterable iterable = mock(FindIterable.class);
        MongoCursor cursor = mock(MongoCursor.class);
        ObjectId id = new ObjectId();
        Document bob = new Document("_id", id).append("errors", true);

        when(mockCollection.find(new Document("errors", true))).thenReturn(iterable);

        when(iterable.iterator()).thenReturn(cursor);

        when(cursor.hasNext()).thenReturn(true).thenReturn(false);

        when(cursor.next()).thenReturn(bob);

        for (ObjectId objectId : wrapper.findErrors()) {
            assertEquals(id, objectId);
        }


    }

    private void initMockLogger() {
        Logger root = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        mockAppender = mock(Appender.class);
        when(mockAppender.getName()).thenReturn("MOCK");
        root.addAppender(mockAppender);
    }

    private void verifyLogging(String logMessage) {
        Mockito.verify(mockAppender).doAppend(Mockito.argThat(new ArgumentMatcher() {
            @Override
            public boolean matches(final Object argument) {
                return ((LoggingEvent) argument).getFormattedMessage().contains(logMessage);
            }
        }));
    }
}
