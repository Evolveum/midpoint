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

package com.evolveum.midpoint.prism;

import javax.xml.namespace.QName;

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
public class PrismObjectDefinition<T extends Objectable> extends PrismContainerDefinition<T> {
	private static final long serialVersionUID = -8298581031956931008L;

	public PrismObjectDefinition(QName elementName, ComplexTypeDefinition complexTypeDefinition, PrismContext prismContext, 
			Class<T> compileTimeClass) {
		// Object definition can only be top-level, hence null parent
		super(elementName, complexTypeDefinition, prismContext, compileTimeClass);
	}
	
	@Override
	public PrismObject<T> instantiate() {
		PrismObject<T> midPointObject = new PrismObject<T>(getName(), this, prismContext);
		return midPointObject;
	}
	
	@Override
	public PrismObject<T> instantiate(QName name) {
        name = addNamespaceIfApplicable(name);
		PrismObject<T> midPointObject = new PrismObject<T>(name, this, prismContext);
		return midPointObject;
	}
	
	public PrismObjectDefinition<T> clone() {
		PrismObjectDefinition<T> clone = new PrismObjectDefinition<T>(name, complexTypeDefinition, prismContext, compileTimeClass);
		copyDefinitionData(clone);
		return clone;
	}
	
	public PrismObjectDefinition<T> cloneWithReplacedDefinition(QName itemName, ItemDefinition newDefinition) {
		return (PrismObjectDefinition<T>) super.cloneWithReplacedDefinition(itemName, newDefinition);
	}
	
	public PrismContainerDefinition<?> getExtensionDefinition() {
		return findContainerDefinition(getExtensionQName());
	}

	public void setExtensionDefinition(ComplexTypeDefinition extensionComplexTypeDefinition) {
		QName extensionQName = getExtensionQName();
		
		PrismContainerDefinition<Containerable> oldExtensionDef = findContainerDefinition(extensionQName);
		
		PrismContainerDefinition<?> newExtensionDef = new PrismContainerDefinition(extensionQName, 
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
		
		ComplexTypeDefinition newCtd = this.complexTypeDefinition.clone();
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
