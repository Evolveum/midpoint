/*
 * Copyright (c) 2010-2013 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
import com.evolveum.midpoint.notifications.api.events.Event;
import com.evolveum.midpoint.notifications.api.events.ModelEvent;
import com.evolveum.midpoint.notifications.api.events.PolicyRuleEvent;
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

    @Autowired private LightweightIdentifierGenerator lightweightIdentifierGenerator;
    @Autowired private HookRegistry hookRegistry;
    @Autowired private NotificationManager notificationManager;
    @Autowired private NotificationFunctionsImpl notificationsUtil;

    @PostConstruct
    public void init() {
        hookRegistry.registerChangeHook(HOOK_URI, this);
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Notifier change hook registered.");
        }
    }

    @Override
    public HookOperationMode invoke(@NotNull ModelContext context, @NotNull Task task, @NotNull OperationResult result) {

        // todo in the future we should perhaps act in POSTEXECUTION state, but currently the clockwork skips this state
        if (context.getState() != ModelState.FINAL) {
            return HookOperationMode.FOREGROUND;
        }
        if (notificationManager.isDisabled()) {
            LOGGER.trace("Notifications are temporarily disabled, exiting the hook.");
            return HookOperationMode.FOREGROUND;
        }
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Notification change hook called with model context: " + context.debugDump());
        }
        if (context.getFocusContext() == null) {
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("Focus context is null, exiting the hook.");
            }
            return HookOperationMode.FOREGROUND;
        }

		emitModelEvent(context, task, result);
		emitPolicyRulesEvents(context, task, result);

        return HookOperationMode.FOREGROUND;
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
		if (rule.isTriggered() && rule.getActions() != null && rule.getActions().getNotification() != null) {
			emitPolicyEvent(rule.getActions().getNotification(), rule, context, task, result);
		}
	}

	@SuppressWarnings("unused")
	private void emitPolicyEvent(NotificationPolicyActionType action, EvaluatedPolicyRule rule,
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

	private PrismObject getObject(@NotNull ModelContext context) {
		PrismObject object = context.getFocusContext().getObjectNew();
		if (object == null) {
			object = context.getFocusContext().getObjectOld();
		}
		return object;
	}

	@Override
    public void invokeOnException(@NotNull ModelContext context, @NotNull Throwable throwable, @NotNull Task task, @NotNull OperationResult result) {
        // todo implement this
    }

	@NotNull
	private PolicyRuleEvent createRuleEvent(EvaluatedPolicyRule rule, ModelContext<?> context, Task task) {
		PolicyRuleEvent ruleEvent = new PolicyRuleEvent(lightweightIdentifierGenerator, rule);
		setCommonEventProperties(getObject(context), task, context, ruleEvent);
		return ruleEvent;
	}


	@NotNull
    private ModelEvent createModelEvent(PrismObject<?> object, ModelContext<?> modelContext, Task task) {
        ModelEvent event = new ModelEvent(lightweightIdentifierGenerator, modelContext);
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
			event.setRequester(new SimpleObjectRefImpl(notificationsUtil, task.getOwner().asObjectable()));
		} else {
			LOGGER.debug("No owner for task " + task + ", therefore no requester will be set for event " + event.getId());
		}

		// if no OID in object (occurs in 'add' operation), we artificially insert it into the object)
		if (object.getOid() == null && modelContext.getFocusContext() != null && modelContext.getFocusContext().getOid() != null) {
			object = object.clone();
			object.setOid(modelContext.getFocusContext().getOid());
		}
		event.setRequestee(new SimpleObjectRefImpl(notificationsUtil, (ObjectType) object.asObjectable()));
	}
}
