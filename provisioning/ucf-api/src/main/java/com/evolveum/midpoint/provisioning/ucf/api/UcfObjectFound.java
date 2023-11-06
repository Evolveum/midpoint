/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.ucf.api;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.internals.InternalsConfig;
import com.evolveum.midpoint.schema.processor.ResourceObjectDefinition;
import com.evolveum.midpoint.schema.processor.SearchHierarchyConstraints;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.PagedSearchCapabilityType;

import org.jetbrains.annotations.NotNull;

import static com.evolveum.midpoint.util.MiscUtil.stateCheck;

/**
 * Represents a resource object (e.g. an account) found by {@link ConnectorInstance#search(ResourceObjectDefinition, ObjectQuery,
 * UcfObjectHandler, AttributesToReturn, PagedSearchCapabilityType, SearchHierarchyConstraints, UcfFetchErrorReportingMethod,
 * UcfExecutionContext, OperationResult)} operation.
 *
 * See also {@link UcfChange}.
 */
@Experimental
public class UcfObjectFound implements DebugDumpable {

    /**
     * Resource object as it is known at this point.
     *
     * Conditions:
     *
     * - if errorState.isSuccess: Object is fully converted. In particular, it has identifiers present.
     * - if errorState.isError: Object may or may not be fully converted. It can be even empty.
     */
    @NotNull private final UcfResourceObject resourceObject;

    /**
     * Error state describing the result of processing within UCF layer.
     */
    @NotNull private final UcfErrorState errorState;

    public UcfObjectFound(
            @NotNull PrismObject<ShadowType> resourceObject,
            Object primaryIdentifierValue,
            @NotNull UcfErrorState errorState) {
        this.resourceObject = UcfResourceObject.of(resourceObject, primaryIdentifierValue);
        this.errorState = errorState;
        checkConsistence();
    }

    public @NotNull UcfResourceObject getResourceObject() {
        return resourceObject;
    }

    public @NotNull PrismObject<ShadowType> getPrismObject() {
        return resourceObject.bean().asPrismObject();
    }

    public @NotNull ShadowType getBean() {
        return resourceObject.bean();
    }

    public Object getPrimaryIdentifierValue() {
        return resourceObject.primaryIdentifierValue();
    }

    public @NotNull UcfErrorState getErrorState() {
        return errorState;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
                "resourceObject=" + resourceObject +
                ", errorState=" + errorState +
                '}';
    }

    @SuppressWarnings("DuplicatedCode")
    @Override
    public String debugDump(int indent) {
        StringBuilder sb = new StringBuilder();
        DebugUtil.indentDebugDump(sb, indent);
        sb.append(getClass().getSimpleName());
        sb.append("\n");
        DebugUtil.debugDumpWithLabelLn(sb, "resourceObject", resourceObject, indent + 1);
        DebugUtil.debugDumpWithLabelLn(sb, "errorState", errorState, indent + 1);
        return sb.toString();
    }

    public void checkConsistence() {
        if (!InternalsConfig.consistencyChecks) {
            return;
        }
        if (errorState.isSuccess()) {
            // Not possible to check identifiers in the resource object, as we do not have OC def here.
            stateCheck(getPrimaryIdentifierValue() != null, "No primary identifier value");
        }
    }
}
