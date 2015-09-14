/*
 * Copyright (c) 2010-2015 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.web.component.progress;

import com.evolveum.midpoint.schema.statistics.GenericStatisticsData;
import com.evolveum.midpoint.schema.statistics.NotificationsStatisticsKey;
import com.evolveum.midpoint.schema.statistics.OperationalInformation;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Pavol Mederly
 */
public class NotificationsLineDto {

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
    private Integer minTime;
    private Integer maxTime;
    private int totalTime;

    public NotificationsLineDto(String transport) {
        this.transport = transport;
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

    public int getAverageTime() {
        int count = countSuccess + countFailure;
        if (count > 0) {
            return totalTime / count;
        } else {
            return 0;
        }
    }

    public int getMinTime() {
        return minTime != null ? minTime : 0;
    }

    public int getMaxTime() {
        return maxTime != null ? maxTime : 0;
    }

    public int getTotalTime() {
        return totalTime;
    }

    public static List<NotificationsLineDto> extractFromOperationalInformation(OperationalInformation operationalInformation) {
        List<NotificationsLineDto> retval = new ArrayList<>();
        Map<NotificationsStatisticsKey, GenericStatisticsData> dataMap = operationalInformation.getNotificationsData();
        if (dataMap == null) {
            return retval;
        }
        for (Map.Entry<NotificationsStatisticsKey, GenericStatisticsData> entry : dataMap.entrySet()) {
            NotificationsStatisticsKey key = entry.getKey();
            String transport = key.getTransport();
            NotificationsLineDto lineDto = findLineDto(retval, transport);
            if (lineDto == null) {
                lineDto = new NotificationsLineDto(transport);
                retval.add(lineDto);
            }
            lineDto.setValue(key.isSuccess(), entry.getValue().getCount(), entry.getValue().getMinDuration(),
                    entry.getValue().getMaxDuration(), entry.getValue().getTotalDuration());
        }
        return retval;
    }

    private static NotificationsLineDto findLineDto(List<NotificationsLineDto> list, String transport) {
        for (NotificationsLineDto lineDto : list) {
            if (StringUtils.equals(lineDto.getTransport(), transport)) {
                return lineDto;
            }
        }
        return null;
    }

    private void setValue(boolean success, int count, int min, int max, long totalDuration) {
        if (success) {
            this.countSuccess += count;
        } else {
            this.countFailure += count;
        }
        if (minTime == null || min < minTime) {
            minTime = min;
        }
        if (maxTime == null || max > maxTime) {
            maxTime = max;
        }
        totalTime += totalDuration;
    }
}
