package com.evolveum.midpoint.schema.processor.deleg;

import javax.xml.namespace.QName;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.deleg.PropertyDefinitionDelegator;
import com.evolveum.midpoint.schema.processor.ObjectClassComplexTypeDefinition;
import com.evolveum.midpoint.schema.processor.ResourceAttribute;
import com.evolveum.midpoint.schema.processor.ResourceAttributeContainerDefinition;
import com.evolveum.midpoint.schema.processor.ResourceAttributeDefinition;

public interface AttributeDefinitionDelegator<T> extends PropertyDefinitionDelegator<T>, ResourceAttributeDefinition<T> {

    @Override
    ResourceAttributeDefinition<T> delegate();

    @Override
    default Boolean getReturnedByDefault() {
        return delegate().getReturnedByDefault();
    }

    @Override
    default boolean isReturnedByDefault() {
        return delegate().isReturnedByDefault();
    }

    @Override
    default boolean isPrimaryIdentifier(ResourceAttributeContainerDefinition objectDefinition) {
        return delegate().isPrimaryIdentifier(objectDefinition);
    }

    @Override
    default boolean isPrimaryIdentifier(ObjectClassComplexTypeDefinition objectDefinition) {
        return delegate().isPrimaryIdentifier(objectDefinition);
    }

    @Override
    default boolean isSecondaryIdentifier(ObjectClassComplexTypeDefinition objectDefinition) {
        return delegate().isSecondaryIdentifier(objectDefinition);
    }

    @Override
    default String getNativeAttributeName() {
        return delegate().getNativeAttributeName();
    }

    @Override
    default String getFrameworkAttributeName() {
        return delegate().getFrameworkAttributeName();
    }

    @Override
    default @NotNull ResourceAttribute<T> instantiate() {
        return delegate().instantiate();
    }

    @Override
    default @NotNull ResourceAttribute<T> instantiate(QName name) {
        return delegate().instantiate(name);
    }

}
