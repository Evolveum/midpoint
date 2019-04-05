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
import com.evolveum.midpoint.xml.ns._public.common.audit_3.AuditEventRecordListType;
import com.evolveum.midpoint.xml.ns._public.common.audit_3.AuditEventRecordType;
import com.evolveum.midpoint.xml.ns._public.common.audit_3.AuditEventStageType;
import com.evolveum.midpoint.xml.ns._public.common.audit_3.AuditEventTypeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.xml.namespace.QName;

import org.apache.commons.collections4.map.HashedMap;
import org.apache.commons.lang.StringUtils;
import org.apache.wicket.Component;
import org.apache.wicket.RestartResponseException;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.request.component.IRequestablePage;
import org.apache.wicket.util.string.StringValue;

import com.evolveum.midpoint.audit.api.AuditEventRecord;
import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.gui.impl.component.box.SmallInfoBoxPanel;
import com.evolveum.midpoint.prism.path.ItemName;
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
import com.evolveum.midpoint.web.page.admin.configuration.PageDebugList;
import com.evolveum.midpoint.web.page.admin.dto.ObjectViewDto;
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
				@Url(mountUrl = "/admin/dashboard"),
		},
		action = {
				@AuthorizationAction(actionUri = PageAdminHome.AUTH_HOME_ALL_URI,
						label = PageAdminHome.AUTH_HOME_ALL_LABEL,
						description = PageAdminHome.AUTH_HOME_ALL_DESCRIPTION),
				@AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_DASHBOARD_URL,
						label = "PageDashboard.auth.dashboard.label",
						description = "PageDashboard.auth.dashboard.description")
		})
public class PageDashboardConfigurable extends PageDashboard {
	private static final long serialVersionUID = 1L;

	private static final Trace LOGGER = TraceManager.getTrace(PageDashboardConfigurable.class);
	
	private IModel<DashboardType> dashboardModel;

	public static final String PARAM_DASHBOARD_OID = "oid";
	private static final String ID_WIDGETS = "widgets";
	private static final String ID_WIDGET = "widget";
//    private static final String ID_INFO_BOX_ERRORS = "smallInfoBoxErrors";
//    private static final String ID_INFO_BOX_MODIFICATIONS = "smallInfoBoxModifications";
//    private static final String ID_INFO_BOX_RESOURCES = "smallInfoBoxResources";
//    private static final String ID_INFO_BOX_TASKS = "smallInfoBoxTasks";

    @Override
    protected void onInitialize(){
    	if (dashboardModel == null){
    		dashboardModel = initDashboardObject();
        }
        super.onInitialize();
    }
    
    @Override
    protected IModel<String> createPageTitleModel() {
    	return new IModel<String>() {

			@Override
			public String getObject() {
				
				if(dashboardModel.getObject().getDisplay() != null && dashboardModel.getObject().getDisplay().getLabel() != null) {
		        	return dashboardModel.getObject().getDisplay().getLabel().getOrig();
				} else {
					return dashboardModel.getObject().getName().getOrig();
				}
			}
		};
    }
    
    private IModel<DashboardType> initDashboardObject() {
    	return new IModel<DashboardType>() {

			@Override
			public DashboardType getObject() {
				StringValue dashboardOid = getPageParameters().get(PARAM_DASHBOARD_OID);
                if (dashboardOid == null || StringUtils.isEmpty(dashboardOid.toString())) {
                    getSession().error(getString("PageDashboardConfigurable.message.oidNotDefined"));
                    throw new RestartResponseException(PageDashboardInfo.class);
                }
                Task task = createSimpleTask("Search dashboard");
                return WebModelServiceUtils.loadObject(DashboardType.class, dashboardOid.toString(), PageDashboardConfigurable.this, task, task.getResult()).getRealValue();
			}
    		
    	};
	}

	protected void initLayout() {
    	initInfoBoxes();

    }

