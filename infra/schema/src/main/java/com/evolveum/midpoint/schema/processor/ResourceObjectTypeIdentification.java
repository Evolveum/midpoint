/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.processor;

import java.io.Serializable;
import java.util.Objects;

import com.evolveum.midpoint.schema.ResourceShadowCoordinates;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.schema.util.ResourceObjectTypeDefinitionTypeUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceObjectTypeDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;

import org.jetbrains.annotations.Nullable;

import static com.evolveum.midpoint.util.MiscUtil.argCheck;

/**
 * Identifies a resource object type - by kind and intent.
 *
 * The values are *never* `unknown`.
 */
public class ResourceObjectTypeIdentification implements Serializable {

    @NotNull private final ShadowKindType kind;
    @NotNull private final String intent;

    private ResourceObjectTypeIdentification(@NotNull ShadowKindType kind, @NotNull String intent) {
        argCheck(kind != ShadowKindType.UNKNOWN, "'unknown' value is not supported for the kind");
        argCheck(!SchemaConstants.INTENT_UNKNOWN.equals(intent), "'unknown' value is not supported for the intent");
        this.kind = kind;
        this.intent = intent;
    }

    public static @NotNull ResourceObjectTypeIdentification of(
            @NotNull ShadowKindType kind, @NotNull String intent) {
        return new ResourceObjectTypeIdentification(kind, intent);
    }

    public static @NotNull ResourceObjectTypeIdentification of(@NotNull ResourceObjectTypeDefinitionType definitionBean) {
        return of(
                ResourceObjectTypeDefinitionTypeUtil.getKind(definitionBean),
                ResourceObjectTypeDefinitionTypeUtil.getIntent(definitionBean));
    }

    public static @Nullable ResourceObjectTypeIdentification createIfKnown(@NotNull ShadowType shadow) {
        return createIfKnown(shadow.getKind(), shadow.getIntent());
    }

    public static @Nullable ResourceObjectTypeIdentification createIfKnown(@NotNull ResourceShadowCoordinates coordinates) {
        return createIfKnown(coordinates.getKind(), coordinates.getIntent());
    }

    public static @Nullable ResourceObjectTypeIdentification createIfKnown(
            @Nullable ShadowKindType kind, @Nullable String intent) {
        return ShadowUtil.isKnown(kind) && ShadowUtil.isKnown(intent) ?
                of(kind, intent) : null;
    }

    public static ResourceObjectTypeIdentification defaultAccount() {
        return new ResourceObjectTypeIdentification(ShadowKindType.ACCOUNT, SchemaConstants.INTENT_DEFAULT);
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
