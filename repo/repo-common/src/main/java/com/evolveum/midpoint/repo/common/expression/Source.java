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
package com.evolveum.midpoint.repo.common.expression;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.util.ItemDeltaItem;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.util.ShortDumpable;
import com.evolveum.midpoint.util.exception.SchemaException;

/**
 * @author semancik
 *
 */
public class Source<V extends PrismValue,D extends ItemDefinition> extends ItemDeltaItem<V,D> implements DebugDumpable, ShortDumpable {

	private QName name;
	// We need explicit definition, because source may be completely null.
	// No item, no delta, nothing. In that case we won't be able to crete properly-typed
	// variable from the source.
	private D definition;

	public Source(Item<V,D> itemOld, ItemDelta<V,D> delta, Item<V,D> itemNew, QName name, D definition) {
		super(itemOld, delta, itemNew);
		this.name = name;
		if (definition == null) {
			// Try to automatically determine definition from content.
			this.definition = super.getDefinition();
			if (this.definition == null) {
				throw new IllegalArgumentException("Cannot determine definition from content in "+this);
			}
		} else {
			this.definition = definition;
		}
	}

	public Source(ItemDeltaItem<V,D> idi, QName name) {
		super(idi);
		this.name = name;
	}

	public QName getName() {
		return name;
	}

	public void setName(QName name) {
		this.name = name;
	}
	
	public D getDefinition() {
		return definition;
	}

	public void setDefinition(D definition) {
		this.definition = definition;
	}

	public Item<V,D> getEmptyItem() throws SchemaException {
		ItemDefinition definition = getDefinition();
		if (definition == null) {
			throw new IllegalStateException("No definition in source "+this);
		}
		return definition.instantiate(getElementName());
	}

	@Override
	public String toString() {
		return "Source(" + shortDump() + ")";
	}

	@Override
	public void shortDump(StringBuilder sb) {
		sb.append(PrettyPrinter.prettyPrint(name)).append(": old=").append(getItemOld()).append(", delta=").append(getDelta()).append(", new=").append(getItemNew());
	}
	
	public void mediumDump(StringBuilder sb) {
		sb.append("Source ").append(PrettyPrinter.prettyPrint(name)).append(":\n");
		sb.append("  old: ").append(getItemOld()).append("\n");
		sb.append("  delta: ").append(getDelta()).append("\n");
		sb.append("  new: ").append(getItemNew());
	}

	@Override
	public String debugDump(int indent) {
		StringBuilder sb = new StringBuilder();
		DebugUtil.indentDebugDump(sb, indent);
		sb.append("Source ").append(PrettyPrinter.prettyPrint(name));
		sb.append("\n");
		DebugUtil.debugDumpWithLabelLn(sb, "old", getItemOld(), indent +1);
		DebugUtil.debugDumpWithLabelLn(sb, "delta", getDelta(), indent +1);
		DebugUtil.debugDumpWithLabelLn(sb, "new", getItemNew(), indent +1);
		DebugUtil.debugDumpWithLabel(sb, "definition", definition, indent +1);
		return sb.toString();
	}

}
