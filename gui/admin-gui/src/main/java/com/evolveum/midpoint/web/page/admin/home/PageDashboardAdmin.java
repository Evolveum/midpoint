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
package com.evolveum.midpoint.web.page.admin.home;

import com.evolveum.midpoint.web.application.Url;
import com.evolveum.midpoint.xml.ns._public.common.audit_4.AuditEventRecordListType;
import com.evolveum.midpoint.xml.ns._public.common.audit_4.AuditEventRecordType;
import com.evolveum.midpoint.xml.ns._public.common.audit_4.AuditEventStageType;
import com.evolveum.midpoint.xml.ns._public.common.audit_4.AuditEventTypeType;
import com.evolveum.midpoint.xml.ns._public.common.common_4.*;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.collections4.map.HashedMap;
import org.apache.wicket.Component;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.component.IRequestablePage;

import com.evolveum.midpoint.audit.api.AuditEventRecord;
import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.application.AuthorizationAction;
import com.evolveum.midpoint.web.application.PageDescriptor;
import com.evolveum.midpoint.web.component.box.BasicInfoBoxPanel;
import com.evolveum.midpoint.web.component.box.InfoBoxPanel;
import com.evolveum.midpoint.web.component.box.InfoBoxType;
import com.evolveum.midpoint.web.component.box.SmallInfoBoxPanel;
import com.evolveum.midpoint.web.page.admin.home.component.DashboardPanel;
import com.evolveum.midpoint.web.page.admin.home.component.PersonalInfoPanel;
import com.evolveum.midpoint.web.page.admin.home.component.SystemInfoPanel;
import com.evolveum.midpoint.web.page.admin.reports.PageAuditLogViewer;
import com.evolveum.midpoint.web.page.admin.reports.dto.AuditSearchDto;
import com.evolveum.midpoint.web.page.admin.resources.PageResources;
import com.evolveum.midpoint.web.page.admin.roles.PageRoles;
import com.evolveum.midpoint.web.page.admin.server.PageTasks;
import com.evolveum.midpoint.web.page.admin.services.PageServices;
import com.evolveum.midpoint.web.page.admin.users.PageOrgTree;
import com.evolveum.midpoint.web.page.admin.users.PageUsers;

/**
 * @author skublik
 */
@PageDescriptor(
		urls = {
				@Url(mountUrl = "/admin", matchUrlForSecurity = "/admin"),
				@Url(mountUrl = "/admin/dashboard/admin"),
		},
		action = {
				@AuthorizationAction(actionUri = PageAdminHome.AUTH_HOME_ALL_URI,
						label = PageAdminHome.AUTH_HOME_ALL_LABEL,
						description = PageAdminHome.AUTH_HOME_ALL_DESCRIPTION),
				@AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_DASHBOARD_URL,
						label = "PageDashboard.auth.dashboard.label",
						description = "PageDashboard.auth.dashboard.description")
		})
public class PageDashboardAdmin extends PageDashboard {
	private static final long serialVersionUID = 1L;

	private static final Trace LOGGER = TraceManager.getTrace(PageDashboardAdmin.class);

    private static final String ID_INFO_BOX_ERRORS = "smallInfoBoxErrors";
    private static final String ID_INFO_BOX_MODIFICATIONS = "smallInfoBoxModifications";
    private static final String ID_INFO_BOX_RESOURCES = "smallInfoBoxResources";
    private static final String ID_INFO_BOX_TASKS = "smallInfoBoxTasks";

    protected void initLayout() {
    	initInfoBoxes();

    }

    private void initInfoBoxes() {
    	Task task = createSimpleTask("PageDashboard.infobox");
    	OperationResult result = task.getResult();

    	add(createResourceInfoBoxPanel(result, task));
    	add(createTaskInfoBoxPanel(result, task));
    	add(createModificationsInfoBoxPanel());
    	add(createErrorsInfoBoxPanel());

	}
    
    @Override
    protected void customizationResourceInfoBoxType(InfoBoxType infoBoxType, OperationResult result, Task task) {
    	Integer totalCount;
		try {
			totalCount = getModelService().countObjects(ResourceType.class, null, null, task, result);
			if (totalCount == null) {
				totalCount = 0;
			}

			ObjectQuery query = getPrismContext().queryFor(ResourceType.class)
					.item(ResourceType.F_OPERATIONAL_STATE, OperationalStateType.F_LAST_AVAILABILITY_STATUS).eq(AvailabilityStatusType.UP)
					.build();
			Integer activeCount = getModelService().countObjects(ResourceType.class, query, null, task, result);
			if (activeCount == null) {
				activeCount = 0;
			}

			infoBoxType.setNumber(activeCount + "/"+ totalCount + " " + getString("PageDashboard.infobox.resources.number"));

		} catch (Exception e) {
			infoBoxType.setNumber("ERROR: "+e.getMessage());
		}
    }

