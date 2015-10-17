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

import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActionsExecutedObjectsEntryType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActionsExecutedInformationType;
import org.apache.commons.lang.StringUtils;

import javax.xml.datatype.DatatypeConstants;
import javax.xml.namespace.QName;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Pavol Mederly
 */
public class ActionsExecutedInformation {

    /*
     * Thread safety: Just like OperationalInformation, instances of this class may be accessed from
     * more than one thread at once. Updates are invoked in the context of the thread executing the task.
     * Queries are invoked either from this thread, or from some observer (task manager or GUI thread).
     */

    private final ActionsExecutedInformationType startValue;

    // key-related fields in value objects (objectType, channel, ...) are not used
    private Map<ActionsExecutedObjectsKey,ActionsExecutedObjectsEntryType> data = new HashMap<>();

    public ActionsExecutedInformation(ActionsExecutedInformationType value) {
        startValue = value;
    }

    public ActionsExecutedInformation() {
        this(null);
    }

    public ActionsExecutedInformationType getStartValue() {
        return (ActionsExecutedInformationType) startValue;
    }

    public synchronized ActionsExecutedInformationType getDeltaValue() {
        ActionsExecutedInformationType rv = toActionsExecutedInformationType();
        rv.setTimestamp(XmlTypeConverter.createXMLGregorianCalendar(new Date()));
        return rv;
    }

    public synchronized ActionsExecutedInformationType getAggregatedValue() {
        ActionsExecutedInformationType delta = toActionsExecutedInformationType();
        ActionsExecutedInformationType rv = aggregate(startValue, delta);
        rv.setTimestamp(XmlTypeConverter.createXMLGregorianCalendar(new Date()));
        return rv;
    }

    private ActionsExecutedInformationType aggregate(ActionsExecutedInformationType startValue, ActionsExecutedInformationType delta) {
        if (startValue == null) {
            return delta;
        }
        ActionsExecutedInformationType rv = new ActionsExecutedInformationType();
        addTo(rv, startValue);
        addTo(rv, delta);
        return rv;
    }

    public static void addTo(ActionsExecutedInformationType sum, ActionsExecutedInformationType delta) {
        for (ActionsExecutedObjectsEntryType entry : delta.getObjectsEntry()) {
            ActionsExecutedObjectsEntryType matchingEntry = findEntry(sum, entry);
            if (matchingEntry != null) {
                addToEntry(matchingEntry, entry);
            } else {
                sum.getObjectsEntry().add(entry);
            }
        }
    }

    private static void addToEntry(ActionsExecutedObjectsEntryType sum, ActionsExecutedObjectsEntryType delta) {
        sum.setTotalSuccessCount(sum.getTotalSuccessCount() + delta.getTotalSuccessCount());
        if (delta.getLastSuccessTimestamp() != null && 
                (sum.getLastSuccessTimestamp() == null || delta.getLastSuccessTimestamp().compare(sum.getLastSuccessTimestamp()) == DatatypeConstants.GREATER)) {
            sum.setLastSuccessObjectName(delta.getLastSuccessObjectName());
            sum.setLastSuccessObjectDisplayName(delta.getLastSuccessObjectDisplayName());
            sum.setLastSuccessObjectOid(delta.getLastSuccessObjectOid());
            sum.setLastSuccessTimestamp(delta.getLastSuccessTimestamp());
        }
        sum.setTotalFailureCount(sum.getTotalFailureCount() + delta.getTotalFailureCount());
        if (delta.getLastFailureTimestamp() != null &&
                (sum.getLastFailureTimestamp() == null || delta.getLastFailureTimestamp().compare(sum.getLastFailureTimestamp()) == DatatypeConstants.GREATER)) {
            sum.setLastFailureObjectName(delta.getLastFailureObjectName());
            sum.setLastFailureObjectDisplayName(delta.getLastFailureObjectDisplayName());
            sum.setLastFailureObjectOid(delta.getLastFailureObjectOid());
            sum.setLastFailureTimestamp(delta.getLastFailureTimestamp());
            sum.setLastFailureExceptionMessage(delta.getLastFailureExceptionMessage());
        }
    }

    private static ActionsExecutedObjectsEntryType findEntry(ActionsExecutedInformationType informationType, ActionsExecutedObjectsEntryType entry) {
        for (ActionsExecutedObjectsEntryType e : informationType.getObjectsEntry()) {
            if (entry.getObjectType().equals(e.getObjectType()) &&
                    entry.getOperation().equals(e.getOperation()) &&
                    StringUtils.equals(entry.getChannel(), e.getChannel())) {
                return e;
            }
        }
        return null;
    }

    private ActionsExecutedInformationType toActionsExecutedInformationType() {
        ActionsExecutedInformationType rv = new ActionsExecutedInformationType();
        toJaxb(rv);
        return rv;
    }

    private void toJaxb(ActionsExecutedInformationType rv) {
        for (Map.Entry<ActionsExecutedObjectsKey,ActionsExecutedObjectsEntryType> entry : data.entrySet()) {
            ActionsExecutedObjectsEntryType e = entry.getValue().clone();
            e.setObjectType(entry.getKey().getObjectType());
            e.setOperation(ChangeType.toChangeTypeType(entry.getKey().getOperation()));
            e.setChannel(entry.getKey().getChannel());
            rv.getObjectsEntry().add(e);
        }
    }

    public synchronized void recordObjectActionExecuted(String objectName, String objectDisplayName, QName objectType, String objectOid, ChangeType changeType, String channel, Throwable exception) {

        ActionsExecutedObjectsKey key = new ActionsExecutedObjectsKey(objectType, changeType, channel);
        ActionsExecutedObjectsEntryType entry = data.get(key);
        if (entry == null) {
            entry = new ActionsExecutedObjectsEntryType();
            data.put(key, entry);
        }
        if (exception == null) {
            entry.setTotalSuccessCount(entry.getTotalSuccessCount() + 1);
            entry.setLastSuccessObjectName(objectName);
            entry.setLastSuccessObjectDisplayName(objectDisplayName);
            entry.setLastSuccessObjectOid(objectOid);
            entry.setLastSuccessTimestamp(XmlTypeConverter.createXMLGregorianCalendar(new Date()));
        } else {
            entry.setTotalFailureCount(entry.getTotalFailureCount() + 1);
            entry.setLastFailureObjectName(objectName);
            entry.setLastFailureObjectDisplayName(objectDisplayName);
            entry.setLastFailureObjectOid(objectOid);
            entry.setLastFailureTimestamp(XmlTypeConverter.createXMLGregorianCalendar(new Date()));
            entry.setLastFailureExceptionMessage(exception.getMessage());
        }
    }

}
