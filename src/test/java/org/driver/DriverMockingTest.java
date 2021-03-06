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
import org.hamcrest.collection.IsIterableContainingInOrder;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 *
 * This file is part of mongodb-testing.

 mongodb-testing is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 Foobar is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with Foobar.  If not, see <http://www.gnu.org/licenses/>.
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
        Document bob = new Document("_id", new ObjectId("579397d20c2dd41b9a8a09eb")).append("firstName", "Bob").append("lastName", "Bobberson");

        when(mockCollection.find(new Document("lastName", "Bobberson"))).thenReturn(iterable);
        when(iterable.iterator()).thenReturn(cursor);
        when(cursor.hasNext()).thenReturn(true).thenReturn(false);
        when(cursor.next()).thenReturn(bob);

        List<Document> found = wrapper.findByLastName("Bobberson");

        assertEquals(bob, found.get(0));
    }

    @Test
    public void findBobUsingInto() {

        FindIterable iterable = mock(FindIterable.class);
        MongoCursor cursor = mock(MongoCursor.class);
        Document bob = new Document("_id", new ObjectId("579397d20c2dd41b9a8a09eb")).append("firstName", "Bob").append("lastName", "Bobberson");

        when(mockCollection.find(new Document("lastName", "Bobberson"))).thenReturn(iterable);
        when(iterable.into(new ArrayList<>())).thenReturn(asList(bob));

        List<Document> found = wrapper.findByLastNameUsingInto("Bobberson");

        assertEquals(bob, found.get(0));
    }

    @Test
    public void findBobUsingIntoAndMockBuilder() {
        Document bob = new Document("_id", new ObjectId("579397d20c2dd41b9a8a09eb")).append("firstName", "Bob").append("lastName", "Bobberson");
        new MockCursorBuilder(mockCollection).withQuery(new Document("lastName", "Bobberson")).usingInto(bob);
        assertEquals(bob, wrapper.findByLastNameUsingInto("Bobberson").get(0));
    }

    @Test
    public void findMultipleUsers() {
        Document bob = new Document("_id", new ObjectId()).append("firstName", "Bob").append("lastName", "Bobberson");
        Document robert = new Document("_id", new ObjectId()).append("firstName", "Robert").append("lastName", "Bobberson");
        Document joe = new Document("_id", new ObjectId()).append("firstName", "JoeBob").append("lastName", "Bobberson");
        Document mark = new Document("_id", new ObjectId()).append("firstName", "MarkBob").append("lastName", "Bobberson");
        new MockCursorBuilder(mockCollection)
                .withQuery(new Document("lastName", "Bobberson")).usingInto(bob, robert, joe, mark);
        Assert.assertThat(wrapper.findByLastNameUsingInto("Bobberson"), IsIterableContainingInOrder.contains(bob, robert, joe, mark));
    }



    @Test
    public void findBobUsingMockCursorBuilder() {

        Document bob = new Document("firstName", "Bob").append("lastName", "Bobberson");
        new MockCursorBuilder(mockCollection)
                .withQuery(new Document("lastName", "Bobberson"))
                .withIteratorHasNext(true, false)
                .withCursorNext(bob);
        List<Document> found = wrapper.findByLastName("Bobberson");
        assertEquals(bob, found.get(0));

    }


    @Test
    public void findBobUsingRefactoredMockCursorBuilder() {

        Document bob = new Document("firstName", "Bob").append("lastName", "Bobberson");
        new MockCursorBuilder(mockCollection)
                .withQuery(new Document("lastName", "Bobberson"))
                .cursorHasNextGeneric(true, false)
                .cursorNextGeneric(bob);
        List<Document> found = wrapper.findByLastName("Bobberson");
        assertEquals(bob, found.get(0));

    }

    @Test
    public void findBobUsingMockConfig() {
        Document bob = new Document("firstName", "Bob").append("lastName", "Bobberson");
        configBasicCursorMock(asList(bob), asList(true, false), new Document("lastName", "Bobberson"));
        List<Document> found = wrapper.findByLastName("Bobberson");
        assertEquals(bob, found.get(0));
    }


    /**
     * Example that did not make it into the blog on a phase one of refactoring the test setup.
     */
    private void configBasicCursorMock(List<Document> documentsToReturn, List<Boolean> hasNextSequence, Document query) {
        FindIterable iterable = mock(FindIterable.class);
        MongoCursor cursor = mock(MongoCursor.class);
        when(mockCollection.find(query)).thenReturn(iterable);
        when(iterable.iterator()).thenReturn(cursor);

        when(cursor.hasNext()).thenAnswer(new Answer<Boolean>() {
            private int count = 0;

            @Override
            public Boolean answer(InvocationOnMock invocation) throws Throwable {
                return hasNextSequence.get(count++);
            }
        });

        when(cursor.next()).thenAnswer(new Answer<Document>() {
            private int count = 0;

            @Override
            public Document answer(InvocationOnMock invocation) throws Throwable {
                return documentsToReturn.get(count++);
            }
        });
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
