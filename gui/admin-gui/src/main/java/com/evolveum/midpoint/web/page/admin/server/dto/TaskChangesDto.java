/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0 
 * and European Union Public License. See LICENSE file for details.
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
