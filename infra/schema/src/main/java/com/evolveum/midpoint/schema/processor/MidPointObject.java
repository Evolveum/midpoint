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

import org.w3c.dom.Node;

import com.evolveum.midpoint.schema.exception.SchemaException;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectType;

/**
 * Common supertype for all identity objects. Defines basic properties that each
 * object must have to live in our system (identifier, name).
 * 
 * Objects consists of identifier and name (see definition below) and a set of
 * properties represented as XML elements in the object's body. The attributes
 * are represented as first-level XML elements (tags) of the object XML
 * representation and may be also contained in other tags (e.g. extension,
 * attributes). The QName (namespace and local name) of the element holding the
 * property is considered to be a property name.
 * 
 * This class is named MidPointObject instead of Object to avoid confusion with
 * java.lang.Object.
 * 
 * @author Radovan Semancik
 * 
 */
public class MidPointObject<T extends ObjectType> extends PropertyContainer {

	protected String oid;
	protected T objectType = null;
	
	public MidPointObject(QName name) {
		super(name);
	}

	public MidPointObject(QName name, ObjectDefinition definition) {
		super(name, definition);
	}
	
	public MidPointObject(QName name, ObjectDefinition definition, Object element) {
		super(name,definition,element);
	}

	/**
	 * Returns Object ID (OID).
	 * 
	 * May return null if the object does not have an OID.
	 * 
	 * @return Object ID (OID)
	 */
	public String getOid() {
		return oid;
	}

	public void setOid(String oid) {
		this.oid = oid;
	}

	public Class<T> getJaxbClass() {
		return ((ObjectDefinition)getDefinition()).getJaxbClass();
	}

	public T getObjectType() {
		return objectType;
	}

	public void setObjectType(T objectType) {
		this.objectType = objectType;
	}
	
	public T getOrParseObjectType() throws SchemaException {
		if (objectType == null) {
			objectType = convertToObjectType();
		}
		return objectType;
	}
	
	private T convertToObjectType() throws SchemaException {
		ObjectDefinition<T> def = (ObjectDefinition<T>) getDefinition();
		if (def == null) {
			throw new IllegalStateException("Cannot convert object with no definition ("+this+")");
		}
		return def.convertToObjectType(this);
	}
	
	

	@Override
	public MidPointObject<T> clone() {
		MidPointObject<T> clone = new MidPointObject<T>(getName());
		copyValues(clone);
		return clone;
	}

	protected void copyValues(MidPointObject<T> clone) {
		super.copyValues(clone);
		clone.oid = this.oid;
		clone.objectType = null; // this will get generated eventually. Copying will not work anyway.
	}

	/**
	 * Return a human readable name of this class suitable for logs.
	 */
	@Override
	protected String getDebugDumpClassName() {
		return "MidPoint object";
	}
	
	@Override
	protected String additionalDumpDescription() {
		return ", "+getOid();
	}

	public Node serializeToDom() throws SchemaException {
		Node doc = DOMUtil.getDocument();
		serializeToDom(doc);
		return doc;
	}
	
}
