/*
 * Copyright (c) 2010-2017 Evolveum
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

package com.evolveum.midpoint.prism;

import java.util.Collection;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.DisplayableValue;
import org.jetbrains.annotations.NotNull;

/**
 * Property Definition.
 * <p/>
 * Property is a basic unit of information in midPoint. This class provides
 * definition of property type, multiplicity and so on.
 * <p/>
 * Property is a specific characteristic of an object. It may be considered
 * object "attribute" or "field". For example User has fullName property that
 * contains string value of user's full name.
 * <p/>
 * Properties may be single-valued or multi-valued
 * <p/>
 * Properties may contain primitive types or complex types (defined by XSD
 * schema)
 * <p/>
 * Property values are unordered, implementation may change the order of values
 * <p/>
 * Duplicate values of properties should be silently removed by implementations,
 * but clients must be able tolerate presence of duplicate values.
 * <p/>
 * Operations that modify the objects work with the granularity of properties.
 * They add/remove/replace the values of properties, but do not "see" inside the
 * property.
 * <p/>
 * This class represents schema definition for property. See {@link Definition}
 * for more details.
 *
 * @author Radovan Semancik
 */
public class PrismPropertyDefinitionImpl<T> extends ItemDefinitionImpl<PrismProperty<T>> implements PrismPropertyDefinition<T> {

    private static final long serialVersionUID = 7259761997904371009L;
    private QName valueType;
    private Collection<? extends DisplayableValue<T>> allowedValues;
    private Boolean indexed = null;
    private T defaultValue;
    private QName matchingRuleQName = null;

    public PrismPropertyDefinitionImpl(QName elementName, QName typeName, PrismContext prismContext) {
        super(elementName, typeName, prismContext);
    }
    
    public PrismPropertyDefinitionImpl(QName elementName, QName typeName, PrismContext prismContext, Collection<? extends DisplayableValue<T>> allowedValues, T defaultValue) {
        super(elementName, typeName, prismContext);
        this.allowedValues = allowedValues;
        this.defaultValue = defaultValue;
    }

    /**
     * Returns allowed values for this property.
     *
     * @return Object array. May be null.
     */
    @Override
	public Collection<? extends DisplayableValue<T>> getAllowedValues() {
        return allowedValues;
    }

    @Override
	public T defaultValue(){
    	return defaultValue;
    }

    @Override
	public QName getValueType() {
        return valueType;
    }

	/**
     * This is XSD annotation that specifies whether a property should 
     * be indexed in the storage. It can only apply to properties. It
     * has following meaning:
     * 
     * true: the property must be indexed. If the storage is not able to
     * index the value, it should indicate an error.
     * 
     * false: the property should not be indexed.
     * 
     * null: data store decides whether to index the property or
     * not.
     */
    @Override
	public Boolean isIndexed() {
		return indexed;
	}

	public void setIndexed(Boolean indexed) {
		this.indexed = indexed;
	}

	/**
	 * Returns matching rule name. Matching rules are algorithms that specify
	 * how to compare, normalize and/or order the values. E.g. there are matching
	 * rules for case insensitive string comparison, for LDAP DNs, etc.
	 *  
	 * @return matching rule name
	 */
	@Override
	public QName getMatchingRuleQName() {
		return matchingRuleQName;
	}

	public void setMatchingRuleQName(QName matchingRuleQName) {
		this.matchingRuleQName = matchingRuleQName;
	}

	@NotNull
	@Override
    public PrismProperty<T> instantiate() {
        return instantiate(getName());
    }

    @NotNull
	@Override
    public PrismProperty<T> instantiate(QName name) {
        name = addNamespaceIfApplicable(name);
        return new PrismProperty<>(name, this, prismContext);
    }

    @Override
	public PropertyDelta<T> createEmptyDelta(ItemPath path) {
		return new PropertyDelta<T>(path, this, prismContext);
	}

	@NotNull
	@Override
	public PrismPropertyDefinition<T> clone() {
		PrismPropertyDefinitionImpl<T> clone = new PrismPropertyDefinitionImpl<T>(getName(), getTypeName(), getPrismContext());
		copyDefinitionData(clone);
		return clone;
	}

	protected void copyDefinitionData(PrismPropertyDefinitionImpl<T> clone) {
		super.copyDefinitionData(clone);
        clone.indexed = this.indexed;
        clone.defaultValue = this.defaultValue;
		clone.allowedValues = this.allowedValues;
		clone.valueType = this.valueType;
	}

    @Override
	protected void extendToString(StringBuilder sb) {
		super.extendToString(sb);
		if (indexed != null && indexed) {
			sb.append(",I");
		}
		if (allowedValues != null && !allowedValues.isEmpty()) {
			sb.append(",AVals:").append(allowedValues.size());
		}
	}
    
    @Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((allowedValues == null) ? 0 : allowedValues.hashCode());
		result = prime * result + ((defaultValue == null) ? 0 : defaultValue.hashCode());
		result = prime * result + ((indexed == null) ? 0 : indexed.hashCode());
		result = prime * result + ((valueType == null) ? 0 : valueType.hashCode());
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
		PrismPropertyDefinitionImpl other = (PrismPropertyDefinitionImpl) obj;
		if (allowedValues == null) {
			if (other.allowedValues != null)
				return false;
		} else if (!allowedValues.equals(other.allowedValues))
			return false;
		if (defaultValue == null) {
			if (other.defaultValue != null)
				return false;
		} else if (!defaultValue.equals(other.defaultValue))
			return false;
		if (indexed == null) {
			if (other.indexed != null)
				return false;
		} else if (!indexed.equals(other.indexed))
			return false;
		if (valueType == null) {
			if (other.valueType != null)
				return false;
		} else if (!valueType.equals(other.valueType))
			return false;
		return true;
	}

	/**
     * Return a human readable name of this class suitable for logs.
     */
    @Override
    protected String getDebugDumpClassName() {
        return "PPD";
    }

    @Override
    public String getDocClassName() {
        return "property";
    }
}
