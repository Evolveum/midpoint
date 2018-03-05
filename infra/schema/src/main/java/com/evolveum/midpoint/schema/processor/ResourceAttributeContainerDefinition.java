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

import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowAttributesType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import org.jetbrains.annotations.NotNull;

import javax.xml.namespace.QName;
import java.util.Collection;
import java.util.List;

/**
 * @author mederly
 */
public interface ResourceAttributeContainerDefinition extends PrismContainerDefinition<ShadowAttributesType> {
	@Override
	ObjectClassComplexTypeDefinition getComplexTypeDefinition();

	// TODO: rename to getPrimaryIdentifiers
	Collection<? extends ResourceAttributeDefinition> getPrimaryIdentifiers();

	Collection<? extends ResourceAttributeDefinition> getSecondaryIdentifiers();

	Collection<? extends ResourceAttributeDefinition> getAllIdentifiers();

	ResourceAttributeDefinition getDescriptionAttribute();

	ResourceAttributeDefinition getNamingAttribute();

	String getNativeObjectClass();

	boolean isDefaultInAKind();

	String getIntent();

	ShadowKindType getKind();

	ResourceAttributeDefinition getDisplayNameAttribute();

	@Override
    @NotNull
	ResourceAttributeContainer instantiate();

	@Override
    @NotNull
	ResourceAttributeContainer instantiate(QName name);

	@Override
    @NotNull
	ResourceAttributeContainerDefinition clone();

	ResourceAttributeDefinition findAttributeDefinition(QName elementQName);

	ResourceAttributeDefinition findAttributeDefinition(QName elementQName, boolean caseInsensitive);

	ResourceAttributeDefinition findAttributeDefinition(ItemPath elementPath);

	ResourceAttributeDefinition findAttributeDefinition(String elementLocalname);

	List<? extends ResourceAttributeDefinition> getAttributeDefinitions();

	// Only attribute definitions should be here.
	@Override
	List<? extends ResourceAttributeDefinition> getDefinitions();

	<T extends ShadowType> PrismObjectDefinition<T> toShadowDefinition();
}
