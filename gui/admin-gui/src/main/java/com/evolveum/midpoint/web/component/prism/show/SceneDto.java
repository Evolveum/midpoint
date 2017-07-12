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

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.model.api.visualizer.Name;
import com.evolveum.midpoint.model.api.visualizer.Scene;
import com.evolveum.midpoint.model.api.visualizer.SceneDeltaItem;
import com.evolveum.midpoint.model.api.visualizer.SceneItem;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.delta.ChangeType;
import org.apache.wicket.Component;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * @author mederly
 */
public class SceneDto implements Serializable {

	public static final java.lang.String F_CHANGE_TYPE = "changeType";
	public static final java.lang.String F_OBJECT_TYPE = "objectType";
	public static final java.lang.String F_DESCRIPTION = "description";
	public static final java.lang.String F_ITEMS = "items";
	public static final java.lang.String F_PARTIAL_SCENES = "partialScenes";

	@NotNull private final Scene scene;
	private boolean minimized;

	private String boxClassOverride;

	private final List<SceneItemDto> items = new ArrayList<>();
	private final List<SceneDto> partialScenes = new ArrayList<>();

	public SceneDto(@NotNull Scene scene) {
		this.scene = scene;
		for (SceneItem item : scene.getItems()) {
			if (item != null) {
				items.add(new SceneItemDto(this, item));
			}
		}
		for (Scene sub : scene.getPartialScenes()) {
			if (sub != null) {
				partialScenes.add(new SceneDto(sub));
			}
		}
	}

	public Scene getScene() {
		return scene;
	}

	public boolean isMinimized() {
		return minimized;
	}

	public void setMinimized(boolean minimized) {
		this.minimized = minimized;
	}

	public List<SceneDto> getPartialScenes() {
		return partialScenes;
	}

	public List<SceneItemDto> getItems() {
		return items;
	}

	public String getName(Component component) {
		if (scene.getName() != null) {
			if (scene.getName().getDisplayName() != null) {
				return resolve(scene.getName().getDisplayName(), component, scene.getName().namesAreResourceKeys());
			} else {
				return resolve(scene.getName().getSimpleName(), component, scene.getName().namesAreResourceKeys());
			}
		} else {
			return resolve("SceneDto.unnamed", component, true);
		}
	}

	private String resolve(String name, Component component, boolean namesAreResourceKeys) {
		if (namesAreResourceKeys) {
			return PageBase.createStringResourceStatic(component, name).getString();
		} else {
			return name;
		}
	}

	public String getDescription(Component component) {
		Name name = scene.getName();
		if (name == null) {
			return "";
		}
		if (scene.getSourceDefinition() != null && !(scene.getSourceDefinition() instanceof PrismObjectDefinition)) {
			return "";
		}
		if (name.getSimpleName() != null && !name.getSimpleName().equals(getName(component))) {
			return "(" + name.getSimpleName() + ")";
		}
		return "";
	}

	public ChangeType getChangeType() {
		return scene.getChangeType();
	}

	public boolean containsDeltaItems() {
		for (SceneItem item : scene.getItems()) {
			if (item instanceof SceneDeltaItem) {
				return true;
			}
		}
		return false;
	}

	public boolean isWrapper() {
		return scene instanceof WrapperScene;
	}

	public String getBoxClassOverride() {
		return boxClassOverride;
	}

	public void setBoxClassOverride(String boxClassOverride) {
		this.boxClassOverride = boxClassOverride;
	}

	// minimized is NOT included in equality check - because the SceneDto's are compared in order to determine
	// whether they should be redrawn (i.e. their content is important, not the presentation)

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		SceneDto sceneDto = (SceneDto) o;

		if (scene != null ? !scene.equals(sceneDto.scene) : sceneDto.scene != null) return false;
		if (boxClassOverride != null ? !boxClassOverride.equals(sceneDto.boxClassOverride) : sceneDto.boxClassOverride != null)
			return false;
		if (items != null ? !items.equals(sceneDto.items) : sceneDto.items != null) return false;
		return !(partialScenes != null ? !partialScenes.equals(sceneDto.partialScenes) : sceneDto.partialScenes != null);

	}

	@Override
	public int hashCode() {
		int result = scene != null ? scene.hashCode() : 0;
		result = 31 * result + (boxClassOverride != null ? boxClassOverride.hashCode() : 0);
		result = 31 * result + (items != null ? items.hashCode() : 0);
		result = 31 * result + (partialScenes != null ? partialScenes.hashCode() : 0);
		return result;
	}

	public void applyFoldingFrom(@NotNull SceneDto source) {
		minimized = source.minimized;
		int partialDst = partialScenes.size();
		int partialSrc = source.getPartialScenes().size();
		if (partialDst != partialSrc) {
			return;	// shouldn't occur
		}
		for (int i = 0; i < partialDst; i++) {
			partialScenes.get(i).applyFoldingFrom(source.getPartialScenes().get(i));
		}
	}
}
