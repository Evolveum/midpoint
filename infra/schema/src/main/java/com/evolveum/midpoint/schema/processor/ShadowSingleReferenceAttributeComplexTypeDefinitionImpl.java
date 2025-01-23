/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.processor;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.util.QNameUtil;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.annotation.ItemDiagramSpecification;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.schemaContext.SchemaContextDefinition;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowReferenceAttributesType;

import static com.evolveum.midpoint.util.MiscUtil.castOrNull;

/**
 * Implementation of a CTD for a {@link ShadowAttributesContainer} providing reference attributes only.
 */
class ShadowSingleReferenceAttributeComplexTypeDefinitionImpl
        extends BaseShadowItemsContainerTypeDefinitionImpl
        implements ShadowAttributesComplexTypeDefinition {

    @NotNull private final ShadowReferenceAttributeDefinition defaultObjectRefDefinition;

    ShadowSingleReferenceAttributeComplexTypeDefinitionImpl(
            @NotNull ShadowReferenceAttributeDefinition defaultObjectRefDefinition) {
        this.defaultObjectRefDefinition = defaultObjectRefDefinition;
    }

    @Override
    public @NotNull List<? extends ItemDefinition<?>> getDefinitions() {
        return List.of(defaultObjectRefDefinition);
    }

    @Override
    public <ID extends ItemDefinition<?>> ID findItemDefinition(@NotNull ItemPath path, @NotNull Class<ID> clazz) {
        if (path.equivalent(defaultObjectRefDefinition.getItemName())) {
            //noinspection unchecked
            return (ID) defaultObjectRefDefinition;
        } else {
            return null;
        }
    }

    @Override
    public @Nullable ShadowAttributeDefinition<?, ?, ?, ?> findAttributeDefinition(QName name, boolean caseInsensitive) {
        if (QNameUtil.match(defaultObjectRefDefinition.getItemName(), name, caseInsensitive)) {
            return defaultObjectRefDefinition;
        } else {
            return null;
        }
    }

    @Override
    public <ID extends ItemDefinition<?>> ID findLocalItemDefinition(
            @NotNull QName name, @NotNull Class<ID> clazz, boolean caseInsensitive) {
        return castOrNull(
                findAttributeDefinition(name, caseInsensitive),
                clazz);
    }

    @Override
    public @NotNull QName getTypeName() {
        return ShadowReferenceAttributesType.COMPLEX_TYPE;
    }

    @Override
    public boolean isRuntimeSchema() {
        return true;
    }

    @Override
    public boolean isAbstract() {
        return false;
    }

    @Override
    public boolean isOptionalCleanup() {
        return false;
    }

    @Override
    public boolean isElaborate() {
        return false;
    }

    @Override
    public Class<?> getTypeClass() {
        return ShadowReferenceAttributesType.class;
    }

    @Override
    public <A> A getAnnotation(QName qname) {
        return null;
    }

    @Override
    public @Nullable Map<QName, Object> getAnnotations() {
        return Map.of();
    }

    @Override
    public @Nullable SchemaContextDefinition getSchemaContextDefinition() {
        return null;
    }

    @Override
    public @NotNull ShadowSingleReferenceAttributeComplexTypeDefinitionImpl clone() {
        return new ShadowSingleReferenceAttributeComplexTypeDefinitionImpl(defaultObjectRefDefinition);
    }

    @Override
    public void trimTo(@NotNull Collection<ItemPath> paths) {
        // ignoring this
    }

    @Override
    public String toString() {
        return "ShadowSingleReferenceAttributeComplexTypeDefinitionImpl";
    }

    @Override
    public @NotNull ResourceObjectDefinition getResourceObjectDefinition() {
        throw new UnsupportedOperationException(); // FIXME resolve this somehow
    }

    @Override
    public boolean isImmutable() {
        return true;
    }

    @Override
    public void freeze() {
    }

    @Override
    public boolean isDeprecated() {
        return false;
    }

    @Override
    public String getDeprecatedSince() {
        return null;
    }

    @Override
    public String getPlannedRemoval() {
        return null;
    }

    @Override
    public boolean isRemoved() {
        return false;
    }

    @Override
    public String getRemovedSince() {
        return null;
    }

    @Override
    public boolean isExperimental() {
        return false;
    }

    @Override
    public @Nullable List<SchemaMigration> getSchemaMigrations() {
        return List.of();
    }

    @Override
    public DisplayHint getDisplayHint() {
        return null;
    }

    @Override
    public boolean isEmphasized() {
        return false;
    }

    @Override
    public String getDisplayName() {
        return null;
    }

    @Override
    public Integer getDisplayOrder() {
        return null;
    }

    @Override
    public String getHelp() {
        return null;
    }

    @Override
    public String getDocumentation() {
        return null;
    }

    @Override
    public List<ItemDiagramSpecification> getDiagrams() {
        return List.of();
    }

    @Override
    public String getDocumentationPreview() {
        return null;
    }

    @Override
    public void revive(PrismContext prismContext) {

    }

    @Override
    public boolean accept(Visitor<Definition> visitor, SmartVisitation<Definition> visitation) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void accept(Visitor<Definition> visitor) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String debugDump(int indent) {
        return toString();
    }

    @Override
    public @NotNull List<? extends ShadowAttributeDefinition<?, ?, ?, ?>> getAttributeDefinitions() {
        return List.of(defaultObjectRefDefinition);
    }

    @Override
    public @NotNull Collection<? extends ShadowSimpleAttributeDefinition<?>> getPrimaryIdentifiers() {
        return List.of();
    }

    @Override
    public @NotNull Collection<QName> getPrimaryIdentifiersNames() {
        return List.of();
    }

    @Override
    public @NotNull Collection<? extends ShadowSimpleAttributeDefinition<?>> getSecondaryIdentifiers() {
        return List.of();
    }

    @Override
    public @NotNull Collection<QName> getSecondaryIdentifiersNames() {
        return List.of();
    }
}
