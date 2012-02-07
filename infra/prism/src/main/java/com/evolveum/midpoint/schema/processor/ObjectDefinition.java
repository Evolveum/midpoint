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

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.exception.SchemaException;
import com.evolveum.midpoint.schema.exception.SystemException;
import com.evolveum.midpoint.xml.ns._public.common.common_1.AccountShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectType;

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
public class ObjectDefinition<T extends ObjectType> extends PropertyContainerDefinition {
	private static final long serialVersionUID = -8298581031956931008L;
	
	protected Class<T> jaxbClass;

	ObjectDefinition(QName name, ComplexTypeDefinition complexTypeDefinition) {
		// Object definition can only be top-level, hence null parent
		super(name, complexTypeDefinition);
		determineJaxbClass();
	}
	
	// TODO: make this smarter, based on actual JAXB classes
	private void determineJaxbClass() {
		jaxbClass = (Class<T>) ObjectTypes.getObjectTypeFromTypeQName(getTypeName()).getClassDefinition();
		if (jaxbClass == null) {
			throw new SystemException("Cannot determine JAXB class name for object type "+getTypeName());
		}
	}

	public Class<T> getJaxbClass() {
		return jaxbClass;
	}

	public void setJaxbClass(Class<T> jaxbClass) {
		this.jaxbClass = jaxbClass;
	}

	public MidPointObject<T> parseObjectType(T objectType) throws SchemaException {
		// Parent is null, objects do not have parents
		MidPointObject<T> object = parseItemFromJaxbObject(objectType, MidPointObject.class, null);
		object.setOid(objectType.getOid());
		object.setObjectType(objectType);
		return object;
	}
	
	public T convertToObjectType(MidPointObject<T> mpObject) throws SchemaException {		
		if (jaxbClass == null) {
			throw new IllegalStateException("No JAXB class was set for "+this);
		}
		T instance = instantiateJaxbClass(jaxbClass);
		fillProperties(instance, mpObject);
		return instance;		
	}

	protected void fillProperties(T instance, MidPointObject<T> mpObject) throws SchemaException {
		instance.setOid(mpObject.getOid());
		super.fillProperties(instance, mpObject);
	}

	public MidPointObject<T> instantiate(QName name) {
		MidPointObject<T> midPointObject = new MidPointObject<T>(name, this, null, null);
		return midPointObject;
	}

	public MidPointObject<T> instantiate(QName name, Object element) {
		return new MidPointObject<T>(name, this, element, null);
	}

	/**
	 * Just for "compatibility".
	 */
	@Override
	public MidPointObject<T> instantiate(QName name, PropertyPath parentPath) {
		if (parentPath != null) {
			throw new IllegalArgumentException("Objects cannot have parents");
		}
		MidPointObject<T> midPointObject = new MidPointObject<T>(name, this, null, parentPath);
		return midPointObject;
	}

	/**
	 * Just for "compatibility".
	 */
	@Override
	public MidPointObject<T> instantiate(QName name, Object element, PropertyPath parentPath) {
		if (parentPath != null) {
			throw new IllegalArgumentException("Objects cannot have parents");
		}
		return new MidPointObject<T>(name, this, element, parentPath);
	}

	
	public ObjectDefinition<T> clone() {
		ObjectDefinition<T> clone = new ObjectDefinition<T>(name, complexTypeDefinition);
		copyDefinitionData(clone);
		return clone;
	}

	public void setExtensionDefinition(ComplexTypeDefinition extensionComplexTypeDefinition) {
		PropertyContainerDefinition newExtensionDef = new PropertyContainerDefinition(SchemaConstants.C_EXTENSION, extensionComplexTypeDefinition);
		ComplexTypeDefinition newCtd = this.complexTypeDefinition.clone();
		newExtensionDef.setRuntimeSchema(true);
		newCtd.replaceDefinition(SchemaConstants.C_EXTENSION, newExtensionDef);
		this.complexTypeDefinition = newCtd;
	}
	
}
