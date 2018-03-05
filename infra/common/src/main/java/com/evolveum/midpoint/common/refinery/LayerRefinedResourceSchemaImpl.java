/*
 * Copyright (c) 2010-2017 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.evolveum.midpoint.common.refinery;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.schema.ResourceShadowDiscriminator;
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

	private final RefinedResourceSchema refinedResourceSchema;
	private final LayerType layer;

	LayerRefinedResourceSchemaImpl(RefinedResourceSchema refinedResourceSchema, LayerType layer) {
		this.refinedResourceSchema = refinedResourceSchema;
		this.layer = layer;
	}

	@Override
	public LayerType getLayer() {
		return layer;
	}

	@Override
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

	@Override
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
		result = prime * result + ((layer == null) ? 0 : layer.hashCode());
		result = prime * result + ((refinedResourceSchema == null) ? 0 : refinedResourceSchema.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		LayerRefinedResourceSchemaImpl other = (LayerRefinedResourceSchemaImpl) obj;
		if (layer != other.layer)
			return false;
		if (refinedResourceSchema == null) {
			if (other.refinedResourceSchema != null)
				return false;
		} else if (!refinedResourceSchema.equals(other.refinedResourceSchema))
			return false;
		return true;
	}

	@Override
	public String debugDump() {
		return debugDump(0);
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

}
