/*
 * Copyright (c) 2010-2016 Evolveum
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
import com.evolveum.midpoint.prism.Definition;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;

import javax.xml.namespace.QName;
import java.util.Collection;

/**
 * @author mederly
 */
public interface ObjectClassComplexTypeDefinition extends ComplexTypeDefinition {
	Collection<? extends ResourceAttributeDefinition> getAttributeDefinitions();

	Collection<? extends ResourceAttributeDefinition> getPrimaryIdentifiers();

	boolean isPrimaryIdentifier(QName attrName);

	Collection<? extends ResourceAttributeDefinition> getSecondaryIdentifiers();

	boolean isSecondaryIdentifier(QName attrName);

	Collection<? extends ResourceAttributeDefinition> getAllIdentifiers();

	ResourceAttributeDefinition<?> getDescriptionAttribute();

	ResourceAttributeDefinition<?> getNamingAttribute();

	String getNativeObjectClass();

	boolean isAuxiliary();

	ShadowKindType getKind();

	boolean isDefaultInAKind();

	String getIntent();

	ResourceAttributeDefinition<?> getDisplayNameAttribute();

	<X> ResourceAttributeDefinition<X> findAttributeDefinition(QName name);

	<X> ResourceAttributeDefinition<X> findAttributeDefinition(QName name, boolean caseInsensitive);

	<X> ResourceAttributeDefinition<X> findAttributeDefinition(String name);

	ResourceAttributeContainerDefinition toResourceAttributeContainerDefinition();

	ResourceAttributeContainerDefinition toResourceAttributeContainerDefinition(QName elementName);

	ObjectQuery createShadowSearchQuery(String resourceOid) throws SchemaException;

	ResourceAttributeContainer instantiate(QName elementName);

	ObjectClassComplexTypeDefinition clone();
}
