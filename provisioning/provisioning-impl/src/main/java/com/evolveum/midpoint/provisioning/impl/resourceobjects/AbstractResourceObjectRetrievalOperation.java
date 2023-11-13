/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl.resourceobjects;

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
 */
abstract class AbstractResourceObjectRetrievalOperation {

    @NotNull final ProvisioningContext ctx;

    /** Whether associations should be fetched for the object found. */
    final boolean fetchAssociations;

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
     * @see ResourceObjectFound#completeResourceObject(ProvisioningContext, ResourceObject, boolean, OperationResult)
     */
    @NotNull CompleteResourceObject complete(@NotNull ResourceObject object, @NotNull OperationResult result) {
        ResourceObjectFound objectFound = new ResourceObjectFound(object, ctx, fetchAssociations);
        objectFound.initialize(ctx.getTask(), result);
        return objectFound.asCompleteResourceObject();
    }
}
