/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.ucf.api;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.processor.ObjectClassComplexTypeDefinition;
import com.evolveum.midpoint.schema.processor.ResourceObjectIdentification;
import com.evolveum.midpoint.schema.processor.SearchHierarchyConstraints;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.task.api.StateReporter;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.PagedSearchCapabilityType;

import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

import org.apache.commons.collections4.CollectionUtils;
import org.jetbrains.annotations.NotNull;

/**
 * Represents a resource object (e.g. an account) fetched by an UCF operation like
 * {@link ConnectorInstance#fetchObject(ResourceObjectIdentification, AttributesToReturn, StateReporter, OperationResult)}
 * or {@link ConnectorInstance#search(ObjectClassComplexTypeDefinition, ObjectQuery, FetchedObjectHandler, AttributesToReturn, PagedSearchCapabilityType, SearchHierarchyConstraints, UcfFetchErrorReportingMethod, StateReporter, OperationResult)}.
 */
@Experimental
public class FetchedUcfObject implements DebugDumpable {

    /**
     * Resource object as it is known at this point.
     */
    @NotNull private final PrismObject<ShadowType> resourceObject;

    /**
     * Real value of the object primary identifier (e.g. ConnId UID).
     * Usually not null (e.g. in ConnId 1.x), but this can change in the future.
     */
    private final Object primaryIdentifierValue;

    /**
     * Error state describing the result of processing within UCF layer.
     */
    private final UcfErrorState errorState;

    public FetchedUcfObject(@NotNull PrismObject<ShadowType> resourceObject, Object primaryIdentifierValue, UcfErrorState errorState) {
        this.resourceObject = resourceObject;
        this.primaryIdentifierValue = primaryIdentifierValue;
        this.errorState = errorState;
    }

    public @NotNull PrismObject<ShadowType> getResourceObject() {
        return resourceObject;
    }

    public Object getPrimaryIdentifierValue() {
        return primaryIdentifierValue;
    }

    public UcfErrorState getErrorState() {
        return errorState;
    }

    @Override
    public String toString() {
        return "FetchedUcfObject{" +
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
}
