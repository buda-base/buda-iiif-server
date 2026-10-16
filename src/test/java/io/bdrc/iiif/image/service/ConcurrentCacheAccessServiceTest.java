package io.bdrc.iiif.image.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import io.bdrc.iiif.exceptions.IIIFException;

public class ConcurrentCacheAccessServiceTest {

    @Test
    public void uncheckedFailureCompletesFutureAndRemovesEntry() throws Exception {
        ConcurrentCacheAccessService service = new ConcurrentCacheAccessService() {
            @Override
            void putInCache(String resId) {
                throw new IllegalStateException("cache write failed");
            }
        };

        CompletableFuture<Integer> result = service.ensureCacheReady("resource");
        ExecutionException exception = assertThrows(ExecutionException.class, result::get);

        assertTrue(exception.getCause() instanceof IllegalStateException);
        assertTrue(service.futuresCache.isEmpty());
    }

    @Test
    public void concurrentRequestsShareOneFetch() throws Exception {
        CountDownLatch fetchStarted = new CountDownLatch(1);
        CountDownLatch releaseFetch = new CountDownLatch(1);
        AtomicInteger fetchCount = new AtomicInteger();
        ConcurrentCacheAccessService service = new ConcurrentCacheAccessService() {
            @Override
            void putInCache(String resId) throws IIIFException {
                fetchCount.incrementAndGet();
                fetchStarted.countDown();
                try {
                    releaseFetch.await();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IIIFException(e);
                }
            }
        };
        ExecutorService executor = Executors.newSingleThreadExecutor();

        try {
            CompletableFuture<CompletableFuture<Integer>> firstCall =
                    CompletableFuture.supplyAsync(() -> service.ensureCacheReady("resource"), executor);
            assertTrue(fetchStarted.await(1, TimeUnit.SECONDS));

            CompletableFuture<Integer> secondResult = service.ensureCacheReady("resource");
            releaseFetch.countDown();

            assertEquals(Integer.valueOf(1), firstCall.get(1, TimeUnit.SECONDS).get(1, TimeUnit.SECONDS));
            assertEquals(Integer.valueOf(1), secondResult.get(1, TimeUnit.SECONDS));
            assertEquals(1, fetchCount.get());
            assertTrue(service.futuresCache.isEmpty());
        } finally {
            releaseFetch.countDown();
            executor.shutdownNow();
        }
    }

    @Test
    public void timedOutWaiterRemovesStaleFuture() throws Exception {
        CountDownLatch fetchStarted = new CountDownLatch(1);
        CountDownLatch releaseFetch = new CountDownLatch(1);
        ConcurrentCacheAccessService service = new ConcurrentCacheAccessService(50, TimeUnit.MILLISECONDS) {
            @Override
            void putInCache(String resId) throws IIIFException {
                fetchStarted.countDown();
                try {
                    releaseFetch.await();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IIIFException(e);
                }
            }
        };
        ExecutorService executor = Executors.newSingleThreadExecutor();

        try {
            CompletableFuture<CompletableFuture<Integer>> firstCall =
                    CompletableFuture.supplyAsync(() -> service.ensureCacheReady("resource"), executor);
            assertTrue(fetchStarted.await(1, TimeUnit.SECONDS));

            CompletableFuture<Integer> waiter = service.ensureCacheReady("resource");
            assertThrows(ExecutionException.class, waiter::get);
            assertTrue(service.futuresCache.isEmpty());

            releaseFetch.countDown();
            firstCall.get(1, TimeUnit.SECONDS);
        } finally {
            releaseFetch.countDown();
            executor.shutdownNow();
        }
    }
}
