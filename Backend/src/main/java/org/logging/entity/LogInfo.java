package org.logging.entity;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LogInfo {
    private String _id;
    private String event_category;
    private String event_id;
    private String event_type;
    private String record_number;
    private String source;
    private String hostname;
    private String username;
    private String time_generated;
    private String time_written;
    private Object[] sortValues;
}

