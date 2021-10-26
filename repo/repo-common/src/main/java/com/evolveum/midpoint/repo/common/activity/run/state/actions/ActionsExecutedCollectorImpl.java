/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.activity.run.state.actions;

import java.util.*;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.schema.statistics.ActionsExecutedCollector;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

/**
 * Collects actions executed information from a single item processing.
 *
 * Assumptions:
 *
 * 1. Exists during a single item processing only.
 * 2. Executed from a single thread only (the worker task).
 */
public class ActionsExecutedCollectorImpl implements ActionsExecutedCollector {

    /**
     * Actions that occurred as part of current "logical" operation.
     */
    private final Map<String, List<ActionExecuted>> actionsByOid = new HashMap<>();

    /**
     * The same; in the order these were executed (because of determination the last success/failure).
     */
    private final List<ActionExecuted> allActions = new ArrayList<>();

    /**
     * Place where the summarized information is sent at the end.
     */
    @NotNull private final ActivityActionsExecuted activityActionsExecuted;

    public ActionsExecutedCollectorImpl(@NotNull ActivityActionsExecuted activityActionsExecuted) {
        this.activityActionsExecuted = activityActionsExecuted;
    }

    @Override
    public void recordActionExecuted(String objectName, String objectDisplayName, QName objectType, String objectOid,
            ChangeType changeType, String channel, Throwable exception) {
        recordInternal(objectName, objectDisplayName, objectType, objectOid, changeType, channel, exception);
    }

    @Override
    public <T extends ObjectType> void recordActionExecuted(PrismObject<T> object, Class<T> objectTypeClass,
            String defaultOid, ChangeType changeType, String channel, Throwable exception) {
        String name, displayName, oid;
        PrismObjectDefinition<?> definition;
        Class<T> clazz;
        if (object != null) {
            name = PolyString.getOrig(object.getName());
            displayName = ObjectTypeUtil.getDetailedDisplayName(object);
            definition = object.getDefinition();
            clazz = object.getCompileTimeClass();
            oid = object.getOid();
            if (oid == null) { // in case of ADD operation
                oid = defaultOid;
            }
        } else {
            name = null;
            displayName = null;
            definition = null;
            clazz = objectTypeClass;
            oid = defaultOid;
        }
        if (definition == null && clazz != null) {
            definition = PrismContext.get().getSchemaRegistry().findObjectDefinitionByCompileTimeClass(clazz);
        }
        QName typeQName;
        if (definition != null) {
            typeQName = definition.getTypeName();
        } else {
            typeQName = ObjectType.COMPLEX_TYPE;
        }
        recordInternal(name, displayName, typeQName, oid, changeType, channel, exception);
    }

    private void recordInternal(String objectName, String objectDisplayName, QName objectType,
            String objectOid, ChangeType changeType, String channel, Throwable exception) {
        XMLGregorianCalendar now = XmlTypeConverter.createXMLGregorianCalendar(new Date());
        ActionExecuted action = new ActionExecuted(objectName, objectDisplayName, objectType, objectOid,
                changeType, channel, exception, now);
        if (action.objectOid == null) {
            action.objectOid = "dummy-" + ((int) (Math.random() * 10000000));     // hack for unsuccessful ADDs
        }

        actionsByOid
                .computeIfAbsent(action.objectOid, k -> new ArrayList<>())
                .add(action);
        allActions.add(action);
    }

    public synchronized void stop() {
        activityActionsExecuted.add(
                new ActionsExecutedSummarizer(actionsByOid, allActions)
                        .summarize());
    }
}
