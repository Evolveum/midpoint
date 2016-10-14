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

package com.evolveum.midpoint.common.refinery;

import com.evolveum.midpoint.common.ResourceObjectPattern;
import com.evolveum.midpoint.prism.ComplexTypeDefinition;
import com.evolveum.midpoint.prism.Definition;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.schema.ResourceShadowDiscriminator;
import com.evolveum.midpoint.schema.processor.ObjectClassComplexTypeDefinition;
import com.evolveum.midpoint.schema.processor.ResourceAttributeDefinition;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.CapabilityType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.PagedSearchCapabilityType;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

import javax.xml.namespace.QName;
import java.util.Collection;
import java.util.List;

/**
 * @author mederly
 */
public interface RefinedObjectClassDefinition extends ObjectClassComplexTypeDefinition {

	LayerRefinedObjectClassDefinition forLayer(LayerType layerType);

	Collection<RefinedAssociationDefinition> getAssociations();

	Collection<RefinedAssociationDefinition> getAssociations(ShadowKindType kind);

	RefinedAssociationDefinition findAssociation(QName name);

	Collection<RefinedAssociationDefinition> getEntitlementAssociations();

	RefinedAssociationDefinition findEntitlementAssociation(QName name);

	Collection<QName> getNamesOfAssociations();

	Collection<? extends QName> getNamesOfAssociationsWithOutboundExpressions();

	Collection<RefinedObjectClassDefinition> getAuxiliaryObjectClassDefinitions();

	boolean hasAuxiliaryObjectClass(QName expectedObjectClassName);

	Collection<ResourceObjectPattern> getProtectedObjectPatterns();

	@Override
	RefinedAttributeDefinition<?> getNamingAttribute();

	@Override
	RefinedAttributeDefinition<?> getDisplayNameAttribute();

	@Override
	Collection<? extends RefinedAttributeDefinition<?>> getPrimaryIdentifiers();

	@Override
	Collection<? extends RefinedAttributeDefinition<?>> getSecondaryIdentifiers();

	@Override
	Collection<? extends RefinedAttributeDefinition<?>> getAllIdentifiers();

	@Override
	<X> RefinedAttributeDefinition<X> findAttributeDefinition(QName elementQName);

	@Override
	<X> RefinedAttributeDefinition<X> findAttributeDefinition(String elementLocalname);

	String getResourceNamespace();

	String getDescription();

	boolean isDefault();

	ObjectClassComplexTypeDefinition getObjectClassDefinition();

	ResourceType getResourceType();

	PrismObjectDefinition<ShadowType> getObjectDefinition();

	ResourceObjectReferenceType getBaseContext();

	RefinedAttributeDefinition<?> getAttributeDefinition(QName attributeName);

	@Override
	Collection<? extends RefinedAttributeDefinition<?>> getAttributeDefinitions();

	boolean containsAttributeDefinition(ItemPathType pathType);

	boolean containsAttributeDefinition(QName attributeName);

	PrismObject<ShadowType> createBlankShadow();

	ResourceShadowDiscriminator getShadowDiscriminator();

	Collection<? extends QName> getNamesOfAttributesWithOutboundExpressions();

	Collection<? extends QName> getNamesOfAttributesWithInboundExpressions();

	List<MappingType> getPasswordInbound();

	MappingType getPasswordOutbound();

	AttributeFetchStrategyType getPasswordFetchStrategy();

	ObjectReferenceType getPasswordPolicy();

	ResourcePasswordDefinitionType getPasswordDefinition();

	ResourceActivationDefinitionType getActivationSchemaHandling();

	ResourceBidirectionalMappingType getActivationBidirectionalMappingType(QName propertyName);

	AttributeFetchStrategyType getActivationFetchStrategy(QName propertyName);

	<T extends CapabilityType> T getEffectiveCapability(Class<T> capabilityClass);

	PagedSearchCapabilityType getPagedSearches();

	boolean isPagedSearchEnabled();

	boolean isObjectCountingEnabled();

	boolean matches(ShadowType shadowType);

	String getHumanReadableName();

	@Override
	RefinedObjectClassDefinition clone();
}
