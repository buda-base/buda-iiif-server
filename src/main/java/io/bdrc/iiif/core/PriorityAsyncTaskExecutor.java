package io.bdrc.iiif.core;

import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import io.bdrc.auth.AccessInfo;

/**
 * Keeps async streaming capacity reserved for authenticated users and API keys.
 */
final class PriorityAsyncTaskExecutor implements AsyncTaskExecutor {

    private final AsyncTaskExecutor publicExecutor;
    private final AsyncTaskExecutor priorityExecutor;

    PriorityAsyncTaskExecutor(AsyncTaskExecutor publicExecutor, AsyncTaskExecutor priorityExecutor) {
        this.publicExecutor = publicExecutor;
        this.priorityExecutor = priorityExecutor;
    }

    @Override
    public void execute(Runnable task) {
        executorForCurrentRequest().execute(task);
    }

    @Override
    public void execute(Runnable task, long startTimeout) {
        executorForCurrentRequest().execute(task, startTimeout);
    }

    @Override
    public Future<?> submit(Runnable task) {
        return executorForCurrentRequest().submit(task);
    }

    @Override
    public <T> Future<T> submit(Callable<T> task) {
        return executorForCurrentRequest().submit(task);
    }

    private AsyncTaskExecutor executorForCurrentRequest() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (attributes instanceof ServletRequestAttributes) {
            Object access = ((ServletRequestAttributes) attributes).getRequest().getAttribute("access");
            if (access instanceof AccessInfo && ((AccessInfo) access).isLogged()) {
                return priorityExecutor;
            }
        }
        return publicExecutor;
    }
}
