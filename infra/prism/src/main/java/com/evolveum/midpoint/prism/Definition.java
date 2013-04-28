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

import java.io.Serializable;
import java.util.List;
import java.util.Set;

import javax.xml.namespace.QName;

import org.apache.commons.lang.StringUtils;
import org.w3c.dom.Element;

import com.evolveum.midpoint.prism.schema.SchemaRegistry;
import com.evolveum.midpoint.prism.xml.XsdTypeMapper;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.Dumpable;
import com.evolveum.midpoint.util.exception.SchemaException;

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
public abstract class Definition implements Serializable, Dumpable, DebugDumpable {

	private static final long serialVersionUID = -2643332934312107274L;
	protected QName defaultName;
	protected QName typeName;
	protected boolean ignored;
	protected String displayName;
	protected Integer displayOrder;
	protected String help;
	protected transient PrismContext prismContext;

	// TODO: annotations
	
	Definition(QName defaultName, QName typeName, PrismContext prismContext) {
		ignored = false;
		if (typeName == null) {
			throw new IllegalArgumentException("Type name can't be null.");
		}
		this.defaultName = defaultName;
		this.typeName = typeName;
		this.prismContext = prismContext;
	}

	/**
	 * Returns default name for the defined entity.
	 * 
	 * The default name is the name that the entity usually takes, but a name
	 * that is not fixed by the schema.
	 * 
	 * The name corresponds to the XML element name in the XML representation of
	 * the schema. It does NOT correspond to a XSD type name.
	 * 
	 * For example the default name may be the element name that is usually used
	 * for a specific object (e.g. "user"), while the same object may be
	 * represented using other names that resolve to the same type.
	 * 
	 * In XML representation it corresponds to "defaultElement" XSD annotation.
	 * 
	 * @return the defaultName
	 */
	public QName getDefaultName() {
		return defaultName;
	}

	/**
	 * Returns the name of the definition type.
	 * 
	 * Returns a name of the type for this definition.
	 * 
	 * In XML representation that corresponds to the name of the XSD type.
	 * 
	 * @return the typeName
	 */
	public QName getTypeName() {
		return typeName;
	}
	
	public void setTypeName(QName typeName) {
		this.typeName = typeName;
	}

	public boolean isIgnored() {
		return ignored;
	}

	public void setIgnored(boolean ignored) {
		this.ignored = ignored;
	}

	/**
	 * Returns display name.
	 * 
	 * Specifies the printable name of the object class or attribute. It must
	 * contain a printable string. It may also contain a key to catalog file.
	 * 
	 * Returns null if no display name is set.
	 * 
	 * Corresponds to "displayName" XSD annotation.
	 * 
	 * @return display name string or catalog key
	 */
	public String getDisplayName() {
		return displayName;
	}
	
	public void setDisplayName(String displayName) {		
		this.displayName = displayName;
	}
	
	/**
	 * Specifies an order in which the item should be displayed relative to other items
	 * at the same level. The items will be displayed by sorting them by the
	 * values of displayOrder annotation (ascending). Items that do not have
	 * any displayOrder annotation will be displayed last. The ordering of
	 * values with the same displayOrder is undefined and it may be arbitrary.
	 */
	public Integer getDisplayOrder() {
		return displayOrder;
	}

	public void setDisplayOrder(Integer displayOrder) {
		this.displayOrder = displayOrder;
	}

	/**
	 * Returns help string.
	 * 
	 * Specifies the help text or a key to catalog file for a help text. The
	 * help text may be displayed in any suitable way by the GUI. It should
	 * explain the meaning of an attribute or object class.
	 * 
	 * Returns null if no help string is set.
	 * 
	 * Corresponds to "help" XSD annotation.
	 * 
	 * @return help string or catalog key
	 */
	public String getHelp() {
		return help;
	}
	
	public void setHelp(String help) {
		this.help = help;
	}
	
	public PrismContext getPrismContext() {
		return prismContext;
	}
	
	protected SchemaRegistry getSchemaRegistry() {
		return prismContext.getSchemaRegistry();
	}
	
	public Class getTypeClass() {
		return XsdTypeMapper.toJavaType(getTypeName());
	}
	
	abstract void revive(PrismContext prismContext);
	
	public abstract Definition clone(); 
	
	protected void copyDefinitionData(Definition clone) {
		clone.defaultName = this.defaultName;
		clone.ignored = this.ignored;
		clone.typeName = this.typeName;
		clone.displayName = this.displayName;
		clone.displayOrder = this.displayOrder;
		clone.help = this.help;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (ignored ? 1231 : 1237);
		result = prime * result + ((typeName == null) ? 0 : typeName.hashCode());
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
		Definition other = (Definition) obj;
		if (ignored != other.ignored)
			return false;
		if (typeName == null) {
			if (other.typeName != null)
				return false;
		} else if (!typeName.equals(other.typeName))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return getDebugDumpClassName() + " ("+getTypeName()+")";
	}
	
	@Override
	public String dump() {
		return debugDump();
	}
	
	@Override
	public String debugDump() {
		return debugDump(0);
	}

	@Override
	public String debugDump(int indent) {
		StringBuilder sb = new StringBuilder();
		for (int i=0; i<indent; i++) {
			sb.append(DebugDumpable.INDENT_STRING);
		}
		sb.append(toString());
		return sb.toString();
	}
	
	/**
     * Return a human readable name of this class suitable for logs.
     */
    protected abstract String getDebugDumpClassName();
}
