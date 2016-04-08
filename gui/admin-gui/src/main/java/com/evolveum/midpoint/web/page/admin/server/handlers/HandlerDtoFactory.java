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

package com.evolveum.midpoint.web.page.admin.server.handlers;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.web.page.admin.server.dto.TaskDto;
import com.evolveum.midpoint.web.page.admin.server.handlers.dto.*;
import com.evolveum.prism.xml.ns._public.query_3.QueryType;

import java.util.Arrays;

import static com.evolveum.midpoint.schema.constants.SchemaConstants.*;
import static com.evolveum.midpoint.web.page.admin.server.handlers.dto.GenericHandlerDto.item;

/**
 * @author mederly
 */
public class HandlerDtoFactory {
	public static HandlerDtoFactory instance() {
		return new HandlerDtoFactory();		// TODO
	}

	public HandlerDto createDtoForTask(TaskDto taskDto, PageBase pageBase, Task opTask, OperationResult thisOpResult) {
		if (taskDto.isLiveSync()) {
			return new LiveSyncHandlerDto(taskDto, pageBase, opTask, thisOpResult);
		} else if (taskDto.isImportAccounts()) {
			return new ResourceRelatedHandlerDto(taskDto, pageBase, opTask, thisOpResult);
		} else if (taskDto.isReconciliation()) {
			return new ResourceRelatedHandlerDto(taskDto, pageBase, opTask, thisOpResult);
		} else if (taskDto.isTriggerScanner() || taskDto.isFocusValidityScanner()) {
			return new ScannerHandlerDto(taskDto);
		} else if (taskDto.isBulkAction()) {
			return new ScriptExecutionHandlerDto(taskDto);
		} else if (taskDto.isDelete()) {
			return new DeleteHandlerDto(taskDto);
		} else if (taskDto.isRecomputation()) {
			return new RecomputeHandlerDto(taskDto);
		} else if (taskDto.isExecuteChanges()) {
			return new ExecuteChangesHandlerDto(taskDto);
		} else if (taskDto.isShadowIntegrityCheck()) {
			return new GenericHandlerDto(taskDto, Arrays.asList(
					item(MODEL_EXTENSION_OBJECT_QUERY, QueryType.class),
					item(MODEL_EXTENSION_DRY_RUN, Boolean.class),
					item(MODEL_EXTENSION_DIAGNOSE, String.class),
					item(MODEL_EXTENSION_FIX, String.class),
					item(MODEL_EXTENSION_DUPLICATE_SHADOWS_RESOLVER, String.class),
					item(MODEL_EXTENSION_CHECK_DUPLICATES_ON_PRIMARY_IDENTIFIERS_ONLY, Boolean.class)), pageBase);
		} else if (taskDto.isReportCreate()) {
			return new ReportCreateHandlerDto(taskDto);
		}
		else {
			return new HandlerDto(taskDto);
		}
	}
}
