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
import com.evolveum.midpoint.schema.statistics.MappingsStatisticsKey;
import com.evolveum.midpoint.schema.statistics.OperationalInformation;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Pavol Mederly
 */
public class MappingsLineDto {

    public static final String F_OBJECT = "object";
    public static final String F_COUNT = "count";
    public static final String F_AVERAGE_TIME = "averageTime";
    public static final String F_MIN_TIME = "minTime";
    public static final String F_MAX_TIME = "maxTime";
    public static final String F_TOTAL_TIME = "totalTime";

    private String object;
    private int count;
    private Integer minTime;
    private Integer maxTime;
    private int totalTime;

    public MappingsLineDto(String object) {
        this.object = object;
    }

    public String getObject() {
        return object;
    }

    public int getCount() {
        return count;
    }

    public int getAverageTime() {
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

    public static List<MappingsLineDto> extractFromOperationalInformation(OperationalInformation operationalInformation) {
        List<MappingsLineDto> retval = new ArrayList<>();
        Map<MappingsStatisticsKey, GenericStatisticsData> dataMap = operationalInformation.getMappingsData();
        if (dataMap == null) {
            return retval;
        }
        // this is much more generic that needs to be - but useful for future, maybe
        for (Map.Entry<MappingsStatisticsKey, GenericStatisticsData> entry : dataMap.entrySet()) {
            MappingsStatisticsKey key = entry.getKey();
            String object = key.getObjectName() != null ? key.getObjectName() : key.getObjectOid();
            MappingsLineDto lineDto = findLineDto(retval, object);
            if (lineDto == null) {
                lineDto = new MappingsLineDto(object);
                retval.add(lineDto);
            }
            lineDto.setValue(entry.getValue().getCount(), entry.getValue().getMinDuration(),
                    entry.getValue().getMaxDuration(), entry.getValue().getTotalDuration());
        }
        return retval;
    }

    private static MappingsLineDto findLineDto(List<MappingsLineDto> list, String object) {
        for (MappingsLineDto lineDto : list) {
            if (StringUtils.equals(lineDto.getObject(), object)) {
                return lineDto;
            }
        }
        return null;
    }

    private void setValue(int count, int min, int max, long totalDuration) {
        this.count += count;
        if (minTime == null || min < minTime) {
            minTime = min;
        }
        if (maxTime == null || max > maxTime) {
            maxTime = max;
        }
        totalTime += totalDuration;
    }
}
