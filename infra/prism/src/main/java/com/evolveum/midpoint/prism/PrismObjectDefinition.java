/*
 * Copyright (c) 2011 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 *
 * Portions Copyrighted 2011 [name of copyright owner]
 */

package com.evolveum.midpoint.prism;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.schema.exception.SchemaException;
import com.evolveum.midpoint.schema.exception.SystemException;

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
public class PrismObjectDefinition<T extends Objectable> extends PrismContainerDefinition {
	private static final long serialVersionUID = -8298581031956931008L;
	
	protected Class<T> jaxbClass;

	PrismObjectDefinition(QName name, ComplexTypeDefinition complexTypeDefinition, PrismContext prismContext, Class<T> jaxbClass) {
		// Object definition can only be top-level, hence null parent
		super(name, complexTypeDefinition, prismContext);
		if (name != null) {
			// Override default name for objects. In this case name is usually the element name
			defaultName = name;
		}
		this.jaxbClass = jaxbClass;
	}
	
	public Class<T> getJaxbClass() {
		return jaxbClass;
	}

	public void setJaxbClass(Class<T> jaxbClass) {
		this.jaxbClass = jaxbClass;
	}

	public PrismObject<T> parseObjectType(T objectType) throws SchemaException {
		// TODO
		throw new UnsupportedOperationException();
		
		// Parent is null, objects do not have parents
//		PrismObject<T> object = parseItemFromJaxbObject(objectType, PrismObject.class, null);
//		object.setOid(objectType.getOid());
//		object.setObjectType(objectType);
//		return object;
	}
	
	@Override
	public PrismObject<T> instantiate() {
		PrismObject<T> midPointObject = new PrismObject<T>(getNameOrDefaultName(), this, prismContext);
		return midPointObject;
	}
	
	@Override
	public PrismObject<T> instantiate(QName name) {
		PrismObject<T> midPointObject = new PrismObject<T>(name, this, prismContext);
		return midPointObject;
	}
	
	public PrismObjectDefinition<T> clone() {
		PrismObjectDefinition<T> clone = new PrismObjectDefinition<T>(name, complexTypeDefinition, prismContext, jaxbClass);
		copyDefinitionData(clone);
		return clone;
	}

	public void setExtensionDefinition(ComplexTypeDefinition extensionComplexTypeDefinition) {
		QName extensionQName = getExtensionQName();
		PrismContainerDefinition newExtensionDef = new PrismContainerDefinition(extensionQName, 
				extensionComplexTypeDefinition, prismContext);
		ComplexTypeDefinition newCtd = this.complexTypeDefinition.clone();
		newExtensionDef.setRuntimeSchema(true);
		newCtd.replaceDefinition(extensionQName, newExtensionDef);
		this.complexTypeDefinition = newCtd;
	}
	
	private QName getExtensionQName() {
		String namespace = getName().getNamespaceURI();
		return new QName(namespace, PrismConstants.EXTENSION_LOCAL_NAME);
	}
	
}
