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
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.util.ObjectQueryUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.xml.namespace.QName;
import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Describes a resource object class.
 * Basically, it is a collection of resource attributes. No other items should be there.
 *
 * @author mederly
 */
public interface ObjectClassComplexTypeDefinition extends ComplexTypeDefinition {

	/**
	 * Returns all attribute definitions as an unmodifiable collection.
	 * (Should be the same content as returned by getDefinitions().)
	 */
	@NotNull
	Collection<? extends ResourceAttributeDefinition<?>> getAttributeDefinitions();

	/**
	 * Finds a attribute definition by looking at the property name.
	 * <p/>
	 * Returns null if nothing is found.
	 *
	 * @param name property definition name
	 * @return found property definition or null
	 */
	@Nullable
	default <X> ResourceAttributeDefinition<X> findAttributeDefinition(QName name) {
		return findItemDefinition(name, ResourceAttributeDefinition.class, false);
	}

	/**
	 * Finds a attribute definition by looking at the property name; not considering the case.
	 * <p/>
	 * Returns null if nothing is found.
	 *
	 * @param name property definition name
	 * @return found property definition or null
	 */
	@Nullable
	default <X> ResourceAttributeDefinition<X> findAttributeDefinition(QName name, boolean caseInsensitive) {
		return findItemDefinition(name, ResourceAttributeDefinition.class, caseInsensitive);
	}

	default <X> ResourceAttributeDefinition<X> findAttributeDefinition(String name) {
		return findAttributeDefinition(new QName(getTypeName().getNamespaceURI(), name));
	}

	/**
	 * Returns the definition of primary identifier attributes of a resource object.
	 *
	 * May return empty set if there are no identifier attributes. Must not
	 * return null.
	 *
	 * The exception should be never thrown unless there is some bug in the
	 * code. The validation of model consistency should be done at the time of
	 * schema parsing.
	 *
	 * @return definition of identifier attributes
	 */
	@NotNull
	Collection<? extends ResourceAttributeDefinition<?>> getPrimaryIdentifiers();

	/**
	 * Returns true if the attribute with a given name is among primary identifiers.
	 * Matching is done using namespace-approximate method (testing only local part if
	 * no namespace is provided), so beware of incidental matching (e.g. ri:uid vs icfs:uid).
	 */
	default boolean isPrimaryIdentifier(QName attrName) {
		return getPrimaryIdentifiers().stream()
				.anyMatch(idDef -> QNameUtil.match(idDef.getName(), attrName));
	}

	/**
	 * Returns the definition of secondary identifier attributes of a resource
	 * object.
	 *
	 * May return empty set if there are no secondary identifier attributes.
	 * Must not return null.
	 *
	 * The exception should be never thrown unless there is some bug in the
	 * code. The validation of model consistency should be done at the time of
	 * schema parsing.
	 *
	 * @return definition of secondary identifier attributes
	 */
	@NotNull
	Collection<? extends ResourceAttributeDefinition<?>> getSecondaryIdentifiers();

	/**
	 * Returns true if the attribute with a given name is among secondary identifiers.
	 * Matching is done using namespace-approximate method (testing only local part if
	 * no namespace is provided), so beware of incidental matching (e.g. ri:uid vs icfs:uid).
	 */
	default boolean isSecondaryIdentifier(QName attrName) {
		return getSecondaryIdentifiers().stream()
				.anyMatch(idDef -> QNameUtil.match(idDef.getName(), attrName));
	}

	/**
	 * Returns the definition of description attribute of a resource object.
	 *
	 * Returns null if there is no description attribute.
	 *
	 * The exception should be never thrown unless there is some bug in the
	 * code. The validation of model consistency should be done at the time of
	 * schema parsing.
	 */
	<X> ResourceAttributeDefinition<X> getDescriptionAttribute();

	/**
	 * TODO
	 */
	<X> ResourceAttributeDefinition<X> getNamingAttribute();

	/**
	 * Returns the definition of display name attribute.
	 *
	 * Display name attribute specifies which resource attribute should be used
	 * as title when displaying objects of a specific resource object class. It
	 * must point to an attribute of String type. If not present, primary
	 * identifier should be used instead (but this method does not handle this
	 * default behavior).
	 *
	 * Returns null if there is no display name attribute.
	 *
	 * The exception should be never thrown unless there is some bug in the
	 * code. The validation of model consistency should be done at the time of
	 * schema parsing.
	 */
	<X> ResourceAttributeDefinition<X> getDisplayNameAttribute();

	/**
	 * Returns both primary and secondary identifiers.
	 */
	default Collection<? extends ResourceAttributeDefinition<?>> getAllIdentifiers() {
		return Stream.concat(getPrimaryIdentifiers().stream(), getSecondaryIdentifiers().stream())
				.collect(Collectors.toList());
	}

	/**
	 * Returns the native object class string for the resource object.
	 *
	 * Native object class is the name of the Resource Object Definition (Object
	 * Class) as it is seen by the resource itself. The name of the Resource
	 * Object Definition may be constrained by XSD or other syntax and therefore
	 * may be "mangled" to conform to such syntax. The <i>native object
	 * class</i> value will contain unmangled name (if available).
	 *
	 * Returns null if there is no native object class.
	 *
	 * The exception should be never thrown unless there is some bug in the
	 * code. The validation of model consistency should be done at the time of
	 * schema parsing.
	 *
	 * @return native object class
	 */
	String getNativeObjectClass();

	/**
	 * TODO
	 * @return
	 */
	boolean isAuxiliary();

	/**
	 * TODO: THIS SHOULD NOT BE HERE
	 * @return
	 */
	ShadowKindType getKind();

	/**
	 * Indicates whether definition is should be used as default definition in ist kind.
	 * E.g. if used in an "account" kind it indicates default account definition.
	 *
	 * If true value is returned then the definition should be used as a default
	 * definition for the kind. This is a way how a resource connector may
	 * suggest applicable object classes (resource object definitions) for
	 * individual shadow kinds (e.g. accounts).
	 *
	 * @return true if the definition should be used as account type.
	 */
	boolean isDefaultInAKind();

	/**
	 * TODO: THIS SHOULD NOT BE HERE
	 * @return
	 */
	String getIntent();

	default ResourceAttributeContainerDefinition toResourceAttributeContainerDefinition() {
		return toResourceAttributeContainerDefinition(ShadowType.F_ATTRIBUTES);
	}

	default ResourceAttributeContainerDefinition toResourceAttributeContainerDefinition(QName elementName) {
		return new ResourceAttributeContainerDefinitionImpl(elementName, this, getPrismContext());
	}

	default ObjectQuery createShadowSearchQuery(String resourceOid) throws SchemaException {
		return ObjectQueryUtil.createResourceAndObjectClassQuery(resourceOid, getTypeName(), getPrismContext());
	}

	ResourceAttributeContainer instantiate(QName elementName);

	@NotNull
	ObjectClassComplexTypeDefinition clone();

	@NotNull
	ObjectClassComplexTypeDefinition deepClone(Map<QName, ComplexTypeDefinition> ctdMap);

	boolean matches(ShadowType shadowType);
}
