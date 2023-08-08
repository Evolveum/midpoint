/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.util.cases;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.schema.SchemaRegistry;
import com.evolveum.midpoint.prism.util.CloneUtil;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.xml.datatype.Duration;
import javax.xml.datatype.XMLGregorianCalendar;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

/**
 * TODO is this the correct place?
 */
public class CaseTriggeringUtil {

    public static @NotNull List<TriggerType> createTriggers(
            int escalationLevel,
            Date workItemCreateTime,
            Date workItemDeadline,
            Collection<WorkItemTimedActionsType> timedActionsCollection,
            PrismContext prismContext,
            Trace logger,
            @Nullable Long workItemId,
            @NotNull String handlerUri)
            throws SchemaException {
        List<TriggerType> triggers = new ArrayList<>();
        for (WorkItemTimedActionsType timedActionsEntry : timedActionsCollection) {
            Integer levelFrom;
            Integer levelTo;
            if (timedActionsEntry.getEscalationLevelFrom() == null && timedActionsEntry.getEscalationLevelTo() == null) {
                levelFrom = levelTo = 0;
            } else {
                levelFrom = timedActionsEntry.getEscalationLevelFrom();
                levelTo = timedActionsEntry.getEscalationLevelTo();
            }
            if (levelFrom != null && escalationLevel < levelFrom) {
                logger.trace("Current escalation level is before 'escalationFrom', skipping timed actions {}", timedActionsEntry);
                continue;
            }
            if (levelTo != null && escalationLevel > levelTo) {
                logger.trace("Current escalation level is after 'escalationTo', skipping timed actions {}", timedActionsEntry);
                continue;
            }
            // TODO evaluate the condition
            List<TimedActionTimeSpecificationType> timeSpecifications = CloneUtil.cloneCollectionMembers(timedActionsEntry.getTime());
            if (timeSpecifications.isEmpty()) {
                timeSpecifications.add(new TimedActionTimeSpecificationType());
            }
            for (TimedActionTimeSpecificationType timeSpec : timeSpecifications) {
                if (timeSpec.getValue().isEmpty()) {
                    timeSpec.getValue().add(XmlTypeConverter.createDuration(0));
                }
                for (Duration duration : timeSpec.getValue()) {
                    XMLGregorianCalendar mainTriggerTime = computeTriggerTime(duration, timeSpec.getBase(),
                            workItemCreateTime, workItemDeadline);
                    TriggerType mainTrigger = createTrigger(mainTriggerTime, timedActionsEntry.getActions(), null, prismContext, workItemId, handlerUri);
                    triggers.add(mainTrigger);
                    List<Pair<Duration, AbstractWorkItemActionType>> notifyInfoList = getNotifyBefore(timedActionsEntry);
                    for (Pair<Duration, AbstractWorkItemActionType> notifyInfo : notifyInfoList) {
                        XMLGregorianCalendar notifyTime = (XMLGregorianCalendar) mainTriggerTime.clone();
                        notifyTime.add(notifyInfo.getKey().negate());
                        TriggerType notifyTrigger = createTrigger(notifyTime, null, notifyInfo, prismContext, workItemId, handlerUri);
                        triggers.add(notifyTrigger);
                    }
                }
            }
        }
        return triggers;
    }

