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

import com.evolveum.midpoint.model.api.visualizer.Scene;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * @author mederly
 */
public class SceneImpl implements Scene, DebugDumpable {

	private NameImpl name;
	private ChangeType changeType;
	private final List<SceneImpl> partialScenes = new ArrayList<>();
	private final List<SceneItemImpl> items = new ArrayList<>();
	private final SceneImpl owner;
	private boolean operational;
	private ItemPath sourceRelPath;
	private ItemPath sourceAbsPath;
	private PrismContainerValue<?> sourceValue;
	private PrismContainerDefinition<?> sourceDefinition;
	private ObjectDelta<?> sourceDelta;

	public SceneImpl(SceneImpl owner) {
		this.owner = owner;
	}

	@Override
	public NameImpl getName() {
		return name;
	}

	public void setName(NameImpl name) {
		this.name = name;
	}

	@Override
	public ChangeType getChangeType() {
		return changeType;
	}

	public void setChangeType(ChangeType changeType) {
		this.changeType = changeType;
	}

	@NotNull
	@Override
	public List<? extends SceneImpl> getPartialScenes() {
		return partialScenes;
	}

	public void addPartialScene(SceneImpl subscene) {
		partialScenes.add(subscene);
	}

	@NotNull
	@Override
	public List<? extends SceneItemImpl> getItems() {
		return items;
	}

	public void addItem(SceneItemImpl item) {
		items.add(item);
	}

	@Override
	public SceneImpl getOwner() {
		return owner;
	}

	@Override
	public boolean isOperational() {
		return operational;
	}

	public void setOperational(boolean operational) {
		this.operational = operational;
	}

	public ItemPath getSourceRelPath() {
		return sourceRelPath;
	}

	public void setSourceRelPath(ItemPath sourceRelPath) {
		this.sourceRelPath = sourceRelPath;
	}

	@Override
	public ItemPath getSourceAbsPath() {
		return sourceAbsPath;
	}

	public void setSourceAbsPath(ItemPath sourceAbsPath) {
		this.sourceAbsPath = sourceAbsPath;
	}

	@Override
	public PrismContainerValue<?> getSourceValue() {
		return sourceValue;
	}

	public void setSourceValue(PrismContainerValue<?> sourceValue) {
		this.sourceValue = sourceValue;
	}

	@Override
	public PrismContainerDefinition<?> getSourceDefinition() {
		return sourceDefinition;
	}

	public void setSourceDefinition(PrismContainerDefinition<?> sourceDefinition) {
		this.sourceDefinition = sourceDefinition;
	}

	@Override
	public ObjectDelta<?> getSourceDelta() {
		return sourceDelta;
	}

	public void setSourceDelta(ObjectDelta<?> sourceDelta) {
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
		sb.append("Scene: ");
		if (changeType != null) {
			sb.append(changeType).append(": ");
		}
		if (name != null) {
			sb.append(name.toDebugDump());
		} else {
			sb.append("(unnamed)");
		}
		sb.append(" [rel-path: ").append(sourceRelPath).append("]");
		sb.append(" [abs-path: ").append(sourceAbsPath).append("]");
		if (sourceValue != null) {
			sb.append(" VAL");
		}
		if (sourceDefinition != null) {
			sb.append(" DEF(").append(sourceDefinition.getName().getLocalPart()).append("/").append(sourceDefinition.getDisplayName()).append(")");
		}
		if (sourceDelta != null) {
			sb.append(" DELTA");
		}
		if (operational) {
			sb.append(" OPER");
		}
		for (SceneItemImpl dataItem : items) {
			sb.append("\n");
			sb.append(dataItem.debugDump(indent+1));
		}
		for (SceneImpl dataContext : partialScenes) {
			sb.append("\n");
			sb.append(dataContext.debugDump(indent+1));
		}
		return sb.toString();
	}

	public String getSourceOid() {
		if (sourceValue != null && sourceValue.getParent() instanceof PrismObject) {
			return ((PrismObject) sourceValue.getParent()).getOid();
		} else {
			return null;
		}
	}

	public boolean isObjectValue() {
		return sourceValue != null && sourceValue.getParent() instanceof PrismObject;
	}

	public boolean isContainerValue() {
		return sourceValue != null && !(sourceValue.getParent() instanceof PrismObject);
	}

	public Long getSourceContainerValueId() {
		if (isContainerValue()) {
			return sourceValue.getId();
		} else {
			return null;
		}
	}

	public boolean isFocusObject() {
		return sourceDefinition != null && sourceDefinition.getCompileTimeClass() != null && FocusType.class.isAssignableFrom(sourceDefinition.getCompileTimeClass());
	}

	public boolean isEmpty() {
		if (changeType != ChangeType.MODIFY) {
			return false;		// ADD or DELETE are never 'empty'
		}
		for (SceneItemImpl item : getItems()) {
			if (item.isDescriptive()) {
				continue;
			}
			return false;
		}
		for (SceneImpl partialScene : getPartialScenes()) {
			if (!partialScene.isEmpty()) {
				return false;
			}
		}
		return true;
	}

	// owner must not be tested here!
	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		SceneImpl scene = (SceneImpl) o;

		if (operational != scene.operational) return false;
		if (name != null ? !name.equals(scene.name) : scene.name != null) return false;
		if (changeType != scene.changeType) return false;
		if (partialScenes != null ? !partialScenes.equals(scene.partialScenes) : scene.partialScenes != null)
			return false;
		if (items != null ? !items.equals(scene.items) : scene.items != null) return false;
		if (sourceRelPath != null ? !sourceRelPath.equals(scene.sourceRelPath) : scene.sourceRelPath != null)
			return false;
		if (sourceAbsPath != null ? !sourceAbsPath.equals(scene.sourceAbsPath) : scene.sourceAbsPath != null)
			return false;
		if (sourceValue != null ? !sourceValue.equals(scene.sourceValue) : scene.sourceValue != null) return false;
		if (sourceDefinition != null ? !sourceDefinition.equals(scene.sourceDefinition) : scene.sourceDefinition != null)
			return false;
		return !(sourceDelta != null ? !sourceDelta.equals(scene.sourceDelta) : scene.sourceDelta != null);

	}

	@Override
	public int hashCode() {
		int result = name != null ? name.hashCode() : 0;
		result = 31 * result + (changeType != null ? changeType.hashCode() : 0);
		result = 31 * result + (partialScenes != null ? partialScenes.hashCode() : 0);
		result = 31 * result + (items != null ? items.hashCode() : 0);
		result = 31 * result + (operational ? 1 : 0);
		result = 31 * result + (sourceRelPath != null ? sourceRelPath.hashCode() : 0);
		result = 31 * result + (sourceAbsPath != null ? sourceAbsPath.hashCode() : 0);
		result = 31 * result + (sourceValue != null ? sourceValue.hashCode() : 0);
		result = 31 * result + (sourceDefinition != null ? sourceDefinition.hashCode() : 0);
		result = 31 * result + (sourceDelta != null ? sourceDelta.hashCode() : 0);
		return result;
	}
}
