package com.workshare.msnos.core;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import com.workshare.msnos.core.Message.Status;

@SuppressWarnings("unchecked")
public class MultipleFutureStatusTest {

    private MultipleFutureStatus multi;
    private Future<Status>[] futures = new Future[3];

    @Before
    public void before() throws Exception {
        multi = new MultipleFutureStatus();
        
        for (int i = 0; i < futures.length; i++) {
            futures[i] = createMockFutureStatus(Status.UNKNOWN);
            multi.add(futures[i]);
        }
    }

    @Test
    public void shouldReturnUnknownAndNotDoneWhenBothUnknown() throws Exception {
        assertEquals(Status.UNKNOWN, multi.get());
        assertEquals(false, multi.isDone());
    }

    @Test
    public void shouldReturnPendingNotDoneWhenOnePending() throws Exception {
        mockStatus(futures[1], Status.PENDING);
        assertEquals(Status.PENDING, multi.get());
        assertEquals(false, multi.isDone());
    }

    @Test
    public void shouldReturnDeliverdAndDoneWhenOneDelivered() throws Exception {
        mockStatus(futures[1], Status.DELIVERED);
        assertEquals(Status.DELIVERED, multi.get());
        assertEquals(true, multi.isDone());
    }

    private Future<Status> createMockFutureStatus(final Status status) throws Exception {
        Future<Status> value = mock(Future.class);
        mockStatus(value, status);
        return value;
    }

    private void mockStatus(Future<Status> value, final Status status) throws Exception {
        when(value.get()).thenReturn(status);
        when(value.get(anyLong(), any(TimeUnit.class))).thenReturn(status);
    }

}
