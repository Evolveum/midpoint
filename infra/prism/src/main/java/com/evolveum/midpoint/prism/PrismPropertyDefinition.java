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

import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.DisplayableValue;
import org.jetbrains.annotations.NotNull;

import javax.xml.namespace.QName;
import java.util.Collection;

/**
 * @author mederly
 */
public interface PrismPropertyDefinition<T> extends ItemDefinition<PrismProperty<T>> {
	Collection<? extends DisplayableValue<T>> getAllowedValues();

	T defaultValue();

	/**
	 * Returns QName of the property value type.
	 * <p/>
	 * The returned type is either XSD simple type or complex type. It may not
	 * be defined in the same schema (especially if it is standard XSD simple
	 * type).
	 *
	 * @return QName of the property value type
	 *
	 * NOTE: This is very strange property. Isn't it the same as typeName().
	 * It is even not used in midPoint. Marking as deprecated.
	 */
	@Deprecated
	QName getValueType();

	Boolean isIndexed();

	default boolean isAnyType() {
		return DOMUtil.XSD_ANYTYPE.equals(getTypeName());
	}

	QName getMatchingRuleQName();

	@Override
	PropertyDelta<T> createEmptyDelta(ItemPath path);

	@NotNull
	@Override
	PrismProperty<T> instantiate();

	@NotNull
	@Override
	PrismProperty<T> instantiate(QName name);

	@NotNull
	@Override
	PrismPropertyDefinition<T> clone();
}
