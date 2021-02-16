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
import com.evolveum.midpoint.schema.processor.ObjectClassComplexTypeDefinition;
import com.evolveum.midpoint.schema.processor.SearchHierarchyConstraints;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.StateReporter;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.PagedSearchCapabilityType;

import org.jetbrains.annotations.NotNull;

import static com.evolveum.midpoint.util.MiscUtil.stateCheck;

/**
 * Represents a resource object (e.g. an account) found by {@link ConnectorInstance#search(ObjectClassComplexTypeDefinition, ObjectQuery, ObjectHandler, AttributesToReturn, PagedSearchCapabilityType, SearchHierarchyConstraints, UcfFetchErrorReportingMethod, StateReporter, OperationResult)}.
 * operation.
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
    @NotNull private final PrismObject<ShadowType> resourceObject;

    /**
     * Real value of the object primary identifier (e.g. ConnId UID).
     *
     * Conditions:
     * - errorState.isSuccess: Not null.
     * - errorState.isError: Usually not null (e.g. never in ConnId 1.x). But this may change in the future.
     */
    private final Object primaryIdentifierValue;

    /**
     * Error state describing the result of processing within UCF layer.
     */
    @NotNull private final UcfErrorState errorState;

    public UcfObjectFound(@NotNull PrismObject<ShadowType> resourceObject, Object primaryIdentifierValue,
            @NotNull UcfErrorState errorState) {
        this.resourceObject = resourceObject;
        this.primaryIdentifierValue = primaryIdentifierValue;
        this.errorState = errorState;
        checkConsistence();
    }

    public @NotNull PrismObject<ShadowType> getResourceObject() {
        return resourceObject;
    }

    public Object getPrimaryIdentifierValue() {
        return primaryIdentifierValue;
    }

    public @NotNull UcfErrorState getErrorState() {
        return errorState;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
                "resourceObject=" + resourceObject +
                ", primaryIdentifierValue=" + primaryIdentifierValue +
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
        DebugUtil.debugDumpWithLabelLn(sb, "primaryIdentifierValue", String.valueOf(primaryIdentifierValue), indent + 1);
        DebugUtil.debugDumpWithLabelLn(sb, "errorState", errorState, indent + 1);
        return sb.toString();
    }

    public void checkConsistence() {
        if (!InternalsConfig.consistencyChecks) {
            return;
        }
        if (errorState.isSuccess()) {
            // Not possible to check identifiers in the resource object, as we do not have OC def here.
            stateCheck(primaryIdentifierValue != null, "No primary identifier value");
        }
    }
}
