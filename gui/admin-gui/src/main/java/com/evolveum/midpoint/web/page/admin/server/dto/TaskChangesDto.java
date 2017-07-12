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

package com.evolveum.midpoint.web.page.admin.server.dto;

import com.evolveum.midpoint.web.component.prism.show.SceneDto;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;

/**
 * @author mederly
 */
public class TaskChangesDto implements Serializable {

    public static final String F_PRIMARY_DELTAS = "primaryDeltas";

	@NotNull
    private SceneDto primarySceneDto;

	public TaskChangesDto(@NotNull SceneDto primarySceneDto) {
		this.primarySceneDto = primarySceneDto;
	}

	public SceneDto getPrimaryDeltas() {
        return primarySceneDto;
    }

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;

		TaskChangesDto that = (TaskChangesDto) o;

		return primarySceneDto.equals(that.primarySceneDto);

	}

	@Override
	public int hashCode() {
		return primarySceneDto.hashCode();
	}

	public void applyFoldingFrom(@NotNull TaskChangesDto source) {
		primarySceneDto.applyFoldingFrom(source.getPrimaryDeltas());
	}
}
