/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.activity.run.state.actions;

import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivityActionsExecutedType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectActionsExecutedEntryType;

import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

class ActionsExecutedSummarizer {

    @NotNull private final Map<String, List<ActionExecuted>> actionsByOid;
    @NotNull private final List<ActionExecuted> allActions;

    ActionsExecutedSummarizer(@NotNull Map<String, List<ActionExecuted>> actionsByOid, @NotNull List<ActionExecuted> allActions) {
        this.actionsByOid = actionsByOid;
        this.allActions = allActions;
    }

    public ActivityActionsExecutedType summarize() {
        // Note: key-related fields in value objects i.e. in ObjectActionsExecutedEntryType instances (objectType, channel, ...)
        // are ignored in the following two maps.
        Map<ActionsExecutedObjectsKey, ObjectActionsExecutedEntryType> allObjectActions = new HashMap<>();
        Map<ActionsExecutedObjectsKey, ObjectActionsExecutedEntryType> resultingObjectActions = new HashMap<>();

        allActions.forEach(action -> addAction(allObjectActions, action));

        for (Map.Entry<String,List<ActionExecuted>> entry : actionsByOid.entrySet()) {
            // Last non-modify operation determines the result
            List<ActionExecuted> actions = entry.getValue();
            assert actions.size() > 0;
            int relevant;
            for (relevant = actions.size()-1; relevant>=0; relevant--) {
                if (actions.get(relevant).changeType != ChangeType.MODIFY) {
                    break;
                }
            }
            if (relevant < 0) {
                // all are "modify" -> take any (we currently don't care whether successful or not; TODO fix this)
                ActionExecuted actionExecuted = actions.get(actions.size()-1);
                addAction(resultingObjectActions, actionExecuted);
            } else {
                addAction(resultingObjectActions, actions.get(relevant));
            }
        }

        ActivityActionsExecutedType rv = new ActivityActionsExecutedType();
        mapToBean(allObjectActions, rv.getObjectActionsEntry());
        mapToBean(resultingObjectActions, rv.getResultingObjectActionsEntry());
        return rv;
    }

    private void addAction(Map<ActionsExecutedObjectsKey, ObjectActionsExecutedEntryType> target, ActionExecuted a) {
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

    private void mapToBean(Map<ActionsExecutedObjectsKey, ObjectActionsExecutedEntryType> map, List<ObjectActionsExecutedEntryType> list) {
        for (Map.Entry<ActionsExecutedObjectsKey,ObjectActionsExecutedEntryType> entry : map.entrySet()) {
            ObjectActionsExecutedEntryType e = entry.getValue().clone();
            e.setObjectType(entry.getKey().getObjectType());
            e.setOperation(ChangeType.toChangeTypeType(entry.getKey().getOperation()));
            e.setChannel(entry.getKey().getChannel());
            list.add(e);
        }
    }
}
