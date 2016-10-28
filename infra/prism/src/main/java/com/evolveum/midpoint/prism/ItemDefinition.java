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

	boolean canRead();

	boolean canModify();

	boolean canAdd();

	PrismReferenceValue getValueEnumerationRef();

	boolean isValidFor(QName elementQName, Class<? extends ItemDefinition> clazz);

	boolean isValidFor(@NotNull QName elementQName, @NotNull Class<? extends ItemDefinition> clazz, boolean caseInsensitive);

	void adoptElementDefinitionFrom(ItemDefinition otherDef);

	/**
	 * Create an item instance. Definition name or default name will
	 * used as an element name for the instance. The instance will otherwise be empty.
	 * @return created item instance
	 */
	I instantiate() throws SchemaException;

	/**
	 * Create an item instance. Definition name will use provided name.
	 * for the instance. The instance will otherwise be empty.
	 * @return created item instance
	 */
	I instantiate(QName name) throws SchemaException;

	<T extends ItemDefinition> T findItemDefinition(@NotNull ItemPath path, @NotNull Class<T> clazz);

	ItemDelta createEmptyDelta(ItemPath path);

	@NotNull
	ItemDefinition<I> clone();

	ItemDefinition<I> deepClone(boolean ultraDeep);

	ItemDefinition<I> deepClone(Map<QName, ComplexTypeDefinition> ctdMap);

	@Override
	void revive(PrismContext prismContext);

}
