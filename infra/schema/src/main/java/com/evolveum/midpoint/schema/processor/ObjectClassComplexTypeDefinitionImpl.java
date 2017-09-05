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
package com.evolveum.midpoint.schema.processor;

import com.evolveum.midpoint.prism.ComplexTypeDefinition;
import com.evolveum.midpoint.prism.ComplexTypeDefinitionImpl;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

import org.jetbrains.annotations.NotNull;

import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author semancik
 *
 */
public class ObjectClassComplexTypeDefinitionImpl extends ComplexTypeDefinitionImpl implements ObjectClassComplexTypeDefinition {
	private static final long serialVersionUID = 1L;

	@NotNull private final Collection<ResourceAttributeDefinition<?>> identifiers = new ArrayList<>(1);
	@NotNull private final Collection<ResourceAttributeDefinition<?>> secondaryIdentifiers = new ArrayList<>(1);
	private ResourceAttributeDefinition descriptionAttribute;
	private ResourceAttributeDefinition displayNameAttribute;
	private ResourceAttributeDefinition namingAttribute;
	private boolean defaultInAKind = false;
	private ShadowKindType kind;
	private String intent;
	private String nativeObjectClass;
	private boolean auxiliary;

	public ObjectClassComplexTypeDefinitionImpl(QName typeName, PrismContext prismContext) {
		super(typeName, prismContext);
	}

	@NotNull
	@Override
	public Collection<? extends ResourceAttributeDefinition<?>> getAttributeDefinitions() {
		return Collections.unmodifiableList(
				getDefinitions().stream()
				.map(def -> (ResourceAttributeDefinition<?>) def)
				.collect(Collectors.toList()));
	}

	@Override
	public void add(ItemDefinition<?> definition) {
		if (ResourceAttributeDefinition.class.isAssignableFrom(definition.getClass())) {
			super.add(definition);
		} else {
			throw new IllegalArgumentException("Only ResourceAttributeDefinitions should be put into"
					+ " a ObjectClassComplexTypeDefinition. Item definition = " + definition + ","
					+ " ObjectClassComplexTypeDefinition = " + this);
		}
	}

	@NotNull
	@Override
	public Collection<? extends ResourceAttributeDefinition<?>> getPrimaryIdentifiers() {
		return identifiers;
	}

	public void addPrimaryIdentifier(ResourceAttributeDefinition<?> identifier) {
		identifiers.add(identifier);
	}

	@NotNull
	@Override
	public Collection<? extends ResourceAttributeDefinition<?>> getSecondaryIdentifiers() {
		return secondaryIdentifiers;
	}

	public void addSecondaryIdentifier(ResourceAttributeDefinition<?> identifier) {
		secondaryIdentifiers.add(identifier);
	}

	@Override
	public <X> ResourceAttributeDefinition<X> getDescriptionAttribute() {
		return descriptionAttribute;
	}

	void setDescriptionAttribute(ResourceAttributeDefinition<?> descriptionAttribute) {
		this.descriptionAttribute = descriptionAttribute;
	}

	@Override
	public <X> ResourceAttributeDefinition<X> getNamingAttribute() {
		return namingAttribute;
	}

	public void setNamingAttribute(ResourceAttributeDefinition<?> namingAttribute) {
		this.namingAttribute = namingAttribute;
	}

	public void setNamingAttribute(QName namingAttribute) {
		setNamingAttribute(findAttributeDefinition(namingAttribute));
	}

	@Override
	public String getNativeObjectClass() {
		return nativeObjectClass;
	}

	public void setNativeObjectClass(String nativeObjectClass) {
		this.nativeObjectClass = nativeObjectClass;
	}

	@Override
	public boolean isAuxiliary() {
		return auxiliary;
	}

	public void setAuxiliary(boolean auxiliary) {
		this.auxiliary = auxiliary;
	}

	@Override
	public ShadowKindType getKind() {
		return kind;
	}

	public void setKind(ShadowKindType kind) {
		this.kind = kind;
	}

	@Override
	public boolean isDefaultInAKind() {
		return defaultInAKind;
	}

	public void setDefaultInAKind(boolean defaultAccountType) {
		this.defaultInAKind = defaultAccountType;
	}

	@Override
	public String getIntent() {
		return intent;
	}

	public void setIntent(String intent) {
		this.intent = intent;
	}

	@Override
	public ResourceAttributeDefinition<?> getDisplayNameAttribute() {
		return displayNameAttribute;
	}

	public void setDisplayNameAttribute(ResourceAttributeDefinition<?> displayName) {
		this.displayNameAttribute = displayName;
	}

	/**
	 * TODO
	 *
	 * Convenience method. It will internally look up the correct definition.
	 */
	public void setDisplayNameAttribute(QName displayName) {
		setDisplayNameAttribute(findAttributeDefinition(displayName));
	}

	public <X> ResourceAttributeDefinitionImpl<X> createAttributeDefinition(QName name, QName typeName) {
		ResourceAttributeDefinitionImpl<X> propDef = new ResourceAttributeDefinitionImpl<>(name, typeName, prismContext);
		add(propDef);
		return propDef;
	}

	public <X> ResourceAttributeDefinitionImpl<X> createAttributeDefinition(String localName, QName typeName) {
		QName name = new QName(getSchemaNamespace(),localName);
		return createAttributeDefinition(name,typeName);
	}

