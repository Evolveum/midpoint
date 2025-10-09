/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.schema.util;

import com.evolveum.midpoint.xml.ns._public.common.common_3.MultipleSubreportResultValuesHandlingType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectCollectionReportEngineConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ReportType;

public class ReportTypeUtil {

    public static boolean isSplitParentRowUsed(ReportType report) {
        ObjectCollectionReportEngineConfigurationType collection = report.getObjectCollection();
        if (collection == null) {
            return false;
        }

        return collection.getSubreport().stream()
                .map(s -> s.getResultHandling())
                .filter(rh -> rh != null)
                .anyMatch(rh -> MultipleSubreportResultValuesHandlingType.SPLIT_PARENT_ROW.equals(rh.getMultipleValues()));

    }
}
