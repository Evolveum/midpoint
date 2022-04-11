package com.evolveum.midpoint.schema.processor.deleg;

import java.util.Collection;
import java.util.List;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.schema.processor.*;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.deleg.ContainerDefinitionDelegator;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowAttributesType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

public interface AttributeContainerDefinitionDelegator extends ContainerDefinitionDelegator<ShadowAttributesType>, ResourceAttributeContainerDefinition {

    @Override
    ResourceAttributeContainerDefinition delegate();

    @Override
    default Collection<? extends ResourceAttributeDefinition<?>> getPrimaryIdentifiers() {
        return delegate().getPrimaryIdentifiers();
    }

    @Override
    default Collection<? extends ResourceAttributeDefinition<?>> getSecondaryIdentifiers() {
        return delegate().getSecondaryIdentifiers();
    }

    @Override
    default Collection<? extends ResourceAttributeDefinition<?>> getAllIdentifiers() {
        return delegate().getAllIdentifiers();
    }

    @Override
    default ResourceAttributeDefinition<?> getDescriptionAttribute() {
        return delegate().getDescriptionAttribute();
    }

    @Override
    default ResourceAttributeDefinition<?> getNamingAttribute() {
        return delegate().getNamingAttribute();
    }

    @Override
    default String getNativeObjectClass() {
        return delegate().getNativeObjectClass();
    }

    @Override
    default boolean isDefaultAccountDefinition() {
        return delegate().isDefaultAccountDefinition();
    }

    @Override
    default ResourceAttributeDefinition<?> getDisplayNameAttribute() {
        return delegate().getDisplayNameAttribute();
    }

    @Override
    default <T> ResourceAttributeDefinition<T> findAttributeDefinition(QName elementQName, boolean caseInsensitive) {
        return delegate().findAttributeDefinition(elementQName, caseInsensitive);
    }

    @Override
    default ResourceAttributeDefinition<?> findAttributeDefinition(ItemPath elementPath) {
        return delegate().findAttributeDefinition(elementPath);
    }

    @Override
    default ResourceAttributeDefinition<?> findAttributeDefinition(String localName) {
        return delegate().findAttributeDefinition(localName);
    }

    @Override
    default List<? extends ResourceAttributeDefinition<?>> getAttributeDefinitions() {
        return delegate().getAttributeDefinitions();
    }

    @Override
    default <T extends ShadowType> @NotNull PrismObjectDefinition<T> toShadowDefinition() {
        return delegate().toShadowDefinition();
    }

    @Override
    default @NotNull ResourceAttributeContainer instantiate() {
        return delegate().instantiate();
    }

    @Override
    default @NotNull ResourceAttributeContainer instantiate(QName name) {
        return delegate().instantiate(name);
    }

    @Override
    default @NotNull List<? extends ResourceAttributeDefinition<?>> getDefinitions() {
        return delegate().getDefinitions();
    }

    @Override
    default ResourceObjectDefinition getComplexTypeDefinition() {
        return delegate().getComplexTypeDefinition();
    }

}
