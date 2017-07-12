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

import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.web.page.admin.server.dto.TaskDto;

/**
 * TODO use composition instead of inheritance
 *
 * @author mederly
 */
public class DeleteHandlerDto extends QueryBasedHandlerDto {

	public static final String F_RAW = "raw";

	public DeleteHandlerDto(TaskDto taskDto) {
		super(taskDto);
	}

	public boolean isRaw() {
		return Boolean.TRUE.equals(taskDto.getExtensionPropertyRealValue(SchemaConstants.MODEL_EXTENSION_OPTION_RAW, Boolean.class));
	}
}
