/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.model.impl.trigger;

import static java.util.Objects.requireNonNull;

import static com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType.F_TRIGGER;

import java.util.*;
import javax.xml.datatype.DatatypeConstants;
import javax.xml.datatype.XMLGregorianCalendar;

import com.evolveum.midpoint.model.api.trigger.MultipleTriggersHandler;
import com.evolveum.midpoint.model.api.trigger.TriggerHandler;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.ContainerDelta;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.schema.internals.InternalCounters;
import com.evolveum.midpoint.schema.internals.InternalMonitor;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.task.api.RunningTask;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TriggerType;

/**
 * Fires triggers on objects found by the search operation.
 */
public class TriggerScanItemProcessor {

    private static final Trace LOGGER = TraceManager.getTrace(TriggerScanItemProcessor.class);

    /**
     * Triggers that were processed by this handler (during execution of this trigger task).
     * This field could reside also in {@link TriggerScanActivityRun} but here it is closer to the usage site.
     */
    @NotNull private final ProcessedTriggers processedTriggers = new ProcessedTriggers();
    @NotNull private final TriggerScanActivityRun activityRun;

    TriggerScanItemProcessor(@NotNull TriggerScanActivityRun activityRun) {
        this.activityRun = activityRun;
    }

    public boolean processObject(@NotNull ObjectType object, @NotNull RunningTask workerTask,
            @NotNull OperationResult result)
            throws CommonException {
        fireTriggers(object.asPrismObject(), workerTask, result);
        return true;
    }

    private void fireTriggers(@NotNull PrismObject<? extends ObjectType> object, RunningTask workerTask, OperationResult result) {
        PrismContainer<TriggerType> triggerContainer = object.findContainer(F_TRIGGER);
        if (triggerContainer == null) {
            LOGGER.warn("Strange thing, attempt to fire triggers on {}, but it does not have trigger container", object);
            return;
        }
        List<PrismContainerValue<TriggerType>> triggerCValues = triggerContainer.getValues();
        if (triggerCValues.isEmpty()) {
            LOGGER.warn("Strange thing, attempt to fire triggers on {}, but it does not have any triggers in trigger container", object);
            return;
        }
        LOGGER.trace("Firing triggers for {} ({} triggers)", object, triggerCValues.size());
        Set<String> handlersExecuted = new HashSet<>();
        List<TriggerType> triggers = getSortedTriggers(triggerCValues);
        while (!triggers.isEmpty()) {
            TriggerType trigger = triggers.get(0);
            triggers.remove(0);
            XMLGregorianCalendar timestamp = trigger.getTimestamp();
            if (timestamp == null) {
                LOGGER.warn("Trigger without a timestamp in {}", object);
                continue;
            }
            if (!isHot(timestamp)) {
                LOGGER.debug("Trigger {} is not hot (timestamp={}, thisScanTimestamp={}, lastScanTimestamp={}) - skipping also the triggers after that",
                        trigger, timestamp, activityRun.getThisScanTimestamp(), activityRun.getLastScanTimestamp());
                return;
            }
            String handlerUri = trigger.getHandlerUri();
            if (handlerUri == null) {
                LOGGER.warn("Trigger without handler URI in {}", object);
                continue;
            }
            // TODO maybe this check is redundant (we already filter on object OID)
            if (processedTriggers.triggerAlreadySeen(handlerUri, object.getOid(), trigger.getId())) {
                LOGGER.debug("Handler {} already executed for {}:{}", handlerUri, ObjectTypeUtil.toShortString(object), trigger.getId());
                // We don't request the trigger removal here. If the trigger was previously seen and processed correctly,
                // it was already removed. But if it was seen and failed, we want to keep it!
                // (We do want to record it as seen even in that case, as we do not want to re-process it multiple times
                // during single task handler run.)
                continue;
            }
            LOGGER.debug("Going to fire trigger {} in {}: id={}, ts={}", handlerUri, object, trigger.getId(), timestamp);
            TriggerHandler handler = activityRun.getActivityHandler().getTriggerHandler(handlerUri);
            if (handler == null) {
                LOGGER.warn("No registered trigger handler for URI {} in {}", handlerUri, trigger);
                continue;
            }
            if (handler instanceof MultipleTriggersHandler) {
                LOGGER.trace("Finding triggers aggregable with {}; handler = {}", trigger.getId(), handlerUri);
                List<TriggerType> compatibleTriggers = new ArrayList<>();
                compatibleTriggers.add(trigger);
                int i = 0;
                // todo consider relaxing "timestamps equal" condition if declared so by the handler
                while (i < triggers.size() && trigger.getTimestamp().equals(triggers.get(0).getTimestamp())) {
                    TriggerType t = triggers.get(i);
                    // TODO maybe this check is redundant (we already filter on object OID)
                    if (processedTriggers.triggerAlreadySeen(handlerUri, object.getOid(), t.getId())) { // see comment above
                        LOGGER.debug("Handler {} already executed for {}:{}", handlerUri, ObjectTypeUtil.toShortString(object), t.getId());
                        triggers.remove(i);
                    } else if (handlerUri.equals(t.getHandlerUri())) {
                        compatibleTriggers.add(t);
                        triggers.remove(i);
                    } else {
                        i++;
                    }
                }
                LOGGER.trace("Trigger batch has {} members", compatibleTriggers.size());
                compatibleTriggers.forEach(t -> InternalMonitor.recordCount(InternalCounters.TRIGGER_FIRED_COUNT));
                try {
                    Collection<TriggerType> processedTriggers;
                    if (!handler.isIdempotent() || !handlersExecuted.contains(handlerUri)) {
                        processedTriggers = ((MultipleTriggersHandler) handler)
                                .handle(object, compatibleTriggers, workerTask, result);
                        handlersExecuted.add(handlerUri);
                    } else {
                        processedTriggers = compatibleTriggers;
                    }
                    removeTriggers(object, processedTriggers, workerTask, triggerContainer.getDefinition());
                } catch (Throwable e) {
                    LOGGER.error("Multiple triggers handler {} executed on {} thrown an error: {} -- it will be retried", handler,
                            object, e.getMessage(), e);
                    result.recordPartialError(e);
                }
            } else if (handler instanceof SingleTriggerHandler) {
                try {
                    InternalMonitor.recordCount(InternalCounters.TRIGGER_FIRED_COUNT);
                    if (!handler.isIdempotent() || !handlersExecuted.contains(handlerUri)) {
                        ((SingleTriggerHandler) handler).handle(object, trigger, workerTask, result);
                        handlersExecuted.add(handlerUri);
                    }
                    removeTriggers(object, Collections.singleton(trigger), workerTask, triggerContainer.getDefinition());
                } catch (Throwable e) {
                    // Properly handle everything that the handler spits out. We do not want this task to die.
                    LOGGER.error("Trigger handler {} executed on {} thrown an error: {} -- it will be retried", handler,
                            object, e.getMessage(), e);
                    result.recordPartialError(e);
                }
            } else {
                throw new IllegalStateException("Unknown kind of trigger handler: " + handler);
            }
        }
    }

