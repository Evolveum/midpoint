/*
 * Copyright (c) 2010-2015 Evolveum
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

package com.evolveum.midpoint.schema.processor;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.Definition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.path.ItemPath;

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
public class ResourceAttributeDefinition<T> extends PrismPropertyDefinition<T> {

	private static final long serialVersionUID = 7092192397127114804L;
	private String nativeAttributeName;
	private Boolean returnedByDefault;

	public ResourceAttributeDefinition(QName elementName, QName typeName, PrismContext prismContext) {
		super(elementName, typeName, prismContext);
	}

	public ResourceAttribute<T> instantiate() {
		return instantiate(getName());
	}

	public ResourceAttribute<T> instantiate(QName name) {
        name = addNamespaceIfApplicable(name);
		return new ResourceAttribute<T>(name, this, prismContext);
	}

	public Boolean getReturnedByDefault() {
		return returnedByDefault;
	}
	
	public boolean isReturnedByDefault() {
		if (returnedByDefault == null) {
			return true;
		} else {
			return returnedByDefault;
		}
	}

	public void setReturnedByDefault(Boolean returnedByDefault) {
		this.returnedByDefault = returnedByDefault;
	}

	/**
	 * Returns true if the attribute is a (primary) identifier.
	 * 
	 * Convenience method.
	 * 
	 * @return true if the attribute is a (primary) identifier.
	 */
	public boolean isIdentifier(ResourceAttributeContainerDefinition objectDefinition) {
		return isIdentifier(objectDefinition.getComplexTypeDefinition());
	}
		
	public boolean isIdentifier(ObjectClassComplexTypeDefinition objectDefinition) {
		for (ResourceAttributeDefinition<T> identifier : objectDefinition.getIdentifiers()) {
			if (this == identifier) {
				return true;
			}
		}
		return false;
	}
	
	public boolean isSecondaryIdentifier(ObjectClassComplexTypeDefinition objectDefinition) {
		for (ResourceAttributeDefinition<T> secondaryIdentifier : objectDefinition.getSecondaryIdentifiers()) {
			if (this == secondaryIdentifier) {
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
	
	
	
	@Override
	public ResourceAttributeDefinition<T> clone() {
		ResourceAttributeDefinition<T> clone = new ResourceAttributeDefinition<T>(getName(), getTypeName(), getPrismContext());
		copyDefinitionData(clone);
		return clone;
	}
	
	protected void copyDefinitionData(ResourceAttributeDefinition<T> clone) {
		super.copyDefinitionData(clone);
		clone.nativeAttributeName = this.nativeAttributeName;
		clone.returnedByDefault = this.returnedByDefault;
	}

	
	@Override
	protected void extendToString(StringBuilder sb) {
		super.extendToString(sb);
		if (getNativeAttributeName()!=null) {
			sb.append(" native=");
			sb.append(getNativeAttributeName());
		}
		if (returnedByDefault != null) {
			sb.append(" returnedByDefault=");
			sb.append(returnedByDefault);
		}
	}
	
	/**
     * Return a human readable name of this class suitable for logs.
     */
    @Override
    protected String getDebugDumpClassName() {
        return "RAD";
    }

}
