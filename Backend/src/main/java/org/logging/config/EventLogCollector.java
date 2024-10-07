package org.logging.config;

import java.util.Map;

public class EventLogCollector {
    static {
        System.load("C:\\Users\\hp\\Documents\\log-export\\LogDLL\\x64\\Debug\\LogDLL.dll");
    }

    public native Map<String, String>[] collectWindowsLogs();

    public static void main(String[] args) {
        EventLogCollector collector = new EventLogCollector();
        Map<String, String>[] logs = collector.collectWindowsLogs();

        if (logs == null) {
            System.err.println("Failed to collect logs.");
            return;
        }

        for (Map<String, String> log : logs) {
            System.out.println(log);
        }
        System.out.println("Size of map "+logs.length);
    }
}
