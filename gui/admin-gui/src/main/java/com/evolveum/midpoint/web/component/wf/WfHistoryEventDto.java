/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.component.wf;

import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

import java.io.Serializable;
import java.text.DateFormat;
import java.util.Date;

public class WfHistoryEventDto implements Comparable<WfHistoryEventDto>, Serializable {

    private static final Trace LOGGER = TraceManager.getTrace(WfHistoryEventDto.class);

    public static final String F_TIMESTAMP = "timestamp";
    public static final String F_TIMESTAMP_FORMATTED = "timestampFormatted";
    public static final String F_EVENT = "event";

    private Date timestamp;
    private String event;

    // format: [timestamp timestamp-formatted] message
    public WfHistoryEventDto(String entry) {
        int i = entry.indexOf('[');
        int j = entry.indexOf(']');
        if (i == 0 && j != -1) {
            int k = entry.indexOf(':');
            if (k == -1) {
                k = j;
            }
            String ts = entry.substring(1, k);
            try {
                timestamp = new Date(Long.parseLong(ts));
                event = entry.substring(j+2);
                return;
            } catch(NumberFormatException e) {
                // handled below
            }
        }

        timestamp = new Date();
        event = "Invalid workflow history entry: " + entry;
        LOGGER.error("Invalid workflow history entry: " + entry);
    }

    public String getTimestampFormatted() {
        DateFormat formatter = DateFormat.getDateTimeInstance();
        return formatter.format(timestamp);
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public String getEvent() {
        return event;
    }

    @Override
    public int compareTo(WfHistoryEventDto o) {
        return timestamp.compareTo(o.getTimestamp());
    }
}
