/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl;

import java.util.Collection;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.provisioning.impl.resourceobjects.ExistingResourceObject;
import com.evolveum.midpoint.provisioning.impl.resourceobjects.ResourceObject;
import com.evolveum.midpoint.schema.processor.ResourceAttributeContainer;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.ShortDumpable;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDeltaUtil;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.provisioning.impl.shadows.manager.ShadowManagerMiscUtil;
import com.evolveum.midpoint.schema.processor.ResourceObjectDefinition;
import com.evolveum.midpoint.schema.processor.ResourceObjectIdentification;
import com.evolveum.midpoint.schema.processor.ResourceObjectIdentifiers;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowLifecycleStateType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

/**
 * Either a resource object, or a repository shadow.
 *
 * TODO decide about {@link Shadow}
 */
public interface AbstractShadow extends ShortDumpable, DebugDumpable {

    /**
     * Returns the {@link ShadowType} bean backing this object.
     *
     * It should meet the criteria for individual subtypes, like {@link ResourceObject}, {@link ExistingResourceObject},
     * or {@link RepoShadow}.
     */
    @NotNull ShadowType getBean();

    /**
     * Returns the definition corresponding to this shadow.
     * It should be the same as applied on the {@link ResourceAttributeContainer} in the {@link #getBean()}.
     */
    @NotNull ResourceObjectDefinition getObjectDefinition();

    default @NotNull PrismObject<ShadowType> getPrismObject() {
        return getBean().asPrismObject();
    }

    default boolean isDead() {
        return ShadowUtil.isDead(getBean());
    }

    default boolean doesExist() {
        return ShadowUtil.isExists(getBean());
    }

    default boolean isImmutable() {
        return getBean().isImmutable();
    }

    default void shortDump(StringBuilder sb) {
        sb.append(ShadowUtil.shortDumpShadow(getBean()));
    }

    default @Nullable ResourceObjectIdentifiers getIdentifiers() throws SchemaException {
        return ResourceObjectIdentifiers
                .optionalOf(getObjectDefinition(), getBean())
                .orElse(null);
    }

    default boolean hasPrimaryIdentifier() throws SchemaException {
        return getIdentifiers() instanceof ResourceObjectIdentifiers.WithPrimary;
    }

    /**
     * Computes the value to be stored into {@link ShadowType#primaryIdentifierValue(String)}.
     * Although the shadow lifecycle state might be present in the backing bean (for some subclasses),
     * this method - to be general - requires the state to be externally provided by the caller.
     *
     * TODO does the method belong here?
     */
    default @Nullable String determinePrimaryIdentifierValue(@NotNull ShadowLifecycleStateType lifecycleState)
            throws SchemaException {
        return ShadowManagerMiscUtil.determinePrimaryIdentifierValue(this, lifecycleState);
    }

    default @NotNull ResourceObjectIdentifiers getIdentifiersRequired() throws SchemaException {
        return ResourceObjectIdentifiers.of(getObjectDefinition(), getBean());
    }

    default @NotNull ResourceObjectIdentification<?> getIdentificationRequired() throws SchemaException {
        return ResourceObjectIdentification.of(
                getObjectDefinition(),
                getIdentifiersRequired());
    }

    default @Nullable Object getPrimaryIdentifierValueFromAttributes() throws SchemaException {
        ResourceObjectIdentifiers identifiers = getIdentifiers();
        if (identifiers == null) {
            return null;
        }
        var primaryIdentifier = identifiers.getPrimaryIdentifier();
        if (primaryIdentifier == null) {
            return null;
        }
        return primaryIdentifier.getRealValue();
    }

    default @Nullable ResourceObjectIdentification<?> getIdentification() throws SchemaException {
        var identifiers = getIdentifiers();
        if (identifiers != null) {
            return ResourceObjectIdentification.of(getObjectDefinition(), identifiers);
        } else {
            return null;
        }
    }

    /** Updates the in-memory representation. */
    default void updateWith(@NotNull Collection<? extends ItemDelta<?, ?>> modifications) throws SchemaException {
        ObjectDeltaUtil.applyTo(getPrismObject(), modifications);
    }

    @NotNull String getResourceOid();

    @NotNull QName getObjectClass() throws SchemaException;

    default PolyString determineShadowName() throws SchemaException {
        return ShadowUtil.determineShadowName(getBean());
    }
}
