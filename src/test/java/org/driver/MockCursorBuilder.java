package org.driver;

import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import org.bson.Document;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Created by jaihirsch on 7/20/16.
 */
public class MockCursorBuilder {

    private FindIterable iterable = mock(FindIterable.class);
    private MongoCursor cursor = mock(MongoCursor.class);
    private MongoCollection mockCollection;

    public MockCursorBuilder(MongoCollection mockCollection) {

        this.mockCollection = mockCollection;
        when(iterable.iterator()).thenReturn(cursor);
    }

    public MockCursorBuilder withQuery(Document query) {
        when(mockCollection.find(query)).thenReturn(iterable);
        return this;
    }

    public MockCursorBuilder withIteratorHasNext(Boolean... hasNextSequence) {
        when(cursor.hasNext()).thenAnswer(new Answer<Boolean>() {
            private int count = 0;

            @Override
            public Boolean answer(InvocationOnMock invocation) throws Throwable {
                return hasNextSequence[count++];
            }
        });
        return this;
    }

    public MockCursorBuilder withCursorNext(Document... nextSequence) {
        when(cursor.next()).thenAnswer(new Answer<Document>() {
            private int count = 0;

            @Override
            public Document answer(InvocationOnMock invocation) throws Throwable {
                return nextSequence[count++];
            }
        });
        return this;
    }

}
