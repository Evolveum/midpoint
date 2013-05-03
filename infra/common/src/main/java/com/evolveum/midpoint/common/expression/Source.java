/**
 * Copyright (c) 2011 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * Portions Copyrighted 2011 [name of copyright owner]
 */
package com.evolveum.midpoint.common.expression;

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
