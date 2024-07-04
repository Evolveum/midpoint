/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.processor;

import java.io.Serial;
import java.util.Collection;
import java.util.List;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.impl.PrismContainerDefinitionImpl;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.schema.SerializableComplexTypeDefinition;
import com.evolveum.midpoint.prism.util.DefinitionUtil;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowAttributesType;

import org.jetbrains.annotations.NotNull;

/**
 * Resource Object Definition (Object Class).
 *
 * Object Class refers to a type of object on the Resource. Unix account, Active
 * Directory group, inetOrgPerson LDAP objectclass or a schema of USERS database
 * table are all Object Classes from the midPoint point of view. Object class
 * defines a set of attribute names, types for each attributes and few
 * additional properties.
 *
 * This class represents schema definition for resource object (object class).
 * See {@link Definition} for more details.
 *
 * Resource Object Definition is immutable. TODO: This will probably need to be changed to a mutable object.
 *
 * @author Radovan Semancik
 *
 */
public class ShadowAttributesContainerDefinitionImpl
        extends PrismContainerDefinitionImpl<ShadowAttributesType>
        implements ShadowAttributesContainerDefinition {

    @Serial private static final long serialVersionUID = 3943909626639924429L;

    ShadowAttributesContainerDefinitionImpl(QName name, @NotNull ComplexTypeDefinition complexTypeDefinition) {
        super(name, complexTypeDefinition);
        super.setCompileTimeClass(ShadowAttributesType.class);
        isRuntimeSchema = true;
    }

    @Override
    public ShadowAttributesComplexTypeDefinition getComplexTypeDefinition() {
        return (ShadowAttributesComplexTypeDefinition) super.getComplexTypeDefinition();
    }

    @Override
    public SerializableComplexTypeDefinition getComplexTypeDefinitionToSerialize() {
        return null; // We won't serialize this
    }

    @Override
    public Collection<? extends ShadowSimpleAttributeDefinition<?>> getPrimaryIdentifiers() {
        return getComplexTypeDefinition().getPrimaryIdentifiers();
    }

    @Override
    public Collection<? extends ShadowSimpleAttributeDefinition<?>> getSecondaryIdentifiers() {
        return getComplexTypeDefinition().getSecondaryIdentifiers();
    }

    @Override
    public Collection<? extends ShadowSimpleAttributeDefinition<?>> getAllIdentifiers() {
        return getComplexTypeDefinition().getAllIdentifiers();
    }

    @NotNull
    @Override
    public ShadowAttributesContainer instantiate() {
        return instantiate(getItemName());
    }

    @NotNull
    @Override
    public ShadowAttributesContainer instantiate(QName name) {
        name = DefinitionUtil.addNamespaceIfApplicable(name, this.itemName);
        return new ShadowAttributesContainerImpl(name, this);
    }

    @SuppressWarnings("MethodDoesntCallSuperMethod")
    @NotNull
    @Override
    public ShadowAttributesContainerDefinitionImpl clone() {
        ShadowAttributesContainerDefinitionImpl clone =
                new ShadowAttributesContainerDefinitionImpl(itemName, complexTypeDefinition);
        clone.copyDefinitionDataFrom(this);
        return clone;
    }

    @SuppressWarnings("WeakerAccess") // open for subclassing
    protected void copyDefinitionDataFrom(ShadowAttributesContainerDefinition source) {
        super.copyDefinitionDataFrom(source);
    }

    @Override
    public <T> ShadowSimpleAttributeDefinition<T> findAttributeDefinition(ItemPath elementPath) {
        if (elementPath.isSingleName()) {
            // this is a bit of hack
            //noinspection unchecked
            return findLocalItemDefinition(elementPath.asSingleNameOrFail(), ShadowSimpleAttributeDefinition.class, false);
        } else {
            //noinspection unchecked
            return findItemDefinition(elementPath, ShadowSimpleAttributeDefinition.class);
        }
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = new StringBuilder();
        sb.append(DebugDumpable.INDENT_STRING.repeat(Math.max(0, indent)));
        sb.append(this);
        for (var def : getDefinitions()) {
            sb.append("\n");
            sb.append(def.debugDump(indent+1));
            if (getComplexTypeDefinition().isPrimaryIdentifier(def.getItemName())) {
                sb.deleteCharAt(sb.length()-1);
                sb.append(" id");
            }
        }
        return sb.toString();
    }

    @Override
    public String toString() {
        return "RACD: " + complexTypeDefinition;
    }

    @Override
    public @NotNull List<? extends ItemDefinition<?>> getDefinitions() {
        return getComplexTypeDefinition().getDefinitions();
    }

    @Override
    public @NotNull List<? extends ShadowSimpleAttributeDefinition<?>> getSimpleAttributesDefinitions() {
        return getComplexTypeDefinition().getSimpleAttributeDefinitions();
    }

    @Override
    public @NotNull ResourceObjectDefinition getResourceObjectDefinition() {
        return getComplexTypeDefinition().getResourceObjectDefinition();
    }

    @Override
    public boolean isUsedInSimpleAssociationObject() {
        return complexTypeDefinition instanceof ShadowSingleReferenceAttributeComplexTypeDefinitionImpl;
    }
}
