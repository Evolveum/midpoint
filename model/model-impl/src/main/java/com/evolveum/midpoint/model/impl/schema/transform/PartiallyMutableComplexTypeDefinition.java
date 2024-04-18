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

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.ComplexTypeDefinition.ComplexTypeDefinitionMutator;
import com.evolveum.midpoint.prism.annotation.ItemDiagramSpecification;
import com.evolveum.midpoint.schema.processor.ResourceObjectClassDefinition;

interface PartiallyMutableComplexTypeDefinition extends ComplexTypeDefinitionMutator {

    @Override
    default void setOptionalCleanup(boolean optionalCleanup) {
        throw new IllegalStateException("ComplexTypeDefinition is not modifiable");
    }

    default void setInstantiationOrder(Integer order) {
        throw new IllegalStateException("ComplexTypeDefinition is not modifiable");

    }

    @Override
    default void setDeprecated(boolean deprecated) {
        throw new IllegalStateException("ComplexTypeDefinition is not modifiable");
    }

    @Override
    default void setRemoved(boolean removed) {
        throw new IllegalStateException("ComplexTypeDefinition is not modifiable");
    }

    @Override
    default void setRemovedSince(String removedSince) {
        throw new IllegalStateException("ComplexTypeDefinition is not modifiable");
    }

    @Override
    default void setPlannedRemoval(String plannedRemoval) {
        throw new IllegalStateException("ComplexTypeDefinition is not modifiable");
    }

    @Override
    default void setDeprecatedSince(String deprecatedSince) {
        throw new IllegalStateException("ComplexTypeDefinition is not modifiable");
    }

    @Override
    default void setExperimental(boolean experimental) {
        throw new IllegalStateException("ComplexTypeDefinition is not modifiable");
    }

    @Override
    default void setDisplayHint(DisplayHint displayHint) {
        throw new IllegalStateException("ComplexTypeDefinition is not modifiable");
    }

    @Override
    default void setMergerIdentifier(String mergerIdentifier) {
        throw new IllegalStateException("ComplexTypeDefinition is not modifiable");
    }

    @Override
    default void setNaturalKeyConstituents(List<QName> naturalKeyConstituents) {
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
    default void setDocumentation(String value) {
        throw new IllegalStateException("ComplexTypeDefinition is not modifiable");
    }

    @Override
    default void addSchemaMigration(SchemaMigration schemaMigration) {
        throw new IllegalStateException("ComplexTypeDefinition is not modifiable");
    }

    @Override
    default void setSchemaMigrations(List<SchemaMigration> value) {
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
    default PrismPropertyDefinition.PrismPropertyDefinitionMutator<?> createPropertyDefinition(QName name, QName typeName) {
        throw new IllegalStateException("ComplexTypeDefinition is not modifiable");
    }

    @Override
    default PrismPropertyDefinition.PrismPropertyDefinitionMutator<?> createPropertyDefinition(String name, QName typeName) {
        throw new IllegalStateException("ComplexTypeDefinition is not modifiable");
    }

    @Override
    default @NotNull ComplexTypeDefinition clone() {
        throw new IllegalStateException("ComplexTypeDefinition is not modifiable");
    }

    default void setExtensionForType(QName type) {
        throw new IllegalStateException("ComplexTypeDefinition is not modifiable");

    }

    default void setAbstract(boolean value) {
        throw new IllegalStateException("ComplexTypeDefinition is not modifiable");

    }

    default void setSuperType(QName superType) {
        throw new IllegalStateException("ComplexTypeDefinition is not modifiable");

    }

    default void setObjectMarker(boolean value) {
        throw new IllegalStateException("ComplexTypeDefinition is not modifiable");

    }

    default void setContainerMarker(boolean value) {
        throw new IllegalStateException("ComplexTypeDefinition is not modifiable");

    }

    default void setReferenceMarker(boolean value) {
        throw new IllegalStateException("ComplexTypeDefinition is not modifiable");

    }

    default void setDefaultNamespace(String namespace) {
        throw new IllegalStateException("ComplexTypeDefinition is not modifiable");

    }

    default void setIgnoredNamespaces(@NotNull List<String> ignoredNamespaces) {
        throw new IllegalStateException("ComplexTypeDefinition is not modifiable");

    }

    default void setXsdAnyMarker(boolean value) {
        throw new IllegalStateException("ComplexTypeDefinition is not modifiable");

    }

    default void setListMarker(boolean value) {
        throw new IllegalStateException("ComplexTypeDefinition is not modifiable");

    }

    default void setCompileTimeClass(Class<?> compileTimeClass) {
        throw new IllegalStateException("ComplexTypeDefinition is not modifiable");

    }

//    @Override
//    default void replaceDefinition(@NotNull QName itemName, ItemDefinition newDefinition) {
//        throw new IllegalStateException("ComplexTypeDefinition is not modifiable");
//
//    }

    @Override
    default void addSubstitution(ItemDefinition<?> itemDef, ItemDefinition<?> maybeSubst) {
        throw new IllegalStateException("ComplexTypeDefinition is not modifiable");
    }

    @Override
    default void setDiagrams(List<ItemDiagramSpecification> value) {
        throw new IllegalStateException("ComplexTypeDefinition is not modifiable");
    }

    // FIXME temporarily commented out
//    interface ObjectClassDefinition extends PartiallyMutableComplexTypeDefinition, ResourceObjectClassDefinition.ResourceObjectClassDefinitionMutator {
//
//        @Override
//        default void add(ItemDefinition<?> definition) {
//            throw new IllegalStateException("ComplexTypeDefinition is not modifiable");
//        }
//
//        @Override
//        @NotNull ResourceObjectClassDefinition clone();
//    }
}
