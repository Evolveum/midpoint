/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.sync.tasks;

import com.evolveum.midpoint.common.refinery.RefinedResourceSchema;
import com.evolveum.midpoint.model.impl.util.ModelImplUtils;
import com.evolveum.midpoint.schema.ResourceShadowDiscriminator;
import com.evolveum.midpoint.schema.processor.ObjectClassComplexTypeDefinition;
import com.evolveum.midpoint.schema.util.ResourceTypeUtil;
import com.evolveum.midpoint.util.exception.MaintenanceException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AvailabilityStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceObjectSetType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.xml.namespace.QName;

import static java.util.Objects.requireNonNull;

/**
 * TODO better name
 */
@SuppressWarnings("FieldCanBeLocal")
public class TargetInfo {
    @NotNull final ResourceShadowDiscriminator coords;
    @NotNull public final ResourceType resource;
    @NotNull private final RefinedResourceSchema refinedResourceSchema;
    @Nullable
    private final ObjectClassComplexTypeDefinition objectClassDefinition;

    TargetInfo(@NotNull ResourceShadowDiscriminator coords, @NotNull ResourceType resource,
            @NotNull RefinedResourceSchema refinedResourceSchema,
            @Nullable ObjectClassComplexTypeDefinition objectClassDefinition) {
        this.coords = coords;
        this.resource = resource;
        this.refinedResourceSchema = refinedResourceSchema;
        this.objectClassDefinition = objectClassDefinition;
    }

    @Override
    public String toString() {
        return "TargetInfo{" +
                "coords=" + coords +
                ", resource=" + resource +
                ", refinedResourceSchema=" + refinedResourceSchema +
                ", objectClassDefinition=" + objectClassDefinition +
                '}';
    }

    public @NotNull ResourceShadowDiscriminator getCoords() {
        return coords;
    }

    public @NotNull ResourceType getResource() {
        return resource;
    }

    public @NotNull RefinedResourceSchema getRefinedResourceSchema() {
        return refinedResourceSchema;
    }

    public @Nullable ObjectClassComplexTypeDefinition getObjectClassDefinition() {
        return objectClassDefinition;
    }

    public @NotNull ObjectClassComplexTypeDefinition getObjectClassDefinitionRequired() {
        return requireNonNull(objectClassDefinition);
    }

    public @NotNull QName getObjectClassName() {
        return getObjectClassDefinitionRequired().getTypeName();
    }

    public SynchronizationObjectsFilterImpl getObjectFilter(ResourceObjectSetType resourceObjectSet) {
        return ModelImplUtils.determineSynchronizationObjectsFilter(objectClassDefinition, resourceObjectSet);
    }

    public String getContextDescription() {
        return String.valueOf(resource); // TODO something more human friendly
    }

    public String getResourceOid() {
        return resource.getOid();
    }

    public void checkResourceUp() {
        if (ResourceTypeUtil.getLastAvailabilityStatus(resource) != AvailabilityStatusType.UP) {
            throw new IllegalStateException("Resource has to have value of last availability status on UP");
        }
    }

    public void checkNotInMaintenance() throws MaintenanceException {
        ResourceTypeUtil.checkNotInMaintenance(resource);
    }
}
