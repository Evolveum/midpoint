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

package com.evolveum.midpoint.prism;

import com.evolveum.midpoint.prism.xnode.*;
import com.evolveum.midpoint.util.exception.SchemaException;
import org.apache.commons.lang.Validate;
import org.jetbrains.annotations.NotNull;

import javax.xml.namespace.QName;

/**
 *
 */
public class PrismValueUtil {

	public static PrismContainerValue<?> getParentContainerValue(PrismValue value) {
		Itemable parent = value.getParent();
		if (parent instanceof Item) {
			PrismValue parentParent = ((Item) parent).getParent();
			return parentParent instanceof PrismContainerValue ? (PrismContainerValue) parentParent : null;
		} else {
			return null;
		}
	}

	public static <T> PrismProperty<T> createRaw(@NotNull XNode node, @NotNull QName itemName, PrismContext prismContext)
			throws SchemaException {
		Validate.isTrue(!(node instanceof RootXNode));
		PrismProperty<T> property = prismContext.itemFactory().createProperty(itemName);
		if (node instanceof ListXNode) {
			for (XNode subnode : ((ListXNode) node).asList()) {
				property.add(createRaw(subnode, prismContext));
			}
		} else {
			property.add(createRaw(node, prismContext));
		}
		return property;
	}

	private static <T> PrismPropertyValue<T> createRaw(XNode rawElement, PrismContext prismContext) {
		return prismContext.itemFactory().createPropertyValue(rawElement);
	}

}
