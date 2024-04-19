package com.evolveum.midpoint.schema.processor.deleg;

import java.util.Collection;
import java.util.List;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.schema.processor.*;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.deleg.ContainerDefinitionDelegator;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowAttributesType;

public interface ResourceAttributeContainerDefinitionDelegator
        extends ContainerDefinitionDelegator<ShadowAttributesType>, ResourceAttributeContainerDefinition {

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
    default ResourceAttributeDefinition<?> findAttributeDefinition(ItemPath elementPath) {
        return delegate().findAttributeDefinition(elementPath);
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
    default ShadowAttributesComplexTypeDefinition getComplexTypeDefinition() {
        return delegate().getComplexTypeDefinition();
    }

}
