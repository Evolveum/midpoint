/*
 * Copyright (c) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.component.progress;

import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationResultStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ProvisioningStatisticsEntryType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ProvisioningStatisticsOperationEntryType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ProvisioningStatisticsType;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.wicket.model.StringResourceModel;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class ProvisioningStatisticsOperationDto implements Serializable {

    public static final String F_AVG_TIME = "avgTime";

    private Long avgTime;

    private ProvisioningStatisticsOperationEntryType entry;


    ProvisioningStatisticsOperationDto(ProvisioningStatisticsOperationEntryType entry) {
        this.entry = entry;
        computeAvg();
    }

    public static List<ProvisioningStatisticsOperationDto> extractFromOperationalInformation(List<ProvisioningStatisticsOperationEntryType> entries) {
        List<ProvisioningStatisticsOperationDto> retval = new ArrayList<>();
        if (CollectionUtils.isEmpty(entries)) {
            return retval;
        }
        for (ProvisioningStatisticsOperationEntryType entry : entries) {
            retval.add(new ProvisioningStatisticsOperationDto(entry));
        }

        retval.sort((op1, op2) -> sortOperations(op1, op2));
        return retval;
    }

    private static int sortOperations(ProvisioningStatisticsOperationDto op1, ProvisioningStatisticsOperationDto op2) {
        if (op1 == null) {
            return op2 == null ? 0 : -1;
        }
        if (op2 == null) {
            return 1;
        }

        String o1 = op1.getOperation();
        String o2 = op2.getOperation();

        return o1.compareTo(o2);
    }

    private void computeAvg() {
        Integer count = entry.getCount();
        if (count == null || count == 0) {
            return;
        }

        Long totalTime = entry.getTotalTime();
        if (totalTime == null) {
            return;
        }

        avgTime = totalTime / count;
    }

    public String getCount() {
        return getString(entry.getCount());
    }

    public String getMaxTime() {
        return getString(entry.getMaxTime());
    }

    public String getMinTime() {
        return getString(entry.getMinTime());
    }

    public String getTotalTime() {
        return getString(entry.getTotalTime());
    }

    public String getAvgTime() {
        return getString(avgTime);
    }

    public OperationResultStatusType getStatus() {
        return entry.getStatus();
    }

    public String getOperation() {
        return entry.getOperation();
    }

    private String getString(Object numberToFormat) {
        return new StringResourceModel("StatisticsPanel.provisioningStatistics.averageTime.formatted").setParameters(numberToFormat).getString();
    }
}
