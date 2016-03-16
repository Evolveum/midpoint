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

import com.evolveum.midpoint.model.api.visualizer.Scene;
import com.evolveum.midpoint.model.api.visualizer.SceneItem;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * @author mederly
 */
public class DataSceneDto implements Serializable {

	public static final java.lang.String F_ITEMS = "items";
	public static final java.lang.String F_PARTIAL_SCENES = "partialScenes";

	private Scene scene;
	private boolean minimized;

	private final List<ItemDto> items = new ArrayList<>();
	private final List<DataSceneDto> partialScenes = new ArrayList<>();

	public DataSceneDto(Scene scene) {
		this.scene = scene;
		for (SceneItem item : scene.getItems()) {
			if (item != null) {
				items.add(new ItemDto(item));
			}
		}
		for (Scene sub : scene.getPartialScenes()) {
			if (sub != null) {
				partialScenes.add(new DataSceneDto(sub));
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

	public List<DataSceneDto> getPartialScenes() {
		return partialScenes;
	}

	public List<ItemDto> getItems() {
		return items;
	}
}
