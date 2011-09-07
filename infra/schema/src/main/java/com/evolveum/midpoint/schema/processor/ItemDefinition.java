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

import java.io.Serializable;
import java.util.List;
import java.util.Set;

import javax.xml.namespace.QName;

import org.apache.commons.lang.StringUtils;
import org.w3c.dom.Element;

import com.evolveum.midpoint.schema.exception.SchemaException;

/**
 * Abstract definition in the schema.
 * 
 * This is supposed to be a superclass for all definitions. It defines common
 * properties for all definitions.
 * 
 * The definitions represent data structures of the schema. Therefore instances
 * of Java objects from this class represent specific <em>definitions</em> from
 * the schema, not specific properties or objects. E.g the definitions does not
 * have any value.
 * 
 * To transform definition to a real property or object use the explicit
 * instantiate() methods provided in the definition classes. E.g. the
 * instantiate() method will create instance of Property using appropriate
 * PropertyDefinition.
 * 
 * The convenience methods in Schema are using this abstract class to find
 * appropriate definitions easily.
 * 
 * @author Radovan Semancik
 * 
 */
public abstract class ItemDefinition extends Definition implements Serializable {

	private static final long serialVersionUID = -2643332934312107274L;
	protected QName name;

	// TODO: annotations
	
	ItemDefinition(){
		super();
	}

	ItemDefinition(QName name, QName defaultName, QName typeName) {
		super(defaultName,typeName);
		this.name = name;
	}

	
	
	/**
	 * Returns name of the defined entity.
	 * 
	 * The name is a name of the entity instance if it is fixed by the schema.
	 * E.g. it may be a name of the property in the container that cannot be
	 * changed.
	 * 
	 * The name corresponds to the XML element name in the XML representation of
	 * the schema. It does NOT correspond to a XSD type name.
	 * 
	 * Name is optional. If name is not set the null value is returned. If name is
	 * not set the type is "abstract", does not correspond to the element.
	 * 
	 * @return the name name of the entity or null.
	 */
	public QName getName() {
		return name;
	}

	/**
	 * Returns either name (if specified) or default name.
	 * 
	 * Convenience method.
	 * 
	 * @return name or default name
	 */
	public QName getNameOrDefaultName() {
		if (name != null) {
			return name;
		}
		return defaultName;
	}
	
	abstract public Item instantiate();
	
	abstract public Item instantiate(QName name);
	
	abstract public Item parseItem(List<Object> elements) throws SchemaException;
	
	@Override
	public String toString() {
		return getClass().getSimpleName() + ":" + getName() + " ("+getTypeName()+")";
	}
	
}
