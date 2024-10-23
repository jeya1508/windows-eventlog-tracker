package org.logging.producerconsumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.file.*;
import java.util.Arrays;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class CircularBlockingQueue<T> implements Serializable {
    private final T[] producerQueue;
    private final T[] consumerQueue;
    private final int capacity;
    private int producerFront = 0;
    private int producerRear = 0;
    private int producerSize = 0;

    private int consumerFront = 0;
    private int consumerRear = 0;
    private int consumerSize = 0;

    final Lock lock = new ReentrantLock();
    final Condition notEmpty = lock.newCondition();

    private static final Logger logger = LoggerFactory.getLogger(CircularBlockingQueue.class);

    private static final Path FILE_PATH = Paths.get("C:\\Users\\hp\\Documents\\GitHub\\windows-eventlog-tracker\\Backend\\src\\main\\java\\org\\logging\\assets\\queue_data_array.ser");

    public CircularBlockingQueue(int capacity) {
        this.capacity = capacity;
        this.producerQueue = (T[]) new Object[capacity];
        this.consumerQueue = (T[]) new Object[capacity];
    }

    public void produce(T item) {
        lock.lock();
        try {
            while (producerSize == capacity) {
                logger.info("Producer queue full, serializing to file.");
                serializeProducerQueueToFile();
            }
            producerQueue[producerRear] = item;
            producerRear = (producerRear + 1) % capacity;
            producerSize++;

            notEmpty.signalAll();
        } finally {
            lock.unlock();
        }
    }


    public T consume() {
        lock.lock();
        try {
            while (true) {
                if (consumerSize > 0) {
                    T item = consumerQueue[consumerFront];
                    consumerQueue[consumerFront] = null;
                    consumerFront = (consumerFront + 1) % capacity;
                    consumerSize--;
                    return item;
                } else {
                    logger.info("Consumer queue empty, deserializing from file...");

                    deserializeToConsumerQueueFromFile();

                    if (consumerSize > 0) {
                        continue;
                    }
                    logger.info("No logs available after deserialization, waiting for producer...");
                    notEmpty.await();
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("Thread interrupted while waiting: {}", e.getMessage());
        } finally {
            lock.unlock();
        }
        return null;
    }



    void serializeProducerQueueToFile() {
        lock.lock();
        try (ObjectOutputStream oos = new ObjectOutputStream(Files.newOutputStream(FILE_PATH, StandardOpenOption.CREATE, StandardOpenOption.WRITE))) {
            oos.writeObject(Arrays.copyOfRange(producerQueue, producerFront, producerFront + producerSize));
            producerFront = 0;
            producerRear = 0;
            producerSize = 0;
            logger.info("Serialized producer queue to file.");
            notEmpty.signalAll();
        } catch (IOException e) {
            logger.info("Error during serialization: {}", e.getMessage());
        } finally {
            lock.unlock();
        }
    }

    private void deserializeToConsumerQueueFromFile() {
        lock.lock();
        try {
            if (Files.exists(FILE_PATH) && Files.size(FILE_PATH) > 0) {
                try (ObjectInputStream ois = new ObjectInputStream(Files.newInputStream(FILE_PATH))) {
                    T[] deserializedArray = (T[]) ois.readObject();

                    if (deserializedArray.length > 0) {
                        int toConsume = Math.min(capacity - consumerSize, deserializedArray.length);
                        logger.info("Deserialized {} items from file.", toConsume);

                        for (int i = 0; i < toConsume; i++) {
                            consumerQueue[consumerRear] = deserializedArray[i];
                            consumerRear = (consumerRear + 1) % capacity;
                            consumerSize++;
                        }
                        if (toConsume < deserializedArray.length) {
                            T[] remainingItems = Arrays.copyOfRange(deserializedArray, toConsume, deserializedArray.length);
                            try (ObjectOutputStream oos = new ObjectOutputStream(Files.newOutputStream(FILE_PATH, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING))) {
                                oos.writeObject(remainingItems);
                                logger.info("Wrote remaining items back to the file after consuming.");
                            }
                        } else {
                            Files.newOutputStream(FILE_PATH, StandardOpenOption.TRUNCATE_EXISTING).close();
                            logger.info("All items consumed.");
                        }
                    } else {
                        logger.warn("No items found to deserialize from the file.");
                    }
                } catch (IOException | ClassNotFoundException e) {
                    logger.error("Error during deserialization: {}", e.getMessage());
                }
            } else {
                logger.info("The file is empty or does not exist, no data to deserialize.");
            }
        } catch (IOException e) {
            logger.error("Error accessing the file: {}", e.getMessage());
        } finally {
            lock.unlock();
        }
    }

    public int getProducerSize() {
        lock.lock();
        try {
            return producerSize;
        } finally {
            lock.unlock();
        }
    }
}