    private void initInfoBoxes() {
    	
        add(new ListView<DashboardWidgetType>(ID_WIDGETS, new PropertyModel(dashboardModel, "widget")) {
            @Override
            protected void populateItem(ListItem<DashboardWidgetType> item) {
            	item.add(new SmallInfoBoxPanel(ID_WIDGET, item.getModel(),
            			PageDashboardConfigurable.this));
            }
        });

//    	Task task = createSimpleTask("PageDashboard.infobox");
//    	OperationResult result = task.getResult();
//        
//    	add(createResourceInfoBoxPanel(result, task));
//    	add(createTaskInfoBoxPanel(result, task));
//    	add(createModificationsInfoBoxPanel());
//    	add(createErrorsInfoBoxPanel());

	}
    
//	@Override
//	protected <O extends ObjectType> void customizationObjectInfoBoxType(InfoBoxType infoBoxType, Class<O> type,
//			List<QName> items, Object eqObject, String bgColor, String icon, String keyPrefix, Integer totalCount,
//			Integer activeCount, OperationResult result, Task task) {
//		
//		if(totalCount == null || activeCount == null) {
//			infoBoxType.setNumber("ERROR: Not found data.");
//			return;
//		}
//		setBoxBackgroundColor(totalCount, activeCount, false, infoBoxType);
//		infoBoxType.setNumber(activeCount + "/"+ totalCount + " " + getString("PageDashboard.infobox.tasks.number"));
//	}
//	
//	private Component createResourceInfoBoxPanel(OperationResult result, Task task) {
//		return new SmallInfoBoxPanel(ID_INFO_BOX_RESOURCES, getObjectInfoBoxTypeModel(ResourceType.class,
//    			Arrays.asList(ResourceType.F_OPERATIONAL_STATE, OperationalStateType.F_LAST_AVAILABILITY_STATUS),
//    			AvailabilityStatusType.UP, "object-resource-bg", GuiStyleConstants.CLASS_OBJECT_RESOURCE_ICON,
//    			"PageDashboard.infobox.resources", result, task), PageResources.class, this);
//	}
//	
//    private Component createTaskInfoBoxPanel(OperationResult result, Task task) {
//		return new SmallInfoBoxPanel(ID_INFO_BOX_TASKS, getObjectInfoBoxTypeModel(TaskType.class,
//    			Arrays.asList(TaskType.F_EXECUTION_STATUS), TaskExecutionStatusType.RUNNABLE, "object-task-bg",
//    			GuiStyleConstants.CLASS_OBJECT_TASK_ICON, "PageDashboard.infobox.tasks", result, task),
//				PageTasks.class, this);
//	}
//    
//    @Override
//    protected void customizationPercentageInfoBoxTypeModel(InfoBoxType infoBoxType, String bgColor, String icon,
//    		String keyPrefix, int totalItems, int actualItems, boolean zeroIsGood) {
//    	setBoxBackgroundColor(totalItems, actualItems, zeroIsGood, infoBoxType);
//    }
//    
//    private Component createModificationsInfoBoxPanel() {
//    	int totalItems = listModificationsRecords(false).size();
//    	int actualItems = listModificationsRecords(true).size();
//    	IModel<InfoBoxType> model = getPercentageInfoBoxTypeModel("", "fa fa-cog",
//    			"PageDashboard.infobox.modifications", totalItems, actualItems, false);
//    	
//		return new SmallInfoBoxPanel(ID_INFO_BOX_MODIFICATIONS, model, PageAuditLogViewer.class, this){
//			private static final long serialVersionUID = 1L;
//
//			@Override
//			protected void setParametersBeforeClickOnMoreInfo() {
//				AuditSearchDto searchDto = new AuditSearchDto();
//				Date date = new Date(System.currentTimeMillis() - (24*3600000));
//				searchDto.setFrom(XmlTypeConverter.createXMLGregorianCalendar(date));
//				searchDto.setEventType(AuditEventTypeType.MODIFY_OBJECT);
//				searchDto.setEventStage(AuditEventStageType.EXECUTION);
//				searchDto.setOutcome(OperationResultStatusType.SUCCESS);
//				getSessionStorage().getAuditLog().setSearchDto(searchDto);
//			}
//		};
//	}
//    
//    private Component createErrorsInfoBoxPanel() {
//    	int totalItems = listAllOperationsRecords().size();
//    	int actualItems = listErrorsRecords().size();
//    	
//    	IModel<InfoBoxType> model = getPercentageInfoBoxTypeModel("", "fa fa-ban",
//    			"PageDashboard.infobox.errors", totalItems, actualItems, true);
//		
//		return new SmallInfoBoxPanel(ID_INFO_BOX_ERRORS, model, PageAuditLogViewer.class, this) {
//			private static final long serialVersionUID = 1L;
//
//			@Override
//			protected void setParametersBeforeClickOnMoreInfo() {
//				AuditSearchDto searchDto = new AuditSearchDto();
//				Date date = new Date(System.currentTimeMillis() - (24*3600000));
//				searchDto.setFrom(XmlTypeConverter.createXMLGregorianCalendar(date));
//				searchDto.setEventStage(AuditEventStageType.EXECUTION);
//				searchDto.setOutcome(OperationResultStatusType.FATAL_ERROR);
//				getSessionStorage().getAuditLog().setSearchDto(searchDto);
//			}
//		};
//	}
//    
//    private List<AuditEventRecordType> listModificationsRecords(boolean isSuccess){
//    	Map<String, Object> parameters = new HashedMap<String, Object>();
//		List<String> conditions = new ArrayList<>();
//		conditions.add("aer.eventType = :auditEventType");
//		parameters.put("auditEventType", AuditEventTypeType.MODIFY_OBJECT);
//		
//		if(isSuccess){
//			conditions.add("aer.outcome = :outcome");
//			parameters.put("outcome", OperationResultStatusType.SUCCESS);
//		}
//		
//		return listAuditRecords(parameters, conditions);
//    }
//    
//    private List<AuditEventRecordType> listErrorsRecords(){
//    	Map<String, Object> parameters = new HashedMap<String, Object>();
//		List<String> conditions = new ArrayList<>();
//		conditions.add("aer.outcome = :outcome");
//		parameters.put("outcome", OperationResultStatusType.FATAL_ERROR);
//		
//		return listAuditRecords(parameters, conditions);
//    }
//    
//    private List<AuditEventRecordType> listAllOperationsRecords(){
//    	Map<String, Object> parameters = new HashedMap<String, Object>();
//		List<String> conditions = new ArrayList<>();
//		return listAuditRecords(parameters, conditions);
//    }
//    
//    private void setBoxBackgroundColor(int totalCount, int activeCount, boolean zeroIsGood, InfoBoxType infoBoxType) {
//		if((zeroIsGood && activeCount == 0) || (totalCount == activeCount)) {
//			infoBoxType.setBoxBackgroundColor("object-access-bg");
//			return;
//		} 
//		infoBoxType.setBoxBackgroundColor("object-failed-bg");
//	}
}
