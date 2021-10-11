/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.notifications.impl;

import com.evolveum.midpoint.model.api.context.EvaluatedAssignment;
import com.evolveum.midpoint.model.api.context.EvaluatedPolicyRule;
import com.evolveum.midpoint.model.api.context.ModelContext;
import com.evolveum.midpoint.model.api.context.ModelState;
import com.evolveum.midpoint.model.api.hooks.ChangeHook;
import com.evolveum.midpoint.model.api.hooks.HookOperationMode;
import com.evolveum.midpoint.model.api.hooks.HookRegistry;
import com.evolveum.midpoint.model.impl.lens.EvaluatedAssignmentImpl;
import com.evolveum.midpoint.model.impl.lens.LensContext;
import com.evolveum.midpoint.model.impl.lens.LensFocusContext;
import com.evolveum.midpoint.notifications.api.NotificationManager;
import com.evolveum.midpoint.notifications.api.events.ModelEvent;
import com.evolveum.midpoint.notifications.api.events.PolicyRuleEvent;
import com.evolveum.midpoint.notifications.impl.events.BaseEventImpl;
import com.evolveum.midpoint.notifications.api.events.Event;
import com.evolveum.midpoint.notifications.impl.events.ModelEventImpl;
import com.evolveum.midpoint.notifications.impl.events.PolicyRuleEventImpl;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.DeltaSetTriple;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.LightweightIdentifierGenerator;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.NotificationPolicyActionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

/**
 * One of interfaces of the notifier to midPoint.
 *
 * Used to catch user-related events.
 *
 * @author mederly
 */
@Component
public class NotificationHook implements ChangeHook {

    private static final Trace LOGGER = TraceManager.getTrace(NotificationHook.class);

    private static final String HOOK_URI = SchemaConstants.NS_MODEL + "/notification-hook-3";

    private static final String OP_INVOKE = NotificationHook.class.getName() + ".invoke";

    @Autowired private LightweightIdentifierGenerator lightweightIdentifierGenerator;
    @Autowired private HookRegistry hookRegistry;
    @Autowired private NotificationManager notificationManager;
    @Autowired private NotificationFunctionsImpl notificationsUtil;

    @PostConstruct
    public void init() {
        hookRegistry.registerChangeHook(HOOK_URI, this);
        LOGGER.trace("Notifier change hook registered.");
    }

    @Override
    public <O extends ObjectType> HookOperationMode invoke(@NotNull ModelContext<O> context, @NotNull Task task,
            @NotNull OperationResult parentResult) {
        OperationResult result = parentResult.createSubresult(OP_INVOKE);
        try {
            if (context.getState() != ModelState.FINAL) {
                return HookOperationMode.FOREGROUND;
            }
            if (notificationManager.isDisabled()) {
                LOGGER.trace("Notifications are temporarily disabled, exiting the hook.");
                return HookOperationMode.FOREGROUND;
            }
            LOGGER.trace("Notification change hook called with model context:\n{}", context.debugDumpLazily());
            if (context.getFocusContext() == null) {
                LOGGER.trace("Focus context is null, exiting the hook.");
                return HookOperationMode.FOREGROUND;
            }

            emitModelEvent(context, task, result);
            emitPolicyRulesEvents(context, task, result);

            return HookOperationMode.FOREGROUND;
        } catch (Throwable t) {
            result.recordFatalError(t);
            throw t;
        } finally {
            result.computeStatusIfUnknown();
        }
    }

    private void emitPolicyRulesEvents(ModelContext<?> context, Task task, OperationResult result) {
        LensFocusContext<?> focusContext = (LensFocusContext<?>) context.getFocusContext();
        for (EvaluatedPolicyRule rule : focusContext.getPolicyRules()) {
            emitPolicyEventIfPresent(rule, context, task, result);
        }
        DeltaSetTriple<EvaluatedAssignmentImpl<?>> triple = ((LensContext<?>) context).getEvaluatedAssignmentTriple();
        if (triple != null) {
            for (EvaluatedAssignment<?> assignment : triple.getNonNegativeValues()) {
                for (EvaluatedPolicyRule rule : assignment.getAllTargetsPolicyRules()) {
                    emitPolicyEventIfPresent(rule, context, task, result);
                }
            }
        }
    }

    private void emitPolicyEventIfPresent(EvaluatedPolicyRule rule, ModelContext<?> context, Task task, OperationResult result) {
        if (rule.isTriggered()) {
            for (NotificationPolicyActionType notificationAction : rule.getEnabledActions(NotificationPolicyActionType.class)) {
                emitPolicyEvent(notificationAction, rule, context, task, result);
            }
        }
    }

    private void emitPolicyEvent(@SuppressWarnings("unused") NotificationPolicyActionType action, EvaluatedPolicyRule rule,
            ModelContext<?> context, Task task, OperationResult result) {
        PolicyRuleEvent ruleEvent = createRuleEvent(rule, context, task);
        notificationManager.processEvent(ruleEvent, task, result);
    }

    private void emitModelEvent(@NotNull ModelContext<?> context, @NotNull Task task, @NotNull OperationResult result) {
        PrismObject<?> object = getObject(context);
        if (object == null) {
            LOGGER.trace("Focus context object is null, not sending the notification.");
            return;
        }
        ModelEvent event = createModelEvent(object, context, task);
        notificationManager.processEvent(event, task, result);
    }

    private PrismObject<?> getObject(@NotNull ModelContext<?> context) {
        PrismObject<?> object = context.getFocusContext().getObjectNew();
        if (object != null) {
            return object;
        } else {
            return context.getFocusContext().getObjectOld();
        }
    }

    @Override
    public void invokeOnException(@NotNull ModelContext context, @NotNull Throwable throwable, @NotNull Task task, @NotNull OperationResult result) {
        // todo implement this
    }

    @NotNull
    private PolicyRuleEvent createRuleEvent(EvaluatedPolicyRule rule, ModelContext<?> context, Task task) {
        PolicyRuleEventImpl ruleEvent = new PolicyRuleEventImpl(lightweightIdentifierGenerator, rule);
        setCommonEventProperties(getObject(context), task, context, ruleEvent);
        return ruleEvent;
    }


    @NotNull
    private ModelEvent createModelEvent(PrismObject<?> object, ModelContext<?> modelContext, Task task) {
        ModelEventImpl event = new ModelEventImpl(lightweightIdentifierGenerator, modelContext);
        setCommonEventProperties(object, task, modelContext, event);
        // TODO is this correct? it's not quite clear how we work with channel info in task / modelContext
        String channel = task.getChannel();
        if (channel == null) {
            channel = modelContext.getChannel();
        }
        event.setChannel(channel);
        return event;
    }

    private void setCommonEventProperties(PrismObject<?> object, Task task, ModelContext<?> modelContext, Event event) {
        if (task.getOwner() != null) {
            ((BaseEventImpl) event).setRequester(new SimpleObjectRefImpl(notificationsUtil, task.getOwner().asObjectable()));
        } else {
            LOGGER.debug("No owner for task " + task + ", therefore no requester will be set for event " + event.getId());
        }

        // if no OID in object (occurs in 'add' operation), we artificially insert it into the object)
        if (object.getOid() == null && modelContext.getFocusContext() != null && modelContext.getFocusContext().getOid() != null) {
            object = object.clone();
            object.setOid(modelContext.getFocusContext().getOid());
        }
        ((BaseEventImpl) event).setRequestee(new SimpleObjectRefImpl(notificationsUtil, (ObjectType) object.asObjectable()));
    }
}
