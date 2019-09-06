/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0 
 * and European Union Public License. See LICENSE file for details.
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
