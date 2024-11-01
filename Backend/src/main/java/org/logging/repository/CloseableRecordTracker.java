package org.logging.repository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.*;
import java.nio.file.*;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

public class CloseableRecordTracker implements Closeable {
    private static CloseableRecordTracker instance;
    private static final Logger logger = LoggerFactory.getLogger(CloseableRecordTracker.class);
    private static final String FILE_PATH = "C:\\Users\\hp\\Documents\\GitHub\\windows-eventlog-tracker\\Backend\\src\\main\\java\\org\\logging\\assets\\recordNumberFile.txt";
    private final AtomicLong currentRecordNumber;
    private final ReentrantLock fileLock;
    private volatile boolean isClosed;
    private static String deviceName;

    public CloseableRecordTracker() throws IOException {
        this.currentRecordNumber = new AtomicLong();
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
    public long initializeRecordNumber(String deviceName) throws IOException {
        Path path = Paths.get(FILE_PATH);
        if (Files.exists(path)) {
            for (String line : Files.readAllLines(path)) {
                String[] parts = line.split(",");
                if (parts.length >= 2 && (parts[0].trim().equals(deviceName)|| (deviceName == null && parts[0].trim().equals("null")) ) ) {
                    CloseableRecordTracker.deviceName = parts[0].trim();
                    return Long.parseLong(parts[1].trim());
                }
            }
        }
        CloseableRecordTracker.deviceName = null;
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
            Path path = Paths.get(FILE_PATH);
            List<String> lines = Files.readAllLines(path);
            List<String> updatedLines = new ArrayList<>();

            boolean deviceFound = false;
            String updatedLine = deviceName + "," + currentRecordNumber.get();

            for (String line : lines) {
                String[] parts = line.split(",");
                if (parts.length >= 2 && (parts[0].trim().equals(deviceName)|| (deviceName == null && parts[0].trim().equals("null")) ) ) {
                    updatedLines.add(updatedLine);
                    deviceFound = true;
                } else {
                    updatedLines.add(line);
                }
            }

            if (!deviceFound) {
                updatedLines.add(updatedLine);
            }
            Files.write(path, updatedLines, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            logger.info("Record number {} for device {} written to file", currentRecordNumber.get(), deviceName);
        } finally {
            fileLock.unlock();
        }
    }
}