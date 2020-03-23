/*
 * Copyright (c) 2010-2015 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.statistics;

import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectActionsExecutedEntryType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActionsExecutedInformationType;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.Nullable;

import javax.xml.datatype.DatatypeConstants;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;
import java.util.*;

/**
 * @author Pavol Mederly
 */
public class ActionsExecutedInformation {

    /*
     * Thread safety: Just like EnvironmentalPerformanceInformation, instances of this class may be accessed from
     * more than one thread at once. Updates are invoked in the context of the thread executing the task.
     * Queries are invoked either from this thread, or from some observer (task manager or GUI thread).
     */

    private final ActionsExecutedInformationType startValue;

    // Note: key-related fields in value objects (objectType, channel, ...) are filled-in but ignored in the following two maps
    // allObjectActions: all executed actions
    private Map<ActionsExecutedObjectsKey,ObjectActionsExecutedEntryType> allObjectActions = new HashMap<>();
    // resultingObjectActions: "cleaned up" actions - i.e. if an object is added and then modified (in one "logical" operation),
    // we count it as 1xADD
    private Map<ActionsExecutedObjectsKey,ObjectActionsExecutedEntryType> resultingObjectActions = new HashMap<>();

    // operations executed in the current scope: we "clean them up" when markObjectActionExecutedBoundary is called
    // (indexed by object oid)
    private Map<String,List<ObjectActionExecuted>> currentScopeObjectActions = new HashMap<>();

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
        return rv;
    }

    public synchronized ActionsExecutedInformationType getAggregatedValue() {
        ActionsExecutedInformationType delta = toActionsExecutedInformationType();
        ActionsExecutedInformationType rv = aggregate(startValue, delta);
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

    public static void addTo(ActionsExecutedInformationType sum, @Nullable ActionsExecutedInformationType delta) {
        if (delta != null) {
            addTo(sum.getObjectActionsEntry(), delta.getObjectActionsEntry());
            addTo(sum.getResultingObjectActionsEntry(), delta.getResultingObjectActionsEntry());
        }
    }

    public static void addTo(List<ObjectActionsExecutedEntryType> sumEntries, List<ObjectActionsExecutedEntryType> deltaEntries) {
        for (ObjectActionsExecutedEntryType entry : deltaEntries) {
            ObjectActionsExecutedEntryType matchingEntry = findEntry(sumEntries, entry);
            if (matchingEntry != null) {
                addToEntry(matchingEntry, entry);
            } else {
                sumEntries.add(entry.clone());
            }
        }
    }

    private static void addToEntry(ObjectActionsExecutedEntryType sum, ObjectActionsExecutedEntryType delta) {
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

    private static ObjectActionsExecutedEntryType findEntry(List<ObjectActionsExecutedEntryType> entries, ObjectActionsExecutedEntryType entry) {
        for (ObjectActionsExecutedEntryType e : entries) {
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
        mapToJaxb(allObjectActions, rv.getObjectActionsEntry());
        mapToJaxb(resultingObjectActions, rv.getResultingObjectActionsEntry());
    }

    private void mapToJaxb(Map<ActionsExecutedObjectsKey, ObjectActionsExecutedEntryType> map, List<ObjectActionsExecutedEntryType> list) {
        for (Map.Entry<ActionsExecutedObjectsKey,ObjectActionsExecutedEntryType> entry : map.entrySet()) {
            ObjectActionsExecutedEntryType e = entry.getValue().clone();
            e.setObjectType(entry.getKey().getObjectType());
            e.setOperation(ChangeType.toChangeTypeType(entry.getKey().getOperation()));
            e.setChannel(entry.getKey().getChannel());
            list.add(e);
        }
    }

    public synchronized void recordObjectActionExecuted(String objectName, String objectDisplayName, QName objectType, String objectOid, ChangeType changeType, String channel, Throwable exception) {
        XMLGregorianCalendar now = XmlTypeConverter.createXMLGregorianCalendar(new Date());
        ObjectActionExecuted action = new ObjectActionExecuted(objectName, objectDisplayName, objectType, objectOid, changeType, channel, exception, now);

        addEntry(allObjectActions, action);

        if (action.objectOid == null) {
            action.objectOid = "dummy-" + ((int) Math.random() * 10000000);     // hack for unsuccessful ADDs
        }
        addAction(action);
    }

    protected void addAction(ObjectActionExecuted action) {
        List<ObjectActionExecuted> actions = currentScopeObjectActions.get(action.objectOid);
        if (actions == null) {
            actions = new ArrayList<>();
            currentScopeObjectActions.put(action.objectOid, actions);
        }
        actions.add(action);
    }

    protected void addEntry(Map<ActionsExecutedObjectsKey,ObjectActionsExecutedEntryType> target, ObjectActionExecuted a) {
        ActionsExecutedObjectsKey key = new ActionsExecutedObjectsKey(a.objectType, a.changeType, a.channel);
        ObjectActionsExecutedEntryType entry = target.get(key);
        if (entry == null) {
            entry = new ObjectActionsExecutedEntryType();
            target.put(key, entry);
        }

        if (a.exception == null) {
            entry.setTotalSuccessCount(entry.getTotalSuccessCount() + 1);
            entry.setLastSuccessObjectName(a.objectName);
            entry.setLastSuccessObjectDisplayName(a.objectDisplayName);
            entry.setLastSuccessObjectOid(a.objectOid);
            entry.setLastSuccessTimestamp(a.timestamp);
        } else {
            entry.setTotalFailureCount(entry.getTotalFailureCount() + 1);
            entry.setLastFailureObjectName(a.objectName);
            entry.setLastFailureObjectDisplayName(a.objectDisplayName);
            entry.setLastFailureObjectOid(a.objectOid);
            entry.setLastFailureTimestamp(a.timestamp);
            entry.setLastFailureExceptionMessage(a.exception.getMessage());
        }
    }

    public synchronized void markObjectActionExecutedBoundary() {
        for (Map.Entry<String,List<ObjectActionExecuted>> entry : currentScopeObjectActions.entrySet()) {
            // Last non-modify operation determines the result
            List<ObjectActionExecuted> actions = entry.getValue();
            assert actions.size() > 0;
            int relevant;
            for (relevant = actions.size()-1; relevant>=0; relevant--) {
                if (actions.get(relevant).changeType != ChangeType.MODIFY) {
                    break;
                }
            }
            if (relevant < 0) {
                // all are "modify" -> take any (we currently don't care whether successful or not; TODO fix this)
                ObjectActionExecuted actionExecuted = actions.get(actions.size()-1);
                addEntry(resultingObjectActions, actionExecuted);
            } else {
                addEntry(resultingObjectActions, actions.get(relevant));
            }
        }
        currentScopeObjectActions.clear();
    }

    private static class ObjectActionExecuted {
        final String objectName;
        final String objectDisplayName;
        final QName objectType;
        String objectOid;
        final ChangeType changeType;
        final String channel;
        final Throwable exception;
        final XMLGregorianCalendar timestamp;

        ObjectActionExecuted(String objectName, String objectDisplayName, QName objectType, String objectOid, ChangeType changeType, String channel, Throwable exception, XMLGregorianCalendar timestamp) {
            this.objectName = objectName;
            this.objectDisplayName = objectDisplayName;
            this.objectType = objectType;
            this.objectOid = objectOid;
            this.changeType = changeType;
            this.channel = channel;
            this.exception = exception;
            this.timestamp = timestamp;
        }
    }

    public static String format(ActionsExecutedInformationType information) {
        StringBuilder sb = new StringBuilder();
        sb.append("  All object actions:\n");
        for (ObjectActionsExecutedEntryType a : information.getObjectActionsEntry()) {
            formatActionExecuted(sb, a);
        }
        sb.append("  Resulting object actions:\n");
        for (ObjectActionsExecutedEntryType a : information.getResultingObjectActionsEntry()) {
            formatActionExecuted(sb, a);
        }
        return sb.toString();
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
