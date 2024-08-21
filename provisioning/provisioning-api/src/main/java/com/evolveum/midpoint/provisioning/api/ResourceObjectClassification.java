/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.api;

import java.util.Objects;

import com.evolveum.midpoint.schema.util.AbstractShadow;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.processor.ResourceObjectTypeDefinition;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;

/**
 * Result of the object classification.
 *
 * Currently it is bound to {@link ResourceObjectTypeDefinition}. We never classify an object to specific `kind` and `intent`
 * without having the corresponding explicit type definition - i.e. no `account/default` hacks; at least not for now.
 *
 * The clockwork will be able to process default accounts without their regular classification by applying so-called
 * emergency classification - one that is not stored in repository (shadow kind/intent), only in memory i.e. during processing.
 */
public class ResourceObjectClassification {

    @Nullable private final ResourceObjectTypeDefinition definition;

    private ResourceObjectClassification(@Nullable ResourceObjectTypeDefinition definition) {
        this.definition = definition;
    }

    public static @NotNull ResourceObjectClassification unknown() {
        return new ResourceObjectClassification(null);
    }

    public static @NotNull ResourceObjectClassification of(@Nullable ResourceObjectTypeDefinition definition) {
        return new ResourceObjectClassification(definition);
    }

    public static @NotNull ResourceObjectClassification of(@NotNull AbstractShadow shadow) {
        return new ResourceObjectClassification(shadow.getObjectDefinition().getTypeDefinition());
    }

    public @Nullable ResourceObjectTypeDefinition getDefinition() {
        return definition;
    }

    public @NotNull ResourceObjectTypeDefinition getDefinitionRequired() {
        return Objects.requireNonNull(definition, "no definition");
    }

    public @NotNull ShadowKindType getKind() {
        return definition != null ? definition.getKind() : ShadowKindType.UNKNOWN;
    }

    public @NotNull String getIntent() {
        return definition != null ? definition.getIntent() : SchemaConstants.INTENT_UNKNOWN;
    }

    public boolean isKnown() {
        return definition != null;
    }

    @Override
    public String toString() {
        return "Classification{" + definition + '}';
    }

    public boolean equivalent(ResourceObjectClassification other) {
        return getKind() == other.getKind()
                && getIntent().equals(other.getIntent());
    }
}
