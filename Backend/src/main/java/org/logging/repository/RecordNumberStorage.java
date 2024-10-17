package org.logging.repository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

public class RecordNumberStorage
{
    public static final Logger logger = LoggerFactory.getLogger(RecordNumberStorage.class);
    public static final String FILE_PATH = "C:\\Users\\hp\\Documents\\GitHub\\windows-eventlog-tracker\\Backend\\src\\main\\java\\org\\logging\\assets\\recordNumberFile.txt";

    public static void saveLastRecordNumber(String lastRecordNumber)
    {
        try(FileChannel fileChannel = FileChannel.open(Paths.get(FILE_PATH),
                StandardOpenOption.WRITE,StandardOpenOption.CREATE,StandardOpenOption.TRUNCATE_EXISTING))
        {
            ByteBuffer buffer = ByteBuffer.allocate(128);
            buffer.put(lastRecordNumber.getBytes());
            buffer.flip();
            fileChannel.write(buffer);
            logger.info("Record number updated successfully");
        }
        catch (IOException e)
        {
            logger.error("Error in indexing the record number {}",e.getMessage());
        }
    }

    public static String getLastRecordNumber()
    {
        Path path = Paths.get(FILE_PATH);
        if(Files.exists(path))
        {
            try
            {
                logger.info("Fetching Record number from file");
                 String content = new String(Files.readAllBytes(path)).trim();
                 return content.isEmpty() ? null : content;
            }
            catch (IOException e)
            {
                logger.error("Error while retrieving record number from the file {}",e.getMessage());
            }
        }
        return null;
    }
}
