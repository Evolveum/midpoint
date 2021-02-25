/*
 * Copyright (c) 2010-2015 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.statistics;

import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.apache.commons.lang.StringUtils;

import javax.xml.datatype.DatatypeConstants;
import javax.xml.namespace.QName;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static org.apache.commons.lang3.ObjectUtils.defaultIfNull;

/**
 * Thread safety: Instances of this class may be accessed from more than one thread at once.
 * Updates are invoked in the context of the thread executing the task.
 * Queries are invoked either from this thread, or from some observer (task manager or GUI thread).
 *
 * We ensure synchronization by making public methods synchronized. We don't expect much contention on this.
 */
public class EnvironmentalPerformanceInformation {

    /**
     * This object is concurrently read (that is thread-safe), not written.
     *
     * NOTE: provisioning part of this value is always null
     */
    private final EnvironmentalPerformanceInformationType startValue;

    private final ProvisioningStatistics provisioningStatistics;
    private final Map<NotificationsStatisticsKey,GenericStatisticsData> notificationsData = new HashMap<>();
    private final Map<MappingsStatisticsKey,GenericStatisticsData> mappingsData = new HashMap<>();

    private static final int AGGREGATION_THRESHOLD = 50;

    private StatusMessage lastMessage;

    public EnvironmentalPerformanceInformation(EnvironmentalPerformanceInformationType value) {
        if (value != null) {
            startValue = value.clone();
            startValue.setProvisioningStatistics(null);
            provisioningStatistics = new ProvisioningStatistics(value.getProvisioningStatistics());
        } else {
            startValue = null;
            provisioningStatistics = new ProvisioningStatistics();
        }
    }

    public EnvironmentalPerformanceInformation() {
        this(null);
    }

    public synchronized EnvironmentalPerformanceInformationType getValueCopy() {
        EnvironmentalPerformanceInformationType delta = toEnvironmentalPerformanceInformationType();
        return aggregate(startValue, delta);
    }

    private EnvironmentalPerformanceInformationType toEnvironmentalPerformanceInformationType() {
        EnvironmentalPerformanceInformationType rv = new EnvironmentalPerformanceInformationType();
        rv.setProvisioningStatistics(provisioningStatistics.getValueCopy());
        rv.setMappingsStatistics(toMappingsStatisticsType());
        rv.setNotificationsStatistics(toNotificationsStatisticsType());
        if (lastMessage != null) {
            rv.setLastMessageTimestamp(XmlTypeConverter.createXMLGregorianCalendar(lastMessage.getDate()));
            rv.setLastMessage(lastMessage.getMessage());
        }
        return rv;
    }

    private NotificationsStatisticsType toNotificationsStatisticsType() {
        NotificationsStatisticsType rv = new NotificationsStatisticsType();
        for (Map.Entry<NotificationsStatisticsKey, GenericStatisticsData> entry : notificationsData.entrySet()) {
            NotificationsStatisticsKey key = entry.getKey();
            String transport = key.getTransport();
            NotificationsStatisticsEntryType entryType = findNotificationsEntryType(rv.getEntry(), transport);
            if (entryType == null) {
                entryType = new NotificationsStatisticsEntryType();
                entryType.setTransport(transport);
                rv.getEntry().add(entryType);
            }
            setValueNotifications(entryType, key.isSuccess(), entry.getValue().getCount(),
                    entry.getValue().getMinDuration(), entry.getValue().getMaxDuration(), entry.getValue().getTotalDuration());
        }
        return rv;
    }

    private MappingsStatisticsType toMappingsStatisticsType() {
        final MappingsStatisticsType rv = new MappingsStatisticsType();
        final Map<String,Integer> entriesPerType = new HashMap<>();
        for (MappingsStatisticsKey key: mappingsData.keySet()) {
            Integer current = entriesPerType.get(key.getObjectType());
            entriesPerType.put(key.getObjectType(), current != null ? current+1 : 1);
        }
        for (Map.Entry<MappingsStatisticsKey, GenericStatisticsData> entry : mappingsData.entrySet()) {
            final MappingsStatisticsKey key = entry.getKey();
            final String targetEntryName;
            if (entriesPerType.get(key.getObjectType()) < AGGREGATION_THRESHOLD) {
                targetEntryName = key.getObjectName();
            } else {
                targetEntryName = key.getObjectType() + " (aggregated)";
            }
            MappingsStatisticsEntryType entryType = findMappingsEntryType(rv.getEntry(), targetEntryName);
            if (entryType == null) {
                entryType = new MappingsStatisticsEntryType();
                entryType.setObject(targetEntryName);
                rv.getEntry().add(entryType);
            }
            setValueMapping(entryType, entry.getValue().getCount(),
                    entry.getValue().getMinDuration(), entry.getValue().getMaxDuration(), entry.getValue().getTotalDuration());
        }
        return rv;
    }

