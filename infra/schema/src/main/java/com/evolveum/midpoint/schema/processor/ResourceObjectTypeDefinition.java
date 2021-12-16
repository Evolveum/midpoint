/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.processor;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.util.ObjectQueryUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LayerType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceObjectTypeDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;

/**
 * Definition of "resource object type". Roughly corresponds to an `objectType` section in `schemaHandling`
 * part of resource definition.
 */
public interface ResourceObjectTypeDefinition
        extends ResourceObjectDefinition {

    @Override
    default @NotNull ObjectQuery createShadowSearchQuery(String resourceOid) throws SchemaException {
        return ObjectQueryUtil.createResourceAndKindIntent(resourceOid, getKind(), getIntent());
    }

    /**
     * Kind of objects covered by this object type.
     */
    @NotNull ShadowKindType getKind();

    /**
     * Intent defining this object type (along with {@link #getKind()}).
     */
    @NotNull String getIntent();

    /**
     * Returns true if this object type matches specified (non-null) kind and intent.
     */
    default boolean matches(@NotNull ShadowKindType kind, @NotNull String intent) {
        return kind == getKind() && intent.equals(getIntent());
    }

    /**
     * Returns true if the type definition is of specified kind.
     * Kind of `null` matches all definitions.
     */
    default boolean matchesKind(@Nullable ShadowKindType kind) {
        return kind == null || kind == getKind();
    }

    /**
     * Is this type explicitly marked as the default object type for given kind?
     * (using default or defaultForKind property).
     *
     * @see ResourceObjectTypeDefinitionType#isDefaultForKind()
     * @see ResourceObjectTypeDefinitionType#isDefault()
     */
    boolean isDefaultForKind();

    /**
     * Is this type explicitly marked as the default object type for given object class?
     * (using default or defaultForObjectClass property).
     *
     * @see ResourceObjectTypeDefinitionType#isDefaultForObjectClass()
     * @see ResourceObjectTypeDefinitionType#isDefault()
     */
    boolean isDefaultForObjectClass();

    ResourceObjectTypeDefinition forLayer(@NotNull LayerType layerType);
}
