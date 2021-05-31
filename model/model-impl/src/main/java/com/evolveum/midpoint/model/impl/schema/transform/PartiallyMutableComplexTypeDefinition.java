/*
 * Copyright (c) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.schema.transform;

import java.util.List;
import javax.xml.namespace.QName;

import org.jetbrains.annotations.NotNull;
import com.evolveum.midpoint.prism.ComplexTypeDefinition;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.ItemProcessing;
import com.evolveum.midpoint.prism.MutableComplexTypeDefinition;
import com.evolveum.midpoint.prism.MutablePrismPropertyDefinition;
import com.evolveum.midpoint.prism.SchemaMigration;
import com.evolveum.midpoint.schema.processor.MutableObjectClassComplexTypeDefinition;
import com.evolveum.midpoint.schema.processor.ResourceAttributeDefinition;
import com.evolveum.midpoint.schema.processor.ResourceAttributeDefinitionImpl;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;
import com.google.common.annotations.VisibleForTesting;

interface PartiallyMutableComplexTypeDefinition extends MutableComplexTypeDefinition {

    @Override
    default void setInstantiationOrder(Integer order) {
        throw new IllegalStateException("ComplexTypeDefinition is not modifiable");

    }

    @Override
    default void setProcessing(ItemProcessing processing) {
        throw new IllegalStateException("ComplexTypeDefinition is not modifiable");

    }

    @Override
    default void setDeprecated(boolean deprecated) {
        throw new IllegalStateException("ComplexTypeDefinition is not modifiable");

    }

    @Override
    default void setExperimental(boolean experimental) {
        throw new IllegalStateException("ComplexTypeDefinition is not modifiable");

    }

    @Override
    default void setEmphasized(boolean emphasized) {
        throw new IllegalStateException("ComplexTypeDefinition is not modifiable");

    }

    @Override
    default void setDisplayName(String displayName) {
        throw new IllegalStateException("ComplexTypeDefinition is not modifiable");

    }

    @Override
    default void setDisplayOrder(Integer displayOrder) {
        throw new IllegalStateException("ComplexTypeDefinition is not modifiable");

    }

    @Override
    default void setHelp(String help) {
        throw new IllegalStateException("ComplexTypeDefinition is not modifiable");

    }

    @Override
    default void setRuntimeSchema(boolean value) {
        throw new IllegalStateException("ComplexTypeDefinition is not modifiable");

    }

    @Override
    default void setTypeName(QName typeName) {
        throw new IllegalStateException("ComplexTypeDefinition is not modifiable");

    }

    @Override
    default void setDocumentation(String value) {
        throw new IllegalStateException("ComplexTypeDefinition is not modifiable");

    }

    @Override
    default void addSchemaMigration(SchemaMigration schemaMigration) {
        throw new IllegalStateException("ComplexTypeDefinition is not modifiable");

    }

    @Override
    default void add(ItemDefinition<?> definition) {
        throw new IllegalStateException("ComplexTypeDefinition is not modifiable");

    }

    @Override
    default void delete(QName itemName) {
        throw new IllegalStateException("ComplexTypeDefinition is not modifiable");

    }

    @Override
    default MutablePrismPropertyDefinition<?> createPropertyDefinition(QName name, QName typeName) {
        throw new IllegalStateException("ComplexTypeDefinition is not modifiable");
    }

    @Override
    default MutablePrismPropertyDefinition<?> createPropertyDefinition(String name, QName typeName) {
        throw new IllegalStateException("ComplexTypeDefinition is not modifiable");
    }

    @Override
    default @NotNull ComplexTypeDefinition clone() {
        throw new IllegalStateException("ComplexTypeDefinition is not modifiable");
    }

    @Override
    default void setExtensionForType(QName type) {
        throw new IllegalStateException("ComplexTypeDefinition is not modifiable");

    }

    @Override
    default void setAbstract(boolean value) {
        throw new IllegalStateException("ComplexTypeDefinition is not modifiable");

    }

    @Override
    default void setSuperType(QName superType) {
        throw new IllegalStateException("ComplexTypeDefinition is not modifiable");

    }

    @Override
    default void setObjectMarker(boolean value) {
        throw new IllegalStateException("ComplexTypeDefinition is not modifiable");

    }

    @Override
    default void setContainerMarker(boolean value) {
        throw new IllegalStateException("ComplexTypeDefinition is not modifiable");

    }

    @Override
    default void setReferenceMarker(boolean value) {
        throw new IllegalStateException("ComplexTypeDefinition is not modifiable");

    }

    @Override
    default void setDefaultNamespace(String namespace) {
        throw new IllegalStateException("ComplexTypeDefinition is not modifiable");

    }

    @Override
    default void setIgnoredNamespaces(@NotNull List<String> ignoredNamespaces) {
        throw new IllegalStateException("ComplexTypeDefinition is not modifiable");

    }

    @Override
    default void setXsdAnyMarker(boolean value) {
        throw new IllegalStateException("ComplexTypeDefinition is not modifiable");

    }

    @Override
    default void setListMarker(boolean value) {
        throw new IllegalStateException("ComplexTypeDefinition is not modifiable");

    }

    @Override
    default void setCompileTimeClass(Class<?> compileTimeClass) {
        throw new IllegalStateException("ComplexTypeDefinition is not modifiable");

    }

    @Override
    default void replaceDefinition(QName itemName, ItemDefinition newDefinition) {
        throw new IllegalStateException("ComplexTypeDefinition is not modifiable");

    }

    @Override
    default void addSubstitution(ItemDefinition<?> itemDef, ItemDefinition<?> maybeSubst) {
        throw new IllegalStateException("ComplexTypeDefinition is not modifiable");
    }


    public interface ObjectClassDefinition extends PartiallyMutableComplexTypeDefinition, MutableObjectClassComplexTypeDefinition {

        @Override
        default void add(ItemDefinition<?> definition) {
            throw new IllegalStateException("ComplexTypeDefinition is not modifiable");
        }

        @Override
        default void addPrimaryIdentifier(ResourceAttributeDefinition<?> identifier) {
            throw new IllegalStateException("ComplexTypeDefinition is not modifiable");
        }

        @Override
        default void addSecondaryIdentifier(ResourceAttributeDefinition<?> identifier) {
            throw new IllegalStateException("ComplexTypeDefinition is not modifiable");
        }

        @Override
        default void setDescriptionAttribute(ResourceAttributeDefinition<?> descriptionAttribute) {
            throw new IllegalStateException("ComplexTypeDefinition is not modifiable");
        }

        @Override
        default void setNamingAttribute(ResourceAttributeDefinition<?> namingAttribute) {
            throw new IllegalStateException("ComplexTypeDefinition is not modifiable");
        }

        @Override
        default void setNamingAttribute(QName namingAttribute) {
            throw new IllegalStateException("ComplexTypeDefinition is not modifiable");
        }

        @Override
        default void setNativeObjectClass(String nativeObjectClass) {
            throw new IllegalStateException("ComplexTypeDefinition is not modifiable");
        }

        @Override
        default void setAuxiliary(boolean auxiliary) {
            throw new IllegalStateException("ComplexTypeDefinition is not modifiable");
        }

        @Override
        default void setKind(ShadowKindType kind) {
            throw new IllegalStateException("ComplexTypeDefinition is not modifiable");
        }

        @Override
        default void setDefaultInAKind(boolean defaultAccountType) {
            throw new IllegalStateException("ComplexTypeDefinition is not modifiable");
        }

        @Override
        default void setIntent(String intent) {
            throw new IllegalStateException("ComplexTypeDefinition is not modifiable");
        }

        @Override
        default void setDisplayNameAttribute(ResourceAttributeDefinition<?> displayName) {
            throw new IllegalStateException("ComplexTypeDefinition is not modifiable");
        }

        @Override
        default void setDisplayNameAttribute(QName displayName) {
            throw new IllegalStateException("ComplexTypeDefinition is not modifiable");
        }

        @Override
        @VisibleForTesting
        default <X> ResourceAttributeDefinitionImpl<X> createAttributeDefinition(QName name, QName typeName) {
            throw new IllegalStateException("ComplexTypeDefinition is not modifiable");
        }

        @Override
        @VisibleForTesting
        default <X> ResourceAttributeDefinitionImpl<X> createAttributeDefinition(String localName, QName typeName) {
            throw new IllegalStateException("ComplexTypeDefinition is not modifiable");
        }

        @Override
        @VisibleForTesting
        default <X> ResourceAttributeDefinition<X> createAttributeDefinition(String localName, String localTypeName) {
            throw new IllegalStateException("ComplexTypeDefinition is not modifiable");
        }

        @Override
        @NotNull MutableObjectClassComplexTypeDefinition clone();
    }


}
