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

import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.report.api.ReportConstants;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.web.page.admin.server.dto.TaskDto;
import com.evolveum.midpoint.web.security.MidPointApplication;
import com.evolveum.midpoint.web.util.WebXmlUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ReportParameterType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;
import org.apache.wicket.Application;

/**
 * @author mederly
 */
public class ReportCreateHandlerDto extends HandlerDto {

	public static final String F_REPORT_PARAMS = "reportParams";
	public static final String F_REPORT_OUTPUT_OID = "reportOutputOid";

	public ReportCreateHandlerDto(TaskDto taskDto) {
		super(taskDto);
	}

	public String getReportOutputOid() {
		return taskDto.getExtensionPropertyRealValue(ReportConstants.REPORT_OUTPUT_OID_PROPERTY_NAME, String.class);
	}

	public String getReportParams() {
		PrismObject<TaskType> taskObject = taskDto.getTaskType().asPrismObject();
		PrismContainer<ReportParameterType> container = taskObject.findContainer(new ItemPath(TaskType.F_EXTENSION, ReportConstants.REPORT_PARAMS_PROPERTY_NAME));
		if (container == null || container.isEmpty()) {
			return null;
		}
		PrismContainerValue<ReportParameterType> pcv = container.getValue();
		PrismContext prismContext = ((MidPointApplication) Application.get()).getPrismContext();
		try {
			return WebXmlUtil.stripNamespaceDeclarations(
					prismContext.xmlSerializer().serialize(pcv, ReportConstants.REPORT_PARAMS_PROPERTY_NAME));
		} catch (SchemaException e) {
			throw new SystemException("Couldn't serialize report parameters: " + e.getMessage(), e);
		}
	}

}
