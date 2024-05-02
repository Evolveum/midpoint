/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.controller;

import com.evolveum.midpoint.model.api.ModelAuthorizationAction;
import com.evolveum.midpoint.model.impl.ModelBeans;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.api.Authorization;
import com.evolveum.midpoint.security.api.MidPointPrincipal;
import com.evolveum.midpoint.security.api.ProfileCompilerOptions;
import com.evolveum.midpoint.security.enforcer.api.CompileConstraintsOptions;
import com.evolveum.midpoint.security.enforcer.api.SecurityEnforcer;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import com.evolveum.prism.xml.ns._public.query_3.SearchFilterType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

/**
 * Evaluates diagnostic authorization requests ("authorization playground").
 *
 * @param <REQ> Type of request
 */
abstract class AuthorizationDiagEvaluation<REQ extends AuthorizationEvaluationRequestType> {

    @NotNull final REQ request;
    @NotNull final Task task;
    @NotNull final ModelBeans b = ModelBeans.get();

    @NotNull private final MyLogCollector logCollector;

    AuthorizationDiagEvaluation(@NotNull REQ request, @NotNull Task task) {
        this.request = request;
        this.task = task;
        this.logCollector = new MyLogCollector(isSelectorTracingEnabled(request));
    }

    private boolean isSelectorTracingEnabled(REQ request) {
        var tracing = request.getTracing();
        return tracing != null && Boolean.TRUE.equals(tracing.isSelectorTracingEnabled());
    }

    static AuthorizationDiagEvaluation<?> of(@NotNull AuthorizationEvaluationRequestType request, @NotNull Task task)
            throws SchemaException {
        if (request instanceof AuthorizationEvaluationAccessDecisionRequestType accessDecision) {
            // TODO this is a temporary criterion between these two
            if (accessDecision.getActionUrl().isEmpty()) {
                return new ItemAccessDecision(accessDecision, task);
            } else {
                return new OperationAccessDecision(accessDecision, task);
            }
        } else if (request instanceof AuthorizationEvaluationFilterProcessingRequestType filterProcessing) {
            return new FilterProcessing(filterProcessing, task);
        } else {
            throw new IllegalArgumentException("Unknown request type: " + request);
        }
    }

    abstract public @NotNull AuthorizationEvaluationResponseType evaluate(@NotNull OperationResult result)
            throws SchemaException, ExpressionEvaluationException, CommunicationException, SecurityViolationException,
            ConfigurationException, ObjectNotFoundException;

    /** Creates a principal for the request: either deriving from the currently logged user, or creating from provided ref. */
    @NotNull MidPointPrincipal createPrincipal(OperationResult result)
            throws SchemaException, ExpressionEvaluationException, CommunicationException, SecurityViolationException,
            ConfigurationException, ObjectNotFoundException {
        MidPointPrincipal newPrincipal = createPrincipalRaw(result);
        List<Authorization> additionalAuthorizations = request.getAdditionalAuthorization().stream()
                .map(bean -> Authorization.create(bean, "additional authorization"))
                .toList();
        if (additionalAuthorizations.isEmpty()) {
            return newPrincipal;
        } else {
            // We are not sure if the elevation is total or partial.
            return newPrincipal.cloneWithAdditionalAuthorizations(additionalAuthorizations, false);
        }
    }

    /** Principal without any additional authorizations. */
    private MidPointPrincipal createPrincipalRaw(OperationResult result)
            throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException,
            SecurityViolationException, ExpressionEvaluationException {
        ObjectReferenceType subjectRef = request.getSubjectRef();
        if (subjectRef == null) {
            var currentPrincipal = b.securityEnforcer.getMidPointPrincipal();
            FocusType focus;
            if (currentPrincipal != null) {
                focus = new UserType()
                        .oid(currentPrincipal.getOid())
                        .name(currentPrincipal.getName());
            } else {
                focus = new UserType()
                        .oid(UUID.randomUUID().toString())
                        .name("Anonymous");
            }
            return MidPointPrincipal.create(focus);
        } else {
            FocusType subject = b.modelObjectResolver.resolve(
                    subjectRef, FocusType.class, null, "subject", task, result);
            // For Authorization Playground page, we don't need to support GUI config
            return b.securityContextManager.getUserProfileService().getPrincipal(
                    subject.asPrismObject(),
                    null,
                    ProfileCompilerOptions.createNotCompileGuiAdminConfiguration()
                            .locateSecurityPolicy(false),
                    result);
        }
    }

    String principalInfo(@NotNull MidPointPrincipal principal) {
        var sb = new StringBuilder();
        sb.append(principal.getFocus()).append(" with ").append(principal.getAuthorities().size()).append(" authorization(s)");
        if (explicitAuthorizationsOnly()) {
            sb.append(" (explicit authorizations only)");
        }
        return sb.toString();
    }

    /** Do we have only the explicit authorizations? */
    private boolean explicitAuthorizationsOnly() {
        return request.getSubjectRef() == null;
    }

    @NotNull String[] getActionUrls() {
        List<String> urls = request.getActionUrl();
        if (!urls.isEmpty()) {
            return urls.toArray(String[]::new);
        } else {
            return getDefaultActionUrls();
        }
    }

    SecurityEnforcer.Options createOptions() {
        return SecurityEnforcer.Options.create()
                .withLogCollector(logCollector);
    }

    @NotNull AuthorizationEvaluationResponseType createResponse(String result) {
        return new AuthorizationEvaluationResponseType()
                .result(result)
                .computation(logCollector.getLog());
    }

