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
import com.evolveum.midpoint.xml.ns._public.common.audit_3.AuditEventRecordType;
import com.evolveum.midpoint.xml.ns._public.common.audit_3.AuditEventStageType;
import com.evolveum.midpoint.xml.ns._public.common.audit_3.AuditEventTypeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.wicket.Component;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.component.IRequestablePage;

import com.evolveum.midpoint.audit.api.AuditEventRecord;
import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.application.AuthorizationAction;
import com.evolveum.midpoint.web.application.PageDescriptor;
import com.evolveum.midpoint.web.component.box.BasicInfoBoxPanel;
import com.evolveum.midpoint.web.component.box.InfoBoxPanel;
import com.evolveum.midpoint.web.component.box.InfoBoxType;
import com.evolveum.midpoint.web.component.box.SmallInfoBoxPanel;
import com.evolveum.midpoint.web.component.breadcrumbs.Breadcrumb;
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
 * @author lazyman
 */

public abstract class PageDashboard extends PageAdminHome {
	private static final long serialVersionUID = 1L;

    private final Model<PrismObject<UserType>> principalModel = new Model<>();

    public PageDashboard() {
        principalModel.setObject(loadUserSelf());
        setTimeZone(PageDashboard.this);
    }
    
    @Override
    protected void onInitialize() {
    	super.onInitialize();
    	initLayout();
    }

    @Override
    protected void createBreadcrumb() {
        super.createBreadcrumb();

        Breadcrumb bc = getLastBreadcrumb();
        bc.setIcon(new Model(GuiStyleConstants.CLASS_DASHBOARD_ICON));
    }
    
    protected abstract void initLayout();
    
    protected Model<InfoBoxType> getResourceInfoBoxTypeModel(OperationResult result, Task task) {
    	InfoBoxType infoBoxType = new InfoBoxType("object-resource-bg", GuiStyleConstants.CLASS_OBJECT_RESOURCE_ICON,
    			getString("PageDashboard.infobox.resources.label"));
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

			infoBoxType.setNumber(activeCount + " " + getString("PageDashboard.infobox.resources.number"));

			int progress = 0;
			if (totalCount != 0) {
				progress = activeCount * 100 / totalCount;
			}
			infoBoxType.setProgress(progress);

			infoBoxType.setDescription(totalCount + " " + getString("PageDashboard.infobox.resources.total"));

		} catch (Exception e) {
			infoBoxType.setNumber("ERROR: "+e.getMessage());
		}
		
		customizationResourceInfoBoxType(infoBoxType, result, task);

