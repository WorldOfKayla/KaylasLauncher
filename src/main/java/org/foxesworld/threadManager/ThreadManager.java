package org.foxesworld.threadManager;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public class ThreadManager {
    private final ExecutorService executorService;
    private final List<Future<?>> tasks;

    public ThreadManager(int numThreads) {
        this.executorService = Executors.newFixedThreadPool(numThreads);
        this.tasks = new ArrayList<>();
    }

    /**
     * Submits a Runnable task for execution.
     *
     * @param task The Runnable task to be executed.
     */
    public void submitTask(Runnable task) {
        Future<?> future = executorService.submit(task);
        tasks.add(future);
    }

    /**
     * Submits a Callable task for execution.
     *
     * @param <V>  The result type of method call.
     * @param task The Callable task to be executed.
     * @return A Future representing pending completion of the task.
     */
    public <V> Future<V> submitTask(Callable<V> task) {
        Future<V> future = executorService.submit(task);
        tasks.add(future);
        return future;
    }

    /**
     * Waits for all tasks to complete, with a timeout.
     *
     * @param timeout The maximum time to wait, in milliseconds.
     * @throws InterruptedException If the current thread was interrupted while waiting.
     */
    public void waitForAllTasks(long timeout) throws InterruptedException {
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(timeout, TimeUnit.MILLISECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException ex) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
            throw ex;
        }
    }

    /**
     * Cancels all running tasks.
     */
    public void cancelAllTasks() {
        for (Future<?> task : tasks) {
            task.cancel(true);
        }
        executorService.shutdownNow();
    }

    /**
     * Shuts down the executor service immediately, attempting to stop all actively executing tasks.
     */
    public void shutdownNow() {
        executorService.shutdownNow();
    }

    /**
     * Checks if all tasks are completed.
     *
     * @return true if all tasks are completed; false otherwise.
     */
    public boolean areAllTasksCompleted() {
        for (Future<?> task : tasks) {
            if (!task.isDone()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Retrieves the result of a specific task.
     *
     * @param <V>    The result type of method call.
     * @param future The Future representing the task whose result is to be retrieved.
     * @return The result of the task.
     * @throws InterruptedException If the current thread was interrupted while waiting.
     * @throws ExecutionException   If the computation threw an exception.
     */
    public <V> V getTaskResult(Future<V> future) throws InterruptedException, ExecutionException {
        return future.get();
    }
}
