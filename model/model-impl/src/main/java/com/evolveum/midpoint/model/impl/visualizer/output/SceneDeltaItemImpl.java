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

import com.evolveum.midpoint.model.api.visualizer.Name;
import com.evolveum.midpoint.model.api.visualizer.SceneDeltaItem;
import com.evolveum.midpoint.model.api.visualizer.SceneItem;
import com.evolveum.midpoint.model.api.visualizer.SceneItemValue;
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
public class SceneDeltaItemImpl extends SceneItemImpl implements SceneDeltaItem, DebugDumpable {

	private List<SceneItemValueImpl> oldValues;
	private List<SceneItemValueImpl> addedValues;
	private List<SceneItemValueImpl> deletedValues;
	private ItemDelta<?,?> sourceDelta;

	public SceneDeltaItemImpl(NameImpl name) {
		super(name);
	}

	@Override
	public List<? extends SceneItemValue> getOldValues() {
		return oldValues;
	}

	public void setOldValues(List<SceneItemValueImpl> oldValues) {
		this.oldValues = oldValues;
	}

	@Override
	public List<SceneItemValueImpl> getAddedValues() {
		return addedValues;
	}

	public void setAddedValues(List<SceneItemValueImpl> addedValues) {
		this.addedValues = addedValues;
	}

	public List<SceneItemValueImpl> getDeletedValues() {
		return deletedValues;
	}

	public void setDeletedValues(List<SceneItemValueImpl> deletedValues) {
		this.deletedValues = deletedValues;
	}

	@Override
	public ItemDelta<?, ?> getSourceDelta() {
		return sourceDelta;
	}

	public void setSourceDelta(ItemDelta<?, ?> sourceDelta) {
		this.sourceDelta = sourceDelta;
	}

	@Override
	public String debugDump() {
		return debugDump(0);
	}

	@Override
	public String debugDump(int indent) {
		StringBuilder sb = new StringBuilder();
		DebugUtil.indentDebugDump(sb, indent);
		sb.append("ItemDelta: ").append(name).append(" [rel-path: ").append(sourcePath).append("]");
		if (sourceItem != null) {
			sb.append(" ITEM");
			if (sourceItem.getDefinition() != null) {
				sb.append(" DEF(").append(sourceItem.getDefinition().getName().getLocalPart()).append(")");
			}
		}
		if (sourceDelta != null) {
			sb.append(" DELTA");
		}
		if (operational) {
			sb.append(" OPER");
		}
		sb.append("\n");
		DebugUtil.indentDebugDump(sb, indent+1);
		sb.append("OLD: ").append(oldValues).append("\n");
		DebugUtil.indentDebugDump(sb, indent+1);
		sb.append("NEW: ").append(newValues).append("\n");
		DebugUtil.indentDebugDump(sb, indent+1);
		sb.append("ADDED: ").append(addedValues).append("\n");
		DebugUtil.indentDebugDump(sb, indent+1);
		sb.append("DELETED: ").append(deletedValues);
		return sb.toString();
	}
}
