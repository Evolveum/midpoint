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

import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.exception.SchemaException;
import org.jetbrains.annotations.NotNull;

import javax.xml.namespace.QName;
import java.io.Serializable;
import java.util.Map;

/**
 * @author mederly
 */
public interface ItemDefinition<I extends Item> extends Definition {

	@NotNull
	QName getName();

	String getNamespace();

	int getMinOccurs();

	int getMaxOccurs();

	boolean isSingleValue();

	boolean isMultiValue();

	boolean isMandatory();

	boolean isOptional();

	boolean isOperational();

	/**
	 * Whether an item is inherited from a supertype.
	 */
	boolean isInherited();

	/**
	 * Returns true if definition was created during the runtime based on a dynamic information
	 * such as xsi:type attributes in XML. This means that the definition needs to be stored
	 * alongside the data to have a successful serialization "roundtrip". The definition is not
	 * part of any schema and therefore cannot be determined. It may even be different for every
	 * instance of the associated item (element name).
	 */
	boolean isDynamic();

	/**
	 * Returns true if this item can be read (displayed).
	 * In case of containers this means that the container itself can be read, e.g. that the container
	 * label or block should be displayed. This usually happens if the container contains at least one
	 * readable item.
	 * This does NOT mean that also all the container items can be displayed. The sub-item permissions
	 * are controlled by similar properties on the items. This property only applies to the container
	 * itself: the "shell" of the container.
	 */
	boolean canRead();

	/**
	 * Returns true if this item can be modified (updated).
	 * In case of containers this means that the container itself should be displayed in modification forms
	 * E.g. that the container label or block should be displayed. This usually happens if the container
	 * contains at least one modifiable item.
	 * This does NOT mean that also all the container items can be modified. The sub-item permissions
	 * are controlled by similar properties on the items. This property only applies to the container
	 * itself: the "shell" of the container.
	 */
	boolean canModify();

	/**
	 * Returns true if this item can be added: it can be part of an object that is created.
	 * In case of containers this means that the container itself should be displayed in creation forms
	 * E.g. that the container label or block should be displayed. This usually happens if the container
	 * contains at least one createable item.
	 * This does NOT mean that also all the container items can be created. The sub-item permissions
	 * are controlled by similar properties on the items. This property only applies to the container
	 * itself: the "shell" of the container.
	 */
	boolean canAdd();

	/**
	 * Returns the name of an element this one can be substituted for (e.g. c:user -&gt; c:object,
	 * s:pipeline -&gt; s:expression, etc). EXPERIMENTAL
	 */
	QName getSubstitutionHead();

	/**
	 * Can be used in heterogeneous lists as a list item. EXPERIMENTAL.
	 */
	boolean isHeterogeneousListItem();

	PrismReferenceValue getValueEnumerationRef();

	boolean isValidFor(QName elementQName, Class<? extends ItemDefinition> clazz);

	boolean isValidFor(@NotNull QName elementQName, @NotNull Class<? extends ItemDefinition> clazz, boolean caseInsensitive);

	void adoptElementDefinitionFrom(ItemDefinition otherDef);

	/**
	 * Create an item instance. Definition name or default name will
	 * used as an element name for the instance. The instance will otherwise be empty.
	 * @return created item instance
	 */
	@NotNull
	I instantiate() throws SchemaException;

	/**
	 * Create an item instance. Definition name will use provided name.
	 * for the instance. The instance will otherwise be empty.
	 * @return created item instance
	 */
	@NotNull
	I instantiate(QName name) throws SchemaException;

	<T extends ItemDefinition> T findItemDefinition(@NotNull ItemPath path, @NotNull Class<T> clazz);

	ItemDelta createEmptyDelta(ItemPath path);

	@NotNull
	ItemDefinition<I> clone();

	ItemDefinition<I> deepClone(boolean ultraDeep);

	ItemDefinition<I> deepClone(Map<QName, ComplexTypeDefinition> ctdMap);

	@Override
	void revive(PrismContext prismContext);

	/**
	 * Used in debugDumping items. Does not need to have name in it as item already has it. Does not need
	 * to have class as that is just too much info that is almost anytime pretty obvious anyway.
	 */
	void debugDumpShortToString(StringBuilder sb);

	// TODO remove this hack eventually
	void setMaxOccurs(int maxOccurs);
}
