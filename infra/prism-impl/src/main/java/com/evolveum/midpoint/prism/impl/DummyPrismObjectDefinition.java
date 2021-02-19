/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.prism.impl;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.ContainerDelta;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.annotation.Experimental;

import org.jetbrains.annotations.NotNull;

import javax.xml.namespace.QName;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * EXPERIMENTAL
 */
@Experimental
public class DummyPrismObjectDefinition implements PrismObjectDefinition<Objectable> {

    @NotNull
    @Override
    public ItemName getItemName() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getNamespace() {
        throw new UnsupportedOperationException();
    }

    @Override
    public int getMinOccurs() {
        throw new UnsupportedOperationException();
    }

    @Override
    public int getMaxOccurs() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isSingleValue() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isMultiValue() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isMandatory() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isOptional() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isOperational() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isIndexOnly() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isInherited() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isDynamic() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean canRead() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean canModify() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean canAdd() {
        throw new UnsupportedOperationException();
    }

    @Override
    public QName getSubstitutionHead() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isHeterogeneousListItem() {
        throw new UnsupportedOperationException();
    }

    @Override
    public PrismReferenceValue getValueEnumerationRef() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isValidFor(QName elementQName, Class<? extends ItemDefinition> clazz) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isValidFor(@NotNull QName elementQName, @NotNull Class<? extends ItemDefinition> clazz,
            boolean caseInsensitive) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void adoptElementDefinitionFrom(ItemDefinition otherDef) {
    }

    @NotNull
    @Override
    public PrismObject<Objectable> instantiate() {
        throw new UnsupportedOperationException();
    }

    @NotNull
    @Override
    public PrismObject<Objectable> instantiate(QName name) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <ID extends ItemDefinition> ID findLocalItemDefinition(@NotNull QName name, @NotNull Class<ID> clazz,
            boolean caseInsensitive) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <T extends ItemDefinition> T findItemDefinition(@NotNull ItemPath path, @NotNull Class<T> clazz) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <ID extends ItemDefinition> ID findNamedItemDefinition(@NotNull QName firstName, @NotNull ItemPath rest,
            @NotNull Class<ID> clazz) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Class<Objectable> getCompileTimeClass() {
        throw new UnsupportedOperationException();
    }

    @Override
    public ComplexTypeDefinition getComplexTypeDefinition() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void revive(PrismContext prismContext) {
    }

    @Override
    public void debugDumpShortToString(StringBuilder sb) {
    }

    @Override
    public boolean canBeDefinitionOf(PrismContainer<Objectable> item) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean canBeDefinitionOf(PrismValue pvalue) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getDefaultNamespace() {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<String> getIgnoredNamespaces() {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<? extends ItemDefinition> getDefinitions() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isCompletelyDefined() {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<PrismPropertyDefinition> getPropertyDefinitions() {
        throw new UnsupportedOperationException();
    }

    @Override
    public ContainerDelta<Objectable> createEmptyDelta(ItemPath path) {
        throw new UnsupportedOperationException();
    }

    @NotNull
    @Override
    public PrismObjectDefinition<Objectable> clone() {
        throw new UnsupportedOperationException();
    }

    @Override
    public PrismObjectDefinition<Objectable> deepClone(boolean ultraDeep, Consumer<ItemDefinition> postCloneAction) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ItemDefinition<PrismContainer<Objectable>> deepClone(Map<QName, ComplexTypeDefinition> ctdMap,
            Map<QName, ComplexTypeDefinition> onThisPath, Consumer<ItemDefinition> postCloneAction) {
        throw new UnsupportedOperationException();
    }

    @Override
    public @NotNull PrismObjectDefinition<Objectable> cloneWithReplacedDefinition(QName itemName, ItemDefinition newDefinition) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void replaceDefinition(QName itemName, ItemDefinition newDefinition) {
    }

    @Override
    public PrismContainerDefinition<?> getExtensionDefinition() {
        throw new UnsupportedOperationException();
    }

    @Override
    public PrismObjectValue<Objectable> createValue() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isEmpty() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean canRepresent(@NotNull QName type) {
        throw new UnsupportedOperationException();
    }

    @Override
    public MutablePrismObjectDefinition<Objectable> toMutable() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isImmutable() {
        throw new UnsupportedOperationException();
    }

    @NotNull
    @Override
    public QName getTypeName() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isRuntimeSchema() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isIgnored() {
        throw new UnsupportedOperationException();
    }

    @Override
    public ItemProcessing getProcessing() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isAbstract() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isDeprecated() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isExperimental() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getPlannedRemoval() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isElaborate() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getDeprecatedSince() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isEmphasized() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getDisplayName() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Integer getDisplayOrder() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getHelp() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getDocumentation() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getDocumentationPreview() {
        throw new UnsupportedOperationException();
    }

    @Override
    public PrismContext getPrismContext() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Class getTypeClassIfKnown() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Class<Objectable> getTypeClass() {
        throw new UnsupportedOperationException();
    }

    @Override
    public <A> A getAnnotation(QName qname) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <A> void setAnnotation(QName qname, A value) {
    }

    @Override
    public List<SchemaMigration> getSchemaMigrations() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void accept(Visitor visitor) {
    }

    @Override
    public boolean accept(Visitor<Definition> visitor, SmartVisitation<Definition> visitation) {
        return false;
    }

    @Override
    public String debugDump(int indent) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void freeze() {
    }
}
