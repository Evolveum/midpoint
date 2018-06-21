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

import com.evolveum.midpoint.model.api.visualizer.SceneItemValue;

import java.io.Serializable;

/**
 * @author mederly
 */
public class SceneItemLineDto implements Serializable {

	public static final String F_NAME = "name";
	public static final String F_OLD_VALUE = "oldValue";
	public static final String F_NEW_VALUE = "newValue";
	public static final String F_NUMBER_OF_LINES = "numberOfLines";

	private final SceneItemDto sceneItemDto;
	private final SceneItemValue sceneItemOldValue;
	private final SceneItemValue sceneItemNewValue;
	private final int index;
	private final boolean isDelta;

	public SceneItemLineDto(SceneItemDto sceneItemDto, SceneItemValue sceneItemOldValue, SceneItemValue sceneItemNewValue, int index, boolean isDelta) {
		this.sceneItemDto = sceneItemDto;
		this.sceneItemOldValue = sceneItemOldValue;
		this.sceneItemNewValue = sceneItemNewValue;
		this.index = index;
		this.isDelta = isDelta;
	}

	public String getName() {
		return sceneItemDto.getName();
	}

	public SceneItemValue getOldValue() {
		return sceneItemOldValue;
	}

	public SceneItemValue getNewValue() {
		return sceneItemNewValue;
	}

	public Integer getNumberOfLines() {
		return sceneItemDto.getLines().size();
	}

	public boolean isFirst() {
		return index == 0;
	}

	public boolean isDelta() {
		return isDelta;
	}

	public boolean isDeltaScene() {
		return sceneItemDto.isDeltaScene();
	}

	public boolean isNullEstimatedOldValues(){
		return sceneItemDto.isNullEstimatedOldValues();
	}

	public boolean isAdd(){
		return sceneItemDto.isAdd();
	}

	public boolean isDelete(){
		return sceneItemDto.isDelete();
	}

	public boolean isReplace(){
		return sceneItemDto.isReplace();
	}


	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		SceneItemLineDto that = (SceneItemLineDto) o;

		if (index != that.index) return false;
		if (isDelta != that.isDelta) return false;
		if (sceneItemOldValue != null ? !sceneItemOldValue.equals(that.sceneItemOldValue) : that.sceneItemOldValue != null)
			return false;
		return !(sceneItemNewValue != null ? !sceneItemNewValue.equals(that.sceneItemNewValue) : that.sceneItemNewValue != null);

	}

	@Override
	public int hashCode() {
		int result = 1;
		result = 31 * result + (sceneItemOldValue != null ? sceneItemOldValue.hashCode() : 0);
		result = 31 * result + (sceneItemNewValue != null ? sceneItemNewValue.hashCode() : 0);
		result = 31 * result + index;
		result = 31 * result + (isDelta ? 1 : 0);
		return result;
	}
}
