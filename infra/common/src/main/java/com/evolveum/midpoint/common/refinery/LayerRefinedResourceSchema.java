/*
 * Copyright (c) 2010-2013 Evolveum
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

import java.util.Collection;

import javax.xml.namespace.QName;

import org.w3c.dom.Document;

import com.evolveum.midpoint.prism.ComplexTypeDefinition;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.Definition;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.Objectable;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.PrismReferenceDefinition;
import com.evolveum.midpoint.schema.processor.ObjectClassComplexTypeDefinition;
import com.evolveum.midpoint.schema.processor.ResourceSchema;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LayerType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

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
public class LayerRefinedResourceSchema extends RefinedResourceSchema {
	
	private RefinedResourceSchema refinedResourceSchema;
	private LayerType layer;
	
	private LayerRefinedResourceSchema(RefinedResourceSchema refinedResourceSchema, LayerType layer) {
		super(refinedResourceSchema.getPrismContext());
		this.refinedResourceSchema = refinedResourceSchema;
		this.layer = layer;
	}
	
	static LayerRefinedResourceSchema wrap(RefinedResourceSchema rSchema, LayerType layer) {
		return new LayerRefinedResourceSchema(rSchema, layer);
	}

	public String getNamespace() {
		return refinedResourceSchema.getNamespace();
	}

	public Collection<? extends RefinedObjectClassDefinition> getRefinedDefinitions(ShadowKindType kind) {
		return LayerRefinedObjectClassDefinition.wrapCollection(refinedResourceSchema.getRefinedDefinitions(kind), layer);
	}

	public Collection<Definition> getDefinitions() {
		return refinedResourceSchema.getDefinitions();
	}

	public ResourceSchema getOriginalResourceSchema() {
		return refinedResourceSchema.getOriginalResourceSchema();
	}

	public <T extends Definition> Collection<T> getDefinitions(Class<T> type) {
		return refinedResourceSchema.getDefinitions(type);
	}

	public LayerRefinedObjectClassDefinition getRefinedDefinition(ShadowKindType kind, ShadowType shadow) {
		return LayerRefinedObjectClassDefinition.wrap(refinedResourceSchema.getRefinedDefinition(kind, shadow),layer);
	}

	public LayerRefinedObjectClassDefinition getRefinedDefinition(ShadowKindType kind, String intent) {
		return LayerRefinedObjectClassDefinition.wrap(refinedResourceSchema.getRefinedDefinition(kind, intent),layer);
	}
	
	public LayerRefinedObjectClassDefinition getRefinedDefinition(QName typeName) {
		return LayerRefinedObjectClassDefinition.wrap(refinedResourceSchema.getRefinedDefinition(typeName),layer);
	}

	public void add(Definition def) {
		refinedResourceSchema.add(def);
	}

	public PrismContext getPrismContext() {
		return refinedResourceSchema.getPrismContext();
	}

	public LayerRefinedObjectClassDefinition getDefaultRefinedDefinition(ShadowKindType kind) {
		return LayerRefinedObjectClassDefinition.wrap(refinedResourceSchema.getDefaultRefinedDefinition(kind),layer);
	}

	public PrismObjectDefinition<ShadowType> getObjectDefinition(ShadowKindType kind, String intent) {
		return refinedResourceSchema.getObjectDefinition(kind, intent);
	}

	public PrismObjectDefinition<ShadowType> getObjectDefinition(ShadowKindType kind, ShadowType shadow) {
		return refinedResourceSchema.getObjectDefinition(kind, shadow);
	}

	public PrismContainerDefinition findContainerDefinitionByType(QName typeName) {
		return refinedResourceSchema.findContainerDefinitionByType(typeName);
	}

	public <X extends Objectable> PrismObjectDefinition<X> findObjectDefinitionByType(QName typeName) {
		return refinedResourceSchema.findObjectDefinitionByType(typeName);
	}

	public <X extends Objectable> PrismObjectDefinition<X> findObjectDefinitionByElementName(QName elementName) {
		return refinedResourceSchema.findObjectDefinitionByElementName(elementName);
	}

	public <T extends Objectable> PrismObjectDefinition<T> findObjectDefinitionByType(QName typeName, Class<T> type) {
		return refinedResourceSchema.findObjectDefinitionByType(typeName, type);
	}

	public <T extends Objectable> PrismObjectDefinition<T> findObjectDefinitionByCompileTimeClass(Class<T> type) {
		return refinedResourceSchema.findObjectDefinitionByCompileTimeClass(type);
	}

	public PrismPropertyDefinition findPropertyDefinitionByElementName(QName elementName) {
		return refinedResourceSchema.findPropertyDefinitionByElementName(elementName);
	}

	public <T extends ItemDefinition> T findItemDefinition(QName definitionName, Class<T> definitionType) {
		return refinedResourceSchema.findItemDefinition(definitionName, definitionType);
	}

	public <T extends ItemDefinition> T findItemDefinition(String localName, Class<T> definitionType) {
		return refinedResourceSchema.findItemDefinition(localName, definitionType);
	}

	public <T extends ItemDefinition> T findItemDefinitionByType(QName typeName, Class<T> definitionType) {
		return refinedResourceSchema.findItemDefinitionByType(typeName, definitionType);
	}

	public PrismContainerDefinition createPropertyContainerDefinition(String localTypeName) {
		return refinedResourceSchema.createPropertyContainerDefinition(localTypeName);
	}

	public PrismContainerDefinition createPropertyContainerDefinition(String localElementName, String localTypeName) {
		return refinedResourceSchema.createPropertyContainerDefinition(localElementName, localTypeName);
	}

	public ComplexTypeDefinition createComplexTypeDefinition(QName typeName) {
		return refinedResourceSchema.createComplexTypeDefinition(typeName);
	}

	public PrismPropertyDefinition createPropertyDefinition(String localName, QName typeName) {
		return refinedResourceSchema.createPropertyDefinition(localName, typeName);
	}

	public PrismPropertyDefinition createPropertyDefinition(String localName, String localTypeName) {
		return refinedResourceSchema.createPropertyDefinition(localName, localTypeName);
	}

	public PrismPropertyDefinition createPropertyDefinition(QName name, QName typeName) {
		return refinedResourceSchema.createPropertyDefinition(name, typeName);
	}

	public LayerRefinedObjectClassDefinition findRefinedDefinitionByObjectClassQName(ShadowKindType kind, QName objectClass) {
		return LayerRefinedObjectClassDefinition.wrap(refinedResourceSchema.findRefinedDefinitionByObjectClassQName(kind, objectClass),layer);
	}

	public PrismContainerDefinition findContainerDefinitionByElementName(QName elementName) {
		return refinedResourceSchema.findContainerDefinitionByElementName(elementName);
	}

	public ComplexTypeDefinition findComplexTypeDefinition(QName typeName) {
		return refinedResourceSchema.findComplexTypeDefinition(typeName);
	}

	public void setNamespace(String namespace) {
		refinedResourceSchema.setNamespace(namespace);
	}

	public Document serializeToXsd() throws SchemaException {
		return refinedResourceSchema.serializeToXsd();
	}

	public boolean isEmpty() {
		return refinedResourceSchema.isEmpty();
	}
	
	public Collection<ObjectClassComplexTypeDefinition> getObjectClassDefinitions() {
		return refinedResourceSchema.getObjectClassDefinitions();
	}

	public ObjectClassComplexTypeDefinition createObjectClassDefinition(String localTypeName) {
		return refinedResourceSchema.createObjectClassDefinition(localTypeName);
	}

	public ObjectClassComplexTypeDefinition createObjectClassDefinition(QName typeName) {
		return refinedResourceSchema.createObjectClassDefinition(typeName);
	}

	public Collection<? extends RefinedObjectClassDefinition> getRefinedDefinitions() {
		return refinedResourceSchema.getRefinedDefinitions();
	}

	public ObjectClassComplexTypeDefinition findObjectClassDefinition(ShadowType shadow) {
		return refinedResourceSchema.findObjectClassDefinition(shadow);
	}

	public ObjectClassComplexTypeDefinition findObjectClassDefinition(String localName) {
		return refinedResourceSchema.findObjectClassDefinition(localName);
	}

	public Collection<PrismObjectDefinition> getObjectDefinitions() {
		return refinedResourceSchema.getObjectDefinitions();
	}

	public Collection<ComplexTypeDefinition> getComplexTypeDefinitions() {
		return refinedResourceSchema.getComplexTypeDefinitions();
	}

	public ObjectClassComplexTypeDefinition findObjectClassDefinition(ShadowKindType kind, String intent) {
		return refinedResourceSchema.findObjectClassDefinition(kind, intent);
	}

	public ObjectClassComplexTypeDefinition findDefaultObjectClassDefinition(ShadowKindType kind) {
		return refinedResourceSchema.findDefaultObjectClassDefinition(kind);
	}

	public ObjectClassComplexTypeDefinition findObjectClassDefinition(QName objectClassQName) {
		return refinedResourceSchema.findObjectClassDefinition(objectClassQName);
	}

	public <X extends Objectable> PrismObjectDefinition<X> findObjectDefinitionByTypeAssumeNs(QName typeName) {
		return refinedResourceSchema.findObjectDefinitionByTypeAssumeNs(typeName);
	}

	public <C extends Containerable> PrismContainerDefinition<C> findContainerDefinitionByCompileTimeClass(
			Class<C> type) {
		return refinedResourceSchema.findContainerDefinitionByCompileTimeClass(type);
	}

	public PrismReferenceDefinition findReferenceDefinitionByElementName(QName elementName) {
		return refinedResourceSchema.findReferenceDefinitionByElementName(elementName);
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
		LayerRefinedResourceSchema other = (LayerRefinedResourceSchema) obj;
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
		sb.append(refinedResourceSchema.debugDump(indent+1));
		return sb.toString();
	}
	
}