    private List<TriggerType> getSortedTriggers(List<PrismContainerValue<TriggerType>> triggerCValues) {
        List<TriggerType> rv = new ArrayList<>();
        triggerCValues.forEach(cval -> rv.add(cval.clone().asContainerable()));
        rv.sort(Comparator.comparingLong(t -> XmlTypeConverter.toMillis(t.getTimestamp())));
        return rv;
    }

    private boolean isHot(XMLGregorianCalendar timestamp) {
        return activityRun.getThisScanTimestamp().compare(timestamp) != DatatypeConstants.LESSER;
    }

    private void removeTriggers(PrismObject<? extends ObjectType> object, Collection<TriggerType> triggers, Task task,
            PrismContainerDefinition<TriggerType> triggerContainerDef) {
        ContainerDelta<TriggerType> triggerDelta = triggerContainerDef.createEmptyDelta(F_TRIGGER);
        for (TriggerType trigger : triggers) {
            //noinspection unchecked
            triggerDelta.addValueToDelete(trigger.asPrismContainerValue().clone());
        }
        Collection<? extends ItemDelta<?, ?>> modifications = MiscSchemaUtil.createCollection(triggerDelta);
        // This is detached result. It will not take part of the task result. We do not really care.
        OperationResult result = new OperationResult(TriggerScanActivityHandler.class.getName() + ".removeTriggers");
        try {
            activityRun.getModelBeans().cacheRepositoryService
                    .modifyObject(requireNonNull(object.getCompileTimeClass()), object.getOid(), modifications, result);
            result.computeStatus();
            task.recordObjectActionExecuted(object, ChangeType.MODIFY, null);
        } catch (ObjectNotFoundException e) {
            // Object is gone. Ergo there are no triggers left. Ergo the trigger was removed.
            // Ergo this is not really an error.
            task.recordObjectActionExecuted(object, ChangeType.MODIFY, e);
            LOGGER.trace("Unable to remove trigger from {}: {} (but this is probably OK)", object, e.getMessage(), e);
        } catch (SchemaException | ObjectAlreadyExistsException e) {
            task.recordObjectActionExecuted(object, ChangeType.MODIFY, e);
            LOGGER.error("Unable to remove trigger from {}: {}", object, e.getMessage(), e);
        } catch (Throwable t) {
            task.recordObjectActionExecuted(object, ChangeType.MODIFY, t);
            throw t;
        }
    }
}
