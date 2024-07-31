/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.processor;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.util.CloneUtil;
import com.evolveum.midpoint.schema.CapabilityUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowCachingPolicyType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.xml.datatype.XMLGregorianCalendar;
import java.io.Serializable;

/**
 * Provides basic information about {@link ResourceType} for the purpose of refined resource object type/class definitions.
 * It is the information from the outside of `schemaHandling`. It is important also as a provider of default values e.g. for
 * caching.
 *
 * The instance of this record should be shared throughout all the definitions for the resource to avoid memory bloat.
 *
 * The instance is immutable. All of its components must be effectively immutable.
 *
 * To be extended later, e.g., with capabilities.
 */
public record BasicResourceInformation(
        @Nullable String oid,
        @Nullable PolyStringType name,
        @Nullable ShadowCachingPolicyType cachingPolicy,
        boolean readCachedCapabilityPresent,
        @Nullable XMLGregorianCalendar cacheInvalidationTimestamp)
        implements Serializable {

    public static BasicResourceInformation of(@NotNull PrismObject<ResourceType> resource) {
        return of(resource.asObjectable());
    }

    public static BasicResourceInformation of(@NotNull ResourceType resource) {
        return new BasicResourceInformation(
                resource.getOid(),
                CloneUtil.clone(resource.getName()),
                CloneUtil.toImmutable(resource.getCaching()),
                CapabilityUtil.isReadingCachingOnly(resource, null),
                CloneUtil.clone(resource.getCacheInvalidationTimestamp()));
    }

    @Override
    public String toString() {
        return name + " (" + oid + ")";
    }
}
