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

import com.evolveum.midpoint.prism.path.ItemPath;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.xml.namespace.QName;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Provides a definition for a complex type, i.e. type that prescribes inner items.
 * It's instances may be container values or property values, depending on container/object
 * markers presence.
 *
 * @author semancik
 * @author mederly
 */
public interface ComplexTypeDefinition extends TypeDefinition, LocalDefinitionStore {

	/**
	 * Returns definitions for all inner items.
	 *
	 * These are of type ItemDefinition. However, very often subtypes of this type are used,
	 * e.g. ResourceAttributeDefinition, RefinedAttributeDefinition, LayerRefinedAttributeDefinition, and so on.
	 *
	 * Although returned as a list, the order of definitions is insignificant. (TODO change to set?)
	 *
	 * The list is unmodifiable.
	 */
	@NotNull
	List<? extends ItemDefinition> getDefinitions();

	/**
	 * Is this definition shared, i.e. used by more than one prism object?
	 * If so, it should not be e.g. trimmed.
	 *
	 * EXPERIMENTAL
 	 */
	boolean isShared();

	/**
	 * If not null, indicates that this type defines the structure of 'extension' element of a given type.
	 * E.g. getExtensionForType() == c:UserType means that this complex type defines structure of
	 * 'extension' elements of UserType objects.
	 */
	@Nullable
	QName getExtensionForType();

	/**
	 * Flag indicating whether this type was marked as "objectReference"
	 * in the original schema.
	 */
	boolean isReferenceMarker();

	/**
	 * Flag indicating whether this type was marked as "container"
	 * in the original schema. Does not provide any information to
	 * schema processing logic, just conveys the marker from original
	 * schema so we can serialize and deserialize the schema without
	 * loss of information.
	 */
	boolean isContainerMarker();

	/**
	 * Flag indicating whether this type was marked as "object"
	 * in the original schema. Does not provide any information to
	 * schema processing logic, just conveys the marker from original
	 * schema so we can serialized and deserialize the schema without
	 * loss of information.
	 */
	boolean isObjectMarker();

	/**
	 * True if the complex type definition contains xsd:any (directly or indirectly).
	 */
	boolean isXsdAnyMarker();

	// TODO. EXPERIMENTAL.
	boolean isListMarker();

	/**
	 * When resolving unqualified names for items contained in this CTD, what should be the default namespace
	 * to look into at first. Currently does NOT apply recursively (to inner CTDs).
	 */
	@Nullable
	String getDefaultNamespace();

	/**
	 * When resolving unqualified names for items contained in this CTD, what namespace(s) should be ignored.
	 * Names in this list are interpreted as a namespace prefixes.
	 * Currently does NOT apply recursively (to inner CTDs).
	 */
	@NotNull
	List<String> getIgnoredNamespaces();

	/**
	 * Copies cloned definitions from the other type definition into this one.
	 * (TODO remove from the interface?)
	 */
	void merge(ComplexTypeDefinition otherComplexTypeDef);

	@Override
	void revive(PrismContext prismContext);

	/**
	 * Returns true if there are no item definitions.
	 */
	boolean isEmpty();

	/**
	 * Does a shallow clone of this definition (i.e. item definitions themselves are NOT cloned).
	 */
	@NotNull
	ComplexTypeDefinition clone();

	/**
	 * Does a deep clone of this definition.
	 *
	 * @param ctdMap Keeps already cloned definitions when 'ultra deep cloning' is not requested.
	 *               Each definition is then cloned only once.
	 * @param onThisPath Keeps already cloned definitions on the path from root to current node;
	 *                   in order to prevent infinite loops when doing ultra deep cloning.
	 */
	@NotNull
	ComplexTypeDefinition deepClone(Map<QName, ComplexTypeDefinition> ctdMap, Map<QName, ComplexTypeDefinition> onThisPath);

	/**
	 * Trims the definition (and any definitions it refers to) to contain only items related to given paths.
	 * USE WITH CARE. Be sure no shared definitions would be affected by this operation!
	 */
	void trimTo(@NotNull Collection<ItemPath> paths);
}
