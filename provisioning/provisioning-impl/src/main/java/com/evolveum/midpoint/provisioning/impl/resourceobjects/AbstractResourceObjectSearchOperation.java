/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl.resourceobjects;

import com.evolveum.midpoint.provisioning.impl.ProvisioningContext;

import com.evolveum.midpoint.provisioning.ucf.api.UcfFetchErrorReportingMethod;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FetchErrorReportingMethodType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Either full-featured `search` or simpler `locate` or `fetch` operations.
 *
 * @see ResourceObjectSearchOperation
 * @see ResourceObjectLocateOrFetchOperation
 */
abstract class AbstractResourceObjectSearchOperation {

    @NotNull final ProvisioningContext ctx;

    /** Whether associations should be fetched for the object found. */
    final boolean fetchAssociations;

    @Nullable private final FetchErrorReportingMethodType errorReportingMethod;

    @NotNull final ResourceObjectsBeans b = ResourceObjectsBeans.get();

    AbstractResourceObjectSearchOperation(
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
}
