package com.evolveum.midpoint.schema.processor.deleg;

import java.util.Collection;
import javax.xml.namespace.QName;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.schema.processor.CompositeObjectDefinition;
import com.evolveum.midpoint.schema.processor.ResourceObjectDefinition;

public interface CompositeObjectDefinitionDelegator extends ResourceObjectDefinitionDelegator, CompositeObjectDefinition {

    @Override
    CompositeObjectDefinition delegate();

    @Override
    default @NotNull Collection<QName> getConfiguredAuxiliaryObjectClassNames() {
        return delegate().getConfiguredAuxiliaryObjectClassNames();
    }

    @Override
    default @NotNull ResourceObjectDefinition getStructuralDefinition() {
        return delegate().getStructuralDefinition();
    }

    @Override
    default @NotNull Collection<? extends ResourceObjectDefinition> getAuxiliaryDefinitions() {
        return delegate().getAuxiliaryDefinitions();
    }
}
