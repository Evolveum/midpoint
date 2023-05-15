/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.processor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.impl.PrismContainerDefinitionImpl;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.util.DefinitionUtil;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowAttributesType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
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
public class ResourceAttributeContainerDefinitionImpl
        extends PrismContainerDefinitionImpl<ShadowAttributesType>
        implements ResourceAttributeContainerDefinition {

    private static final long serialVersionUID = 3943909626639924429L;

    public ResourceAttributeContainerDefinitionImpl(QName name, ComplexTypeDefinition complexTypeDefinition) {
        super(name, complexTypeDefinition);
        super.setCompileTimeClass(ShadowAttributesType.class);
        isRuntimeSchema = true;
    }

    @Override
    public ResourceObjectDefinition getComplexTypeDefinition() {
        return (ResourceObjectDefinition) super.getComplexTypeDefinition();
    }

    @Override
    public Collection<? extends ResourceAttributeDefinition<?>> getPrimaryIdentifiers() {
        return getComplexTypeDefinition().getPrimaryIdentifiers();
    }

    @Override
    public Collection<? extends ResourceAttributeDefinition<?>> getSecondaryIdentifiers() {
        return getComplexTypeDefinition().getSecondaryIdentifiers();
    }

    @Override
    public Collection<? extends ResourceAttributeDefinition<?>> getAllIdentifiers() {
        return getComplexTypeDefinition().getAllIdentifiers();
    }

    @Override
    public ResourceAttributeDefinition<?> getDescriptionAttribute() {
        return getComplexTypeDefinition().getDescriptionAttribute();
    }

    public void setDescriptionAttribute(QName name) {
        // We can afford to delegate a set here as we know that there is one-to-one correspondence between
        // object class definition and attribute container
        ((ResourceObjectClassDefinitionImpl) getComplexTypeDefinition())
                .setDescriptionAttributeName(name);
    }

    @Override
    public ResourceAttributeDefinition<?> getNamingAttribute() {
        return getComplexTypeDefinition().getNamingAttribute();
    }

    public void setNamingAttribute(ResourceAttributeDefinition<?> namingAttribute) {
        // We can afford to delegate a set here as we know that there is one-to-one correspondence between
        // object class definition and attribute container
        ((ResourceObjectClassDefinitionImpl) getComplexTypeDefinition()).setNamingAttributeName(namingAttribute.getItemName());
    }

    public void setNamingAttribute(QName namingAttribute) {
        ((ResourceObjectClassDefinitionImpl) getComplexTypeDefinition()).setNamingAttributeName(namingAttribute);
    }

    @Override
    public String getNativeObjectClass() {
        return getComplexTypeDefinition()
                .getObjectClassDefinition()
                .getNativeObjectClass();
    }

    public void setNativeObjectClass(String nativeObjectClass) {
        // We can afford to delegate a set here as we know that there is one-to-one correspondence between
        // object class definition and attribute container
        ((ResourceObjectClassDefinitionImpl) getComplexTypeDefinition()).setNativeObjectClass(nativeObjectClass);
    }

    @Override
    public boolean isDefaultAccountDefinition() {
        return getComplexTypeDefinition()
                .getObjectClassDefinition()
                .isDefaultAccountDefinition();
    }

    @Override
    public ResourceAttributeDefinition<?> getDisplayNameAttribute() {
        return getComplexTypeDefinition().getDisplayNameAttribute();
    }

    public void setDisplayNameAttribute(ResourceAttributeDefinition<?> displayName) {
        ((ResourceObjectClassDefinitionImpl) getComplexTypeDefinition()).setDisplayNameAttributeName(displayName.getItemName());
    }

    /**
     * TODO
     *
     * Convenience method. It will internally look up the correct definition.
     */
    public void setDisplayNameAttribute(QName displayName) {
        ((ResourceObjectClassDefinitionImpl) getComplexTypeDefinition()).setDisplayNameAttributeName(displayName);
    }

    @NotNull
    @Override
    public ResourceAttributeContainer instantiate() {
        return instantiate(getItemName());
    }

    @NotNull
    @Override
    public ResourceAttributeContainer instantiate(QName name) {
        name = DefinitionUtil.addNamespaceIfApplicable(name, this.itemName);
        return new ResourceAttributeContainerImpl(name, this);
    }

    @SuppressWarnings("MethodDoesntCallSuperMethod")
    @NotNull
    @Override
    public ResourceAttributeContainerDefinitionImpl clone() {
        ResourceAttributeContainerDefinitionImpl clone =
                new ResourceAttributeContainerDefinitionImpl(itemName, complexTypeDefinition);
        clone.copyDefinitionDataFrom(this);
        return clone;
    }

    @SuppressWarnings("WeakerAccess") // open for subclassing
    protected void copyDefinitionDataFrom(ResourceAttributeContainerDefinition source) {
        super.copyDefinitionDataFrom(source);
    }

    @Override
    public <T> ResourceAttributeDefinition<T> findAttributeDefinition(QName elementQName, boolean caseInsensitive) {
        var ctd = complexTypeDefinition;
        if (ctd instanceof ResourceObjectDefinition) {
            // Shortcut to more efficient lookup implementation - FIXME this is a hack
            //noinspection unchecked
            return (ResourceAttributeDefinition<T>)
                    ((ResourceObjectDefinition) ctd).findAttributeDefinition(elementQName, caseInsensitive);
        }
        //noinspection unchecked
        return findLocalItemDefinition(ItemName.fromQName(elementQName), ResourceAttributeDefinition.class, caseInsensitive);
    }

    @Override
    public ResourceAttributeDefinition<?> findAttributeDefinition(ItemPath elementPath) {
        if (elementPath.isSingleName()) {
            // this is a bit of hack
            return findLocalItemDefinition(elementPath.asSingleNameOrFail(), ResourceAttributeDefinition.class, false);
        } else {
            return findItemDefinition(elementPath, ResourceAttributeDefinition.class);
        }
    }

    @Override
    public ResourceAttributeDefinition<?> findAttributeDefinition(String localName) {
        ItemName elementQName = new ItemName(getItemName().getNamespaceURI(), localName);
        return findAttributeDefinition(elementQName);
    }

    @Override
    public List<? extends ResourceAttributeDefinition<?>> getAttributeDefinitions() {
        List<ResourceAttributeDefinition<?>> attrs = new ArrayList<>();
        for (ItemDefinition<?> def: complexTypeDefinition.getDefinitions()) {
            if (def instanceof ResourceAttributeDefinition) {
                attrs.add((ResourceAttributeDefinition<?>)def);
            } else {
                throw new IllegalStateException("Found "+def+" in resource attribute container, only attribute definitions are expected here");
            }
        }
        return attrs;
    }

    @Override
    public @NotNull <T extends ShadowType> PrismObjectDefinition<T> toShadowDefinition() {
        //noinspection unchecked
        PrismObjectDefinition<T> origShadowDef =  (PrismObjectDefinition<T>) getPrismContext().getSchemaRegistry().
            findObjectDefinitionByCompileTimeClass(ShadowType.class);
        return origShadowDef.cloneWithReplacedDefinition(ShadowType.F_ATTRIBUTES, this);
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = new StringBuilder();
        sb.append(DebugDumpable.INDENT_STRING.repeat(Math.max(0, indent)));
        sb.append(this);
        for (ResourceAttributeDefinition<?> def : getDefinitions()) {
            sb.append("\n");
            sb.append(def.debugDump(indent+1));
            if (((ResourceObjectDefinition) super.getComplexTypeDefinition())
                    .isPrimaryIdentifier(def.getItemName())) {
                sb.deleteCharAt(sb.length()-1);
                sb.append(" id");
            }
        }
        return sb.toString();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getClass().getSimpleName()).append(":").append(getItemName()).append(" (").append(getTypeName()).append(")");
        if (isDefaultAccountDefinition()) {
            sb.append(" def");
        }
        if (getNativeObjectClass()!=null) {
            sb.append(" native=");
            sb.append(getNativeObjectClass());
        }
        return sb.toString();
    }

    // Only attribute definitions should be here.
    @Override
    public @NotNull List<? extends ResourceAttributeDefinition<?>> getDefinitions() {
        return getAttributeDefinitions();
    }
}
