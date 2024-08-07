/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.processor;

import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.CapabilityCollectionType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.CapabilityType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.util.ObjectQueryUtil;
import com.evolveum.midpoint.util.exception.SchemaException;

import javax.xml.namespace.QName;
import java.util.Collection;

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
     * Identification (kind+intent) of this object type.
     */
    @NotNull ResourceObjectTypeIdentification getTypeIdentification();

    @NotNull ResourceObjectTypeDefinition getTypeDefinition();

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

    @NotNull ResourceObjectTypeDefinition forLayerMutable(@NotNull LayerType layer);

    /** Returns the configured capability of given class, if present. */
    @Nullable <T extends CapabilityType> T getConfiguredCapability(Class<T> capabilityClass);

    /** Returns all configured capabilities, if present. */
    @Nullable CapabilityCollectionType getSpecificCapabilities();

    /** Returns the correlation definition bean, if present here. (It may be standalone.) */
    @Nullable CorrelationDefinitionType getCorrelationDefinitionBean();

    /** Returns the "synchronization enabled" flag value, if present here. (It may be standalone.) FIXME */
    @Nullable Boolean isSynchronizationEnabled();

    /** Returns the "synchronization opportunistic" flag value, if present here. (It may be standalone.) */
    @Nullable Boolean isSynchronizationOpportunistic();

    /** Returns the focus type name, if present here. (It may be standalone.) */
    @Nullable QName getFocusTypeName();

    /** Archetype reference - not present in standalone definitions. */
    @Nullable ObjectReferenceType getArchetypeRef();

    /** Archetype OID - a convenience method. */
    default @Nullable String getArchetypeOid() {
        ObjectReferenceType archetypeRef = getArchetypeRef();
        if (archetypeRef == null) {
            return null;
        }
        String oid = archetypeRef.getOid();
        if (oid != null) {
            return oid;
        }
        throw new UnsupportedOperationException("Dynamic references are not supported for archetypeRef; in " + this);
    }

    /** Returns true if there is "synchronization reactions" definition section here (even if it's empty). */
    boolean hasSynchronizationReactionsDefinition();

    /** Returns the synchronization reactions defined here. (They may be standalone.) */
    @NotNull Collection<SynchronizationReactionDefinition> getSynchronizationReactions();

    /** Temporary? */
    @Nullable ExpressionType getClassificationCondition();
}
