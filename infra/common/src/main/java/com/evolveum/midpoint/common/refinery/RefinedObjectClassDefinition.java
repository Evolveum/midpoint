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

package com.evolveum.midpoint.common.refinery;

import com.evolveum.midpoint.common.ResourceObjectPattern;
import com.evolveum.midpoint.prism.ComplexTypeDefinition;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.util.ItemPathUtil;
import com.evolveum.midpoint.schema.ResourceShadowDiscriminator;
import com.evolveum.midpoint.schema.processor.ObjectClassComplexTypeDefinition;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.CapabilityType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.PagedSearchCapabilityType;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;
import org.jetbrains.annotations.NotNull;

import javax.xml.namespace.QName;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author mederly
 */
public interface RefinedObjectClassDefinition extends ObjectClassComplexTypeDefinition {

	//region General attribute definitions ========================================================

	/**
	 * Returns definitions of all attributes as an unmodifiable collection.
	 * Note: content of this is exactly the same as for getDefinitions
	 */
	@NotNull
	@Override
	Collection<? extends RefinedAttributeDefinition<?>> getAttributeDefinitions();

	default boolean containsAttributeDefinition(ItemPathType pathType) {
		QName segmentQName = ItemPathUtil.getOnlySegmentQName(pathType);
		return containsAttributeDefinition(segmentQName);
	}

	default boolean containsAttributeDefinition(QName attributeName) {
		return findAttributeDefinition(attributeName) != null;
	}

	Collection<? extends QName> getNamesOfAttributesWithOutboundExpressions();

	Collection<? extends QName> getNamesOfAttributesWithInboundExpressions();

	//endregion

	//region Special attribute definitions ========================================================
	// Note that these are simply type-narrowed versions of methods in ObjectClassComplexTypeDefinition

	@NotNull
	@Override
	Collection<? extends RefinedAttributeDefinition<?>> getPrimaryIdentifiers();

	@NotNull
	@Override
	Collection<? extends RefinedAttributeDefinition<?>> getSecondaryIdentifiers();

	@Override
	default Collection<? extends RefinedAttributeDefinition<?>> getAllIdentifiers() {
		return Stream.concat(getPrimaryIdentifiers().stream(), getSecondaryIdentifiers().stream())
				.collect(Collectors.toList());
	}

	<X> RefinedAttributeDefinition<X> getDescriptionAttribute();
	<X> RefinedAttributeDefinition<X> getNamingAttribute();
	<X> RefinedAttributeDefinition<X> getDisplayNameAttribute();

	//endregion

	//region General association definitions ========================================================

	/**
	 * Returns definitions of all associations as an unmodifiable collection.
	 * Note: these items are _not_ included in getDefinitions. (BTW, RefinedAssociationDefinition
	 * is not a subtype of ItemDefinition, not even of Definition.)
	 */
	@NotNull
	Collection<RefinedAssociationDefinition> getAssociationDefinitions();

	Collection<RefinedAssociationDefinition> getAssociationDefinitions(ShadowKindType kind);

	RefinedAssociationDefinition findAssociationDefinition(QName name);

	Collection<QName> getNamesOfAssociations();

	Collection<? extends QName> getNamesOfAssociationsWithOutboundExpressions();
	//endregion

	//region General information ========================================================

	String getDescription();

	ObjectClassComplexTypeDefinition getObjectClassDefinition();

	ResourceType getResourceType();

	String getResourceNamespace();

	boolean isDefault();

	ResourceObjectReferenceType getBaseContext();

	String getHumanReadableName();
	
	ResourceObjectVolatilityType getVolatility();
	//endregion


	//region Generating and matching artifacts ========================================================
	PrismObjectDefinition<ShadowType> getObjectDefinition();

	default PrismObject<ShadowType> createBlankShadow() {
		return createBlankShadow(this);
	}

	PrismObject<ShadowType> createBlankShadow(RefinedObjectClassDefinition definition);

	ResourceShadowDiscriminator getShadowDiscriminator();

	boolean matches(ShadowType shadowType);
	//endregion

	//region Accessing parts of schema handling ========================================================

	@NotNull
	Collection<RefinedObjectClassDefinition> getAuxiliaryObjectClassDefinitions();

	boolean hasAuxiliaryObjectClass(QName expectedObjectClassName);

	Collection<ResourceObjectPattern> getProtectedObjectPatterns();

	ResourcePasswordDefinitionType getPasswordDefinition();

	List<MappingType> getPasswordInbound();

	List<MappingType> getPasswordOutbound();

	AttributeFetchStrategyType getPasswordFetchStrategy();

	ObjectReferenceType getPasswordPolicy();

	ResourceActivationDefinitionType getActivationSchemaHandling();

	ResourceBidirectionalMappingType getActivationBidirectionalMappingType(QName propertyName);

	AttributeFetchStrategyType getActivationFetchStrategy(QName propertyName);
	//endregion

	//region Capabilities ========================================================

	<T extends CapabilityType> T getEffectiveCapability(Class<T> capabilityClass);

	PagedSearchCapabilityType getPagedSearches();

	boolean isPagedSearchEnabled();

	boolean isObjectCountingEnabled();

	//endregion

	//region Cloning ========================================================
	@NotNull
	@Override
	RefinedObjectClassDefinition clone();

	@NotNull
	@Override
	RefinedObjectClassDefinition deepClone(Map<QName, ComplexTypeDefinition> ctdMap);
	//endregion

	LayerRefinedObjectClassDefinition forLayer(@NotNull LayerType layerType);

	//region Type variance ========================================================

	<X> RefinedAttributeDefinition<X> findAttributeDefinition(@NotNull QName name);

	default <X> RefinedAttributeDefinition<X> findAttributeDefinition(String name) {
		return findAttributeDefinition(new QName(getTypeName().getNamespaceURI(), name));
	}


	//endregion

	String getDebugDumpClassName();

}
