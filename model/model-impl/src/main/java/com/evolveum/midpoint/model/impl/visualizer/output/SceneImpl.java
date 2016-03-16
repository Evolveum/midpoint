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

import com.evolveum.midpoint.model.api.visualizer.Scene;
import com.evolveum.midpoint.model.api.visualizer.SceneItem;
import com.evolveum.midpoint.model.api.visualizer.Name;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;

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
	private ItemPath sourcePath;
	private ItemPath sourceFullPath;
	private PrismContainerValue<?> sourceValue;
	private PrismContainerDefinition<?> sourceDefinition;
	private ObjectDelta<?> sourceDelta;

	public SceneImpl(SceneImpl owner) {
		this.owner = owner;
	}

	@Override
	public Name getName() {
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

	@Override
	public List<? extends SceneImpl> getPartialScenes() {
		return partialScenes;
	}

	public void addPartialScene(SceneImpl subscene) {
		partialScenes.add(subscene);
	}

	@Override
	public List<? extends SceneItem> getItems() {
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

	@Override
	public ItemPath getSourcePath() {
		return sourcePath;
	}

	public void setSourcePath(ItemPath sourcePath) {
		this.sourcePath = sourcePath;
	}

	public ItemPath getSourceFullPath() {
		return sourceFullPath;
	}

	public void setSourceFullPath(ItemPath sourceFullPath) {
		this.sourceFullPath = sourceFullPath;
	}

	@Override
	public PrismContainerValue<?> getSourceValue() {
		return sourceValue;
	}

	public void setSourceValue(PrismContainerValue<?> sourceValue) {
		this.sourceValue = sourceValue;
	}

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
		sb.append(" [rel-path: ").append(sourcePath).append("]");
		sb.append(" [full-path: ").append(sourceFullPath).append("]");
		if (sourceValue != null) {
			sb.append(" VAL");
		}
		if (sourceDefinition != null) {
			sb.append(" DEF(").append(sourceDefinition.getName().getLocalPart()).append(")");
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
}
