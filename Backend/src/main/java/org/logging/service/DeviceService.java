package org.logging.service;

import org.logging.entity.DeviceInfo;
import org.logging.exception.ValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class DeviceService {
    public static final String FILE_PATH = "C:\\Users\\hp\\Documents\\GitHub\\windows-eventlog-tracker\\Backend\\src\\main\\java\\org\\logging\\assets\\deviceFile.txt";
    private static final ValidationService validationService = new ValidationService();
    private static final Logger logger = LoggerFactory.getLogger(DeviceService.class);

    public String addDeviceToFile(String deviceName,String ipAddress,String hostName, String password) throws IOException, ValidationException {
        List<String> deviceNames = getAllDeviceName();
        if(!deviceNames.contains(deviceName))
        {
            if(!validationService.isValidIPAddress(ipAddress))
            {
                throw new ValidationException("Invalid IP address");
            }
        }
        else{
            throw new ValidationException("Device name already exists");
        }
        String textToAppend = deviceName+","+ipAddress+","+hostName+","+password+"\n";
        Path path = Paths.get(FILE_PATH);
        if(Files.exists(path))
        {
            Files.write(path,textToAppend.getBytes(), StandardOpenOption.APPEND);
            return "Device added successfully";
        }
        else{
            throw new IOException("File does not exist");
        }
    }
    public DeviceInfo getDeviceFromDeviceName(String deviceName ) {

        try (BufferedReader reader = new BufferedReader(new FileReader(FILE_PATH))) {
            String line;

            while ((line = reader.readLine()) != null) {
                if(line.trim().split(",")[0].equals(deviceName)) {
                    String[] deviceSplit = line.trim().split(",");
                    String name = deviceSplit[0];
                    String ipAddress = deviceSplit[1];
                    String hostName = deviceSplit[2];
                    String password = deviceSplit[3];
                    return new DeviceInfo(name,ipAddress, hostName, password);
                }
            }
        } catch (IOException e) {
            logger.error("Error in retrieving the devices {}",e.getMessage());
        }
        return null;
    }
    public List<String> getAllDeviceName()
    {
        List<String > deviceNameList = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(FILE_PATH))) {
            String line;

            while ((line = reader.readLine()) != null) {
                deviceNameList.add(line.trim().split(",")[0]);
            }
        } catch (IOException e) {
            logger.error("Error in retrieving the device names {}",e.getMessage());
        }
        return deviceNameList;
    }

}
