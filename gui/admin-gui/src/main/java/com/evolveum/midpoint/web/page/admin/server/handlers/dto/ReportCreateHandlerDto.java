/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0 
 * and European Union Public License. See LICENSE file for details.
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
		PrismContainer<ReportParameterType> container = taskObject.findContainer(
				ItemPath.create(TaskType.F_EXTENSION, ReportConstants.REPORT_PARAMS_PROPERTY_NAME));
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
