/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.processor;

import java.util.ArrayList;
import java.util.List;
import javax.xml.namespace.QName;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.ComplexTypeDefinition;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.impl.PrismContainerDefinitionImpl;
import com.evolveum.midpoint.prism.util.DefinitionUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowAssociationsType;

public class ShadowAssociationsContainerDefinitionImpl
        extends PrismContainerDefinitionImpl<ShadowAssociationsType>
        implements ShadowAssociationsContainerDefinition {

    ShadowAssociationsContainerDefinitionImpl(QName name, @NotNull ComplexTypeDefinition complexTypeDefinition) {
        super(name, complexTypeDefinition);
        super.setCompileTimeClass(ShadowAssociationsType.class);
        isRuntimeSchema = true;
    }

    @Override
    public @NotNull ShadowAssociationsContainer instantiate() {
        return instantiate(getItemName());
    }

    @Override
    public @NotNull ShadowAssociationsContainer instantiate(QName name) {
        name = DefinitionUtil.addNamespaceIfApplicable(name, this.itemName);
        return new ShadowAssociationsContainerImpl(name, this);
    }

    @SuppressWarnings("MethodDoesntCallSuperMethod")
    @Override
    public @NotNull ShadowAssociationsContainerDefinitionImpl clone() {
        ShadowAssociationsContainerDefinitionImpl clone =
                new ShadowAssociationsContainerDefinitionImpl(itemName, complexTypeDefinition);
        clone.copyDefinitionDataFrom(this);
        return clone;
    }

    @SuppressWarnings("WeakerAccess") // open for subclassing
    protected void copyDefinitionDataFrom(ShadowAssociationsContainerDefinition source) {
        super.copyDefinitionDataFrom(source);
    }

    @Override
    public String toString() {
        return "SAssocCD: " + complexTypeDefinition;
    }

    @Override
    public @NotNull List<? extends ShadowAssociationDefinition> getDefinitions() {
        return getAssociationsDefinitions();
    }

    @Override
    public @NotNull List<? extends ShadowAssociationDefinition> getAssociationsDefinitions() {
        // TODO Remove copying
        List<ShadowAssociationDefinition> assocDefs = new ArrayList<>();
        for (ItemDefinition<?> def : complexTypeDefinition.getDefinitions()) {
            if (def instanceof ShadowAssociationDefinition associationDefinition) {
                assocDefs.add(associationDefinition);
            } else {
                throw new IllegalStateException(
                        "Found " + def + " in associations container definition, only association definitions are expected here");
            }
        }
        return assocDefs;
    }
}
