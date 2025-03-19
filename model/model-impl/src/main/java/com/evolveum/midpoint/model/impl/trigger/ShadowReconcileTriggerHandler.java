/*
 * Copyright (c) 2013-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.trigger;

import jakarta.annotation.PostConstruct;
import javax.xml.datatype.Duration;
import javax.xml.datatype.XMLGregorianCalendar;

import com.evolveum.midpoint.model.api.ModelPublicConstants;
import com.evolveum.midpoint.model.api.trigger.TriggerHandlerRegistry;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.provisioning.api.EventDispatcher;
import com.evolveum.midpoint.provisioning.api.ProvisioningService;
import com.evolveum.midpoint.provisioning.api.ResourceObjectShadowChangeDescription;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.util.annotation.Experimental;

import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.RunningTask;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

import java.util.List;

import static com.evolveum.midpoint.prism.polystring.PolyString.getOrig;

import static com.evolveum.midpoint.schema.GetOperationOptions.readOnly;

import static org.apache.commons.lang3.ObjectUtils.defaultIfNull;

/**
 * "Synchronizes" resource object shadow: retrieves the resource objects and calls change notification dispatcher.
 */
@Experimental
@Component
public class ShadowReconcileTriggerHandler implements SingleTriggerHandler {

    public static final String HANDLER_URI = ModelPublicConstants.NS_MODEL_TRIGGER_PREFIX + "/shadow-reconcile/handler-3";

    private static final String OP_SYNCHRONIZE_SHADOW = ShadowReconcileTriggerHandler.class.getName() + ".synchronizeShadow";
    private static final String OP_RESCHEDULE = ShadowReconcileTriggerHandler.class.getName() + ".reschedule";

    private static final Trace LOGGER = TraceManager.getTrace(ShadowReconcileTriggerHandler.class);

    private static final Duration DEFAULT_INTERVAL = XmlTypeConverter.createDuration("PT6H");

    @Autowired private TriggerHandlerRegistry triggerHandlerRegistry;
    @Autowired private ProvisioningService provisioningService;
    @Autowired private EventDispatcher eventDispatcher;
    @Autowired private PrismContext prismContext;
    @Autowired @Qualifier("cacheRepositoryService") private RepositoryService repositoryService;

    @PostConstruct
    private void initialize() {
        triggerHandlerRegistry.register(HANDLER_URI, this);
    }

    @Override
    public <O extends ObjectType> void handle(@NotNull PrismObject<O> object, @NotNull TriggerType trigger,
            @NotNull RunningTask task, @NotNull OperationResult result) {
        if (object.asObjectable() instanceof ShadowType) {
            synchronizeShadowChecked((ShadowType) object.asObjectable(), trigger, task, result);
        } else {
            // Not throwing an exception means that the operation will not be retried.
            // It is logical, because there's no point of trying this operation on non-shadow object.
            LOGGER.error("Couldn't synchronize non-shadow object: {}", object);
            result.recordPartialError("Couldn't synchronize non-shadow object: " + object);
        }
    }

    /**
     * Executes "synchronize shadow" attempt. If it fails, schedules another retry (if possible).
     */
    private void synchronizeShadowChecked(ShadowType shadow, TriggerType trigger, RunningTask task, OperationResult parentResult) {
        OperationResult result = synchronizeShadow(shadow, trigger, task, parentResult);
        LOGGER.trace("Synchronization of {} ended with status: {}", shadow, result.getStatus());
        if (result.isError()) {
            rescheduleSynchronizationIfPossible(shadow, trigger, parentResult);
        }
    }

    /**
     * @return Result of the operation. This is to allow deciding on the status (success/error).
     */
    private OperationResult synchronizeShadow(ShadowType shadow, TriggerType trigger, RunningTask task, OperationResult parentResult) {
        String shadowOid = shadow.getOid();
        OperationResult result = parentResult.subresult(OP_SYNCHRONIZE_SHADOW)
                .addParam("shadowOid", shadowOid)
                .addParam("shadowName", getOrig(shadow.getName()))
                .addParam("attemptNumber", getNumber(trigger))
                .build();
        try {
            // TODO read-only later
            PrismObject<ShadowType> shadowedResourceObject = provisioningService.getObject(ShadowType.class, shadowOid, null, task, result);
            PrismObject<ResourceType> resource = getResource(shadowedResourceObject, task, result);

            ResourceObjectShadowChangeDescription change = new ResourceObjectShadowChangeDescription();
            change.setShadowedResourceObject(shadowedResourceObject);
            change.setResource(resource);
            change.setSourceChannel(SchemaConstants.CHANNEL_RECON_URI); // to be reconsidered later

            eventDispatcher.notifyChange(change, task, result);
        } catch (Throwable t) {
            result.recordFatalError(t);
            LoggingUtils.logUnexpectedException(LOGGER, "Failed to synchronize shadow {}", t, shadow);
        } finally {
            result.computeStatusIfUnknown();
        }
        return result;
    }

