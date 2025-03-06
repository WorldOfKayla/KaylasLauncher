package org.foxesworld.test;

import java.util.concurrent.*;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Universal DataInjector class for asynchronous data setting and retrieval,
 * supporting multithreading and listener notifications.
 *
 * @param <T> Type of injected data (e.g., String[], ConcurrentHashMap<String, AtomicInteger>, etc.).
 */
public class DataInjector<T> {
    private static final Logger LOGGER = LoggerFactory.getLogger(DataInjector.class);

    private final CompletableFuture<T> futureData = new CompletableFuture<>();
    private final CopyOnWriteArrayList<Consumer<T>> listeners = new CopyOnWriteArrayList<>();
    public DataInjector(){
        //LOGGER.warn("Using experimental DataInjector!");
    }

    /**
     * Registers a listener that will be called when data becomes available.
     * If data is already set, the listener is called immediately.
     *
     * @param listener Consumer to process the data.
     */
    public void addListener(Consumer<T> listener) {
        if (futureData.isDone()) {
            try {
                LOGGER.debug("Data is already set, invoking listener immediately.");
                listener.accept(futureData.get());
            } catch (Exception e) {
                LOGGER.error("Error invoking listener", e);
            }
        } else {
            LOGGER.debug("Adding listener {} to the queue", listener);
            listeners.add(listener);
        }
    }

    /**
     * Sets the data and notifies all registered listeners.
     * Ensures data is set only once.
     *
     * @param data Data to be set.
     */
    public void setContent(T data) {
        if (!futureData.isDone()) {
            LOGGER.debug("Setting data: {}", data);
            futureData.complete(data);
            LOGGER.debug("Notifying {} listeners.", listeners.size());
            for (Consumer<T> listener : listeners) {
                try {
                    listener.accept(data);
                } catch (Exception e) {
                    LOGGER.error("Error notifying listener", e);
                }
            }
            listeners.clear();
            LOGGER.debug("All listeners have been notified and cleared.");
        } else {
            LOGGER.warn("Data is already set. Ignoring duplicate setContent call.");
        }
    }

    /**
     * Blocking method to retrieve the data.
     * If data is not set, this call will wait indefinitely.
     *
     * @return The set data.
     * @throws Exception if interrupted while waiting.
     */
    public T getContent() throws Exception {
        LOGGER.debug("getContent() called, waiting for data if necessary.");
        T data = futureData.get();
        LOGGER.debug("Data retrieved: {}", data);
        return data;
    }

    /**
     * Retrieves the data with a timeout.
     *
     * @param timeout Maximum time to wait.
     * @param unit    Time unit for the timeout.
     * @return The set data.
     * @throws TimeoutException if data is not available within the timeout.
     * @throws Exception        if interrupted while waiting.
     */
    public T getContent(long timeout, TimeUnit unit) throws Exception {
        LOGGER.debug("getContent() called with timeout: {} {}", timeout, unit);
        try {
            T data = futureData.get(timeout, unit);
            LOGGER.debug("Data retrieved: {}", data);
            return data;
        } catch (TimeoutException e) {
            LOGGER.warn("Timeout while waiting for data.");
            throw e;
        }
    }

    /**
     * Checks if the data is already available.
     *
     * @return true if data is set, false otherwise.
     */
    public boolean isDataAvailable() {
        boolean available = futureData.isDone();
        LOGGER.debug("isDataAvailable() -> {}", available);
        return available;
    }
}