    @NotNull
    private static TriggerType createTrigger(
            XMLGregorianCalendar triggerTime,
            WorkItemActionsType actions,
            Pair<Duration, AbstractWorkItemActionType> notifyInfo,
            PrismContext prismContext,
            Long workItemId,
            @NotNull String handlerUri)
            throws SchemaException {
        TriggerType trigger = new TriggerType();
        trigger.setTimestamp(triggerTime);
        trigger.setHandlerUri(handlerUri);
        ExtensionType extension = new ExtensionType();
        trigger.setExtension(extension);

        SchemaRegistry schemaRegistry = prismContext.getSchemaRegistry();
        if (workItemId != null) {
            // work item id
            @SuppressWarnings("unchecked")
            @NotNull PrismPropertyDefinition<Long> workItemIdDef =
                    prismContext.getSchemaRegistry().findPropertyDefinitionByElementName(SchemaConstants.MODEL_EXTENSION_WORK_ITEM_ID);
            PrismProperty<Long> workItemIdProp = workItemIdDef.instantiate();
            workItemIdProp.addRealValue(workItemId);
            //noinspection unchecked
            trigger.getExtension().asPrismContainerValue().add(workItemIdProp);
        }
        // actions
        if (actions != null) {
            @NotNull PrismContainerDefinition<WorkItemActionsType> workItemActionsDef =
                    schemaRegistry.findContainerDefinitionByElementName(SchemaConstants.MODEL_EXTENSION_WORK_ITEM_ACTIONS);
            PrismContainer<WorkItemActionsType> workItemActionsCont = workItemActionsDef.instantiate();
            //noinspection unchecked
            workItemActionsCont.add(actions.asPrismContainerValue().clone());
            //noinspection unchecked
            extension.asPrismContainerValue().add(workItemActionsCont);
        }
        // time before + action
        if (notifyInfo != null) {
            @NotNull PrismContainerDefinition<AbstractWorkItemActionType> workItemActionDef =
                    schemaRegistry.findContainerDefinitionByElementName(SchemaConstants.MODEL_EXTENSION_WORK_ITEM_ACTION);
            PrismContainer<AbstractWorkItemActionType> workItemActionCont = workItemActionDef.instantiate();
            //noinspection unchecked
            workItemActionCont.add(notifyInfo.getValue().asPrismContainerValue().clone());
            //noinspection unchecked
            extension.asPrismContainerValue().add(workItemActionCont);
            @SuppressWarnings("unchecked")
            @NotNull PrismPropertyDefinition<Duration> timeBeforeActionDef =
                    schemaRegistry.findPropertyDefinitionByElementName(SchemaConstants.MODEL_EXTENSION_TIME_BEFORE_ACTION);
            PrismProperty<Duration> timeBeforeActionProp = timeBeforeActionDef.instantiate();
            timeBeforeActionProp.addRealValue(notifyInfo.getKey());
            //noinspection unchecked
            extension.asPrismContainerValue().add(timeBeforeActionProp);
        }
        return trigger;
    }

    private static List<Pair<Duration,AbstractWorkItemActionType>> getNotifyBefore(WorkItemTimedActionsType timedActions) {
        List<Pair<Duration,AbstractWorkItemActionType>> rv = new ArrayList<>();
        WorkItemActionsType actions = timedActions.getActions();
        if (actions.getComplete() != null) {
            collectNotifyBefore(rv, actions.getComplete());
        }
        if (actions.getDelegate() != null) {
            collectNotifyBefore(rv, actions.getDelegate());
        }
        if (actions.getEscalate() != null) {
            collectNotifyBefore(rv, actions.getEscalate());
        }
        return rv;
    }

    private static void collectNotifyBefore(List<Pair<Duration,AbstractWorkItemActionType>> rv, CompleteWorkItemActionType complete) {
        collectNotifyBefore(rv, complete.getNotifyBeforeAction(), complete);
    }

    private static void collectNotifyBefore(List<Pair<Duration,AbstractWorkItemActionType>> rv, DelegateWorkItemActionType delegate) {
        collectNotifyBefore(rv, delegate.getNotifyBeforeAction(), delegate);
    }

    private static void collectNotifyBefore(List<Pair<Duration, AbstractWorkItemActionType>> rv,
            List<Duration> beforeTimes, AbstractWorkItemActionType action) {
        beforeTimes.forEach(beforeTime -> rv.add(new ImmutablePair<>(beforeTime, action)));
    }

    @NotNull
    private static XMLGregorianCalendar computeTriggerTime(Duration duration, WfTimeBaseType base, Date start, Date deadline) {
        Date baseTime;
        if (base == null) {
            base = duration.getSign() <= 0 ? WfTimeBaseType.DEADLINE : WfTimeBaseType.WORK_ITEM_CREATION;
        }
        baseTime = switch (base) {
            case DEADLINE -> {
                if (deadline == null) {
                    throw new IllegalStateException("Couldn't set timed action relative to work item's deadline because"
                            + " the deadline is not set. Requested interval: " + duration);
                }
                yield deadline;
            }
            case WORK_ITEM_CREATION -> {
                if (start == null) {
                    throw new IllegalStateException("Work item create time is null");
                }
                yield start;
            }
        };
        XMLGregorianCalendar rv = XmlTypeConverter.createXMLGregorianCalendar(baseTime);
        rv.add(duration);
        return rv;
    }
}
