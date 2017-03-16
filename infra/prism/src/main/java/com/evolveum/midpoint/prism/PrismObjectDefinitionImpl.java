/*
 * Copyright (c) 2010-2016 Evolveum
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

package com.evolveum.midpoint.prism;

import javax.xml.namespace.QName;

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
		PrismObjectDefinition<O> {
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
		return new PrismObject<O>(getName(), this, prismContext);
	}
	
	@NotNull
	@Override
	public PrismObject<O> instantiate(QName name) throws SchemaException {
		if (isAbstract()) {
			throw new SchemaException("Cannot instantiate abstract definition "+this);
		}
        name = addNamespaceIfApplicable(name);
		return new PrismObject<>(name, this, prismContext);
	}
	
	@NotNull
	@Override
	public PrismObjectDefinitionImpl<O> clone() {
		PrismObjectDefinitionImpl<O> clone = new PrismObjectDefinitionImpl<>(name, complexTypeDefinition, prismContext, compileTimeClass);
		copyDefinitionData(clone);
		return clone;
	}
	
	@Override
	public PrismObjectDefinition<O> deepClone(boolean ultraDeep) {
		return (PrismObjectDefinition<O>) super.deepClone(ultraDeep);
	}

	@Override
	public PrismObjectDefinition<O> cloneWithReplacedDefinition(QName itemName, ItemDefinition newDefinition) {
		return (PrismObjectDefinition<O>) super.cloneWithReplacedDefinition(itemName, newDefinition);
	}
	
	@Override
	public PrismContainerDefinition<?> getExtensionDefinition() {
		return findContainerDefinition(getExtensionQName());
	}

	public void setExtensionDefinition(ComplexTypeDefinition extensionComplexTypeDefinition) {
		QName extensionQName = getExtensionQName();
		
		PrismContainerDefinition<Containerable> oldExtensionDef = findContainerDefinition(extensionQName);
		
		PrismContainerDefinitionImpl<?> newExtensionDef = new PrismContainerDefinitionImpl<>(extensionQName,
				extensionComplexTypeDefinition, prismContext);
		newExtensionDef.setRuntimeSchema(true);
		if (oldExtensionDef != null) {
			if (newExtensionDef.getDisplayName() == null) {
				newExtensionDef.setDisplayName(oldExtensionDef.getDisplayName());
			}
			if (newExtensionDef.getDisplayOrder() == null) {
				newExtensionDef.setDisplayOrder(oldExtensionDef.getDisplayOrder());
			}
			if (newExtensionDef.getHelp() == null) {
				newExtensionDef.setHelp(oldExtensionDef.getHelp());
			}
		}
		
		ComplexTypeDefinitionImpl newCtd = (ComplexTypeDefinitionImpl) this.complexTypeDefinition.clone();
		newCtd.replaceDefinition(extensionQName, newExtensionDef);
		if (newCtd.getDisplayName() == null) {
			newCtd.setDisplayName(this.complexTypeDefinition.getDisplayName());
		}
		if (newCtd.getDisplayOrder() == null) {
			newCtd.setDisplayOrder(this.complexTypeDefinition.getDisplayOrder());
		}
		if (newCtd.getHelp() == null) {
			newCtd.setHelp(this.complexTypeDefinition.getHelp());
		}

		this.complexTypeDefinition = newCtd;
	}

	@Override
	public PrismObjectValue<O> createValue() {
		return new PrismObjectValue<>(prismContext);
	}


	private QName getExtensionQName() {
		String namespace = getName().getNamespaceURI();
		return new QName(namespace, PrismConstants.EXTENSION_LOCAL_NAME);
	}
	
	public <I extends ItemDefinition> I getExtensionItemDefinition(QName elementName) {
		PrismContainerDefinition<?> extensionDefinition = getExtensionDefinition();
		if (extensionDefinition == null) {
			return null;
		}
		return (I) extensionDefinition.findItemDefinition(elementName);
	}

	@Override
	protected String getDebugDumpClassName() {
		return "POD";
	}

    @Override
    public String getDocClassName() {
        return "object";
    }
	
}
