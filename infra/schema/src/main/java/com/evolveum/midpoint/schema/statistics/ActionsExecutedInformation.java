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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.xml.datatype.DatatypeConstants;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;
import java.util.*;

/**
 * Thread safety: Just like EnvironmentalPerformanceInformation, instances of this class may be accessed from
 * more than one thread at once. Updates are invoked in the context of the thread executing the task.
 * Queries are invoked either from this thread, or from some observer (task manager or GUI thread).
 *
 * However, the updates should come from a single thread! Otherwise the "actions boundaries" will not be
 * applied correctly.
 */
public class ActionsExecutedInformation {

    public enum Part {
        ALL, RESULTING
    }

    /**
     * Base to which the actual value is computed (actualValue = startValue + content in maps)
     */
    private final ActionsExecutedInformationType startValue;

    /**
     * All executed actions.
     *
     * Note: key-related fields in value objects i.e. in ObjectActionsExecutedEntryType instances (objectType, channel, ...)
     * are ignored in the following two maps.
     */
    private final Map<ActionsExecutedObjectsKey,ObjectActionsExecutedEntryType> allObjectActions = new HashMap<>();

    /**
     * "Cleaned up" actions. For example, if an object is added and then modified in one "logical" operation, we count these
     * two operations as a single ADD operation. This is usually what the user is interested in.
     */
    private final Map<ActionsExecutedObjectsKey,ObjectActionsExecutedEntryType> resultingObjectActions = new HashMap<>();

    /**
     * Actions that occurred as part of current "logical" operation. We assume that this object is used from single thread
     * only, so we simply collect all operations. But we are prepared to use ThreadLocal here, if such a need would arise.
     *
     * We summarize these actions when markObjectActionExecutedBoundary is called.
     */
    private static class CurrentScopeActions {
        // The map is indexed by object oid.
        private final Map<String,List<ObjectActionExecuted>> actionsMap = new HashMap<>();
    }

    private CurrentScopeActions currentScopeActions = new CurrentScopeActions();

    public ActionsExecutedInformation(ActionsExecutedInformationType value) {
        startValue = value != null ? value.clone() : null;
    }

    public synchronized ActionsExecutedInformationType getAggregatedValue() {
        ActionsExecutedInformationType delta = toActionsExecutedInformationType();
        if (startValue == null) {
            return delta;
        } else {
            ActionsExecutedInformationType aggregated = new ActionsExecutedInformationType();
            addTo(aggregated, startValue);
            addTo(aggregated, delta);
            return aggregated;
        }
    }

    public static void addTo(ActionsExecutedInformationType sum, @Nullable ActionsExecutedInformationType delta) {
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
        if (action.objectOid == null) {
            action.objectOid = "dummy-" + ((int) (Math.random() * 10000000));     // hack for unsuccessful ADDs
        }

        addAction(allObjectActions, action);
        addActionToCurrentScope(action);
    }

    private void addActionToCurrentScope(ObjectActionExecuted action) {
        currentScopeActions.actionsMap
                .computeIfAbsent(action.objectOid, k -> new ArrayList<>())
                .add(action);
    }

    private void addAction(Map<ActionsExecutedObjectsKey, ObjectActionsExecutedEntryType> target, ObjectActionExecuted a) {
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
        Map<String, List<ObjectActionExecuted>> actionsMap = currentScopeActions.actionsMap;
        for (Map.Entry<String,List<ObjectActionExecuted>> entry : actionsMap.entrySet()) {
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
                addAction(resultingObjectActions, actionExecuted);
            } else {
                addAction(resultingObjectActions, actions.get(relevant));
            }
        }
        actionsMap.clear();
    }

    private static class ObjectActionExecuted {
        private final String objectName;
        private final String objectDisplayName;
        private final QName objectType;
        private String objectOid;
        private final ChangeType changeType;
        private final String channel;
        private final Throwable exception;
        private final XMLGregorianCalendar timestamp;

        private ObjectActionExecuted(String objectName, String objectDisplayName, QName objectType, String objectOid, ChangeType changeType, String channel, Throwable exception, XMLGregorianCalendar timestamp) {
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

    @NotNull
    public static String format(@NotNull ActionsExecutedInformationType information) {
        return format(information, null);
    }

    @NotNull
    public static String format(@NotNull ActionsExecutedInformationType information, Part part) {
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
