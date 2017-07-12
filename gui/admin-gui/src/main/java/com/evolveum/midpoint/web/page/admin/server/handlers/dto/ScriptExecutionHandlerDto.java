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
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.web.page.admin.server.dto.TaskDto;
import com.evolveum.midpoint.web.security.MidPointApplication;
import com.evolveum.midpoint.web.util.WebXmlUtil;
import com.evolveum.midpoint.xml.ns._public.model.scripting_3.ExecuteScriptType;
import org.apache.wicket.Application;

/**
 * @author mederly
 */
public class ScriptExecutionHandlerDto extends HandlerDto {

	public static final String F_SCRIPT = "script";

	public ScriptExecutionHandlerDto(TaskDto taskDto) {
		super(taskDto);
	}

	public String getScript() {
		ExecuteScriptType script = taskDto.getExtensionPropertyRealValue(SchemaConstants.SE_EXECUTE_SCRIPT, ExecuteScriptType.class);
		if (script == null) {
			return null;
		}
		PrismContext prismContext = ((MidPointApplication) Application.get()).getPrismContext();
		try {
			return WebXmlUtil.stripNamespaceDeclarations(
					prismContext.xmlSerializer().serializeAnyData(script, SchemaConstants.SE_EXECUTE_SCRIPT));
		} catch (SchemaException e) {
			throw new SystemException("Couldn't serialize script: " + e.getMessage(), e);
		}
	}

}
