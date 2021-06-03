package com.evolveum.midpoint.schema.processor.deleg;

import java.util.Collection;
import javax.xml.namespace.QName;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.deleg.ComplexTypeDefinitionDelegator;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.processor.ObjectClassComplexTypeDefinition;
import com.evolveum.midpoint.schema.processor.ResourceAttributeContainer;
import com.evolveum.midpoint.schema.processor.ResourceAttributeContainerDefinition;
import com.evolveum.midpoint.schema.processor.ResourceAttributeDefinition;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

public interface ObjectClassTypeDefinitionDelegator extends ComplexTypeDefinitionDelegator, ObjectClassComplexTypeDefinition {

    @Override
    ObjectClassComplexTypeDefinition delegate();

    @Override
    default @NotNull Collection<? extends ResourceAttributeDefinition<?>> getAttributeDefinitions() {
        return delegate().getAttributeDefinitions();
    }

    @Override
    default <X> @Nullable ResourceAttributeDefinition<X> findAttributeDefinition(QName name) {
        return delegate().findAttributeDefinition(name);
    }

    @Override
    default <X> @Nullable ResourceAttributeDefinition<X> findAttributeDefinition(QName name, boolean caseInsensitive) {
        return delegate().findAttributeDefinition(name, caseInsensitive);
    }

    @Override
    default <X> ResourceAttributeDefinition<X> findAttributeDefinition(String name) {
        return delegate().findAttributeDefinition(name);
    }

    @Override
    default @NotNull Collection<? extends ResourceAttributeDefinition<?>> getPrimaryIdentifiers() {
        return delegate().getPrimaryIdentifiers();
    }

    @Override
    default boolean isPrimaryIdentifier(QName attrName) {
        return delegate().isPrimaryIdentifier(attrName);
    }

    @Override
    default @NotNull Collection<? extends ResourceAttributeDefinition<?>> getSecondaryIdentifiers() {
        return delegate().getSecondaryIdentifiers();
    }

    @Override
    default boolean isSecondaryIdentifier(QName attrName) {
        return delegate().isSecondaryIdentifier(attrName);
    }

    @Override
    default <X> ResourceAttributeDefinition<X> getDescriptionAttribute() {
        return delegate().getDescriptionAttribute();
    }

    @Override
    default <X> ResourceAttributeDefinition<X> getNamingAttribute() {
        return delegate().getNamingAttribute();
    }

    @Override
    default <X> ResourceAttributeDefinition<X> getDisplayNameAttribute() {
        return delegate().getDisplayNameAttribute();
    }

    @Override
    default Collection<? extends ResourceAttributeDefinition<?>> getAllIdentifiers() {
        return delegate().getAllIdentifiers();
    }

    @Override
    default String getNativeObjectClass() {
        return delegate().getNativeObjectClass();
    }

    @Override
    default boolean isAuxiliary() {
        return delegate().isAuxiliary();
    }

    @Override
    default ShadowKindType getKind() {
        return delegate().getKind();
    }

    @Override
    default boolean isDefaultInAKind() {
        return delegate().isDefaultInAKind();
    }

    @Override
    default String getIntent() {
        return delegate().getIntent();
    }

    @Override
    default ResourceAttributeContainerDefinition toResourceAttributeContainerDefinition() {
        return delegate().toResourceAttributeContainerDefinition();
    }

    @Override
    default ResourceAttributeContainerDefinition toResourceAttributeContainerDefinition(QName elementName) {
        return delegate().toResourceAttributeContainerDefinition(elementName);
    }

    @Override
    default ObjectQuery createShadowSearchQuery(String resourceOid) throws SchemaException {
        return delegate().createShadowSearchQuery(resourceOid);
    }

    @Override
    default ResourceAttributeContainer instantiate(QName elementName) {
        return delegate().instantiate(elementName);
    }

    @Deprecated
    @Override
    default boolean matches(ShadowType shadow) {
        return delegate().matches(shadow);
    }
}
