/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.model.impl.tasks.scanner;

import static com.evolveum.midpoint.model.impl.tasks.scanner.FocusValidityScanPartialRun.ScanScope.*;

import java.util.ArrayList;

import com.evolveum.midpoint.repo.common.activity.handlers.ActivityHandlerUtils;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

import com.evolveum.midpoint.repo.common.activity.run.state.ActivityStateDefinition;
import com.evolveum.midpoint.repo.common.activity.run.CompositeActivityRun;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.model.api.ModelPublicConstants;
import com.evolveum.midpoint.model.impl.tasks.ModelActivityHandler;
import com.evolveum.midpoint.repo.common.activity.Activity;
import com.evolveum.midpoint.repo.common.activity.EmbeddedActivity;
import com.evolveum.midpoint.repo.common.activity.run.AbstractActivityRun;
import com.evolveum.midpoint.repo.common.activity.run.ActivityRunInstantiationContext;
import com.evolveum.midpoint.schema.result.OperationResult;

@Component
public class FocusValidityScanActivityHandler
        extends ModelActivityHandler<FocusValidityScanWorkDefinition, FocusValidityScanActivityHandler> {

    private static final String ARCHETYPE_OID = SystemObjectsType.ARCHETYPE_SYSTEM_TASK.value();

    @PostConstruct
    public void register() {
        handlerRegistry.register(
                FocusValidityScanWorkDefinitionType.COMPLEX_TYPE, WorkDefinitionsType.F_FOCUS_VALIDITY_SCAN,
                FocusValidityScanWorkDefinition.class, FocusValidityScanWorkDefinition::new, this);
    }

    @PreDestroy
    public void unregister() {
        handlerRegistry.unregister(
                FocusValidityScanWorkDefinitionType.COMPLEX_TYPE, FocusValidityScanWorkDefinition.class);
    }

    @Override
    public AbstractActivityRun<FocusValidityScanWorkDefinition, FocusValidityScanActivityHandler, ?> createActivityRun(
            @NotNull ActivityRunInstantiationContext<FocusValidityScanWorkDefinition, FocusValidityScanActivityHandler> context,
            @NotNull OperationResult result) {
        return new CompositeActivityRun<>(context);
    }

    @Override
    public ArrayList<Activity<?, ?>> createChildActivities(
            Activity<FocusValidityScanWorkDefinition, FocusValidityScanActivityHandler> parentActivity) {
        ArrayList<Activity<?, ?>> children = new ArrayList<>();
        ActivityStateDefinition stateDef =
                ActivityStateDefinition.perpetual(ScanWorkStateType.COMPLEX_TYPE);
        ValidityScanQueryStyleType queryStyle = parentActivity.getWorkDefinition().getQueryStyle();
        switch (queryStyle) {
            case SINGLE_QUERY:
                children.add(EmbeddedActivity.create(
                        ActivityHandlerUtils.cloneWithoutIdForChildActivity(parentActivity.getDefinition()),
                        (context, result) -> new FocusValidityScanPartialRun(context, COMBINED),
                        null,
                        (i) -> ModelPublicConstants.FOCUS_VALIDITY_SCAN_FULL_ID,
                        stateDef,
                        parentActivity));
                break;
            case SEPARATE_OBJECT_AND_ASSIGNMENT_QUERIES:
                children.add(EmbeddedActivity.create(
                        ActivityHandlerUtils.cloneWithoutIdForChildActivity(parentActivity.getDefinition()),
                        (context, result) -> new FocusValidityScanPartialRun(context, OBJECTS),
                        null,
                        (i) -> ModelPublicConstants.FOCUS_VALIDITY_SCAN_OBJECTS_ID,
                        stateDef,
                        parentActivity));
                children.add(EmbeddedActivity.create(
                        ActivityHandlerUtils.cloneWithoutIdForChildActivity(parentActivity.getDefinition()),
                        (context, result) -> new FocusValidityScanPartialRun(context, ASSIGNMENTS),
                        null,
                        (i) -> ModelPublicConstants.FOCUS_VALIDITY_SCAN_ASSIGNMENTS_ID,
                        stateDef,
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

    @Override
    public String getDefaultArchetypeOid() {
        return ARCHETYPE_OID;
    }
}
