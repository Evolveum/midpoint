/*
 * Copyright (c) 2013-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
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
 * @author Radovan Semancik
 *
 */
@Component
public class RecomputeTriggerHandler implements SingleTriggerHandler {

    public static final String HANDLER_URI = ModelPublicConstants.NS_MODEL_TRIGGER_PREFIX + "/recompute/handler-3";

    private static final Trace LOGGER = TraceManager.getTrace(RecomputeTriggerHandler.class);

    @Autowired private TriggerHandlerRegistry triggerHandlerRegistry;
    @Autowired private ModelController modelController;

    @PostConstruct
    private void initialize() {
        triggerHandlerRegistry.register(HANDLER_URI, this);
    }

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

    @Override
    public boolean isIdempotent() {
        return true;
    }
}
