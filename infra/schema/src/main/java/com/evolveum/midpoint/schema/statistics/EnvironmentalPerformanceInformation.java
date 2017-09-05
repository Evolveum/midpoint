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

package com.evolveum.midpoint.schema.statistics;

import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.EnvironmentalPerformanceInformationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MappingsStatisticsEntryType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MappingsStatisticsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.NotificationsStatisticsEntryType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.NotificationsStatisticsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ProvisioningStatisticsEntryType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ProvisioningStatisticsType;
import org.apache.commons.lang.StringUtils;

import javax.xml.datatype.DatatypeConstants;
import javax.xml.namespace.QName;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Pavol Mederly
 */
public class EnvironmentalPerformanceInformation {

    /*
     * Thread safety: Instances of this class may be accessed from more than one thread at once.
     * Updates are invoked in the context of the thread executing the task.
     * Queries are invoked either from this thread, or from some observer (task manager or GUI thread).
     *
     * We ensure synchronization by making public methods synchronized. We don't expect much contention on this.
     */
    private final EnvironmentalPerformanceInformationType startValue;        // this object is concurrently read (that is thread-safe), not written

    private Map<ProvisioningStatisticsKey,ProvisioningStatisticsData> provisioningData = new HashMap<>();
    private Map<NotificationsStatisticsKey,GenericStatisticsData> notificationsData = new HashMap<>();
    private Map<MappingsStatisticsKey,GenericStatisticsData> mappingsData = new HashMap<>();

	private static final int AGGREGATION_THRESHOLD = 50;

    private StatusMessage lastMessage;

    public EnvironmentalPerformanceInformation(EnvironmentalPerformanceInformationType value) {
        startValue = value;
    }

    public EnvironmentalPerformanceInformation() {
        this(null);
    }

    public EnvironmentalPerformanceInformationType getStartValue() {
        return startValue;
    }

    public synchronized EnvironmentalPerformanceInformationType getDeltaValue() {
        EnvironmentalPerformanceInformationType rv = toEnvironmentalPerformanceInformationType();
        return rv;
    }

    public synchronized EnvironmentalPerformanceInformationType getAggregatedValue() {
        EnvironmentalPerformanceInformationType delta = toEnvironmentalPerformanceInformationType();
        EnvironmentalPerformanceInformationType rv = aggregate(startValue, delta);
        return rv;
    }

