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

import java.util.List;
import java.util.Set;

import javax.xml.namespace.QName;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.evolveum.midpoint.schema.exception.SchemaException;
import com.evolveum.midpoint.util.Dumpable;


/**
 * Item is a common abstraction of Property and PropertyContainer.
 * 
 * @author Radovan Semancik
 * 
 */
public abstract class Item implements Dumpable {

	protected QName name;
	protected Definition definition;

	public Item() {
		super();
	}

	public Item(QName name) {
		super();
		this.name = name;
		this.definition = null;
	}

	public Item(QName name, Definition definition) {
		super();
		this.name = name;
		this.definition = definition;
	}

	/**
	 * Returns applicable property definition.
	 * 
	 * May return null if no definition is applicable or the definition is not
	 * know.
	 * 
	 * @return applicable property definition
	 */
	public Definition getDefinition() {
		return definition;
	}

	/**
	 * Returns the name of the property.
	 * 
	 * The name is a QName. It uniquely defines a property.
	 * 
	 * The name may be null, but such a property will not work.
	 * 
	 * The name is the QName of XML element in the XML representation.
	 * 
	 * @return property name
	 */
	public QName getName() {
		return name;
	}

	/**
	 * Sets the name of the property.
	 * 
	 * The name is a QName. It uniquely defines a property.
	 * 
	 * The name may be null, but such a property will not work.
	 * 
	 * The name is the QName of XML element in the XML representation.
	 * 
	 * @param name
	 *            the name to set
	 */
	public void setName(QName name) {
		this.name = name;
	}

	/**
	 * Sets applicable property definition.
	 * 
	 * @param definition
	 *            the definition to set
	 */
	public void setDefinition(PropertyDefinition definition) {
		this.definition = definition;
	}

	/**
	 * Returns a display name for the property type.
	 * 
	 * Returns null if the display name cannot be determined.
	 * 
	 * The display name is fetched from the definition. If no definition
	 * (schema) is available, the display name will not be returned.
	 * 
	 * @return display name for the property type
	 */
	public String getDisplayName() {
		return getDefinition() == null ? null : getDefinition().getDisplayName();
	}

	/**
	 * Returns help message defined for the property type.
	 * 
	 * Returns null if the help message cannot be determined.
	 * 
	 * The help message is fetched from the definition. If no definition
	 * (schema) is available, the help message will not be returned.
	 * 
	 * @return help message for the property type
	 */
	public String getHelp() {
		return getDefinition() == null ? null : getDefinition().getHelp();
	}
	
	/**
	 * Serializes property to DOM or JAXB element(s).
	 * 
	 * The property name will be used as an element QName.
	 * The values will be in the element content. Single-value
	 * properties will produce one element (on none), multi-valued
	 * properies may produce several elements. All of the elements will
	 * have the same QName.
	 * 
	 * The property must have a definition (getDefinition() must not
	 * return null).
	 * 
	 * @param doc DOM Document
	 * @return property serialized to DOM Element or JAXBElement
	 * @throws SchemaProcessorException No definition or inconsistent definition 
	 */
	abstract public void serializeToDom(Node parentNode) throws SchemaException;
	
	@Override
	public String toString() {
		return getClass().getSimpleName()+"("+getName()+")";
	}

	public String dump() {
		return toString();
	}

}
