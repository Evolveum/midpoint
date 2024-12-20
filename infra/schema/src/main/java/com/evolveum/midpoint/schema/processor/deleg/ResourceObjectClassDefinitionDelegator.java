package com.evolveum.midpoint.schema.processor.deleg;

import com.evolveum.midpoint.schema.processor.ResourceObjectDefinition;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.processor.ResourceObjectClassDefinition;
import com.evolveum.midpoint.util.exception.SchemaException;

import org.jetbrains.annotations.Nullable;

import javax.xml.namespace.QName;
import java.util.Collection;

public interface ResourceObjectClassDefinitionDelegator extends ResourceObjectDefinitionDelegator, ResourceObjectClassDefinition {

    @Override
    ResourceObjectClassDefinition delegate();

    @Override
    default String getNativeObjectClassName() {
        return delegate().getNativeObjectClassName();
    }

    @Override
    default boolean isAuxiliary() {
        return delegate().isAuxiliary();
    }

    @Override
    default boolean isEmbedded() {
        return delegate().isEmbedded();
    }

    @Override
    default boolean isDefaultAccountDefinition() {
        return delegate().isDefaultAccountDefinition();
    }

    @Override
    default boolean isRaw() {
        return delegate().isRaw();
    }

    @Override
    default boolean hasRefinements() {
        return delegate().hasRefinements();
    }

    @Override
    default @Nullable QName getNamingAttributeName() {
        return ResourceObjectDefinitionDelegator.super.getNamingAttributeName();
    }

    @Override
    default @Nullable QName getDisplayNameAttributeName() {
        return ResourceObjectDefinitionDelegator.super.getDisplayNameAttributeName();
    }

    @Override
    default @Nullable QName getDescriptionAttributeName() {
        return ResourceObjectDefinitionDelegator.super.getDescriptionAttributeName();
    }

    @Override
    default @NotNull ObjectQuery createShadowSearchQuery(String resourceOid) {
        return delegate().createShadowSearchQuery(resourceOid);
    }

    @Override
    @NotNull
    default Collection<? extends ResourceObjectDefinition> getAuxiliaryDefinitions() {
        return delegate().getAuxiliaryDefinitions();
    }

    @Override
    @NotNull
    default ResourceObjectDefinition getEffectiveDefinition() {
        return delegate().getEffectiveDefinition();
    }
}
