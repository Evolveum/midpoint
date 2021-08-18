/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.trigger;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import com.evolveum.midpoint.model.impl.tasks.ModelSearchBasedActivityExecution;
import com.evolveum.midpoint.repo.common.activity.ActivityStateDefinition;
import com.evolveum.midpoint.repo.common.activity.execution.AbstractActivityExecution;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ScanWorkStateType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemObjectsType;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.model.api.ModelPublicConstants;
import com.evolveum.midpoint.model.impl.sync.tasks.imp.ImportWorkDefinition;
import com.evolveum.midpoint.model.impl.tasks.ModelActivityHandler;
import com.evolveum.midpoint.repo.common.activity.execution.ExecutionInstantiationContext;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TriggerScanWorkDefinitionType;

/**
 * Task handler for the trigger scanner.
 *
 * Keeps a registry of trigger handlers.
 *
 * @author Radovan Semancik
 */
@Component
public class TriggerScanActivityHandler
        extends ModelActivityHandler<TriggerScanWorkDefinition, TriggerScanActivityHandler> {

    public static final String LEGACY_HANDLER_URI = ModelPublicConstants.TRIGGER_SCANNER_TASK_HANDLER_URI;
    private static final String ARCHETYPE_OID = SystemObjectsType.ARCHETYPE_SYSTEM_TASK.value();

    @Autowired private TriggerHandlerRegistry triggerHandlerRegistry;

    @PostConstruct
    public void register() {
        handlerRegistry.register(TriggerScanWorkDefinitionType.COMPLEX_TYPE, LEGACY_HANDLER_URI,
                TriggerScanWorkDefinition.class, TriggerScanWorkDefinition::new, this);
    }

    @PreDestroy
    public void unregister() {
        handlerRegistry.unregister(TriggerScanWorkDefinitionType.COMPLEX_TYPE, LEGACY_HANDLER_URI,
                ImportWorkDefinition.class);
    }

    @Override
    public AbstractActivityExecution<TriggerScanWorkDefinition, TriggerScanActivityHandler, ?> createExecution(
            @NotNull ExecutionInstantiationContext<TriggerScanWorkDefinition, TriggerScanActivityHandler> context,
            @NotNull OperationResult result) {
        return new ModelSearchBasedActivityExecution<>(context, "Trigger scan", TriggerScanActivityExecutionSpecifics::new);
    }

    @Override
    public String getIdentifierPrefix() {
        return "trigger-scan";
    }

    TriggerHandler getTriggerHandler(String handlerUri) {
        return triggerHandlerRegistry.getHandler(handlerUri);
    }

    @Override
    public @NotNull ActivityStateDefinition<?> getRootActivityStateDefinition() {
        return ActivityStateDefinition.perpetual(ScanWorkStateType.COMPLEX_TYPE);
    }

    @Override
    public String getDefaultArchetypeOid() {
        return ARCHETYPE_OID;
    }
}
