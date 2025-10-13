/*
 * Copyright (c) 2010-2015 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.web.component.progress;

import com.evolveum.midpoint.schema.statistics.EnvironmentalPerformanceInformation;
import com.evolveum.midpoint.xml.ns._public.common.common_3.NotificationsStatisticsEntryType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.NotificationsStatisticsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.EnvironmentalPerformanceInformationType;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class NotificationsLineDto implements Serializable {

    public static final String F_TRANSPORT = "transport";
    public static final String F_COUNT_SUCCESS = "countSuccess";
    public static final String F_COUNT_FAILURE = "countFailure";
    public static final String F_AVERAGE_TIME = "averageTime";
    public static final String F_MIN_TIME = "minTime";
    public static final String F_MAX_TIME = "maxTime";
    public static final String F_TOTAL_TIME = "totalTime";

    private String transport;
    private int countSuccess;
    private int countFailure;
    private Long minTime;
    private Long maxTime;
    private long totalTime;

    public NotificationsLineDto(String transport) {
        this.transport = transport;
    }

    public NotificationsLineDto(NotificationsStatisticsEntryType entry) {
        transport = entry.getTransport();
        countSuccess = entry.getCountSuccess();
        countFailure = entry.getCountFailure();
        minTime = entry.getMinTime();
        maxTime = entry.getMaxTime();
        totalTime = entry.getTotalTime();
    }

    public Long getAverageTime() {
        int count = countSuccess + countFailure;
        if (count > 0) {
            return totalTime / count;
        } else {
            return null;
        }
    }

    public String getTransport() {
        return transport;
    }

    public int getCountSuccess() {
        return countSuccess;
    }

    public int getCountFailure() {
        return countFailure;
    }

    public Long getMinTime() {
        return minTime;
    }

    public Long getMaxTime() {
        return maxTime;
    }

    public long getTotalTime() {
        return totalTime;
    }

    protected static List<NotificationsLineDto> extractFromOperationalInformation(NotificationsStatisticsType notificationsStatisticsType) {
        List<NotificationsLineDto> retval = new ArrayList<>();
        if (notificationsStatisticsType == null) {
            return retval;
        }

        for (NotificationsStatisticsEntryType entry : notificationsStatisticsType.getEntry()) {
            retval.add(new NotificationsLineDto(entry));
        }
        return retval;
    }
}
