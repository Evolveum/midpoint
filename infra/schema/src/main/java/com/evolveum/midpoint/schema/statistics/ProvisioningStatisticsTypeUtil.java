/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.statistics;

import static com.evolveum.midpoint.schema.statistics.ProvisioningOperation.find;

import java.util.Comparator;

import com.google.common.collect.ComparisonChain;
import com.google.common.collect.Ordering;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ProvisioningStatisticsEntryType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ProvisioningStatisticsOperationEntryType;

public class ProvisioningStatisticsTypeUtil {

    public static Comparator<? super ProvisioningStatisticsEntryType> createEntryComparator() {
        return (o1, o2) ->
                ComparisonChain.start()
                        .compare(getResourceName(o1), getResourceName(o2), Ordering.natural().nullsLast())
                        .compare(getObjectClassLocalName(o1), getObjectClassLocalName(o2), Ordering.natural().nullsLast())
                        .result();
    }

    public static String getResourceName(ProvisioningStatisticsEntryType e) {
        if (e == null || e.getResourceRef() == null) {
            return null;
        } else if (e.getResourceRef().getTargetName() != null) {
            return e.getResourceRef().getTargetName().getOrig();
        } else {
            return e.getResourceRef().getOid();
        }
    }

    public static String getObjectClassLocalName(ProvisioningStatisticsEntryType e) {
        return e.getObjectClass() != null ? e.getObjectClass().getLocalPart() : null;
    }

    public static Comparator<? super ProvisioningStatisticsOperationEntryType> createOperationComparator() {
        return (o1, o2) ->
                ComparisonChain.start()
                        .compare(find(o1.getOperation()), find(o2.getOperation()), Ordering.natural().nullsLast())
                        .compare(o1.getStatus(), o2.getStatus(), Ordering.natural().nullsLast())
                        .result();
    }
}
