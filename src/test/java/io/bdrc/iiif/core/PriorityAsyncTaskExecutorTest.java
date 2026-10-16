package io.bdrc.iiif.core;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import io.bdrc.iiif.auth.APIKeyNormalAccessInfo;

public class PriorityAsyncTaskExecutorTest {

    private final RecordingExecutor publicExecutor = new RecordingExecutor();
    private final RecordingExecutor priorityExecutor = new RecordingExecutor();
    private final PriorityAsyncTaskExecutor executor =
            new PriorityAsyncTaskExecutor(publicExecutor, priorityExecutor);

    @AfterEach
    public void clearRequestContext() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    public void anonymousRequestUsesPublicExecutor() {
        bindRequest(null);

        executor.submit(() -> { });

        assertEquals(1, publicExecutor.submissions.get());
        assertEquals(0, priorityExecutor.submissions.get());
    }

    @Test
    public void authenticatedRequestUsesReservedExecutor() {
        bindRequest(APIKeyNormalAccessInfo.INSTANCE);

        executor.submit(() -> { });

        assertEquals(0, publicExecutor.submissions.get());
        assertEquals(1, priorityExecutor.submissions.get());
    }

    @Test
    public void overloadResponseIsServiceUnavailableAndRetryable() {
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE,
                new OverloadExceptionHandler().serviceOverloaded().getStatusCode());
        assertEquals("5",
                new OverloadExceptionHandler().serviceOverloaded().getHeaders().getFirst("Retry-After"));
    }

    private static void bindRequest(Object access) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        if (access != null) {
            request.setAttribute("access", access);
        }
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }

    private static class RecordingExecutor implements AsyncTaskExecutor {
        private final AtomicInteger submissions = new AtomicInteger();

        @Override
        public void execute(Runnable task) {
            submit(task);
        }

        @Override
        public void execute(Runnable task, long startTimeout) {
            submit(task);
        }

        @Override
        public Future<?> submit(Runnable task) {
            submissions.incrementAndGet();
            FutureTask<Void> future = new FutureTask<>(task, null);
            future.run();
            return future;
        }

        @Override
        public <T> Future<T> submit(Callable<T> task) {
            submissions.incrementAndGet();
            FutureTask<T> future = new FutureTask<>(task);
            future.run();
            return future;
        }
    }
}