	public <X> ResourceAttributeDefinition<X> createAttributeDefinition(String localName, String localTypeName) {
		QName name = new QName(getSchemaNamespace(),localName);
		QName typeName = new QName(getSchemaNamespace(),localTypeName);
		return createAttributeDefinition(name,typeName);
	}

	@Override
	public boolean matches(ShadowType shadowType) {
		if (shadowType == null) {
			return false;
		}
		if (!QNameUtil.match(getTypeName(), shadowType.getObjectClass())) {
			return false;
		}
		return true;
	}

	/**
	 * This may not be really "clean" as it actually does two steps instead of one. But it is useful.
	 */
	@Override
	public ResourceAttributeContainer instantiate(QName elementName) {
		return instantiate(elementName, this);
	}

	public static ResourceAttributeContainer instantiate(QName elementName, ObjectClassComplexTypeDefinition ocdef) {
		ResourceAttributeContainerDefinition racDef = ocdef.toResourceAttributeContainerDefinition(elementName);
		return new ResourceAttributeContainer(elementName, racDef, ocdef.getPrismContext());
	}

	@NotNull
	@Override
	public ObjectClassComplexTypeDefinitionImpl clone() {
		ObjectClassComplexTypeDefinitionImpl clone = new ObjectClassComplexTypeDefinitionImpl(
				getTypeName(), prismContext);
		copyDefinitionData(clone);
		clone.shared = false;
		return clone;
	}

	@NotNull
	@Override
	public ObjectClassComplexTypeDefinition deepClone(Map<QName, ComplexTypeDefinition> ctdMap) {
		return (ObjectClassComplexTypeDefinition) super.deepClone(ctdMap);
	}

	protected void copyDefinitionData(ObjectClassComplexTypeDefinitionImpl clone) {
		super.copyDefinitionData(clone);
		clone.kind = this.kind;
		clone.intent = this.intent;
		clone.defaultInAKind = this.defaultInAKind;
		clone.descriptionAttribute = this.descriptionAttribute;
		clone.displayNameAttribute = this.displayNameAttribute;
		clone.identifiers.addAll(this.identifiers);
		clone.namingAttribute = this.namingAttribute;
		clone.nativeObjectClass = this.nativeObjectClass;
		clone.secondaryIdentifiers.addAll(this.secondaryIdentifiers);
		clone.auxiliary = this.auxiliary;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + (auxiliary ? 1231 : 1237);
		result = prime * result + (defaultInAKind ? 1231 : 1237);
		result = prime * result + ((descriptionAttribute == null) ? 0 : descriptionAttribute.hashCode());
		result = prime * result + ((displayNameAttribute == null) ? 0 : displayNameAttribute.hashCode());
		result = prime * result + identifiers.hashCode();
		result = prime * result + ((intent == null) ? 0 : intent.hashCode());
		result = prime * result + ((kind == null) ? 0 : kind.hashCode());
		result = prime * result + ((namingAttribute == null) ? 0 : namingAttribute.hashCode());
		result = prime * result + ((nativeObjectClass == null) ? 0 : nativeObjectClass.hashCode());
		result = prime * result + secondaryIdentifiers.hashCode();
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!super.equals(obj)) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		ObjectClassComplexTypeDefinitionImpl other = (ObjectClassComplexTypeDefinitionImpl) obj;
		if (auxiliary != other.auxiliary) {
			return false;
		}
		if (defaultInAKind != other.defaultInAKind) {
			return false;
		}
		if (descriptionAttribute == null) {
			if (other.descriptionAttribute != null) {
				return false;
			}
		} else if (!descriptionAttribute.equals(other.descriptionAttribute)) {
			return false;
		}
		if (displayNameAttribute == null) {
			if (other.displayNameAttribute != null) {
				return false;
			}
		} else if (!displayNameAttribute.equals(other.displayNameAttribute)) {
			return false;
		}
		if (!identifiers.equals(other.identifiers)) {
			return false;
		}
		if (intent == null) {
			if (other.intent != null) {
				return false;
			}
		} else if (!intent.equals(other.intent)) {
			return false;
		}
		if (kind != other.kind) {
			return false;
		}
		if (namingAttribute == null) {
			if (other.namingAttribute != null) {
				return false;
			}
		} else if (!namingAttribute.equals(other.namingAttribute)) {
			return false;
		}
		if (nativeObjectClass == null) {
			if (other.nativeObjectClass != null) {
				return false;
			}
		} else if (!nativeObjectClass.equals(other.nativeObjectClass)) {
			return false;
		}
		if (!secondaryIdentifiers.equals(other.secondaryIdentifiers)) {
			return false;
		}
		return true;
	}

	@Override
	protected String getDebugDumpClassName() {
		return "OCD";
	}

	@Override
	protected void extendDumpHeader(StringBuilder sb) {
		super.extendDumpHeader(sb);
		if (defaultInAKind) {
			sb.append(" def");
		}
		if (auxiliary) {
			sb.append(" aux");
		}
		if (kind != null) {
			sb.append(" ").append(kind.value());
		}
		if (intent != null) {
			sb.append(" intent=").append(intent);
		}
	}

	@Override
	protected void extendDumpDefinition(StringBuilder sb, ItemDefinition<?> def) {
		super.extendDumpDefinition(sb, def);
		if (getPrimaryIdentifiers().contains(def)) {
			sb.append(",primID");
		}
		if (getSecondaryIdentifiers().contains(def)) {
			sb.append(",secID");
		}
	}

}
