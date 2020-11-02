/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.statistics;

import com.evolveum.midpoint.xml.ns._public.common.common_3.WorkBucketManagementPerformanceInformationType;

public class TaskWorkBucketManagementPerformanceInformationUtil {

    public static String format(WorkBucketManagementPerformanceInformationType i, AbstractStatisticsPrinter.Options options,
            Integer iterations, Integer seconds) {
        return new TaskWorkBucketManagementPerformanceInformationPrinter(i, options, iterations, seconds)
                .print();
    }
}
