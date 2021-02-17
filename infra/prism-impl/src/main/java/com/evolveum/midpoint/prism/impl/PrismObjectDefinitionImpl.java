/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.prism.impl;

import java.util.function.Consumer;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.util.DefinitionUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import org.jetbrains.annotations.NotNull;

/**
 * MidPoint Object Definition.
 *
 * Objects are storable entities in midPoint.
 *
 * This is mostly just a marker class to identify object boundaries in schema.
 *
 * This class represents schema definition for objects. See {@link Definition}
 * for more details.
 *
 * "Instance" class of this class is MidPointObject, not Object - to avoid
 * confusion with java.lang.Object.
 *
 * @author Radovan Semancik
 *
 */
public class PrismObjectDefinitionImpl<O extends Objectable> extends PrismContainerDefinitionImpl<O> implements
        MutablePrismObjectDefinition<O> {
    private static final long serialVersionUID = -8298581031956931008L;

    public PrismObjectDefinitionImpl(QName elementName, ComplexTypeDefinition complexTypeDefinition, PrismContext prismContext,
            Class<O> compileTimeClass) {
        // Object definition can only be top-level, hence null parent
        super(elementName, complexTypeDefinition, prismContext, compileTimeClass);
    }

    @Override
    @NotNull
    public PrismObject<O> instantiate() throws SchemaException {
        if (isAbstract()) {
            throw new SchemaException("Cannot instantiate abstract definition "+this);
        }
        return new PrismObjectImpl<>(getItemName(), this, prismContext);
    }

    @NotNull
    @Override
    public PrismObject<O> instantiate(QName name) throws SchemaException {
        if (isAbstract()) {
            throw new SchemaException("Cannot instantiate abstract definition "+this);
        }
        name = DefinitionUtil.addNamespaceIfApplicable(name, this.itemName);
        return new PrismObjectImpl<>(name, this, prismContext);
    }

    @NotNull
    @Override
    public PrismObjectDefinitionImpl<O> clone() {
        PrismObjectDefinitionImpl<O> clone = new PrismObjectDefinitionImpl<>(itemName, complexTypeDefinition, prismContext, compileTimeClass);
        copyDefinitionData(clone);
        return clone;
    }

    @Override
    public PrismObjectDefinition<O> deepClone(boolean ultraDeep, Consumer<ItemDefinition> postCloneAction) {
        return (PrismObjectDefinition<O>) super.deepClone(ultraDeep, postCloneAction);
    }

    @Override
    @NotNull
    public PrismObjectDefinition<O> cloneWithReplacedDefinition(QName itemName, ItemDefinition newDefinition) {
        return (PrismObjectDefinition<O>) super.cloneWithReplacedDefinition(itemName, newDefinition);
    }

    @Override
    public PrismContainerDefinition<?> getExtensionDefinition() {
        return findContainerDefinition(getExtensionQName());
    }

//    public void setExtensionDefinition(ComplexTypeDefinition extensionComplexTypeDefinition) {
//        QName extensionQName = getExtensionQName();
//
//        PrismContainerDefinition<Containerable> oldExtensionDef = findContainerDefinition(extensionQName);
//
//        PrismContainerDefinitionImpl<?> newExtensionDef = new PrismContainerDefinitionImpl<>(extensionQName,
//                extensionComplexTypeDefinition, prismContext);
//        newExtensionDef.setRuntimeSchema(true);
//        if (oldExtensionDef != null) {
//            if (newExtensionDef.getDisplayName() == null) {
//                newExtensionDef.setDisplayName(oldExtensionDef.getDisplayName());
//            }
//            if (newExtensionDef.getDisplayOrder() == null) {
//                newExtensionDef.setDisplayOrder(oldExtensionDef.getDisplayOrder());
//            }
//            if (newExtensionDef.getHelp() == null) {
//                newExtensionDef.setHelp(oldExtensionDef.getHelp());
//            }
//        }
//
//        ComplexTypeDefinitionImpl newCtd = (ComplexTypeDefinitionImpl) this.complexTypeDefinition.clone();
//        newCtd.replaceDefinition(extensionQName, newExtensionDef);
//        if (newCtd.getDisplayName() == null) {
//            newCtd.setDisplayName(this.complexTypeDefinition.getDisplayName());
//        }
//        if (newCtd.getDisplayOrder() == null) {
//            newCtd.setDisplayOrder(this.complexTypeDefinition.getDisplayOrder());
//        }
//        if (newCtd.getHelp() == null) {
//            newCtd.setHelp(this.complexTypeDefinition.getHelp());
//        }
//
//        this.complexTypeDefinition = newCtd;
//    }

    @Override
    public PrismObjectValue<O> createValue() {
        return new PrismObjectValueImpl<>(prismContext);
    }


    private ItemName getExtensionQName() {
        String namespace = getItemName().getNamespaceURI();
        return new ItemName(namespace, PrismConstants.EXTENSION_LOCAL_NAME);
    }

//    public <I extends ItemDefinition> I getExtensionItemDefinition(QName elementName) {
//        PrismContainerDefinition<?> extensionDefinition = getExtensionDefinition();
//        if (extensionDefinition == null) {
//            return null;
//        }
//        return (I) extensionDefinition.findItemDefinition(elementName);
//    }

    @Override
    public String getDebugDumpClassName() {
        return "POD";
    }

    @Override
    public String getDocClassName() {
        return "object";
    }

    @Override
    public MutablePrismObjectDefinition<O> toMutable() {
        checkMutableOnExposing();
        return this;
    }
}