    private PrismObject<ResourceType> getResource(PrismObject<ShadowType> fullShadow, RunningTask task, OperationResult result)
            throws ObjectNotFoundException, CommunicationException, SchemaException, ConfigurationException,
            SecurityViolationException, ExpressionEvaluationException {
        String resourceOid = ShadowUtil.getResourceOid(fullShadow);
        if (resourceOid == null) {
            throw new IllegalStateException("No resource OID in " + fullShadow);
        }
        return provisioningService.getObject(ResourceType.class, resourceOid, readOnly(), task, result);
    }

    private void rescheduleSynchronizationIfPossible(ShadowType shadow, TriggerType trigger, OperationResult parentResult) {
        String shadowOid = shadow.getOid();
        OperationResult result = parentResult.subresult(OP_RESCHEDULE)
                .addParam("shadowOid", shadowOid)
                .addParam("shadowName", getOrig(shadow.getName()))
                .build();
        try {
            PlannedOperationAttemptType currentAttemptInfo = getAttemptInfo(trigger);
            int number = getNumber(currentAttemptInfo);
            Integer limit = getLimit(currentAttemptInfo);
            if (limit != null && number >= limit) {
                LOGGER.warn("Maximum number of attempts ({}) reached for {}. Will not attempt to synchronize "
                        + "this object any more.", limit, shadow);
                result.recordNotApplicable();
            } else {
                createNextAttemptTrigger(shadow, currentAttemptInfo, result);
            }
        } catch (Throwable t) {
            result.recordFatalError(t);
            // We don't want to lose this trigger, so we're throwing an exception
            throw new SystemException("Couldn't reschedule the synchronization: " + t.getMessage(), t);
        } finally {
            result.computeStatusIfUnknown();
        }
    }

    private void createNextAttemptTrigger(ShadowType shadow, PlannedOperationAttemptType currentAttemptInfo, OperationResult result)
            throws SchemaException, ObjectNotFoundException, ObjectAlreadyExistsException {
        Duration interval = getInterval(currentAttemptInfo);

        PlannedOperationAttemptType nextAttemptInfo = new PlannedOperationAttemptType(prismContext)
                .number(getNumber(currentAttemptInfo) + 1)
                .interval(interval)
                .limit(getLimit(currentAttemptInfo));
        TriggerType nextTrigger = new TriggerType(prismContext)
                .handlerUri(HANDLER_URI)
                .timestamp(getNextAttemptTimestamp(interval));
        ObjectTypeUtil.setExtensionContainerRealValues(prismContext, nextTrigger.asPrismContainerValue(),
                SchemaConstants.MODEL_EXTENSION_PLANNED_OPERATION_ATTEMPT, nextAttemptInfo);

        LOGGER.debug("Scheduling new attempt for the synchronization of {} (will be #{} of {}, at {})",
                shadow, nextAttemptInfo.getNumber(), nextAttemptInfo.getLimit(), nextTrigger.getTimestamp());

        List<ItemDelta<?, ?>> modifications = prismContext.deltaFor(ShadowType.class)
                .item(ShadowType.F_TRIGGER)
                .add(nextTrigger)
                .asItemDeltas();
        repositoryService.modifyObject(ShadowType.class, shadow.getOid(), modifications, result);
    }

    private XMLGregorianCalendar getNextAttemptTimestamp(Duration interval) {
        return XmlTypeConverter.fromNow(defaultIfNull(interval, DEFAULT_INTERVAL));
    }

    private PlannedOperationAttemptType getAttemptInfo(TriggerType trigger) {
        return ObjectTypeUtil.getExtensionItemRealValue(trigger.asPrismContainerValue(), SchemaConstants.MODEL_EXTENSION_PLANNED_OPERATION_ATTEMPT);
    }

    private Duration getInterval(PlannedOperationAttemptType attemptInfo) {
        return attemptInfo != null ? attemptInfo.getInterval() : null;
    }

    private Integer getLimit(PlannedOperationAttemptType attemptInfo) {
        return attemptInfo != null ? attemptInfo.getLimit() : null;
    }

    private int getNumber(PlannedOperationAttemptType attemptInfo) {
        return attemptInfo != null && attemptInfo.getNumber() != null ? attemptInfo.getNumber() : 1;
    }

    private int getNumber(TriggerType trigger) {
        return getNumber(getAttemptInfo(trigger));
    }

    @Override
    public boolean isIdempotent() {
        return true;
    }
}
