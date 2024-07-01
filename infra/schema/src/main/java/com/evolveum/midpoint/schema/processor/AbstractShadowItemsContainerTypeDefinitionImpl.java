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
import java.util.Optional;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.delta.ItemMerger;
import com.evolveum.midpoint.prism.key.NaturalKeyDefinition;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.annotation.ItemDiagramSpecification;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.QNameUtil;

/**
 * Abstract implementation of a CTD for `attributes` and `associations` containers.
 */
abstract class AbstractShadowItemsContainerTypeDefinitionImpl
        extends BaseShadowItemsContainerTypeDefinitionImpl {

    @NotNull final ResourceObjectDefinition objectDefinition;

    AbstractShadowItemsContainerTypeDefinitionImpl(@NotNull ResourceObjectDefinition objectDefinition) {
        this.objectDefinition = objectDefinition;
    }

    @Override
    public void trimTo(@NotNull Collection<ItemPath> paths) {
        objectDefinition.trimAttributesTo(paths);
    }

    // TODO do we really want to return e.g. inetOrgPerson as the type name for `attributes` and `associations` container?!
    @Override
    public @NotNull QName getTypeName() {
        return objectDefinition.getTypeName();
    }

    @Override
    public boolean isRuntimeSchema() {
        return objectDefinition.isRuntimeSchema();
    }

    @Override
    public boolean isAbstract() {
        return objectDefinition.isAbstract();
    }

    @Override
    public boolean isOptionalCleanup() {
        return objectDefinition.isOptionalCleanup();
    }

    @Override
    public boolean isElaborate() {
        return objectDefinition.isElaborate();
    }

    @Override
    public Class<?> getTypeClass() {
        return objectDefinition.getTypeClass();
    }

    @Override
    public <A> A getAnnotation(QName qname) {
        return objectDefinition.getAnnotation(qname);
    }

    @Override
    public @Nullable Map<QName, Object> getAnnotations() {
        return objectDefinition.getAnnotations();
    }

    @Override
    public boolean isImmutable() {
        return objectDefinition.isImmutable();
    }

    @Override
    public void freeze() {
        objectDefinition.freeze();
    }

    @Override
    public boolean isDeprecated() {
        return objectDefinition.isDeprecated();
    }

    @Override
    public String getDeprecatedSince() {
        return objectDefinition.getDeprecatedSince();
    }

    @Override
    public String getPlannedRemoval() {
        return objectDefinition.getPlannedRemoval();
    }

    @Override
    public boolean isRemoved() {
        return objectDefinition.isRemoved();
    }

    @Override
    public String getRemovedSince() {
        return objectDefinition.getRemovedSince();
    }

    @Override
    public boolean isExperimental() {
        return objectDefinition.isExperimental();
    }

    @Override
    public @Nullable List<SchemaMigration> getSchemaMigrations() {
        // TODO filter out irrelevant migrations
        //  But currently there are no migrations, anyway; so this is OK for now.
        return objectDefinition.getSchemaMigrations();
    }

    @Override
    public DisplayHint getDisplayHint() {
        return objectDefinition.getDisplayHint();
    }

    @Override
    public boolean isEmphasized() {
        return objectDefinition.isEmphasized();
    }

    @Override
    public String getDisplayName() {
        return objectDefinition.getDisplayName();
    }

    @Override
    public Integer getDisplayOrder() {
        return objectDefinition.getDisplayOrder();
    }

    @Override
    public String getHelp() {
        return objectDefinition.getHelp();
    }

    @Override
    public String getDocumentation() {
        return objectDefinition.getDocumentation();
    }

    @Override
    public List<ItemDiagramSpecification> getDiagrams() {
        return objectDefinition.getDiagrams();
    }

    @Override
    public String getDocumentationPreview() {
        return objectDefinition.getDocumentationPreview();
    }

    @Override
    public void revive(PrismContext prismContext) {
        objectDefinition.revive(prismContext);
    }

    @Override
    public boolean accept(Visitor<Definition> visitor, SmartVisitation<Definition> visitation) {
        return objectDefinition.accept(visitor, visitation);
    }

    @Override
    public void accept(Visitor<Definition> visitor) {
        objectDefinition.accept(visitor);
    }

    // FIXME
    @Override
    public String debugDump(int indent) {
        return objectDefinition.debugDump(indent);
    }

    @Override
    public @NotNull ResourceObjectDefinition getResourceObjectDefinition() {
        return objectDefinition;
    }

    @Override
    public abstract @NotNull AbstractShadowItemsContainerTypeDefinitionImpl clone();
}
