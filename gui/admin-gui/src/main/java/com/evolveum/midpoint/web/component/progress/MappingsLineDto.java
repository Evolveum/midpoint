/*
 * Copyright (c) 2010-2015 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.component.progress;

import com.evolveum.midpoint.schema.statistics.EnvironmentalPerformanceInformation;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MappingsStatisticsEntryType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MappingsStatisticsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.EnvironmentalPerformanceInformationType;

import org.apache.wicket.model.StringResourceModel;

import java.io.Serializable;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Pavol Mederly
 */
public class MappingsLineDto implements Serializable {

    public static final String F_OBJECT = "object";
    public static final String F_COUNT = "count";
    public static final String F_AVERAGE_TIME = "averageTime";
    public static final String F_MIN_TIME = "minTime";
    public static final String F_MAX_TIME = "maxTime";
    public static final String F_TOTAL_TIME = "totalTime";

    private String object;
    private int count;
    private Long minTime;
    private Long maxTime;
    private long totalTime;

    public MappingsLineDto(MappingsStatisticsEntryType entry) {
        object = entry.getObject();
        count = entry.getCount();
        minTime = entry.getMinTime();
        maxTime = entry.getMaxTime();
        totalTime = entry.getTotalTime();
    }

    public String getObject() {
        return object;
    }

    public String getCount() {
        return getString(count);
    }

    public String getAverageTime() {
        if (count > 0) {
            return getString(totalTime / count);
        } else {
            return null;
        }
    }

    public String getMinTime() {
        return getString(minTime);
    }

    public String getMaxTime() {
        return getString(maxTime);
    }

    public String getTotalTime() {
        return getString(totalTime);
    }

    protected static List<MappingsLineDto> extractFromOperationalInformation(MappingsStatisticsType mappingsStatisticsType) {
        List<MappingsLineDto> retval = new ArrayList<>();
        if (mappingsStatisticsType == null) {
            return retval;
        }
        for (MappingsStatisticsEntryType entry : mappingsStatisticsType.getEntry()) {
            retval.add(new MappingsLineDto(entry));
        }
        return retval;
    }

    private String getString(Object numerToFormat) {
        return new StringResourceModel("StatisticsPanel.provisioningStatistics.averageTime.formatted").setParameters(numerToFormat).getString();
    }
}
