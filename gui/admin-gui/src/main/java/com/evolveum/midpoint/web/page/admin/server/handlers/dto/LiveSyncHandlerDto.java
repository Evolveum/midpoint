/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0 
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.page.admin.server.handlers.dto;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.web.page.admin.server.dto.TaskDto;

/**
 * @author mederly
 */
public class LiveSyncHandlerDto extends ResourceRelatedHandlerDto {

	public static final String F_TOKEN = "token";

	private String token;

	public LiveSyncHandlerDto(TaskDto taskDto, PageBase pageBase, Task opTask, OperationResult thisOpResult) {
		super(taskDto, pageBase, opTask, thisOpResult);
		PrismProperty<Object> tokenProperty = taskDto.getExtensionProperty(SchemaConstants.SYNC_TOKEN);
		if (tokenProperty != null && tokenProperty.getRealValue() != null) {
			token = String.valueOf(tokenProperty.getRealValue());
		}
	}

	public String getToken() {
		return token;
	}

	public boolean hasToken() {
		return token != null;
	}
}
