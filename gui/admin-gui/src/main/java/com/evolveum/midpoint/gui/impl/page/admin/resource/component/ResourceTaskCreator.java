/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.resource.component;

import com.evolveum.midpoint.authentication.api.util.AuthUtil;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.impl.page.admin.abstractrole.component.MemberOperationsTaskCreator;
import com.evolveum.midpoint.model.api.ActivitySubmissionOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.schema.util.task.ActivityDefinitionBuilder;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.exception.NotLoggedInException;
import com.evolveum.midpoint.web.page.admin.resources.SynchronizationTaskFlavor;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.xml.namespace.QName;

import java.util.Objects;

import static com.evolveum.midpoint.util.MiscUtil.stateCheck;

/**
 * Creates resource-related tasks: import, reconciliation, live sync, and maybe others in the future.
 *
 * @see MemberOperationsTaskCreator
 */
public class ResourceTaskCreator {

    @NotNull private final ResourceType resource;
    @NotNull private final PageBase pageBase;

    private String specifiedArchetypeOid;
    private SynchronizationTaskFlavor specifiedFlavor;

    private ShadowKindType kind;
    private String intent;
    private QName objectClass;

    private ExecutionModeType executionMode;
    private PredefinedConfigurationType predefinedConfiguration;
    private SimulationDefinitionType simulationDefinition;

    private ActivitySubmissionOptions submissionOptions;

    private FocusType owner;

    private ResourceTaskCreator(@NotNull ResourceType resource, @NotNull PageBase pageBase) {
        this.resource = resource;
        this.pageBase = pageBase;
    }

    public static @NotNull ResourceTaskCreator forResource(@NotNull ResourceType resource, @NotNull PageBase pageBase) {
        return new ResourceTaskCreator(resource, pageBase);
    }

    /** Assuming not used along with {@link #ofFlavor(SynchronizationTaskFlavor)} */
    @SuppressWarnings("WeakerAccess")
    public ResourceTaskCreator withArchetype(@Nullable String archetypeOid) {
        stateCheck(specifiedFlavor == null, "Cannot specify both flavor and archetype");
        specifiedArchetypeOid = archetypeOid;
        return this;
    }

    /** Assuming not used along with {@link #withArchetype(String)} */
    public ResourceTaskCreator ofFlavor(SynchronizationTaskFlavor flavor) {
        stateCheck(specifiedArchetypeOid == null, "Cannot specify both flavor and archetype");
        specifiedFlavor = flavor;
        return this;
    }

    public ResourceTaskCreator ownedByCurrentUser() throws NotLoggedInException {
        this.owner = AuthUtil.getPrincipalObjectRequired();
        return this;
    }

    public ResourceTaskCreator withCoordinates(ShadowKindType kind, String intent, QName objectClass) {
        this.kind = kind;
        this.intent = intent;
        this.objectClass = objectClass;
        return this;
    }

    public ResourceTaskCreator withCoordinates(QName objectClass) {
        this.objectClass = objectClass;
        return this;
    }

    public ResourceTaskCreator withExecutionMode(ExecutionModeType executionMode) {
        this.executionMode = executionMode;
        return this;
    }

    public ResourceTaskCreator withPredefinedConfiguration(PredefinedConfigurationType predefinedConfiguration) {
        this.predefinedConfiguration = predefinedConfiguration;
        return this;
    }

    @SuppressWarnings("WeakerAccess")
    public ResourceTaskCreator withSubmissionOptions(ActivitySubmissionOptions options) {
        this.submissionOptions = options;
        return this;
    }

    public ResourceTaskCreator withSimulationResultDefinition(SimulationDefinitionType simulationDefinition) {
        this.simulationDefinition = simulationDefinition;
        return this;
    }

    private ActivityDefinitionType activityDefinition() {
        var builder = ActivityDefinitionBuilder.create(workDefinitions());

        if (executionMode != null) {
            builder = builder.withExecutionMode(executionMode);
        }

        if (predefinedConfiguration != null) {
            builder = builder.withPredefinedConfiguration(predefinedConfiguration);
        }

        if (simulationDefinition != null) {
            builder = builder.withSimulationDefinition(simulationDefinition);
        }

        return builder.build();
    }

    private WorkDefinitionsType workDefinitions() {
        for (SynchronizationTaskFlavor flavorToCheck : SynchronizationTaskFlavor.values()) {
            if (flavorToCheck == specifiedFlavor || flavorToCheck.getArchetypeOid().equals(specifiedArchetypeOid)) {
                return flavorToCheck.createWorkDefinitions(objectSetBean());
            }
        }
        return new WorkDefinitionsType(); // probably will not work much, but let us try; TODO or should we signal a problem?
    }

    /**
     * TODO decide what to do when kind is specified but intent is null; should we ignore the kind/intent, and use objectclass,
     *  or should we go for the default intent? Or, maybe, should we let the caller sort this out?
     */
    private ResourceObjectSetType objectSetBean() {
        var bean = new ResourceObjectSetType()
                .resourceRef(ObjectTypeUtil.createObjectRef(resource));
        if (kind != null && intent != null) {
            // the most recommended case
            return bean
                    .kind(kind)
                    .intent(intent);
        } else if (objectClass != null) {
            // second most recommended case
            return bean
                    .objectclass(objectClass);
        } else {
            // unusual case - let us provide all we have
            return bean
                    .kind(kind)
                    .intent(intent)
                    .objectclass(objectClass);
        }
    }

    private @NotNull ActivitySubmissionOptions submissionOptions() {
        return Objects.requireNonNullElseGet(
                        submissionOptions,
                        () -> ActivitySubmissionOptions.create())
                .withTaskTemplate(new TaskType()
                        .objectRef(ObjectTypeUtil.createObjectRef(resource)))
                .withOwner(owner);
    }

    public @NotNull String submit(@NotNull Task task, @NotNull OperationResult result) throws CommonException {
        return pageBase.getModelInteractionService().submit(
                activityDefinition(),
                submissionOptions(),
                task, result);
    }

    public @NotNull TaskType create(@NotNull Task task, @NotNull OperationResult result) throws CommonException {
        return pageBase.getModelInteractionService().createExecutionTask(
                activityDefinition(),
                submissionOptions(),
                task, result);
    }
}
