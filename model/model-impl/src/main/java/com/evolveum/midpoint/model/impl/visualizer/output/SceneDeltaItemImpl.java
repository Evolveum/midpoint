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

package com.evolveum.midpoint.model.impl.visualizer.output;

import com.evolveum.midpoint.model.api.visualizer.SceneDeltaItem;
import com.evolveum.midpoint.model.api.visualizer.SceneItemValue;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

/**
 * @author mederly
 */
public class SceneDeltaItemImpl extends SceneItemImpl implements SceneDeltaItem, DebugDumpable {

	@NotNull private List<SceneItemValueImpl> oldValues = Collections.emptyList();
	@NotNull private List<SceneItemValueImpl> addedValues = Collections.emptyList();
	@NotNull private List<SceneItemValueImpl> deletedValues = Collections.emptyList();
	@NotNull private List<SceneItemValueImpl> unchangedValues = Collections.emptyList();
	private ItemDelta<?,?> sourceDelta;

	public SceneDeltaItemImpl(NameImpl name) {
		super(name);
	}

	@NotNull
	@Override
	public List<? extends SceneItemValue> getOldValues() {
		return oldValues;
	}

	public void setOldValues(@NotNull List<SceneItemValueImpl> oldValues) {
		this.oldValues = oldValues;
	}

	@NotNull
	@Override
	public List<SceneItemValueImpl> getAddedValues() {
		return addedValues;
	}

	public void setAddedValues(@NotNull List<SceneItemValueImpl> addedValues) {
		this.addedValues = addedValues;
	}

	@Override
    @NotNull
	public List<SceneItemValueImpl> getDeletedValues() {
		return deletedValues;
	}

	public void setDeletedValues(@NotNull List<SceneItemValueImpl> deletedValues) {
		this.deletedValues = deletedValues;
	}

	@Override
    @NotNull
	public List<SceneItemValueImpl> getUnchangedValues() {
		return unchangedValues;
	}

	public void setUnchangedValues(@NotNull List<SceneItemValueImpl> unchangedValues) {
		this.unchangedValues = unchangedValues;
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
		StringBuilder sb = debugDumpCommon(indent);
		if (sourceDelta != null) {
			sb.append(" DELTA");
		}
		sb.append("\n");
		DebugUtil.indentDebugDump(sb, indent+1);
		sb.append("OLD: ").append(oldValues).append("\n");
		DebugUtil.indentDebugDump(sb, indent+1);
		sb.append("NEW: ").append(newValues).append("\n");
		DebugUtil.indentDebugDump(sb, indent+1);
		sb.append("ADDED: ").append(addedValues).append("\n");
		DebugUtil.indentDebugDump(sb, indent+1);
		sb.append("DELETED: ").append(deletedValues).append("\n");
		DebugUtil.indentDebugDump(sb, indent+1);
		sb.append("UNCHANGED: ").append(unchangedValues);
		return sb.toString();
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		if (!super.equals(o)) return false;

		SceneDeltaItemImpl that = (SceneDeltaItemImpl) o;

		if (!oldValues.equals(that.oldValues)) return false;
		if (!addedValues.equals(that.addedValues)) return false;
		if (!deletedValues.equals(that.deletedValues)) return false;
		if (!unchangedValues.equals(that.unchangedValues)) return false;
		return !(sourceDelta != null ? !sourceDelta.equals(that.sourceDelta) : that.sourceDelta != null);

	}

	@Override
	public int hashCode() {
		int result = super.hashCode();
		result = 31 * result + oldValues.hashCode();
		result = 31 * result + addedValues.hashCode();
		result = 31 * result + deletedValues.hashCode();
		result = 31 * result + unchangedValues.hashCode();
		result = 31 * result + (sourceDelta != null ? sourceDelta.hashCode() : 0);
		return result;
	}
}
