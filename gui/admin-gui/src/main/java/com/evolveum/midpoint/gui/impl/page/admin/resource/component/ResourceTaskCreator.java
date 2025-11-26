/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.resource.component;

import java.util.Objects;
import javax.xml.namespace.QName;

import org.jetbrains.annotations.NotNull;

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
import com.evolveum.midpoint.web.page.admin.resources.ResourceTaskFlavor;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.query_3.QueryType;

/**
 * Creates resource-related tasks: import, reconciliation, live sync, and others.
 *
 * @param <T> The type of optional task configuration.
 * @see MemberOperationsTaskCreator
 */
public class ResourceTaskCreator<T> {

    private final ResourceType resource;
    private final PageBase pageBase;
    private final ResourceTaskFlavor<T> specifiedFlavor;

    private T workDefinitionConfiguration;

    private ShadowKindType kind;
    private String intent;
    private QName objectClass;

    private ExecutionModeType executionMode;
    private PredefinedConfigurationType predefinedConfiguration;
    private SimulationDefinitionType simulationDefinition;

    private SelectorQualifiedGetOptionsType searchOptions;
    private QueryType query;

    /** Options provided by the caller. See {@link #submissionOptions()}. */
    private ActivitySubmissionOptions submissionOptions;

    /** Owner provided by the caller. If set, it has precedence over the owner specified in the options. */
    private FocusType owner;

    private ResourceTaskCreator(@NotNull PageBase pageBase, @NotNull ResourceType resource,
            ResourceTaskFlavor<T> flavor) {
        this.resource = resource;
        this.pageBase = pageBase;
        this.specifiedFlavor = flavor;
    }

    public static <T> FlavoredResourceTaskCreator<T> of(ResourceTaskFlavor<T> flavor, PageBase pageBase) {
        return resource -> new ResourceTaskCreator<>(pageBase, resource, flavor);
    }

    @SuppressWarnings("WeakerAccess")
    public ResourceTaskCreator<T> ownedByCurrentUser() throws NotLoggedInException {
        this.owner = AuthUtil.getPrincipalObjectRequired();
        return this;
    }

    public ResourceTaskCreator<T> withCoordinates(ShadowKindType kind, String intent, QName objectClass) {
        this.kind = kind;
        this.intent = intent;
        this.objectClass = objectClass;
        return this;
    }

    @SuppressWarnings({"WeakerAccess"})
    public ResourceTaskCreator<T> withCoordinates(QName objectClass) {
        this.objectClass = objectClass;
        return this;
    }

    public ResourceTaskCreator<T> withExecutionMode(ExecutionModeType executionMode) {
        this.executionMode = executionMode;
        return this;
    }

    public ResourceTaskCreator<T> withPredefinedConfiguration(PredefinedConfigurationType predefinedConfiguration) {
        this.predefinedConfiguration = predefinedConfiguration;
        return this;
    }

    @SuppressWarnings("WeakerAccess")
    public ResourceTaskCreator<T> withSubmissionOptions(ActivitySubmissionOptions options) {
        this.submissionOptions = options;
        return this;
    }

    public ResourceTaskCreator<T> withSimulationResultDefinition(SimulationDefinitionType simulationDefinition) {
        this.simulationDefinition = simulationDefinition;
        return this;
    }

    public ResourceTaskCreator<T> withSearchOptions(SelectorQualifiedGetOptionsType searchOptions) {
        this.searchOptions = searchOptions;
        return this;
    }

    public ResourceTaskCreator<T> withQuery(QueryType query) {
        this.query = query;
        return this;
    }

    public ResourceTaskCreator<T> withConfiguration(T workConfiguration) {
        this.workDefinitionConfiguration = workConfiguration;
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
        if (this.specifiedFlavor != null) {
            return this.specifiedFlavor.createWorkDefinitions(objectSetBean(), this.workDefinitionConfiguration);
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
        bean.searchOptions(searchOptions);
        bean.query(query);
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
        var options = Objects.requireNonNullElseGet(
                submissionOptions,
                ActivitySubmissionOptions::create);
        options = options.updateTaskTemplate(
                t -> t.objectRef(ObjectTypeUtil.createObjectRef(resource)));
        if (owner != null) {
            return options.withOwner(owner);
        } else {
            return options;
        }
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

    /**
     * Creates resource task creator for given resource.
     *
     * @param <T> The type of optional task configuration
     */
    public interface FlavoredResourceTaskCreator<T> {
        ResourceTaskCreator<T> forResource(ResourceType resource);
    }
}
