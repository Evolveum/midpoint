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
