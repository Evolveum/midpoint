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

package com.evolveum.midpoint.web.component.prism.show;

import com.evolveum.midpoint.model.api.visualizer.Name;
import com.evolveum.midpoint.model.api.visualizer.Scene;
import com.evolveum.midpoint.model.api.visualizer.SceneDeltaItem;
import com.evolveum.midpoint.model.api.visualizer.SceneItem;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.delta.ChangeType;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * @author mederly
 */
public class SceneDto implements Serializable {

	public static final java.lang.String F_NAME = "name";
	public static final java.lang.String F_CHANGE_TYPE = "changeType";
	public static final java.lang.String F_OBJECT_TYPE = "objectType";
	public static final java.lang.String F_DESCRIPTION = "description";
	public static final java.lang.String F_ITEMS = "items";
	public static final java.lang.String F_PARTIAL_SCENES = "partialScenes";

	private Scene scene;
	private boolean minimized;

	private final List<SceneItemDto> items = new ArrayList<>();
	private final List<SceneDto> partialScenes = new ArrayList<>();

	public SceneDto(Scene scene) {
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

	public void setScene(Scene scene) {
		this.scene = scene;
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

	public String getName() {
		if (scene.getName() != null) {
			if (scene.getName().getDisplayName() != null) {
				return scene.getName().getDisplayName();
			} else {
				return scene.getName().getSimpleName();
			}
		} else {
			return "(unnamed)";		// TODO i18n
		}
	}

	public String getDescription() {
		Name name = scene.getName();
		if (name == null) {
			return "";
		}
		if (scene.getSourceDefinition() != null && !(scene.getSourceDefinition() instanceof PrismObjectDefinition)) {
			return "";
		}
		if (name.getSimpleName() != null && !name.getSimpleName().equals(getName())) {
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
}