    abstract @NotNull String[] getDefaultActionUrls();

    static abstract class AccessDecision extends AuthorizationDiagEvaluation<AuthorizationEvaluationAccessDecisionRequestType> {

        AccessDecision(@NotNull AuthorizationEvaluationAccessDecisionRequestType request, @NotNull Task task) {
            super(request, task);
        }

        @Override
        @NotNull String[] getDefaultActionUrls() {
            return ModelAuthorizationAction.AUTZ_ACTIONS_URLS_GET;
        }
    }

    static class ItemAccessDecision extends AccessDecision {

        ItemAccessDecision(@NotNull AuthorizationEvaluationAccessDecisionRequestType request, @NotNull Task task) {
            super(request, task);
        }

        @Override
        public @NotNull AuthorizationEvaluationResponseType evaluate(@NotNull OperationResult result)
                throws SchemaException, ExpressionEvaluationException, CommunicationException, SecurityViolationException,
                ConfigurationException, ObjectNotFoundException {

            ObjectReferenceType objectRef = MiscUtil.argNonNull(request.getObjectRef(), "objectRef is missing");

            var object = // FIXME what if the object is embedded?
                    b.modelObjectResolver.resolve(
                            objectRef, ObjectType.class, null, "object", task, result);

            MidPointPrincipal principal = createPrincipal(result);
            var constraints = b.securityEnforcer.compileOperationConstraints(
                    principal,
                    object.asPrismObject().getValue(),
                    null,
                    getActionUrls(),
                    createOptions(),
                    CompileConstraintsOptions.create(),
                    task, result);

            // Temporary solution for handling "no access" situations
            AuthorizationException exception = null;
            PrismObject<? extends ObjectType> objectWithConstraints = null;
            try {
                objectWithConstraints = b.dataAccessProcessor.applyReadConstraints(object.asPrismObject(), constraints);
            } catch (AuthorizationException e) {
                exception = e;
            }

            var responseText =
                    ("""
                            Principal: %s

                            Object: %s

                            ---------------------------------------------

                            Computed item constraints:
                            %s

                            ---------------------------------------------

                            Object with constraints applied:
                            %s""")

                            .formatted(
                                    principalInfo(principal),
                                    object,
                                    DebugUtil.debugDump(constraints, 1),
                                    exception != null ?
                                            exception.getMessage() : DebugUtil.debugDump(objectWithConstraints, 1));

            return createResponse(responseText);
        }
    }

    static class OperationAccessDecision extends AccessDecision {

        OperationAccessDecision(@NotNull AuthorizationEvaluationAccessDecisionRequestType request, @NotNull Task task) {
            super(request, task);
        }

        @Override
        public @NotNull AuthorizationEvaluationResponseType evaluate(@NotNull OperationResult result)
                throws SchemaException, ExpressionEvaluationException, CommunicationException, SecurityViolationException,
                ConfigurationException, ObjectNotFoundException {
            throw new UnsupportedOperationException();
        }
    }

    static class FilterProcessing extends AuthorizationDiagEvaluation<AuthorizationEvaluationFilterProcessingRequestType> {

        @NotNull private final Class<?> objectType;
        @Nullable private final ObjectFilter originalFilter;

        FilterProcessing(@NotNull AuthorizationEvaluationFilterProcessingRequestType request, @NotNull Task task)
                throws SchemaException {
            super(request, task);

            objectType = b.prismContext.getSchemaRegistry().determineClassForTypeRequired(
                    MiscUtil.argNonNull(request.getType(), "Type is not specified"));

            SearchFilterType filterBean = request.getFilter();
            originalFilter =
                    filterBean != null ?
                            b.prismContext.getQueryConverter().parseFilter(filterBean, objectType) :
                            null;
        }

        @Override
        public @NotNull AuthorizationEvaluationResponseType evaluate(@NotNull OperationResult result)
                throws SchemaException, ExpressionEvaluationException, CommunicationException, SecurityViolationException,
                ConfigurationException, ObjectNotFoundException {

            MidPointPrincipal principal = createPrincipal(result);

            var augmentedFilter = b.securityEnforcer.preProcessObjectFilter(
                    principal, getActionUrls(), ModelAuthorizationAction.AUTZ_ACTIONS_URLS_SEARCH_BY, null,
                    objectType, originalFilter,
                    null, List.of(),
                    createOptions(), task, result);

            var responseText =
                    ("""
                            Principal: %s

                            Original filter with type %s:
                            %s

                            ---------------------------------------------

                            Augmented filter:
                            %s""")

                            .formatted(
                                    principalInfo(principal),
                                    objectType.getSimpleName(),
                                    DebugUtil.debugDump(originalFilter, 1),
                                    DebugUtil.debugDump(augmentedFilter, 1));

            return createResponse(responseText);
        }

        @Override
        @NotNull String[] getDefaultActionUrls() {
            return ModelAuthorizationAction.AUTZ_ACTIONS_URLS_SEARCH;
        }
    }

    /** Special log collector that provides a String output. Temporary solution. */
    static class MyLogCollector implements SecurityEnforcer.LogCollector {

        private final StringBuilder sb = new StringBuilder();

        private final boolean selectorTracingEnabled;

        MyLogCollector(boolean selectorTracingEnabled) {
            this.selectorTracingEnabled = selectorTracingEnabled;
        }

        @Override
        public void log(String message) {
            sb.append(message).append("\n");
        }

        public @NotNull String getLog() {
            return sb.toString();
        }

        public boolean isSelectorTracingEnabled() {
            return selectorTracingEnabled;
        }
    }
}
