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

package com.evolveum.midpoint.schema.processor;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.Objectable;
import com.evolveum.midpoint.prism.PrismConstants;
import com.evolveum.midpoint.prism.PrismContext;
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
public class ObjectDefinition<T extends Objectable> extends PropertyContainerDefinition {
	private static final long serialVersionUID = -8298581031956931008L;
	
	protected Class<T> jaxbClass;

	ObjectDefinition(QName name, ComplexTypeDefinition complexTypeDefinition, PrismContext prismContext, Class<T> jaxbClass) {
		// Object definition can only be top-level, hence null parent
		super(name, complexTypeDefinition, prismContext);
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
	
	public T convertToObjectType(PrismObject<T> mpObject) throws SchemaException {		
		if (jaxbClass == null) {
			throw new IllegalStateException("No JAXB class was set for "+this);
		}
		T instance = instantiateJaxbClass(jaxbClass);
		fillProperties(instance, mpObject);
		return instance;		
	}

	protected void fillProperties(T instance, PrismObject<T> mpObject) throws SchemaException {
		instance.setOid(mpObject.getOid());
		super.fillProperties(instance, mpObject);
	}

	public PrismObject<T> instantiate(QName name) {
		PrismObject<T> midPointObject = new PrismObject<T>(name, this, prismContext, null);
		return midPointObject;
	}

	/**
	 * Just for "compatibility".
	 */
	@Override
	public PrismObject<T> instantiate(QName name, PropertyPath parentPath) {
		if (parentPath != null) {
			throw new IllegalArgumentException("Objects cannot have parents");
		}
		PrismObject<T> midPointObject = new PrismObject<T>(name, this, null, parentPath);
		return midPointObject;
	}
	
	public ObjectDefinition<T> clone() {
		ObjectDefinition<T> clone = new ObjectDefinition<T>(name, complexTypeDefinition, prismContext, jaxbClass);
		copyDefinitionData(clone);
		return clone;
	}

	public void setExtensionDefinition(ComplexTypeDefinition extensionComplexTypeDefinition) {
		QName extensionQName = getExtensionQName();
		PropertyContainerDefinition newExtensionDef = new PropertyContainerDefinition(extensionQName, 
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
