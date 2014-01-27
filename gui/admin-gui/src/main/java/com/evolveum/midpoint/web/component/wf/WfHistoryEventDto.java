/*
 * Copyright (c) 2010-2013 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.web.component.wf;

import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

import java.io.Serializable;
import java.text.DateFormat;
import java.util.Date;

/**
 * @author mederly
 */
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