	private Component createResourceInfoBoxPanel(OperationResult result, Task task) {
		return new SmallInfoBoxPanel(ID_INFO_BOX_RESOURCES, getResourceInfoBoxTypeModel(result, task), PageResources.class, this);
	}
	
	@Override
	protected void customizationTaskInfoBoxType(InfoBoxType infoBoxType, OperationResult result, Task task) {
		infoBoxType.setBoxBackgroundColor("object-task-bg-gray");
		Integer totalCount;
		try {
			totalCount = getModelService().countObjects(TaskType.class, null, null, task, result);
			if (totalCount == null) {
				totalCount = 0;
			}
			ObjectQuery query = getPrismContext().queryFor(TaskType.class)
					.item(TaskType.F_EXECUTION_STATUS).eq(TaskExecutionStatusType.RUNNABLE)
					.build();
			Integer activeCount = getModelService().countObjects(TaskType.class, query, null, task, result);
			if (activeCount == null) {
				activeCount = 0;
			}

			infoBoxType.setNumber(activeCount + "/"+ totalCount + " " + getString("PageDashboard.infobox.tasks.number"));

		} catch (Exception e) {
			infoBoxType.setNumber("ERROR: "+e.getMessage());
		}
	}

    private Component createTaskInfoBoxPanel(OperationResult result, Task task) {
		return new SmallInfoBoxPanel(ID_INFO_BOX_TASKS, getTaskInfoBoxTypeModel(result, task), PageTasks.class, this);
	}
    
    private Component createModificationsInfoBoxPanel() {
    	int totalItems = listModificationsRecords(false).size();
    	int actualItems = listModificationsRecords(true).size();
    	IModel<InfoBoxType> model = getPercentageInfoBoxTypeModel("object-role-bg", "fa fa-cog",
    			"PageDashboard.infobox.modifications", totalItems, actualItems);
    	
		return new SmallInfoBoxPanel(ID_INFO_BOX_MODIFICATIONS, model, PageAuditLogViewer.class, this){
			private static final long serialVersionUID = 1L;

			@Override
			protected void setParametersBeforeClickOnMoreInfo() {
				AuditSearchDto searchDto = new AuditSearchDto();
				Date date = new Date(System.currentTimeMillis() - (24*3600000));
				searchDto.setFrom(XmlTypeConverter.createXMLGregorianCalendar(date));
				searchDto.setEventType(AuditEventTypeType.MODIFY_OBJECT);
				searchDto.setEventStage(AuditEventStageType.EXECUTION);
				getSessionStorage().getAuditLog().setSearchDto(searchDto);
			}
		};
	}
    
    private Component createErrorsInfoBoxPanel() {
    	int totalItems = listAllOperationsRecords().size();
    	int actualItems = listErrorsRecords().size();
    	
    	IModel<InfoBoxType> model = getPercentageInfoBoxTypeModel("object-user-bg", "fa fa-ban",
    			"PageDashboard.infobox.errors", totalItems, actualItems);
		
		return new SmallInfoBoxPanel(ID_INFO_BOX_ERRORS, model, PageAuditLogViewer.class, this) {
			private static final long serialVersionUID = 1L;

			@Override
			protected void setParametersBeforeClickOnMoreInfo() {
				AuditSearchDto searchDto = new AuditSearchDto();
				Date date = new Date(System.currentTimeMillis() - (24*3600000));
				searchDto.setFrom(XmlTypeConverter.createXMLGregorianCalendar(date));
				searchDto.setEventStage(AuditEventStageType.EXECUTION);
				searchDto.setOutcome(OperationResultStatusType.FATAL_ERROR);
				getSessionStorage().getAuditLog().setSearchDto(searchDto);
			}
		};
	}
    
    private List<AuditEventRecordType> listModificationsRecords(boolean isSuccess){
    	Map<String, Object> parameters = new HashedMap<String, Object>();
		List<String> conditions = new ArrayList<>();
		conditions.add("aer.eventType = :auditEventType");
		parameters.put("auditEventType", AuditEventTypeType.MODIFY_OBJECT);
		
		if(isSuccess){
			conditions.add("aer.outcome = :outcome");
			parameters.put("outcome", OperationResultStatusType.SUCCESS);
		}
		
		return listAuditRecords(parameters, conditions);
    }
    
    private List<AuditEventRecordType> listErrorsRecords(){
    	Map<String, Object> parameters = new HashedMap<String, Object>();
		List<String> conditions = new ArrayList<>();
		conditions.add("aer.outcome = :outcome");
		parameters.put("outcome", OperationResultStatusType.FATAL_ERROR);
		
		return listAuditRecords(parameters, conditions);
    }
    
    private List<AuditEventRecordType> listAllOperationsRecords(){
    	Map<String, Object> parameters = new HashedMap<String, Object>();
		List<String> conditions = new ArrayList<>();
		return listAuditRecords(parameters, conditions);
    }
}
