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

import com.evolveum.midpoint.model.api.visualizer.*;
import org.apache.commons.lang3.Validate;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * @author mederly
 */
public class SceneItemDto implements Serializable {

	public static final String F_NAME = "name";
	public static final String F_LINES = "lines";

	@NotNull private final SceneItem sceneItem;
	@NotNull private final SceneDto sceneDto;
	@NotNull private final List<SceneItemLineDto> lines;

	public SceneItemDto(@NotNull SceneDto sceneDto, @NotNull SceneItem sceneItem) {
		Validate.notNull(sceneDto);
		Validate.notNull(sceneItem);
		this.sceneDto = sceneDto;
		this.sceneItem = sceneItem;
		this.lines = computeLines();
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

	public List<SceneItemLineDto> computeLines() {
		List<SceneItemLineDto> rv = new ArrayList<>();
		int index = 0;
		if (!isDelta()) {
			for (SceneItemValue itemValue : sceneItem.getNewValues()) {
				rv.add(new SceneItemLineDto(this, null, itemValue, index++, false));
			}
		} else {
			SceneDeltaItem deltaItem = (SceneDeltaItem) sceneItem;
			for (SceneItemValue itemValue : deltaItem.getUnchangedValues()) {
				rv.add(new SceneItemLineDto(this, null, itemValue, index++, false));
			}
			Iterator<? extends SceneItemValue> deletedValuesIter = deltaItem.getDeletedValues().iterator();
			Iterator<? extends SceneItemValue> addedValuesIter = deltaItem.getAddedValues().iterator();
			while (deletedValuesIter.hasNext() || addedValuesIter.hasNext()) {
				SceneItemValue deletedValue = deletedValuesIter.hasNext() ? deletedValuesIter.next() : null;
				SceneItemValue addedValue = addedValuesIter.hasNext() ? addedValuesIter.next() : null;
				rv.add(new SceneItemLineDto(this, deletedValue, addedValue, index++, true));
			}
		}
		return rv;
	}

	@NotNull
	public List<SceneItemLineDto> getLines() {
		return lines;
	}

	public boolean isDelta() {
		return sceneItem instanceof SceneDeltaItem;
	}

	public boolean isNullEstimatedOldValues(){
		return isDelta() && ((SceneDeltaItem)sceneItem).getSourceDelta() != null && ((SceneDeltaItem)sceneItem).getSourceDelta().getEstimatedOldValues() == null;
	}

	public boolean isAdd(){
		return isDelta() && ((SceneDeltaItem)sceneItem).getSourceDelta() != null && ((SceneDeltaItem)sceneItem).getSourceDelta().isAdd();
	}

	public boolean isDelete(){
		return isDelta() && ((SceneDeltaItem)sceneItem).getSourceDelta() != null && ((SceneDeltaItem)sceneItem).getSourceDelta().isDelete();
	}

	public boolean isReplace(){
		return isDelta() && ((SceneDeltaItem)sceneItem).getSourceDelta() != null && ((SceneDeltaItem)sceneItem).getSourceDelta().isReplace();
	}

	public boolean isDeltaScene() {
		return sceneDto.containsDeltaItems();
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		SceneItemDto that = (SceneItemDto) o;

		if (!sceneItem.equals(that.sceneItem)) return false;
		return lines.equals(that.lines);

	}

	@Override
	public int hashCode() {
		int result = sceneItem.hashCode();
		result = 31 * result + lines.hashCode();
		return result;
	}
}
