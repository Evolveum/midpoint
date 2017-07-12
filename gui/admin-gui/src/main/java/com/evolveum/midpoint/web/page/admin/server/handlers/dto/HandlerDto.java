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

package com.evolveum.midpoint.web.page.admin.server.handlers.dto;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.web.page.admin.server.dto.TaskDto;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;

/**
 * @author mederly
 */
public class HandlerDto implements Serializable {
	public static final String F_OBJECT_REF_NAME = "objectRefName";
	public static final String F_OBJECT_REF = "objectRef";

	protected TaskDto taskDto;

	public HandlerDto(TaskDto taskDto) {
		this.taskDto = taskDto;
	}

	public TaskDto getTaskDto() {
		return taskDto;
	}

	public String getObjectRefName() {
		return taskDto.getObjectRefName();
	}

	public ObjectReferenceType getObjectRef() {
		return taskDto.getObjectRef();
	}

	public HandlerDtoEditableState getEditableState() {
		return null;
	}

	@NotNull
	public Collection<? extends ItemDelta<?, ?>> getDeltasToExecute(HandlerDtoEditableState origState, HandlerDtoEditableState currState, PrismContext prismContext)
			throws SchemaException {
		return new ArrayList<>();
	}
}