    private void setValueNotifications(NotificationsStatisticsEntryType e, boolean success, int count, long min, long max, long totalDuration) {
        if (success) {
            e.setCountSuccess(e.getCountSuccess() + count);
        } else {
            e.setCountFailure(e.getCountFailure() + count);
        }

        if (e.getMinTime() == null || min < e.getMinTime()) {
            e.setMinTime(min);
        }
        if (e.getMaxTime() == null || max > e.getMaxTime()) {
            e.setMaxTime(max);
        }
        e.setTotalTime(e.getTotalTime() + totalDuration);
    }

    private void setValueMapping(MappingsStatisticsEntryType e, int count, long min, long max, long totalDuration) {
        e.setCount(e.getCount() + count);
        if (e.getMinTime() == null || min < e.getMinTime()) {
            e.setMinTime(min);
        }
        if (e.getMaxTime() == null || max > e.getMaxTime()) {
            e.setMaxTime(max);
        }
        e.setTotalTime(e.getTotalTime() + totalDuration);
    }

    private EnvironmentalPerformanceInformationType aggregate(EnvironmentalPerformanceInformationType startValue, EnvironmentalPerformanceInformationType delta) {
        if (startValue == null) {
            return delta;
        }
        EnvironmentalPerformanceInformationType rv = new EnvironmentalPerformanceInformationType();
        addTo(rv, startValue);
        addTo(rv, delta);
        return rv;
    }

    public static void addTo(EnvironmentalPerformanceInformationType rv, EnvironmentalPerformanceInformationType delta) {
        addProvisioningTo(rv, delta.getProvisioningStatistics());
        addMappingsTo(rv, delta.getMappingsStatistics());
        addNotificationsTo(rv, delta.getNotificationsStatistics());
        if (delta.getLastMessageTimestamp() != null) {
            if (rv.getLastMessageTimestamp() == null || rv.getLastMessageTimestamp().compare(delta.getLastMessageTimestamp()) == DatatypeConstants.LESSER) {
                rv.setLastMessageTimestamp(delta.getLastMessageTimestamp());
                rv.setLastMessage(delta.getLastMessage());
            }
        }
    }

    private static void addNotificationsTo(EnvironmentalPerformanceInformationType rv, NotificationsStatisticsType delta) {
        if (delta == null) {
            return;
        }
        if (rv.getNotificationsStatistics() == null) {
            rv.setNotificationsStatistics(delta.clone());
            return;
        }

        NotificationsStatisticsType rvNST = rv.getNotificationsStatistics();
        for (NotificationsStatisticsEntryType de : delta.getEntry()) {
            String transport = de.getTransport();
            NotificationsStatisticsEntryType e = findNotificationsEntryType(rvNST.getEntry(), transport);
            if (e == null) {
                e = new NotificationsStatisticsEntryType();
                e.setTransport(transport);
                rvNST.getEntry().add(e);
            }
            e.setCountSuccess(e.getCountSuccess() + de.getCountSuccess());
            e.setCountFailure(e.getCountFailure() + de.getCountFailure());
            int count = e.getCountSuccess() + e.getCountFailure();
            e.setMinTime(min(e.getMinTime(), de.getMinTime()));
            e.setMaxTime(max(e.getMaxTime(), de.getMaxTime()));
            e.setTotalTime(e.getTotalTime() + de.getTotalTime());
            if (count > 0) {
                e.setAverageTime(e.getTotalTime() / count);
            } else {
                e.setAverageTime(null);
            }
        }
    }

    private static NotificationsStatisticsEntryType findNotificationsEntryType(List<NotificationsStatisticsEntryType> list, String transport) {
        for (NotificationsStatisticsEntryType entry : list) {
            if (StringUtils.equals(entry.getTransport(), transport)) {
                return entry;
            }
        }
        return null;
    }


    private static void addMappingsTo(EnvironmentalPerformanceInformationType rv, MappingsStatisticsType delta) {
        if (delta == null) {
            return;
        }
        if (rv.getMappingsStatistics() == null) {
            rv.setMappingsStatistics(delta.clone());
            return;
        }

        MappingsStatisticsType rvMST = rv.getMappingsStatistics();
        for (MappingsStatisticsEntryType de : delta.getEntry()) {
            String object = de.getObject();
            MappingsStatisticsEntryType e = findMappingsEntryType(rvMST.getEntry(), object);
            if (e == null) {
                e = new MappingsStatisticsEntryType();
                e.setObject(object);
                rvMST.getEntry().add(e);
            }
            e.setCount(e.getCount() + de.getCount());
            e.setMinTime(min(e.getMinTime(), de.getMinTime()));
            e.setMaxTime(max(e.getMaxTime(), de.getMaxTime()));
            e.setTotalTime(e.getTotalTime() + de.getTotalTime());
            if (e.getCount() > 0) {
                e.setAverageTime(e.getTotalTime() / e.getCount());
            } else {
                e.setAverageTime(null);
            }
        }
    }

