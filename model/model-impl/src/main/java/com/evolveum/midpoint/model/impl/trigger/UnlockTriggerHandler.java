/*
 * Copyright (c) 2013-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.trigger;

import static javax.xml.datatype.DatatypeConstants.LESSER;

import java.util.List;
import javax.annotation.PostConstruct;
import javax.xml.datatype.XMLGregorianCalendar;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.common.Clock;
import com.evolveum.midpoint.model.api.ModelPublicConstants;
import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.RunningTask;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Unlocks the focus object (if the time has come).
 */
@Component
public class UnlockTriggerHandler implements SingleTriggerHandler {

    public static final String HANDLER_URI = ModelPublicConstants.UNLOCK_TRIGGER_HANDLER_URI;

    private static final Trace LOGGER = TraceManager.getTrace(UnlockTriggerHandler.class);

    @Autowired private TriggerHandlerRegistry triggerHandlerRegistry;
    @Autowired private ModelService modelService;
    @Autowired private PrismContext prismContext;
    @Autowired private Clock clock;

    @PostConstruct
    private void initialize() {
        triggerHandlerRegistry.register(HANDLER_URI, this);
    }

    @Override
    public <O extends ObjectType> void handle(@NotNull PrismObject<O> object, @NotNull TriggerType trigger,
            @NotNull RunningTask task, @NotNull OperationResult result) {
        O objectable = object.asObjectable();
        LOGGER.trace("Considering unlocking {}", objectable);
        if (!(objectable instanceof FocusType)) {
            LOGGER.debug("Not a focus object: {}", objectable);
            result.recordNotApplicable("Not a focus object");
            return;
        }
        FocusType focus = (FocusType) objectable;
        try {
            ActivationType activation = focus.getActivation();
            if (activation == null) {
                LOGGER.debug("No activation in {}", objectable);
                result.recordNotApplicable("No activation");
                return;
            }

            XMLGregorianCalendar lockoutExpirationTimestamp = activation.getLockoutExpirationTimestamp();
            if (lockoutExpirationTimestamp != null
                    && clock.currentTimeXMLGregorianCalendar().compare(lockoutExpirationTimestamp) == LESSER) {
                LOGGER.debug("The lockout for {} has not expired yet: {}", focus, lockoutExpirationTimestamp);
                result.recordNotApplicable("The lockout has not expired yet");
                return;
            }

            LOGGER.debug("Unlocking {}", focus);
            // We do this intentionally via model API, as there's some non-trivial processing inside
            // (e.g., clearing the number of failed logins).
            // This also causes the change to be audited.
            modelService.executeChanges(
                    List.of(
                            prismContext.deltaFor(focus.getClass())
                                    .item(FocusType.F_ACTIVATION, ActivationType.F_LOCKOUT_STATUS)
                                    .replace(LockoutStatusType.NORMAL)
                                    .asObjectDelta(focus.getOid())),
                    null,
                    task,
                    result);
            LOGGER.debug("Unlocked {}", focus);
        } catch (CommonException | RuntimeException | Error e) {
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't unlock object {}", e, object);
            // Intentionally not retrying.
        }
    }

    @Override
    public boolean isIdempotent() {
        return true;
    }
}
