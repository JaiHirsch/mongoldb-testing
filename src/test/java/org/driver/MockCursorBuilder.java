package org.driver;

import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import org.bson.Document;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.util.ArrayList;
import java.util.concurrent.Callable;

import static java.util.Arrays.asList;
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

    public MockCursorBuilder usingInto(Document... documents) {
        when(iterable.into(new ArrayList<>())).thenReturn(asList(documents));
        return this;
    }

    public MockCursorBuilder cursorNextGeneric(Document... nextSequence) {
        return new GenericBuilder<>(this).build(cursor::next, nextSequence);
    }

    public MockCursorBuilder cursorHasNextGeneric(Boolean... hasNext) {
        return new GenericBuilder<>(this).build(cursor::hasNext, hasNext);
    }

    class GenericBuilder<K> {
        private MockCursorBuilder mockBuilder;

        public GenericBuilder(MockCursorBuilder mockBuilder) {
            this.mockBuilder = mockBuilder;
        }

        public MockCursorBuilder build(Callable<K> invoker, K[] sequence) {
            try {
                when(invoker.call()).thenAnswer(new Answer<K>() {
                    private int count = 0;
                    @Override
                    public K answer(InvocationOnMock invocation) throws Throwable {
                        return sequence[count++];
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
            return mockBuilder;
        }


    }
}
