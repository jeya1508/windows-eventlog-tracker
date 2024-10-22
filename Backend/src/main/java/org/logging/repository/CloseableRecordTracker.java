package org.logging.repository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.*;
import java.nio.file.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

public class CloseableRecordTracker implements Closeable {
    private static CloseableRecordTracker instance;
    private static final Logger logger = LoggerFactory.getLogger(CloseableRecordTracker.class);
    private static final String FILE_PATH = "C:\\Users\\hp\\Documents\\GitHub\\windows-eventlog-tracker\\Backend\\src\\main\\java\\org\\logging\\assets\\recordNumberFile.txt";
    private final AtomicLong currentRecordNumber;
    private final ReentrantLock fileLock;
    private volatile boolean isClosed;

    public CloseableRecordTracker() throws IOException {
        this.currentRecordNumber = new AtomicLong(initializeRecordNumber());
        this.fileLock = new ReentrantLock();
        this.isClosed = false;
    }
    public static synchronized CloseableRecordTracker getInstance() throws IOException
    {
        if(instance == null)
        {
            instance = new CloseableRecordTracker();
        }
        return instance;
    }
    private long initializeRecordNumber() throws IOException {
        Path path = Paths.get(FILE_PATH);
        if (Files.exists(path)) {
            String content = new String(Files.readAllBytes(path)).trim();
            return content.isEmpty() ? -1 : Long.parseLong(content);
        }
        return -1;
    }

    public long getCurrentRecordNumber() {
        checkClosed();
        return currentRecordNumber.get();
    }

    public void updateRecordNumber(long newRecordNumber) {
        checkClosed();
        currentRecordNumber.set(newRecordNumber);
        logger.info("Record number updated to {}",currentRecordNumber);
    }

    private void checkClosed() {
        if (isClosed) {
            throw new IllegalStateException("RecordTracker is closed");
        }
    }

    @Override
    public void close() throws IOException {
        if (!isClosed) {
            isClosed = true;
            writeRecordNumberToFile();
            logger.info("CloseableRecordTracker closed successfully");
        }
    }

    public void writeRecordNumberToFile() throws IOException {
        fileLock.lock();
        try {
            Files.write(Paths.get(FILE_PATH),
                    String.valueOf(currentRecordNumber.get()).getBytes(),
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING);
            logger.info("Final record number {} written to file", currentRecordNumber.get());
        } finally {
            fileLock.unlock();
        }
    }
}