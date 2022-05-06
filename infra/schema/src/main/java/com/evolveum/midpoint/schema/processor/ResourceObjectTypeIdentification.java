/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.processor;

import java.util.Objects;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.schema.util.ResourceObjectTypeDefinitionTypeUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceObjectTypeDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;

/**
 * Identifies a resource object type - by kind and intent.
 */
public class ResourceObjectTypeIdentification {

    @NotNull private final ShadowKindType kind;
    @NotNull private final String intent;

    private ResourceObjectTypeIdentification(@NotNull ShadowKindType kind, @NotNull String intent) {
        this.kind = kind;
        this.intent = intent;
    }

    public static @NotNull ResourceObjectTypeIdentification of(
            @NotNull ShadowKindType kind, @NotNull String intent) {
        return new ResourceObjectTypeIdentification(kind, intent);
    }

    public static ResourceObjectTypeIdentification of(@NotNull ResourceObjectTypeDefinitionType definitionBean) {
        return of(
                ResourceObjectTypeDefinitionTypeUtil.getKind(definitionBean),
                ResourceObjectTypeDefinitionTypeUtil.getIntent(definitionBean));
    }

    public @NotNull ShadowKindType getKind() {
        return kind;
    }

    public @NotNull String getIntent() {
        return intent;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ResourceObjectTypeIdentification that = (ResourceObjectTypeIdentification) o;
        return kind == that.kind && intent.equals(that.intent);
    }

    @Override
    public int hashCode() {
        return Objects.hash(kind, intent);
    }

    @Override
    public String toString() {
        return kind + "/" + intent;
    }
}
