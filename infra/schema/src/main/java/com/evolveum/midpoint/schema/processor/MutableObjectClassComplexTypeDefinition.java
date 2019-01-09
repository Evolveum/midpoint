/*
 * Copyright (c) 2010-2018 Evolveum
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

import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.MutableComplexTypeDefinition;
import com.evolveum.midpoint.xml.ns._public.common.common_4.ShadowKindType;

import javax.xml.namespace.QName;

/**
 *
 */
public interface MutableObjectClassComplexTypeDefinition extends ObjectClassComplexTypeDefinition, MutableComplexTypeDefinition {
	void add(ItemDefinition<?> definition);

	void addPrimaryIdentifier(ResourceAttributeDefinition<?> identifier);

	void addSecondaryIdentifier(ResourceAttributeDefinition<?> identifier);

	void setDescriptionAttribute(ResourceAttributeDefinition<?> descriptionAttribute);

	void setNamingAttribute(ResourceAttributeDefinition<?> namingAttribute);

	void setNamingAttribute(QName namingAttribute);

	void setNativeObjectClass(String nativeObjectClass);

	void setAuxiliary(boolean auxiliary);

	void setKind(ShadowKindType kind);

	void setDefaultInAKind(boolean defaultAccountType);

	void setIntent(String intent);

	void setDisplayNameAttribute(ResourceAttributeDefinition<?> displayName);

	void setDisplayNameAttribute(QName displayName);

	<X> ResourceAttributeDefinitionImpl<X> createAttributeDefinition(QName name, QName typeName);

	<X> ResourceAttributeDefinitionImpl<X> createAttributeDefinition(String localName, QName typeName);

	<X> ResourceAttributeDefinition<X> createAttributeDefinition(String localName, String localTypeName);
}
