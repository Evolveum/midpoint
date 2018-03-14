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
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.apache.wicket.Component;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.component.IRequestablePage;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.builder.QueryBuilder;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.application.AuthorizationAction;
import com.evolveum.midpoint.web.application.PageDescriptor;
import com.evolveum.midpoint.web.component.box.InfoBoxPanel;
import com.evolveum.midpoint.web.component.box.InfoBoxType;
import com.evolveum.midpoint.web.component.breadcrumbs.Breadcrumb;
import com.evolveum.midpoint.web.page.admin.home.component.DashboardPanel;
import com.evolveum.midpoint.web.page.admin.home.component.PersonalInfoPanel;
import com.evolveum.midpoint.web.page.admin.home.component.SystemInfoPanel;
import com.evolveum.midpoint.web.page.admin.resources.PageResources;
import com.evolveum.midpoint.web.page.admin.roles.PageRoles;
import com.evolveum.midpoint.web.page.admin.server.PageTasks;
import com.evolveum.midpoint.web.page.admin.services.PageServices;
import com.evolveum.midpoint.web.page.admin.users.PageOrgTree;
import com.evolveum.midpoint.web.page.admin.users.PageUsers;

/**
 * @author lazyman
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
public class PageDashboard extends PageAdminHome {
	private static final long serialVersionUID = 1L;

	private static final Trace LOGGER = TraceManager.getTrace(PageDashboard.class);

    private static final String DOT_CLASS = PageDashboard.class.getName() + ".";

    private static final String ID_INFO_BOX_USERS = "infoBoxUsers";
    private static final String ID_INFO_BOX_ORGS = "infoBoxOrgs";
    private static final String ID_INFO_BOX_ROLES = "infoBoxRoles";
    private static final String ID_INFO_BOX_SERVICES = "infoBoxServices";
    private static final String ID_INFO_BOX_RESOURCES = "infoBoxResources";
    private static final String ID_INFO_BOX_TASKS = "infoBoxTasks";

    private static final String ID_PERSONAL_INFO = "personalInfo";
    private static final String ID_SYSTEM_INFO = "systemInfo";

    private final Model<PrismObject<UserType>> principalModel = new Model<>();

    public PageDashboard() {
        principalModel.setObject(loadUserSelf());
        setTimeZone(PageDashboard.this);
        initLayout();
    }

    @Override
    protected void createBreadcrumb() {
        super.createBreadcrumb();

        Breadcrumb bc = getLastBreadcrumb();
        bc.setIcon(new Model("fa fa-dashboard"));
    }

    private void initLayout() {
    	initInfoBoxes();
        initPersonalInfo();
        initSystemInfo();

    }

    private void initInfoBoxes() {
    	Task task = createSimpleTask("PageDashboard.infobox");
    	OperationResult result = task.getResult();

    	add(createFocusInfoBoxPanel(ID_INFO_BOX_USERS, UserType.class, "object-user-bg",
    			GuiStyleConstants.CLASS_OBJECT_USER_ICON, "PageDashboard.infobox.users", PageUsers.class,
    			result, task));

    	add(createFocusInfoBoxPanel(ID_INFO_BOX_ORGS, OrgType.class, "object-org-bg",
    			GuiStyleConstants.CLASS_OBJECT_ORG_ICON, "PageDashboard.infobox.orgs", PageOrgTree.class,
    			result, task));

    	add(createFocusInfoBoxPanel(ID_INFO_BOX_ROLES, RoleType.class, "object-role-bg",
    			GuiStyleConstants.CLASS_OBJECT_ROLE_ICON, "PageDashboard.infobox.roles", PageRoles.class,
    			result, task));

    	add(createFocusInfoBoxPanel(ID_INFO_BOX_SERVICES, ServiceType.class, "object-service-bg",
    			GuiStyleConstants.CLASS_OBJECT_SERVICE_ICON, "PageDashboard.infobox.services", PageServices.class,
    			result, task));

    	add(createResourceInfoBoxPanel(result, task));
    	add(createTaskInfoBoxPanel(result, task));

	}

	private <F extends FocusType> InfoBoxPanel createFocusInfoBoxPanel(String id, Class<F> type, String bgColor,
			String icon, String keyPrefix, Class<? extends IRequestablePage> linkPage, OperationResult result, Task task) {
    	InfoBoxType infoBoxType = new InfoBoxType(bgColor, icon, getString(keyPrefix + ".label"));
    	Integer allCount;
		try {
			allCount = getModelService().countObjects(type, null, null, task, result);
			if (allCount == null) {
				allCount = 0;
			}

			ObjectQuery queryDisabled = QueryBuilder.queryFor(type, getPrismContext())
					.item(FocusType.F_ACTIVATION, ActivationType.F_EFFECTIVE_STATUS).eq(ActivationStatusType.DISABLED)
					.build();
			Integer disabledCount = getModelService().countObjects(type, queryDisabled, null, task, result);
			if (disabledCount == null) {
				disabledCount = 0;
			}

			ObjectQuery queryArchived = QueryBuilder.queryFor(type, getPrismContext())
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

		Model<InfoBoxType> boxModel = new Model<>(infoBoxType);

		return new InfoBoxPanel(id, boxModel, linkPage);
    }

    private Component createResourceInfoBoxPanel(OperationResult result, Task task) {
    	InfoBoxType infoBoxType = new InfoBoxType("object-resource-bg", GuiStyleConstants.CLASS_OBJECT_RESOURCE_ICON,
    			getString("PageDashboard.infobox.resources.label"));
    	Integer totalCount;
		try {
			totalCount = getModelService().countObjects(ResourceType.class, null, null, task, result);
			if (totalCount == null) {
				totalCount = 0;
			}

			ObjectQuery query = QueryBuilder.queryFor(ResourceType.class, getPrismContext())
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

		Model<InfoBoxType> boxModel = new Model<>(infoBoxType);

		return new InfoBoxPanel(ID_INFO_BOX_RESOURCES, boxModel, PageResources.class);
	}

    private Component createTaskInfoBoxPanel(OperationResult result, Task task) {
    	InfoBoxType infoBoxType = new InfoBoxType("object-task-bg", GuiStyleConstants.CLASS_OBJECT_TASK_ICON,
    			getString("PageDashboard.infobox.tasks.label"));
    	Integer totalCount;
		try {
			totalCount = getModelService().countObjects(TaskType.class, null, null, task, result);
			if (totalCount == null) {
				totalCount = 0;
			}
			ObjectQuery query = QueryBuilder.queryFor(TaskType.class, getPrismContext())
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

		Model<InfoBoxType> boxModel = new Model<>(infoBoxType);

		return new InfoBoxPanel(ID_INFO_BOX_TASKS, boxModel, PageTasks.class);
	}


    private void initPersonalInfo() {
        DashboardPanel personalInfo = new DashboardPanel(ID_PERSONAL_INFO, null,
                createStringResource("PageDashboard.personalInfo"), GuiStyleConstants.CLASS_OBJECT_USER_BOX_CSS_CLASSES,
                GuiStyleConstants.CLASS_OBJECT_USER_BOX_CSS_CLASSES) {
        	private static final long serialVersionUID = 1L;

            @Override
            protected Component getMainComponent(String componentId) {
                return new PersonalInfoPanel(componentId, PageDashboard.this);
            }
        };
        add(personalInfo);
    }

    private void initSystemInfo() {
        DashboardPanel systemInfo = new DashboardPanel(ID_SYSTEM_INFO, null,
                createStringResource("PageDashboard.systemInfo"),
                GuiStyleConstants.CLASS_ICON_TACHOMETER, GuiStyleConstants.CLASS_OBJECT_RESOURCE_BOX_CSS_CLASSES) {
			private static final long serialVersionUID = 1L;

			@Override
            protected Component getMainComponent(String componentId) {
                return new SystemInfoPanel(componentId);
            }
        };
        add(systemInfo);
    }





}
