/*
 * Copyright (c) 2010-2015 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.statistics;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javax.xml.datatype.DatatypeConstants;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivityActionsExecutedType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectActionsExecutedEntryType;

public class ActionsExecutedInformationUtil {

    public enum Part {
        ALL, RESULTING
    }

    public static @NotNull List<ObjectActionsExecutedEntryType> summarize(@NotNull List<ObjectActionsExecutedEntryType> raw) {
        List<ObjectActionsExecutedEntryType> summarized = new ArrayList<>();
        addTo(summarized, raw);
        return summarized;
    }

    public static void addTo(ActivityActionsExecutedType sum, @Nullable ActivityActionsExecutedType delta) {
        if (delta != null) {
            addTo(sum.getObjectActionsEntry(), delta.getObjectActionsEntry());
            addTo(sum.getResultingObjectActionsEntry(), delta.getResultingObjectActionsEntry());
        }
    }

    public static void addTo(List<ObjectActionsExecutedEntryType> sumEntries, List<ObjectActionsExecutedEntryType> deltaEntries) {
        for (ObjectActionsExecutedEntryType deltaEntry : deltaEntries) {
            ObjectActionsExecutedEntryType matchingSumEntry = findMatchingEntry(sumEntries, deltaEntry);
            if (matchingSumEntry != null) {
                addToEntry(matchingSumEntry, deltaEntry);
            } else {
                sumEntries.add(deltaEntry.clone());
            }
        }
    }

    /**
     * Note that comparing dates using != LESSER instead of == GREATER ensures that the data from the delta
     * (that can be assumed to be the more current e.g. in the case of updating aggregate values with item processing
     * delta) will be used as the 'last' object.
     */
    private static void addToEntry(ObjectActionsExecutedEntryType sum, ObjectActionsExecutedEntryType delta) {
        sum.setTotalSuccessCount(sum.getTotalSuccessCount() + delta.getTotalSuccessCount());
        if (delta.getLastSuccessTimestamp() != null &&
                (sum.getLastSuccessTimestamp() == null || delta.getLastSuccessTimestamp().compare(sum.getLastSuccessTimestamp()) != DatatypeConstants.LESSER)) {
            sum.setLastSuccessObjectName(delta.getLastSuccessObjectName());
            sum.setLastSuccessObjectDisplayName(delta.getLastSuccessObjectDisplayName());
            sum.setLastSuccessObjectOid(delta.getLastSuccessObjectOid());
            sum.setLastSuccessTimestamp(delta.getLastSuccessTimestamp());
        }
        sum.setTotalFailureCount(sum.getTotalFailureCount() + delta.getTotalFailureCount());
        if (delta.getLastFailureTimestamp() != null &&
                (sum.getLastFailureTimestamp() == null || delta.getLastFailureTimestamp().compare(sum.getLastFailureTimestamp()) != DatatypeConstants.LESSER)) {
            sum.setLastFailureObjectName(delta.getLastFailureObjectName());
            sum.setLastFailureObjectDisplayName(delta.getLastFailureObjectDisplayName());
            sum.setLastFailureObjectOid(delta.getLastFailureObjectOid());
            sum.setLastFailureTimestamp(delta.getLastFailureTimestamp());
            sum.setLastFailureExceptionMessage(delta.getLastFailureExceptionMessage());
        }
    }

    private static ObjectActionsExecutedEntryType findMatchingEntry(List<ObjectActionsExecutedEntryType> sumEntries, ObjectActionsExecutedEntryType deltaEntry) {
        for (ObjectActionsExecutedEntryType e : sumEntries) {
            if (deltaEntry.getObjectType().equals(e.getObjectType()) &&
                    deltaEntry.getOperation().equals(e.getOperation()) &&
                    StringUtils.equals(deltaEntry.getChannel(), e.getChannel())) {
                return e;
            }
        }
        return null;
    }

    @NotNull
    public static String format(@NotNull ActivityActionsExecutedType information) {
        return format(information, null);
    }

    @NotNull
    public static String format(@NotNull ActivityActionsExecutedType information, Part part) {
        StringBuilder sb = new StringBuilder();
        if (part == null || part == Part.ALL) {
            formatActions(sb, information.getObjectActionsEntry(), "  All object actions:\n");
        }
        if (part == null || part == Part.RESULTING) {
            formatActions(sb, information.getResultingObjectActionsEntry(), "  Resulting object actions:\n");
        }
        return sb.toString();
    }

    private static void formatActions(StringBuilder sb, List<ObjectActionsExecutedEntryType> entries, String label) {
        sb.append(label);
        for (ObjectActionsExecutedEntryType a : entries) {
            formatActionExecuted(sb, a);
        }
    }

    private static void formatActionExecuted(StringBuilder sb, ObjectActionsExecutedEntryType a) {
        sb.append(String.format("    %-10s %-30s %s\n", a.getOperation(), QNameUtil.getLocalPart(a.getObjectType()), a.getChannel()));
        if (a.getTotalSuccessCount() > 0) {
            sb.append(String.format(Locale.US, "      success: %6d time(s), last: %s (%s, %s) on %tc\n", a.getTotalSuccessCount(),
                    a.getLastSuccessObjectName(), a.getLastSuccessObjectDisplayName(), a.getLastSuccessObjectOid(),
                    XmlTypeConverter.toDate(a.getLastSuccessTimestamp())));
        }
        if (a.getTotalFailureCount() > 0) {
            sb.append(String.format(Locale.US, "      failure: %6d time(s), last: %s (%s, %s) on %tc\n", a.getTotalFailureCount(),
                    a.getLastFailureObjectName(), a.getLastFailureObjectDisplayName(), a.getLastFailureObjectOid(),
                    XmlTypeConverter.toDate(a.getLastFailureTimestamp())));
        }
    }
}