		return new Model<>(infoBoxType);
	}

	protected void customizationResourceInfoBoxType(InfoBoxType infoBoxType, OperationResult result, Task task) {
		
	}
	
	protected Model<InfoBoxType> getTaskInfoBoxTypeModel(OperationResult result, Task task) {
    	InfoBoxType infoBoxType = new InfoBoxType("object-task-bg", GuiStyleConstants.CLASS_OBJECT_TASK_ICON,
    			getString("PageDashboard.infobox.tasks.label"));
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

			infoBoxType.setNumber(activeCount + " " + getString("PageDashboard.infobox.tasks.number"));

			int progress = 0;
			if (totalCount != 0) {
				progress = activeCount * 100 / totalCount;
			}
			infoBoxType.setProgress(progress);

			infoBoxType.setDescription(totalCount + " " + getString("PageDashboard.infobox.tasks.total"));

		} catch (Exception e) {
			infoBoxType.setNumber("ERROR: "+e.getMessage());
		}
		
		customizationTaskInfoBoxType(infoBoxType, result, task);

		return new Model<>(infoBoxType);
	}

	protected void customizationTaskInfoBoxType(InfoBoxType infoBoxType, OperationResult result, Task task) {
		
	}
	
	protected <F extends FocusType> Model<InfoBoxType> getFocusInfoBoxType(Class<F> type, String bgColor,
			String icon, String keyPrefix, OperationResult result, Task task) {
    	InfoBoxType infoBoxType = new InfoBoxType(bgColor, icon, getString(keyPrefix + ".label"));
    	Integer allCount;
		try {
			allCount = getModelService().countObjects(type, null, null, task, result);
			if (allCount == null) {
				allCount = 0;
			}

			ObjectQuery queryDisabled = getPrismContext().queryFor(type)
					.item(FocusType.F_ACTIVATION, ActivationType.F_EFFECTIVE_STATUS).eq(ActivationStatusType.DISABLED)
					.build();
			Integer disabledCount = getModelService().countObjects(type, queryDisabled, null, task, result);
			if (disabledCount == null) {
				disabledCount = 0;
			}

			ObjectQuery queryArchived = getPrismContext().queryFor(type)
					.item(FocusType.F_ACTIVATION, ActivationType.F_EFFECTIVE_STATUS).eq(ActivationStatusType.ARCHIVED)
					.build();
			Integer archivedCount = getModelService().countObjects(type, queryArchived, null, task, result);
			if (archivedCount == null) {
				archivedCount = 0;
			}

			int activeCount = allCount - disabledCount - archivedCount;
			int totalCount = allCount - archivedCount;

			infoBoxType.setNumber(activeCount + " " + getString(keyPrefix + ".number"));

			int progress = 0;
			if (totalCount != 0) {
				progress = activeCount * 100 / totalCount;
			}
			infoBoxType.setProgress(progress);

			StringBuilder descSb = new StringBuilder();
			descSb.append(totalCount).append(" ").append(getString(keyPrefix + ".total"));
			if (archivedCount != 0) {
				descSb.append(" ( + ").append(archivedCount).append(" ").append(getString(keyPrefix + ".archived")).append(")");
			}
			infoBoxType.setDescription(descSb.toString());

		} catch (Exception e) {
			infoBoxType.setNumber("ERROR: "+e.getMessage());
		}

		customizationFocusInfoBoxType(infoBoxType, result, task);

		return new Model<>(infoBoxType);
    }

	protected void customizationFocusInfoBoxType(InfoBoxType infoBoxType, OperationResult result, Task task) {
		
	}
	
	protected List<AuditEventRecordType> listAuditRecords(Map<String, Object> parameters, List<String> conditions) {
		
		Date date = new Date(System.currentTimeMillis() - (24*3600000));
		conditions.add("aer.timestamp >= :from");
		parameters.put("from", XmlTypeConverter.createXMLGregorianCalendar(date));
		conditions.add("aer.eventStage = :auditStageType");
		parameters.put("auditStageType", AuditEventStageType.EXECUTION);
		
		String query = "from RAuditEventRecord as aer";
		if (!conditions.isEmpty()) {
			query += " where ";
		}
		
		query += conditions.stream().collect(Collectors.joining(" and "));
		query += " order by aer.timestamp desc";


        List<AuditEventRecord> auditRecords;
		auditRecords = getAuditService().listRecords(query, parameters);
		
		if (auditRecords == null) {
			auditRecords = new ArrayList<>();
		}
		List<AuditEventRecordType> auditRecordList = new ArrayList<>();
		for (AuditEventRecord record : auditRecords){
			auditRecordList.add(record.createAuditEventRecordType());
		}
		return auditRecordList;
	}
    
    protected String formatPercentage(int totalItems, int actualItems) {
    	float percentage = (totalItems==0 ? 0 : actualItems*100.0f/totalItems);
    	String format = "%.0f";
    	
    	if(percentage < 100.0f && percentage % 10 != 0 && ((percentage % 10) % 1) != 0) {
    		format = "%.1f";
    	}
    	return String.format(format, percentage);
    }
    
    protected Model<InfoBoxType> getPercentageInfoBoxTypeModel(String bgColor, String icon, String keyPrefix,
    		int totalItems, int actualItems) {
    	InfoBoxType infoBoxType = new InfoBoxType(bgColor, icon,
    			getString(keyPrefix +".label"));
    	
		infoBoxType.setNumber(formatPercentage(totalItems, actualItems) + " %" + " " + getString(keyPrefix + ".number"));
		
		int progress = 0;
		if (totalItems != 0) {
			progress = actualItems * 100 / totalItems;
		}
		infoBoxType.setProgress(progress);
		
		StringBuilder descSb = new StringBuilder();
		descSb.append(totalItems).append(" ").append(getString(keyPrefix + ".total"));
		infoBoxType.setDescription(descSb.toString());
		
		return new Model<>(infoBoxType);
	}

}
