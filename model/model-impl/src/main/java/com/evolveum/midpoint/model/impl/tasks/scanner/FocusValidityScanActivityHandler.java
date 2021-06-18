/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.tasks.scanner;

import static com.evolveum.midpoint.model.impl.tasks.scanner.FocusValidityScanPartialExecution.ScanScope.*;

import java.util.ArrayList;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.model.api.ModelPublicConstants;
import com.evolveum.midpoint.model.impl.tasks.ModelActivityHandler;
import com.evolveum.midpoint.repo.common.activity.Activity;
import com.evolveum.midpoint.repo.common.activity.EmbeddedActivity;
import com.evolveum.midpoint.repo.common.activity.execution.AbstractActivityExecution;
import com.evolveum.midpoint.repo.common.activity.execution.ExecutionInstantiationContext;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusValidityScanWorkDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ValidityScanQueryStyleType;

@Component
public class FocusValidityScanActivityHandler
        extends ModelActivityHandler<FocusValidityScanWorkDefinition, FocusValidityScanActivityHandler> {

    private static final String LEGACY_HANDLER_URI = ModelPublicConstants.FOCUS_VALIDITY_SCANNER_TASK_HANDLER_URI;

    @PostConstruct
    public void register() {
        handlerRegistry.register(FocusValidityScanWorkDefinitionType.COMPLEX_TYPE, LEGACY_HANDLER_URI,
                FocusValidityScanWorkDefinition.class, FocusValidityScanWorkDefinition::new, this);
    }

    @PreDestroy
    public void unregister() {
        handlerRegistry.unregister(FocusValidityScanWorkDefinitionType.COMPLEX_TYPE, LEGACY_HANDLER_URI,
                FocusValidityScanWorkDefinition.class);
    }

    @Override
    public AbstractActivityExecution<FocusValidityScanWorkDefinition, FocusValidityScanActivityHandler, ?> createExecution(
            @NotNull ExecutionInstantiationContext<FocusValidityScanWorkDefinition, FocusValidityScanActivityHandler> context,
            @NotNull OperationResult result) {
        return new FocusValidityScanCompositeExecution(context);
    }

    @Override
    public ArrayList<Activity<?, ?>> createChildActivities(
            Activity<FocusValidityScanWorkDefinition, FocusValidityScanActivityHandler> parentActivity) {
        ArrayList<Activity<?, ?>> children = new ArrayList<>();
        ValidityScanQueryStyleType queryStyle = parentActivity.getWorkDefinition().getQueryStyle();
        switch (queryStyle) {
            case SINGLE_QUERY:
                children.add(EmbeddedActivity.create(parentActivity.getDefinition(),
                        (context, result) -> new FocusValidityScanPartialExecution(context, COMBINED),
                        (i) -> ModelPublicConstants.FOCUS_VALIDITY_SCAN_FULL_ID,
                        parentActivity));
                break;
            case SEPARATE_OBJECT_AND_ASSIGNMENT_QUERIES:
                children.add(EmbeddedActivity.create(parentActivity.getDefinition(),
                        (context, result) -> new FocusValidityScanPartialExecution(context, OBJECTS),
                        (i) -> ModelPublicConstants.FOCUS_VALIDITY_SCAN_OBJECTS_ID,
                        parentActivity));
                children.add(EmbeddedActivity.create(parentActivity.getDefinition(),
                        (context, result) -> new FocusValidityScanPartialExecution(context, ASSIGNMENTS),
                        (i) -> ModelPublicConstants.FOCUS_VALIDITY_SCAN_ASSIGNMENTS_ID,
                        parentActivity));
                break;
            default:
                throw new AssertionError(queryStyle);
        }
        return children;
    }

    @Override
    public String getIdentifierPrefix() {
        return "focus-validity-scan";
    }

}
