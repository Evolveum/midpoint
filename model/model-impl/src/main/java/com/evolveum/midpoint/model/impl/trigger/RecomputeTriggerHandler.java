/*
 * Copyright (c) 2013-2017 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.model.impl.trigger;

import jakarta.annotation.PostConstruct;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.model.api.ModelPublicConstants;
import com.evolveum.midpoint.model.api.trigger.TriggerHandlerRegistry;
import com.evolveum.midpoint.model.impl.controller.ModelController;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.RunningTask;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TriggerType;

/**
 * Executes generic recompute triggers that were scheduled e.g. by mappings or activation logic.
 *
 * This handler is used for delayed-delete scheduling and other features that need to revisit an object later.
 * When a trigger fires, the handler requests a recompute of the affected object through the model controller.
 *
 * @author Radovan Semancik
 */
@Component
public class RecomputeTriggerHandler implements SingleTriggerHandler {

    /**
     * URI of the recompute trigger handler used by midPoint's generic trigger mechanism.
     */
    public static final String HANDLER_URI = ModelPublicConstants.NS_MODEL_TRIGGER_PREFIX + "/recompute/handler-3";

    private static final Trace LOGGER = TraceManager.getTrace(RecomputeTriggerHandler.class);

    @Autowired private TriggerHandlerRegistry triggerHandlerRegistry;
    @Autowired private ModelController modelController;

    @PostConstruct
    private void initialize() {
        triggerHandlerRegistry.register(HANDLER_URI, this);
    }

    /**
     * Handles a fired recompute trigger by executing a recompute of the affected object.
     */
    @Override
    public <O extends ObjectType> void handle(@NotNull PrismObject<O> object, @NotNull TriggerType trigger,
            @NotNull RunningTask task, @NotNull OperationResult result) {
        try {

            // Reconcile option used for compatibility. TODO: do we need it?
            ModelExecuteOptions options = ModelExecuteOptions.create().reconcile();
            modelController.executeRecompute(object, options, task, result);

        } catch (CommonException | RuntimeException | Error  e) {
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't recompute object {}", e, object);
            // do not retry (TODO is this ok?)
        }

    }

    /**
     * Recompute triggers are safe to replay because the recompute operation is idempotent.
     */
    @Override
    public boolean isIdempotent() {
        return true;
    }
}
