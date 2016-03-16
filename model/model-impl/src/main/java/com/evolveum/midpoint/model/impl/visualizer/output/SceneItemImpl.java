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

package com.evolveum.midpoint.model.impl.visualizer.output;

import com.evolveum.midpoint.model.api.visualizer.SceneItem;
import com.evolveum.midpoint.model.api.visualizer.Name;
import com.evolveum.midpoint.model.api.visualizer.SceneItemValue;
import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;

import java.util.List;

import static org.apache.commons.lang.Validate.notNull;

/**
 * @author mederly
 */
public class SceneItemImpl implements SceneItem, DebugDumpable {

	protected final NameImpl name;
	protected List<SceneItemValueImpl> newValues;
	protected boolean operational;
	protected Item<?,?> sourceItem;
	protected ItemPath sourcePath;

	public SceneItemImpl(NameImpl name) {
		notNull(name);
		this.name = name;
	}

	@Override
	public Name getName() {
		return name;
	}

	@Override
	public List<? extends SceneItemValue> getNewValues() {
		return newValues;
	}

	public void setNewValues(List<SceneItemValueImpl> newValues) {
		this.newValues = newValues;
	}

	@Override
	public boolean isOperational() {
		return operational;
	}

	public void setOperational(boolean operational) {
		this.operational = operational;
	}

	@Override
	public Item<?, ?> getSourceItem() {
		return sourceItem;
	}

	public void setSourceItem(Item<?, ?> sourceItem) {
		this.sourceItem = sourceItem;
	}

	@Override
	public ItemPath getSourcePath() {
		return sourcePath;
	}

	public void setSourcePath(ItemPath sourcePath) {
		this.sourcePath = sourcePath;
	}

	@Override
	public String debugDump() {
		return debugDump(0);
	}

	@Override
	public String debugDump(int indent) {
		StringBuilder sb = new StringBuilder();
		DebugUtil.indentDebugDump(sb, indent);
		sb.append("Item: ").append(name).append(" [path: ").append(sourcePath).append("]");
		if (sourceItem != null) {
			sb.append(" SITEM");
			if (sourceItem.getDefinition() != null) {
				sb.append(" SDEF");
			}
		}
		sb.append("\n");
		DebugUtil.indentDebugDump(sb, indent+1);
		sb.append("VALUES: ").append(newValues);
		return sb.toString();
	}
}
