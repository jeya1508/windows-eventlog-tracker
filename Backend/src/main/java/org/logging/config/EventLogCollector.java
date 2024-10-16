package org.logging.config;


import java.util.Map;
public class EventLogCollector {
    static {
        System.load("C:\\Users\\hp\\Documents\\GitHub\\windows-eventlog-tracker\\LogDLL\\x64\\Debug\\LogDLL.dll");
    }

    public native Map<String, String>[] collectWindowsLogs(long lastRecordNumber);

    public static void main(String[] args) {
        EventLogCollector collector = new EventLogCollector();
        Map<String, String>[] logs = collector.collectWindowsLogs(5836683);

        if (logs == null) {
            return;
        }

        for (Map<String, String> log : logs) {
            System.out.println(log);
        }
        System.out.println("Size of map "+logs.length);
    }
}