    private static MappingsStatisticsEntryType findMappingsEntryType(List<MappingsStatisticsEntryType> list, String object) {
        for (MappingsStatisticsEntryType lineDto : list) {
            if (StringUtils.equals(lineDto.getObject(), object)) {
                return lineDto;
            }
        }
        return null;
    }

    private static void addProvisioningTo(EnvironmentalPerformanceInformationType rv, ProvisioningStatisticsType delta) {
        if (delta == null) {
            return;
        }
        if (rv.getProvisioningStatistics() == null) {
            rv.setProvisioningStatistics(delta.clone());
            return;
        }
        ProvisioningStatistics.addTo(rv.getProvisioningStatistics(), delta);
    }

    private static Long min(Long a, Long b) {
        if (a == null) {
            return b;
        }
        if (b == null) {
            return a;
        }
        return Math.min(a, b);
    }

    private static Long max(Long a, Long b) {
        if (a == null) {
            return b;
        }
        if (b == null) {
            return a;
        }
        return Math.max(a, b);
    }

    public synchronized void recordProvisioningOperation(String resourceOid, String resourceName, QName objectClassName,
            ProvisioningOperation operation, boolean success, int count, long duration) {
        provisioningStatistics.recordProvisioningOperation(resourceOid, resourceName, objectClassName, operation,
                success, count, duration);
    }

    public synchronized void recordNotificationOperation(String transportName, boolean success, long duration) {
        NotificationsStatisticsKey key = new NotificationsStatisticsKey(transportName, success);
        GenericStatisticsData data = notificationsData.get(key);
        if (data == null) {
            data = new GenericStatisticsData();
            notificationsData.put(key, data);
        }
        data.recordOperation(duration, 1);
    }

    public synchronized void recordMappingOperation(String objectOid, String objectName, String objectTypeName, String mappingName, long duration) {
        // ignoring mapping name for now
        MappingsStatisticsKey key = new MappingsStatisticsKey(objectOid, objectName, objectTypeName);
        GenericStatisticsData data = mappingsData.get(key);
        if (data == null) {
            data = new GenericStatisticsData();
            mappingsData.put(key, data);
        }
        data.recordOperation(duration, 1);
    }

    public synchronized StatusMessage getLastMessage() {
        return lastMessage;
    }

    public synchronized void recordState(String message) {
        lastMessage = new StatusMessage(message);
    }

    public static String format(EnvironmentalPerformanceInformationType information) {
        StringBuilder sb = new StringBuilder();
        if (information.getProvisioningStatistics() != null && !information.getProvisioningStatistics().getEntry().isEmpty()) {
            sb.append("  Provisioning:\n");
            sb.append(ProvisioningStatistics.format(information.getProvisioningStatistics()));
        }
        if (information.getMappingsStatistics() != null && !information.getMappingsStatistics().getEntry().isEmpty()) {
            sb.append("  Mappings:\n");
            sb.append(format(information.getMappingsStatistics()));
        }
        if (information.getNotificationsStatistics() != null && !information.getNotificationsStatistics().getEntry().isEmpty()) {
            sb.append("  Notifications:\n");
            sb.append(format(information.getNotificationsStatistics()));
        }
        if (information.getLastMessage() != null) {
            sb.append("  Last message: ").append(information.getLastMessage()).append("\n");
            sb.append("  On:           ").append(information.getLastMessageTimestamp()).append("\n");
        }
        return sb.toString();
    }

    private static float avg(long totalTime, int count) {
        return count > 0 ? (float) totalTime / count : 0;
    }

    private static String format(MappingsStatisticsType information) {
        StringBuilder sb = new StringBuilder();
        for (MappingsStatisticsEntryType e : information.getEntry()) {
            sb.append(String.format(Locale.US, "    %-40s count: %6d, total time: %6d ms [min: %5d, max: %5d, avg: %7.1f]\n", e.getObject(),
                    e.getCount(), e.getTotalTime(), defaultIfNull(e.getMinTime(), 0L),
                    defaultIfNull(e.getMaxTime(), 0L), avg(e.getTotalTime(), e.getCount())));
        }
        return sb.toString();
    }

    private static String format(NotificationsStatisticsType information) {
        StringBuilder sb = new StringBuilder();
        for (NotificationsStatisticsEntryType e : information.getEntry()) {
            sb.append(String.format(Locale.US, "    %-30s success: %6d, failure: %5d, total time: %6d ms [min: %5d, max: %5d, avg: %7.1fd]\n", e.getTransport(),
                    e.getCountSuccess(), e.getCountFailure(), e.getTotalTime(), defaultIfNull(e.getMinTime(), 0L),
                    defaultIfNull(e.getMaxTime(), 0L), avg(e.getTotalTime(), e.getCountSuccess() + e.getCountFailure())));
        }
        return sb.toString();
    }


}
