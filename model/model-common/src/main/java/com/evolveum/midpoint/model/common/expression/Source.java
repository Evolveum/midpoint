/*
 * Copyright (c) 2010-2013 Evolveum
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
package com.evolveum.midpoint.model.common.expression;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.util.PrettyPrinter;

/**
 * @author semancik
 *
 */
public class Source<V extends PrismValue> extends ItemDeltaItem<V> {

	private QName name;

	public Source(Item<V> itemOld, ItemDelta<V> delta, Item<V> itemNew, QName name) {
		super(itemOld, delta, itemNew);
		this.name = name;
	}
	
	public Source(ItemDeltaItem<V> idi, QName name) {
		super(idi);
		this.name = name;
	}

	public QName getName() {
		return name;
	}

	public void setName(QName name) {
		this.name = name;
	}
	
	public Item<V> getEmptyItem() {
		ItemDefinition definition = getDefinition();
		if (definition == null) {
			throw new IllegalStateException("No definition in source "+this);
		}
		return definition.instantiate(getElementName());
	}

	@Override
	public String toString() {
		return "Source(" + shortDebugDump() + ")";
	}
	
	public String shortDebugDump() {
		return PrettyPrinter.prettyPrint(name) + ": old=" + itemOld + ", delta=" + delta + ", new=" + itemNew;
	}
	
}
