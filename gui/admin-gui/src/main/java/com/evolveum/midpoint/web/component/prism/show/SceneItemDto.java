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
import com.evolveum.midpoint.model.api.visualizer.SceneItem;
import com.evolveum.midpoint.model.api.visualizer.SceneItemValue;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * @author mederly
 */
public class SceneItemDto implements Serializable {

	public static final String F_NAME = "name";
	public static final String F_NEW_VALUE = "newValue";
	public static final String F_LINES = "lines";

	private final SceneItem sceneItem;

	public SceneItemDto(SceneItem sceneItem) {
		this.sceneItem = sceneItem;
	}

	public String getName() {
		Name n = sceneItem.getName();
		if (n == null) {
			return "";
		} else if (n.getDisplayName() != null) {
			return n.getDisplayName();
		} else {
			return n.getSimpleName();
		}
	}

	public String getNewValue() {
		return String.valueOf(sceneItem.getNewValues());
	}

	public List<SceneItemLineDto> getLines() {
		List<SceneItemLineDto> rv = new ArrayList<>();
		int index = 0;
		for (SceneItemValue itemValue : sceneItem.getNewValues()) {		// TODO
			rv.add(new SceneItemLineDto(this, itemValue, index++));
		}
		return rv;
	}
}
