/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl.resourceobjects;

import com.evolveum.midpoint.util.exception.*;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.provisioning.impl.ProvisioningContext;
import com.evolveum.midpoint.provisioning.ucf.api.UcfFetchErrorReportingMethod;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FetchErrorReportingMethodType;

/**
 * Either full-featured `search` or simpler `locate` or `fetch` operations.
 *
 * @see ResourceObjectSearchOperation
 * @see ResourceObjectLocateOperation
 * @see ResourceObjectFetchOperation
 */
abstract class AbstractResourceObjectRetrievalOperation {

    @NotNull final ProvisioningContext ctx;

    /** Whether associations should be fetched for the object(s) found. */
    final boolean fetchAssociations;

    /** Currently used only for search operations. Placed here to remind about future extension to fetch/locate ops. */
    @Nullable private final FetchErrorReportingMethodType errorReportingMethod;

    @NotNull final ResourceObjectsBeans b = ResourceObjectsBeans.get();

    AbstractResourceObjectRetrievalOperation(
            @NotNull ProvisioningContext ctx,
            boolean fetchAssociations,
            @Nullable FetchErrorReportingMethodType errorReportingMethod) {
        this.ctx = ctx;
        this.fetchAssociations = fetchAssociations;
        this.errorReportingMethod = errorReportingMethod;
    }

    @NotNull UcfFetchErrorReportingMethod getUcfErrorReportingMethod() {
        if (errorReportingMethod == FetchErrorReportingMethodType.FETCH_RESULT) {
            return UcfFetchErrorReportingMethod.UCF_OBJECT;
        } else {
            return UcfFetchErrorReportingMethod.EXCEPTION;
        }
    }

    /**
     * Does all the necessary processing at "resource objects" layer: activation, protected flag, associations, and so on.
     *
     * Limitation: to be used only for fetch/locate operations, which do not support in-object error reporting.
     * Hence, we do not need to use the error-handling machinery provided by lazy initialization
     * in {@link ResourceObjectFound} here.
     */
    @NotNull CompleteResourceObject complete(@NotNull ExistingResourceObjectShadow object, @NotNull OperationResult result)
            throws SchemaException, ExpressionEvaluationException, CommunicationException, SecurityViolationException,
            ConfigurationException, ObjectNotFoundException {
        assert errorReportingMethod == null;
        return ResourceObjectCompleter.completeResourceObject(ctx, object, fetchAssociations, result);
    }
}
