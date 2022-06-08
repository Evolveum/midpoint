package com.evolveum.midpoint.schema.processor.deleg;

import com.evolveum.midpoint.schema.processor.ResourceObjectDefinition;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.processor.ResourceAttributeContainer;
import com.evolveum.midpoint.schema.processor.ResourceObjectClassDefinition;
import com.evolveum.midpoint.util.exception.SchemaException;

import java.util.Collection;

public interface ResourceObjectClassDefinitionDelegator extends ResourceObjectDefinitionDelegator, ResourceObjectClassDefinition {

    @Override
    ResourceObjectClassDefinition delegate();

    @Override
    default String getNativeObjectClass() {
        return delegate().getNativeObjectClass();
    }

    @Override
    default boolean isAuxiliary() {
        return delegate().isAuxiliary();
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
    default @NotNull ObjectQuery createShadowSearchQuery(String resourceOid) throws SchemaException {
        return delegate().createShadowSearchQuery(resourceOid);
    }

    @Override
    @NotNull
    default Collection<ResourceObjectDefinition> getAuxiliaryDefinitions() {
        return delegate().getAuxiliaryDefinitions();
    }

    @Override
    default ResourceAttributeContainer instantiate(ItemName elementName) {
        return delegate().instantiate(elementName);
    }
}
