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

package com.evolveum.midpoint.web.component.prism.show;

import com.evolveum.midpoint.model.api.visualizer.Name;
import com.evolveum.midpoint.model.api.visualizer.Scene;
import com.evolveum.midpoint.model.api.visualizer.SceneItem;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.DebugUtil;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Artificial implementation of a scene used to hold a list of deltas.
 * (A bit of hack, unfortunately.)
 *
 * @author mederly
 */
public class WrapperScene implements Scene {

	private String displayNameKey;
	private Object[] displayNameParameters;
	private List<? extends Scene> partialScenes;

	public WrapperScene(List<? extends Scene> partialScenes, String displayNameKey, Object... displayNameParameters) {
		this.partialScenes = partialScenes;
		this.displayNameKey = displayNameKey;
		this.displayNameParameters = displayNameParameters;
	}

	public String getDisplayNameKey() {
		return displayNameKey;
	}

	public Object[] getDisplayNameParameters() {
		return displayNameParameters;
	}

	@Override
	public Name getName() {
		return new Name() {

			@Override
			public String getSimpleName() {
				return "";
			}

			@Override
			public String getDisplayName() {
				return null;
			}

			@Override
			public String getId() {
				return null;
			}

			@Override
			public String getDescription() {
				return null;
			}

			@Override
			public boolean namesAreResourceKeys() {
				return false;
			}
		};
	}

	@Override
	public ChangeType getChangeType() {
		return null;
	}

	@NotNull
	@Override
	public List<? extends Scene> getPartialScenes() {
		return partialScenes;
	}

	@NotNull
	@Override
	public List<? extends SceneItem> getItems() {
		return Collections.emptyList();
	}

	@Override
	public boolean isOperational() {
		return false;
	}

	@Override
	public Scene getOwner() {
		return null;
	}

	@Override
	public ItemPath getSourceRelPath() {
		return null;
	}

	@Override
	public ItemPath getSourceAbsPath() {
		return null;
	}

	@Override
	public PrismContainerValue<?> getSourceValue() {
		return null;
	}

	@Override
	public PrismContainerDefinition<?> getSourceDefinition() {
		return null;
	}

	@Override
	public ObjectDelta<?> getSourceDelta() {
		return null;
	}

	@Override
	public boolean isEmpty() {
		if (partialScenes == null) {
			return true;
		}
		for (Scene scene : partialScenes) {
			if (!scene.isEmpty()) {
				return false;
			}
		}
		return true;
	}

	@Override
	public String debugDump() {
		return debugDump(0);
	}

	@Override
	public String debugDump(int indent) {
		return DebugUtil.debugDump(partialScenes, indent);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		WrapperScene that = (WrapperScene) o;

		if (displayNameKey != null ? !displayNameKey.equals(that.displayNameKey) : that.displayNameKey != null)
			return false;
		// Probably incorrect - comparing Object[] arrays with Arrays.equals
		if (!Arrays.equals(displayNameParameters, that.displayNameParameters)) return false;
		return !(partialScenes != null ? !partialScenes.equals(that.partialScenes) : that.partialScenes != null);

	}

	@Override
	public int hashCode() {
		int result = displayNameKey != null ? displayNameKey.hashCode() : 0;
		result = 31 * result + (displayNameParameters != null ? Arrays.hashCode(displayNameParameters) : 0);
		result = 31 * result + (partialScenes != null ? partialScenes.hashCode() : 0);
		return result;
	}
}
