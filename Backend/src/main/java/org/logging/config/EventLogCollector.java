package org.logging.config;


import java.util.Map;
public class EventLogCollector {
    static {
        System.load("C:\\Users\\hp\\Documents\\GitHub\\windows-eventlog-tracker\\LogDLL\\x64\\Debug\\LogDLL.dll");
    }

    public native Map<String, String>[] collectWindowsLogs(String ipAddress, String hostName, String password, long lastRecordNumber);

    public static void main(String[] args) {
        EventLogCollector collector = new EventLogCollector();
        Map<String, String>[] logs = collector.collectWindowsLogs("192.168.1.39","hp","Ranaja@1744",5909810);

        if (logs == null) {
            return;
        }

        for (Map<String, String> log : logs) {
            System.out.println(log);
        }
        System.out.println("Size of map "+logs.length);
    }
}
