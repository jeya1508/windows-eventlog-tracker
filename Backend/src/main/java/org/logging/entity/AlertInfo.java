package org.logging.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class AlertInfo {
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
    private String profile_name;
    private Object[] sortValues;
}
