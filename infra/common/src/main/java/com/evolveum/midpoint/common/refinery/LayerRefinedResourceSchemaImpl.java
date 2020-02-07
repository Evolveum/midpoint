/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.common.refinery;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.schema.ResourceShadowDiscriminator;
import com.evolveum.midpoint.schema.processor.MutableResourceSchema;
import com.evolveum.midpoint.schema.processor.ObjectClassComplexTypeDefinition;
import com.evolveum.midpoint.schema.processor.ResourceSchema;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LayerType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Document;

import javax.xml.namespace.QName;
import java.util.Collection;
import java.util.List;

/**
 * @author semancik
 *
 * This class enhances RefinedResourceSchema with a layer-specific view.
 *
 * TODO: However, there are a few unresolved issues that should be dealt with:
 *
 * 1) Although it might seem to contain LayerRefinedObjectClassDefinitions (LROCDs), it is not the case:
 * it generates them on the fly by calling LROCD.wrap method every time.
 *
 * 2) When accessing attributes via findItemDefinition of this object, a non-layered version
 * of attribute container is returned.
 *
 */
public class LayerRefinedResourceSchemaImpl implements LayerRefinedResourceSchema {

    @NotNull private final RefinedResourceSchema refinedResourceSchema;
    @NotNull private final LayerType layer;

    LayerRefinedResourceSchemaImpl(@NotNull RefinedResourceSchema refinedResourceSchema, @NotNull LayerType layer) {
        this.refinedResourceSchema = refinedResourceSchema;
        this.layer = layer;
    }

    @NotNull
    @Override
    public LayerType getLayer() {
        return layer;
    }

    public List<? extends RefinedObjectClassDefinition> getRefinedDefinitions(ShadowKindType kind) {
        return LayerRefinedObjectClassDefinitionImpl
                .wrapCollection(refinedResourceSchema.getRefinedDefinitions(kind), layer);
    }

    @Override
    public ResourceSchema getOriginalResourceSchema() {
        return refinedResourceSchema.getOriginalResourceSchema();
    }

    @Override
    public LayerRefinedObjectClassDefinition getRefinedDefinition(ShadowKindType kind, ShadowType shadow) {
        return LayerRefinedObjectClassDefinitionImpl
                .wrap(refinedResourceSchema.getRefinedDefinition(kind, shadow), layer);
    }

    @NotNull
    @Override
    public String getNamespace() {
        return refinedResourceSchema.getNamespace();
    }

    @Override
    public LayerRefinedObjectClassDefinition getRefinedDefinition(ShadowKindType kind, String intent) {
        return LayerRefinedObjectClassDefinitionImpl
                .wrap(refinedResourceSchema.getRefinedDefinition(kind, intent), layer);
    }

    @Override
    @NotNull
    public Collection<Definition> getDefinitions() {
        return refinedResourceSchema.getDefinitions();
    }

    @Override
    public CompositeRefinedObjectClassDefinition determineCompositeObjectClassDefinition(
            ResourceShadowDiscriminator discriminator) {
        return refinedResourceSchema.determineCompositeObjectClassDefinition(discriminator);
    }

    @Override
    public CompositeRefinedObjectClassDefinition determineCompositeObjectClassDefinition(
            PrismObject<ShadowType> shadow) throws SchemaException {
        return refinedResourceSchema.determineCompositeObjectClassDefinition(shadow);
    }

    @Override
    @NotNull
    public <T extends Definition> List<T> getDefinitions(@NotNull Class<T> type) {
        return refinedResourceSchema.getDefinitions(type);
    }

    @Override
    public CompositeRefinedObjectClassDefinition determineCompositeObjectClassDefinition(
            PrismObject<ShadowType> shadow, Collection<QName> additionalAuxiliaryObjectClassQNames) throws SchemaException {
        return refinedResourceSchema.determineCompositeObjectClassDefinition(shadow, additionalAuxiliaryObjectClassQNames);
    }


    @Override
    public CompositeRefinedObjectClassDefinition determineCompositeObjectClassDefinition(
            QName structuralObjectClassQName, ShadowKindType kind, String intent) {
        return refinedResourceSchema.determineCompositeObjectClassDefinition(structuralObjectClassQName, kind, intent);
    }

    @Override
    public PrismContext getPrismContext() {
        return refinedResourceSchema.getPrismContext();
    }

    @Override
    @NotNull
    public Document serializeToXsd() throws SchemaException {
        return refinedResourceSchema.serializeToXsd();
    }

    @Override
    public boolean isEmpty() {
        return refinedResourceSchema.isEmpty();
    }

