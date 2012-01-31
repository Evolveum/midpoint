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

/**
 * Resource Object Attribute Definition.
 * 
 * Resource Object Attribute is a Property of Resource Object. All that applies
 * to property applies also to attribute, e.g. only a whole attributes can be
 * changed, they may be simple or complex types, they should be representable in
 * XML, etc. In addition, attribute definition may have some annotations that
 * suggest its purpose and use on the Resource.
 * 
 * Resource Object Attribute understands resource-specific annotations such as
 * native attribute name.
 * 
 * This class represents schema definition for resource object attribute. See
 * {@link Definition} for more details.
 * 
 * @author Radovan Semancik
 * 
 */
public class ResourceObjectAttributeDefinition extends PropertyDefinition {

	private static final long serialVersionUID = 7092192397127114804L;
	private String nativeAttributeName;
	private String attributeDisplayName;

	public ResourceObjectAttributeDefinition(QName name, QName typeName) {
		super(name, null, typeName);
	}
	
	public ResourceObjectAttributeDefinition(QName name, QName defaultName, QName typeName) {
		super(name, defaultName, typeName);
	}

	public ResourceObjectAttribute instantiate(PropertyPath parentPath) {
		return instantiate(getNameOrDefaultName(), parentPath);
	}

	public ResourceObjectAttribute instantiate(QName name, PropertyPath parentPath) {
		return new ResourceObjectAttribute(name, this, null, parentPath);
	}

	public ResourceObjectAttribute instantiate(QName name, Object element, PropertyPath parentPath) {
		return new ResourceObjectAttribute(name, this, element, parentPath);
	}

	/**
	 * Returns true if the attribute is a (primary) identifier.
	 * 
	 * Convenience method.
	 * 
	 * @return true if the attribute is a (primary) identifier.
	 */
	public boolean isIdentifier(ResourceObjectDefinition objectDefinition) {
		for (ResourceObjectAttributeDefinition identifier : objectDefinition.getIdentifiers()) {
			if (this == identifier) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Returns native attribute name.
	 * 
	 * Native name of the attribute is a name as it is used on the resource or
	 * as seen by the connector. It is used for diagnostics purposes and may be
	 * used by the connector itself. As the attribute names in XSD have to
	 * comply with XML element name limitations, this may be the only way how to
	 * determine original attribute name.
	 * 
	 * Returns null if native attribute name is not set or unknown.
	 * 
	 * The name should be the same as the one used by the resource, if the
	 * resource supports naming of attributes. E.g. in case of LDAP this
	 * annotation should contain "cn", "givenName", etc. If the resource is not
	 * that flexible, the native attribute names may be hardcoded (e.g.
	 * "username", "homeDirectory") or may not be present at all.
	 * 
	 * @return native attribute name
	 */
	public String getNativeAttributeName() {
		return nativeAttributeName;
	}

	public void setNativeAttributeName(String nativeAttributeName) {
		this.nativeAttributeName = nativeAttributeName;
	}

	/**
	 * @return the attributeDisplayName
	 */
	public String getAttributeDisplayName() {
		return attributeDisplayName;
	}

	/**
	 * @param attributeDisplayName the attributeDisplayName to set
	 */
	public void setAttributeDisplayName(String attributeDisplayName) {
		this.attributeDisplayName = attributeDisplayName;
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(getClass().getSimpleName()).append(":").append(getName()).append(" (").append(getTypeName()).append(")");
		if (getNativeAttributeName()!=null) {
			sb.append(" native=");
			sb.append(getNativeAttributeName());
		}
		return sb.toString();
	}

}
