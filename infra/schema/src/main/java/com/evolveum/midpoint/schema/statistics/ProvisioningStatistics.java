/*
 * Copyright (c) 2010-2015 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.statistics;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

import javax.xml.namespace.QName;

import static com.evolveum.midpoint.util.MiscUtil.*;

/**
 * This is "live" provisioning statistics.
 *
 * Thread safety: Must be thread safe.
 *
 * 1. Updates are invoked in the context of the thread executing the task.
 * 2. But queries are invoked either from this thread, or from some observer (task manager or GUI thread).
 *
 */
public class ProvisioningStatistics {

    private static final Trace LOGGER = TraceManager.getTrace(ProvisioningStatistics.class);

    /** Current value */
    @NotNull private final ProvisioningStatisticsType value = new ProvisioningStatisticsType();

    public ProvisioningStatistics() {
    }

    public ProvisioningStatistics(ProvisioningStatisticsType value) {
        if (value != null) {
            addTo(this.value, value);
        }
    }

    /** Returns a current value of this statistics. It is copied because of thread safety issues. */
    public synchronized ProvisioningStatisticsType getValueCopy() {
        return value.clone();
    }

    public synchronized void recordProvisioningOperation(String resourceOid, String resourceName, QName objectClassName,
            ProvisioningOperation operation, boolean success, int count, long duration) {

        LOGGER.info("Recording provisioning operation {} on {}/{}", operation, resourceName, resourceOid); // todo trace

        ObjectReferenceType resourceRef = new ObjectReferenceType();
        resourceRef.setOid(resourceOid);
        resourceRef.setType(ResourceType.COMPLEX_TYPE);
        resourceRef.setTargetName(PolyStringType.fromOrig(resourceName));

        ProvisioningStatisticsType delta = new ProvisioningStatisticsType();
        delta.beginEntry()
                .resourceRef(resourceRef)
                .objectClass(objectClassName)
                .beginOperation()
                .operation(operation.name())
                   .status(success ? OperationResultStatusType.SUCCESS : OperationResultStatusType.FATAL_ERROR)
                    .count(count)
                    .totalTime(duration)
                    .minTime(duration)
                    .maxTime(duration);

        addTo(this.value, delta);
    }

    /** Updates specified summary with given delta. */
    public static void addTo(@NotNull ProvisioningStatisticsType sum, @NotNull ProvisioningStatisticsType delta) {
        addMatchingEntries(sum.getEntry(), delta.getEntry());
    }

    /** Looks for matching entries (created if necessary) and adds them. */
    private static void addMatchingEntries(List<ProvisioningStatisticsEntryType> sumEntries,
            List<ProvisioningStatisticsEntryType> deltaEntries) {
        for (ProvisioningStatisticsEntryType deltaEntry : deltaEntries) {
            ProvisioningStatisticsEntryType matchingEntry = findOrCreateMatchingEntry(sumEntries, deltaEntry.getResourceRef(),
                    deltaEntry.getObjectClass());
            addEntryInformation(matchingEntry, deltaEntry);
        }
    }

    private static ProvisioningStatisticsEntryType findOrCreateMatchingEntry(
            List<ProvisioningStatisticsEntryType> entries, ObjectReferenceType resourceRef, QName objectClass) {
        return findMatchingEntry(entries, resourceRef, objectClass)
                .orElseGet(
                        () -> add(entries, new ProvisioningStatisticsEntryType()
                                .resourceRef(resourceRef)
                                .objectClass(objectClass)));
    }

    private static <T> T add(List<T> list, T value) {
        list.add(value);
        return value;
    }

    private static Optional<ProvisioningStatisticsEntryType> findMatchingEntry(
            List<ProvisioningStatisticsEntryType> entries, ObjectReferenceType resourceRef, QName objectClass) {
        return entries.stream()
                .filter(entry -> entryMatches(entry, resourceRef, objectClass))
                .findFirst();
    }

    private static boolean entryMatches(ProvisioningStatisticsEntryType entry, ObjectReferenceType resourceRef, QName objectClass) {
        return Objects.equals(getOid(entry.getResourceRef()), getOid(resourceRef)) &&
                QNameUtil.match(entry.getObjectClass(), objectClass);
    }

    private static Object getOid(ObjectReferenceType ref) {
        return ref != null ? ref.getOid() : null;
    }

    /** Adds two "part information" */
    private static void addEntryInformation(ProvisioningStatisticsEntryType sum, ProvisioningStatisticsEntryType delta) {
        addOperations(sum.getOperation(), delta.getOperation());
    }

    static void addOperations(List<ProvisioningStatisticsOperationEntryType> sumOperations, List<ProvisioningStatisticsOperationEntryType> deltaOperations) {
        for (ProvisioningStatisticsOperationEntryType deltaOperation : deltaOperations) {
            ProvisioningStatisticsOperationEntryType matchingOperation =
                    findOrCreateOperation(sumOperations, deltaOperation.getOperation(), deltaOperation.getStatus());
            addMatchingOperations(matchingOperation, deltaOperation);
        }
    }

    static ProvisioningStatisticsOperationEntryType findOrCreateOperation(
            List<ProvisioningStatisticsOperationEntryType> operations, String operationName, OperationResultStatusType status) {
        return operations.stream()
                .filter(op -> Objects.equals(op.getOperation(), operationName) && op.getStatus() == status)
                .findFirst()
                .orElseGet(
                        () -> add(operations, new ProvisioningStatisticsOperationEntryType()
                                .operation(operationName)
                                .status(status)));
    }

    private static void addMatchingOperations(ProvisioningStatisticsOperationEntryType sum,
            ProvisioningStatisticsOperationEntryType delta) {
        sum.setCount(or0(sum.getCount()) + or0(delta.getCount()));
        sum.setTotalTime(or0(sum.getTotalTime()) + or0(delta.getTotalTime()));
        sum.setMinTime(min(sum.getMinTime(), delta.getMinTime()));
        sum.setMaxTime(max(sum.getMaxTime(), delta.getMaxTime()));
    }

    public static String format(ProvisioningStatisticsType source) {
        return format(source, null);
    }

    /** Formats the information. */
    public static String format(ProvisioningStatisticsType source, AbstractStatisticsPrinter.Options options) {
        ProvisioningStatisticsType information = source != null ? source : new ProvisioningStatisticsType();
        return new ProvisioningStatisticsPrinter(information, options).print();
    }
}