    @Override
    public RefinedObjectClassDefinition getRefinedDefinition(ShadowKindType kind, Collection<String> intents)
            throws SchemaException {
        return refinedResourceSchema.getRefinedDefinition(kind, intents);
    }

    @Override
    public LayerRefinedObjectClassDefinition getRefinedDefinition(QName typeName) {
        return LayerRefinedObjectClassDefinitionImpl
                .wrap(refinedResourceSchema.getRefinedDefinition(typeName), layer);
    }

    @Override
    public LayerRefinedObjectClassDefinition getDefaultRefinedDefinition(ShadowKindType kind) {
        return LayerRefinedObjectClassDefinitionImpl
                .wrap(refinedResourceSchema.getDefaultRefinedDefinition(kind), layer);
    }

    public LayerRefinedObjectClassDefinition findRefinedDefinitionByObjectClassQName(ShadowKindType kind, QName objectClass) {
        return LayerRefinedObjectClassDefinitionImpl
                .wrap(refinedResourceSchema.findRefinedDefinitionByObjectClassQName(kind, objectClass), layer);
    }

    @Override
    public ObjectClassComplexTypeDefinition findObjectClassDefinition(QName objectClassQName) {
        return refinedResourceSchema.findObjectClassDefinition(objectClassQName);
    }

    @Override
    public LayerRefinedResourceSchema forLayer(LayerType layer) {
        return refinedResourceSchema.forLayer(layer);
    }

    @NotNull
    @Override
    public <ID extends ItemDefinition> List<ID> findItemDefinitionsByCompileTimeClass(
            @NotNull Class<?> compileTimeClass, @NotNull Class<ID> definitionClass) {
        return refinedResourceSchema.findItemDefinitionsByCompileTimeClass(compileTimeClass, definitionClass);
    }

    @Override
    @Nullable
    public <ID extends ItemDefinition> ID findItemDefinitionByType(@NotNull QName typeName,
            @NotNull Class<ID> definitionClass) {
        return refinedResourceSchema.findItemDefinitionByType(typeName, definitionClass);
    }

    @Override
    @NotNull
    public <ID extends ItemDefinition> List<ID> findItemDefinitionsByElementName(@NotNull QName elementName,
            @NotNull Class<ID> definitionClass) {
        return refinedResourceSchema.findItemDefinitionsByElementName(elementName, definitionClass);
    }

    @Override
    @Nullable
    public <TD extends TypeDefinition> TD findTypeDefinitionByCompileTimeClass(@NotNull Class<?> compileTimeClass, @NotNull Class<TD> definitionClass) {
        return refinedResourceSchema.findTypeDefinitionByCompileTimeClass(compileTimeClass, definitionClass);
    }

    @Override
    @Nullable
    public <TD extends TypeDefinition> TD findTypeDefinitionByType(@NotNull QName typeName, @NotNull Class<TD> definitionClass) {
        return refinedResourceSchema.findTypeDefinitionByType(typeName, definitionClass);
    }

    @NotNull
    @Override
    public <TD extends TypeDefinition> Collection<? extends TD> findTypeDefinitionsByType(@NotNull QName typeName,
            @NotNull Class<TD> definitionClass) {
        return refinedResourceSchema.findTypeDefinitionsByType(typeName, definitionClass);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + layer.hashCode();
        result = prime * result + refinedResourceSchema.hashCode();
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        LayerRefinedResourceSchemaImpl other = (LayerRefinedResourceSchemaImpl) obj;
        if (layer != other.layer) return false;
        if (!refinedResourceSchema.equals(other.refinedResourceSchema)) {
            return false;
        }
        return true;
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = new StringBuilder();
        DebugUtil.indentDebugDump(sb, indent);
        sb.append("LRSchema(layer=").append(layer).append(",\n");
        sb.append(refinedResourceSchema.debugDump(indent + 1));
        return sb.toString();
    }

    @Override
    public ObjectClassComplexTypeDefinition findObjectClassDefinition(
            ShadowKindType kind, String intent) {
        return refinedResourceSchema.findObjectClassDefinition(kind, intent);
    }

    @Override
    public List<? extends RefinedObjectClassDefinition> getRefinedDefinitions() {
        return refinedResourceSchema.getRefinedDefinitions();
    }

    @Override
    public ObjectClassComplexTypeDefinition findDefaultObjectClassDefinition(
            ShadowKindType kind) {
        return refinedResourceSchema.findDefaultObjectClassDefinition(kind);
    }

    @Override
    public MutableResourceSchema toMutable() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void freeze() {
        // TODO should we freeze referenced refinedResourceSchema?
    }
}