    private EnvironmentalPerformanceInformationType toEnvironmentalPerformanceInformationType() {
        EnvironmentalPerformanceInformationType rv = new EnvironmentalPerformanceInformationType();
        rv.setProvisioningStatistics(toProvisioningStatisticsType());
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
        if (notificationsData == null) {
            return rv;
        }
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
        if (mappingsData == null) {
            return rv;
        }
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

    private ProvisioningStatisticsType toProvisioningStatisticsType() {
        ProvisioningStatisticsType rv = new ProvisioningStatisticsType();
        if (provisioningData == null) {
            return rv;
        }
        for (Map.Entry<ProvisioningStatisticsKey, ProvisioningStatisticsData> entry : provisioningData.entrySet()) {
            ProvisioningStatisticsKey key = entry.getKey();
            String resource = key.getResourceName();
            QName oc = key.getObjectClass();
            ProvisioningStatisticsEntryType entryType = findProvisioningEntryType(rv.getEntry(), resource, oc);
            if (entryType == null) {
                entryType = new ProvisioningStatisticsEntryType();
                entryType.setResource(resource);
                entryType.setObjectClass(oc);
                rv.getEntry().add(entryType);
            }
            setValue(entryType, key.getOperation(), key.getStatusType(), entry.getValue().getCount(),
                    entry.getValue().getMinDuration(), entry.getValue().getMaxDuration(), entry.getValue().getTotalDuration());
        }
        return rv;
    }

    private static ProvisioningStatisticsEntryType findProvisioningEntryType(List<ProvisioningStatisticsEntryType> list, String resource, QName objectClass) {
        for (ProvisioningStatisticsEntryType entryType : list) {
            if (StringUtils.equals(entryType.getResource(), resource) && QNameUtil.match(entryType.getObjectClass(), objectClass)) {
                return entryType;
            }
        }
        return null;
    }

    private void setValue(ProvisioningStatisticsEntryType e, ProvisioningOperation operation, ProvisioningStatusType statusType, int count, long min, long max, long totalDuration) {
        switch (operation) {
            case ICF_GET:
                if (statusType == ProvisioningStatusType.SUCCESS) {
                    e.setGetSuccess(e.getGetSuccess() + count);
                } else {
                    e.setGetFailure(e.getGetFailure() + count);
                }
                break;
            case ICF_SEARCH:
                if (statusType == ProvisioningStatusType.SUCCESS) {
                    e.setSearchSuccess(e.getSearchSuccess() + count);
                } else {
                    e.setSearchFailure(e.getSearchFailure() + count);
                }
                break;
            case ICF_CREATE:
                if (statusType == ProvisioningStatusType.SUCCESS) {
                    e.setCreateSuccess(e.getCreateSuccess() + count);
                } else {
                    e.setCreateFailure(e.getCreateFailure() + count);
                }
                break;
            case ICF_UPDATE:
                if (statusType == ProvisioningStatusType.SUCCESS) {
                    e.setUpdateSuccess(e.getUpdateSuccess() + count);
                } else {
                    e.setUpdateFailure(e.getUpdateFailure() + count);
                }
                break;
            case ICF_DELETE:
                if (statusType == ProvisioningStatusType.SUCCESS) {
                    e.setDeleteSuccess(e.getDeleteSuccess() + count);
                } else {
                    e.setDeleteFailure(e.getDeleteFailure() + count);
                }
                break;
            case ICF_SYNC:
                if (statusType == ProvisioningStatusType.SUCCESS) {
                    e.setSyncSuccess(e.getSyncSuccess() + count);
                } else {
                    e.setSyncFailure(e.getSyncFailure() + count);
                }
                break;
            case ICF_SCRIPT:
                if (statusType == ProvisioningStatusType.SUCCESS) {
                    e.setScriptSuccess(e.getScriptSuccess() + count);
                } else {
                    e.setScriptFailure(e.getScriptFailure() + count);
                }
                break;
            case ICF_GET_LATEST_SYNC_TOKEN:
            case ICF_GET_SCHEMA:
                if (statusType == ProvisioningStatusType.SUCCESS) {
                    e.setOtherSuccess(e.getOtherSuccess() + count);
                } else {
                    e.setOtherFailure(e.getOtherFailure() + count);
                }
                break;
            default:
                throw new IllegalArgumentException("Illegal operation name: " + operation);
        }
        if (e.getMinTime() == null || min < e.getMinTime()) {
            e.setMinTime(min);
        }
        if (e.getMaxTime() == null || max > e.getMaxTime()) {
            e.setMaxTime(max);
        }
        e.setTotalTime(e.getTotalTime() + totalDuration);
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

        ProvisioningStatisticsType rvPST = rv.getProvisioningStatistics();
        for (ProvisioningStatisticsEntryType de : delta.getEntry()) {
            String resource = de.getResource();
            QName oc = de.getObjectClass();
            ProvisioningStatisticsEntryType e = findProvisioningEntryType(rvPST.getEntry(), resource, oc);
            if (e == null) {
                e = new ProvisioningStatisticsEntryType();
                e.setResource(resource);
                e.setObjectClass(oc);
                rvPST.getEntry().add(e);
            }
            e.setGetSuccess(e.getGetSuccess() + de.getGetSuccess());
            e.setSearchSuccess(e.getSearchSuccess() + de.getSearchSuccess());
            e.setCreateSuccess(e.getCreateSuccess() + de.getCreateSuccess());
            e.setUpdateSuccess(e.getUpdateSuccess() + de.getUpdateSuccess());
            e.setDeleteSuccess(e.getDeleteSuccess() + de.getDeleteSuccess());
            e.setSyncSuccess(e.getSyncSuccess() + de.getSyncSuccess());
            e.setScriptSuccess(e.getScriptSuccess() + de.getScriptSuccess());
            e.setOtherSuccess(e.getOtherSuccess() + de.getOtherSuccess());

            e.setGetFailure(e.getGetFailure() + de.getGetFailure());
            e.setSearchFailure(e.getSearchFailure() + de.getSearchFailure());
            e.setCreateFailure(e.getCreateFailure() + de.getCreateFailure());
            e.setUpdateFailure(e.getUpdateFailure() + de.getUpdateFailure());
            e.setDeleteFailure(e.getDeleteFailure() + de.getDeleteFailure());
            e.setSyncFailure(e.getSyncFailure() + de.getSyncFailure());
            e.setScriptFailure(e.getScriptFailure() + de.getScriptFailure());
            e.setOtherFailure(e.getOtherFailure() + de.getOtherFailure());

            int totalCount = e.getGetSuccess() + e.getGetFailure() +
                    e.getSearchSuccess() + e.getSearchFailure() +
                    e.getCreateSuccess() + e.getCreateFailure() +
                    e.getUpdateSuccess() + e.getUpdateFailure() +
                    e.getDeleteSuccess() + e.getDeleteFailure() +
                    e.getSyncSuccess() + e.getSyncFailure() +
                    e.getScriptSuccess() + e.getScriptFailure() +
                    e.getOtherSuccess() + e.getOtherFailure();

            e.setMinTime(min(e.getMinTime(), de.getMinTime()));
            e.setMaxTime(max(e.getMaxTime(), de.getMaxTime()));
            e.setTotalTime(e.getTotalTime() + de.getTotalTime());
            if (totalCount > 0) {
                e.setAverageTime(e.getTotalTime() / totalCount);
            } else {
                e.setAverageTime(null);
            }
        }
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

    public synchronized void recordProvisioningOperation(String resourceOid, String resourceName, QName objectClassName, ProvisioningOperation operation, boolean success, int count, long duration) {
        ProvisioningStatisticsKey key = new ProvisioningStatisticsKey(resourceOid, resourceName, objectClassName, operation, success);
        ProvisioningStatisticsData data = provisioningData.get(key);
        if (data == null) {
            data = new ProvisioningStatisticsData();
            provisioningData.put(key, data);
        }
        data.recordOperation(duration, count);
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
}
