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

import com.evolveum.midpoint.prism.Definition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismPropertyDefinitionImpl;
import org.jetbrains.annotations.NotNull;

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
public class ResourceAttributeDefinitionImpl<T> extends PrismPropertyDefinitionImpl<T> implements ResourceAttributeDefinition<T> {

	private static final long serialVersionUID = 7092192397127114804L;
	private String nativeAttributeName;
	private String frameworkAttributeName;
	private Boolean returnedByDefault;

	public ResourceAttributeDefinitionImpl(QName elementName, QName typeName, PrismContext prismContext) {
		super(elementName, typeName, prismContext);
	}

	@NotNull
	@Override
	public ResourceAttribute<T> instantiate() {
		return instantiate(getName());
	}

	@NotNull
	@Override
	public ResourceAttribute<T> instantiate(QName name) {
        name = addNamespaceIfApplicable(name);
		return new ResourceAttribute<>(name, this, prismContext);
	}

	@Override
	public Boolean getReturnedByDefault() {
		return returnedByDefault;
	}

	@Override
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
	@Override
	public boolean isIdentifier(ResourceAttributeContainerDefinition objectDefinition) {
		return isIdentifier(objectDefinition.getComplexTypeDefinition());
	}

	@Override
	public boolean isIdentifier(ObjectClassComplexTypeDefinition objectDefinition) {
		for (ResourceAttributeDefinition<?> identifier : objectDefinition.getPrimaryIdentifiers()) {
			if (this == identifier) {
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean isSecondaryIdentifier(ObjectClassComplexTypeDefinition objectDefinition) {
		for (ResourceAttributeDefinition<?> secondaryIdentifier : objectDefinition.getSecondaryIdentifiers()) {
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
	@Override
	public String getNativeAttributeName() {
		return nativeAttributeName;
	}

	public void setNativeAttributeName(String nativeAttributeName) {
		this.nativeAttributeName = nativeAttributeName;
	}

	/**
	 * Returns name of the attribute as given in the connector framework.
	 * This is not used for any significant logic. It is mostly for diagnostics.
	 *
	 * @return name of the attribute as given in the connector framework.
	 */
	@Override
	public String getFrameworkAttributeName() {
		return frameworkAttributeName;
	}

	public void setFrameworkAttributeName(String frameworkAttributeName) {
		this.frameworkAttributeName = frameworkAttributeName;
	}

	@NotNull
	@Override
	public ResourceAttributeDefinition<T> clone() {
		ResourceAttributeDefinitionImpl<T> clone = new ResourceAttributeDefinitionImpl<T>(getName(), getTypeName(), getPrismContext());
		copyDefinitionData(clone);
		return clone;
	}

	protected void copyDefinitionData(ResourceAttributeDefinitionImpl<T> clone) {
		super.copyDefinitionData(clone);
		clone.nativeAttributeName = this.nativeAttributeName;
		clone.frameworkAttributeName = this.frameworkAttributeName;
		clone.returnedByDefault = this.returnedByDefault;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((frameworkAttributeName == null) ? 0 : frameworkAttributeName.hashCode());
		result = prime * result + ((nativeAttributeName == null) ? 0 : nativeAttributeName.hashCode());
		result = prime * result + ((returnedByDefault == null) ? 0 : returnedByDefault.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		ResourceAttributeDefinitionImpl other = (ResourceAttributeDefinitionImpl) obj;
		if (frameworkAttributeName == null) {
			if (other.frameworkAttributeName != null)
				return false;
		} else if (!frameworkAttributeName.equals(other.frameworkAttributeName))
			return false;
		if (nativeAttributeName == null) {
			if (other.nativeAttributeName != null)
				return false;
		} else if (!nativeAttributeName.equals(other.nativeAttributeName))
			return false;
		if (returnedByDefault == null) {
			if (other.returnedByDefault != null)
				return false;
		} else if (!returnedByDefault.equals(other.returnedByDefault))
			return false;
		return true;
	}

	@Override
	protected void extendToString(StringBuilder sb) {
		super.extendToString(sb);
		if (getNativeAttributeName()!=null) {
			sb.append(" native=");
			sb.append(getNativeAttributeName());
		}
		if (getFrameworkAttributeName()!=null) {
			sb.append(" framework=");
			sb.append(getFrameworkAttributeName());
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
