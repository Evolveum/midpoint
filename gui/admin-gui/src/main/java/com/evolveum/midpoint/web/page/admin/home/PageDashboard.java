/*
 * Copyright (c) 2010-2015 Evolveum
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import com.evolveum.midpoint.prism.query.builder.QueryBuilder;
import com.evolveum.midpoint.web.component.breadcrumbs.Breadcrumb;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.springframework.security.core.Authentication;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismReference;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.query.EqualFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectQueryUtil;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.application.AuthorizationAction;
import com.evolveum.midpoint.web.application.PageDescriptor;
import com.evolveum.midpoint.web.component.SecurityContextAwareCallable;
import com.evolveum.midpoint.web.component.assignment.AssignmentEditorDtoType;
import com.evolveum.midpoint.web.component.box.InfoBoxPanel;
import com.evolveum.midpoint.web.component.box.InfoBoxType;
import com.evolveum.midpoint.web.component.util.CallableResult;
import com.evolveum.midpoint.web.page.admin.home.component.AsyncDashboardPanel;
import com.evolveum.midpoint.web.page.admin.home.component.DashboardColor;
import com.evolveum.midpoint.web.page.admin.home.component.DashboardPanel;
import com.evolveum.midpoint.web.page.admin.home.component.MyAccountsPanel;
import com.evolveum.midpoint.web.page.admin.home.component.MyAssignmentsPanel;
import com.evolveum.midpoint.web.page.admin.home.component.PersonalInfoPanel;
import com.evolveum.midpoint.web.page.admin.home.component.SystemInfoPanel;
import com.evolveum.midpoint.web.page.admin.home.dto.AccountCallableResult;
import com.evolveum.midpoint.web.page.admin.home.dto.AssignmentItemDto;
import com.evolveum.midpoint.web.page.admin.home.dto.SimpleAccountDto;

/**
 * @author lazyman
 */
@PageDescriptor(url = {"/admin/dashboard", "/admin"}, action = {
        @AuthorizationAction(actionUri = PageAdminHome.AUTH_HOME_ALL_URI,
                label = PageAdminHome.AUTH_HOME_ALL_LABEL, description = PageAdminHome.AUTH_HOME_ALL_DESCRIPTION),
        @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_DASHBOARD_URL,
                label = "PageDashboard.auth.dashboard.label", description = "PageDashboard.auth.dashboard.description")})
public class PageDashboard extends PageAdminHome {

    private static final Trace LOGGER = TraceManager.getTrace(PageDashboard.class);

    private static final String DOT_CLASS = PageDashboard.class.getName() + ".";
    private static final String OPERATION_LOAD_ACCOUNTS = DOT_CLASS + "loadAccounts";
    private static final String OPERATION_LOAD_ASSIGNMENTS = DOT_CLASS + "loadAssignments";

    private static final String ID_INFO_BOX_USERS = "infoBoxUsers";
    private static final String ID_INFO_BOX_ORGS = "infoBoxOrgs";
    private static final String ID_INFO_BOX_ROLES = "infoBoxRoles";
    private static final String ID_INFO_BOX_SERVICES = "infoBoxServices";
    private static final String ID_INFO_BOX_RESOURCES = "infoBoxResources";
    private static final String ID_INFO_BOX_TASKS = "infoBoxTasks";
    
    private static final String ID_PERSONAL_INFO = "personalInfo";
    private static final String ID_ACCOUNTS = "accounts";
    private static final String ID_ASSIGNMENTS = "assignments";
    private static final String ID_SYSTEM_INFO = "systemInfo";

    private final Model<PrismObject<UserType>> principalModel = new Model<PrismObject<UserType>>();

    public PageDashboard() {
        principalModel.setObject(loadUserSelf(PageDashboard.this));
        setTimeZone(PageDashboard.this);
        initLayout();
    }

    @Override
    protected void createBreadcrumb() {
        super.createBreadcrumb();

        Breadcrumb bc = getSessionStorage().peekBreadcrumb();
        bc.setIcon(new Model("fa fa-dashboard"));
    }

    private void initLayout() {
    	initInfoBoxes();
        initPersonalInfo();
        initMyAccounts();
        initAssignments();
        initSystemInfo();
        
//        DraggablesAcceptedByDroppable defaultValues = new DraggablesAcceptedByDroppable("one", "two", "three");
//        DraggableElement draggable1 = new DraggableElement("draggable1");
//        add(draggable1);
//        
//       
//        draggable1.setOutputMarkupId(true);
//
//
//        DroppableElement droppable1 = new DroppableElement("droppable1", defaultValues);
//        add(droppable1);
//        
//        add(new DraggableAndDroppableElement("draggableDroppable1", new Model<String>("Drag me!"), defaultValues));
//
//        
//        DraggableElement draggable2 = new DraggableElement("draggable2");
//        add(draggable2);
//        draggable2.setOutputMarkupId(true);
//
//        add(new DraggableAndDroppableElement("draggableDroppable2", new Model<String>("Drag me!"), defaultValues));
//
//        add(new DroppableElement("droppable2", defaultValues));
    }

	private AccountCallableResult<List<SimpleAccountDto>> loadAccounts() throws Exception {
        LOGGER.debug("Loading accounts.");

        AccountCallableResult callableResult = new AccountCallableResult();
        List<SimpleAccountDto> list = new ArrayList<SimpleAccountDto>();
        callableResult.setValue(list);
        PrismObject<UserType> user = principalModel.getObject();
        if (user == null) {
            return callableResult;
        }

        Task task = createSimpleTask(OPERATION_LOAD_ACCOUNTS);
        OperationResult result = task.getResult();
        callableResult.setResult(result);
        GetOperationOptions getOpts = GetOperationOptions.createResolve();
        getOpts.setNoFetch(Boolean.TRUE);
        Collection<SelectorOptions<GetOperationOptions>> options =
                SelectorOptions.createCollection(ShadowType.F_RESOURCE, getOpts);

        
        List<ObjectReferenceType> references = user.asObjectable().getLinkRef();
        for (ObjectReferenceType reference : references) {
            PrismObject<ShadowType> account = WebModelServiceUtils.loadObject(ShadowType.class, reference.getOid(),
                    options, this, task, result);
            if (account == null) {
                continue;
            }

            ShadowType accountType = account.asObjectable();

            OperationResultType fetchResult = accountType.getFetchResult();

            if (fetchResult != null) {
                callableResult.getFetchResults().add(OperationResult.createOperationResult(fetchResult));
            }

            ResourceType resource = accountType.getResource();
            String resourceName = WebComponentUtil.getName(resource);
            list.add(new SimpleAccountDto(WebComponentUtil.getOrigStringFromPoly(accountType.getName()), resourceName));
        }
        result.recordSuccessIfUnknown();
        result.recomputeStatus();

        LOGGER.debug("Finished accounts loading.");

        return callableResult;
    }
	
    private void initInfoBoxes() {
    	Task task = createSimpleTask("PageDashboard.infobox");
    	OperationResult result = task.getResult();
    	
    	add(createFocusInfoBoxPanel(ID_INFO_BOX_USERS, UserType.class, "object-user-bg", 
    			GuiStyleConstants.CLASS_OBJECT_USER_ICON, "PageDashboard.infobox.users", result, task));
    	
    	add(createFocusInfoBoxPanel(ID_INFO_BOX_ORGS, OrgType.class, "object-org-bg", 
    			GuiStyleConstants.CLASS_OBJECT_ORG_ICON, "PageDashboard.infobox.orgs", result, task));
    	
    	add(createFocusInfoBoxPanel(ID_INFO_BOX_ROLES, RoleType.class, "object-role-bg", 
    			GuiStyleConstants.CLASS_OBJECT_ROLE_ICON, "PageDashboard.infobox.roles", result, task));
    	
    	add(createFocusInfoBoxPanel(ID_INFO_BOX_SERVICES, ServiceType.class, "object-service-bg", 
    			GuiStyleConstants.CLASS_OBJECT_SERVICE_ICON, "PageDashboard.infobox.services", result, task));
    	
    	add(createResourceInfoBoxPanel(result, task));
    	add(createTaskInfoBoxPanel(result, task));
    	
	}

	private <F extends FocusType> InfoBoxPanel createFocusInfoBoxPanel(String id, Class<F> type, String bgColor, String icon, String keyPrefix, OperationResult result, Task task) {
    	InfoBoxType infoBoxType = new InfoBoxType(bgColor, icon, getString(keyPrefix + ".label"));
    	Integer allCount;
		try {
			allCount = getModelService().countObjects(type, null, null, task, result);
			if (allCount == null) {
				allCount = 0;
			}
			
			EqualFilter<ActivationStatusType> filterDisabled = EqualFilter.createEqual(SchemaConstants.PATH_ACTIVATION_EFFECTIVE_STATUS, 
					type, getPrismContext(), ActivationStatusType.DISABLED);
			Integer disabledCount = getModelService().countObjects(type, ObjectQuery.createObjectQuery(filterDisabled), null, task, result);
			if (disabledCount == null) {
				disabledCount = 0;
			}
			
			EqualFilter<ActivationStatusType> filterArchived = EqualFilter.createEqual(SchemaConstants.PATH_ACTIVATION_EFFECTIVE_STATUS, 
					type, getPrismContext(), ActivationStatusType.ARCHIVED);
			Integer archivedCount = getModelService().countObjects(type, ObjectQuery.createObjectQuery(filterArchived), null, task, result);
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

		Model<InfoBoxType> boxModel = new Model<InfoBoxType>(infoBoxType);

		return new InfoBoxPanel(id, boxModel);
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

		Model<InfoBoxType> boxModel = new Model<InfoBoxType>(infoBoxType);

		return new InfoBoxPanel(ID_INFO_BOX_RESOURCES, boxModel);
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
			
			EqualFilter<TaskExecutionStatusType> filter = EqualFilter.createEqual(TaskType.F_EXECUTION_STATUS, 
					TaskType.class, getPrismContext(), TaskExecutionStatusType.RUNNABLE);
			ObjectQuery query = ObjectQuery.createObjectQuery(filter);
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

		Model<InfoBoxType> boxModel = new Model<InfoBoxType>(infoBoxType);

		return new InfoBoxPanel(ID_INFO_BOX_TASKS, boxModel);
	}


    private void initPersonalInfo() {
        DashboardPanel personalInfo = new DashboardPanel(ID_PERSONAL_INFO, null,
                createStringResource("PageDashboard.personalInfo"), "fa fa-fw fa-male", DashboardColor.GRAY) {

            @Override
            protected Component getMainComponent(String componentId) {
                return new PersonalInfoPanel(componentId);
            }
        };
        add(personalInfo);
    }

    private void initSystemInfo() {
        DashboardPanel systemInfo = new DashboardPanel(ID_SYSTEM_INFO, null,
                createStringResource("PageDashboard.systemInfo"), "fa fa-tachometer", DashboardColor.GREEN) {

            @Override
            protected Component getMainComponent(String componentId) {
                return new SystemInfoPanel(componentId);
            }
        };
        add(systemInfo);
    }

    private void initMyAccounts() {
        AsyncDashboardPanel<Object, List<SimpleAccountDto>> accounts =
                new AsyncDashboardPanel<Object, List<SimpleAccountDto>>(ID_ACCOUNTS, createStringResource("PageDashboard.accounts"),
                        "fa fa-fw fa-external-link", DashboardColor.BLUE) {

                    @Override
                    protected SecurityContextAwareCallable<CallableResult<List<SimpleAccountDto>>> createCallable(
                            Authentication auth, IModel<Object> callableParameterModel) {

                        return new SecurityContextAwareCallable<CallableResult<List<SimpleAccountDto>>>(
                                getSecurityEnforcer(), auth) {

                            @Override
                            public AccountCallableResult<List<SimpleAccountDto>> callWithContextPrepared()
                                    throws Exception {
                                return loadAccounts();
                            }
                        };
                    }

                    @Override
                    protected Component getMainComponent(String markupId) {
                        return new MyAccountsPanel(markupId,
                                new PropertyModel<List<SimpleAccountDto>>(getModel(), CallableResult.F_VALUE));
                    }

                    @Override
                    protected void onPostSuccess(AjaxRequestTarget target) {
                        showFetchResult();
                        super.onPostSuccess(target);
                    }

                    @Override
                    protected void onUpdateError(AjaxRequestTarget target, Exception ex) {
                        showFetchResult();
                        super.onUpdateError(target, ex);
                    }

                    private void showFetchResult() {
                        AccountCallableResult<List<SimpleAccountDto>> result =
                                (AccountCallableResult<List<SimpleAccountDto>>) getModel().getObject();

                        PageBase page = (PageBase) getPage();
                        for (OperationResult res : result.getFetchResults()) {
                            if (!WebComponentUtil.isSuccessOrHandledError(res)) {
                                page.showResult(res);
                            }
                        }
                    }
                };
        add(accounts);
    }

    private void initAssignments() {
        AsyncDashboardPanel<Object, List<AssignmentItemDto>> assignedOrgUnits =
                new AsyncDashboardPanel<Object, List<AssignmentItemDto>>(ID_ASSIGNMENTS, createStringResource("PageDashboard.assignments"),
                        "fa fa-fw fa-star", DashboardColor.YELLOW) {

                    @Override
                    protected SecurityContextAwareCallable<CallableResult<List<AssignmentItemDto>>> createCallable(
                            Authentication auth, IModel callableParameterModel) {

                        return new SecurityContextAwareCallable<CallableResult<List<AssignmentItemDto>>>(
                                getSecurityEnforcer(), auth) {

                            @Override
                            public CallableResult<List<AssignmentItemDto>> callWithContextPrepared() throws Exception {
                                return loadAssignments();
                            }
                        };
                    }

                    @Override
                    protected Component getMainComponent(String markupId) {
                        return new MyAssignmentsPanel(markupId,
                                new PropertyModel<List<AssignmentItemDto>>(getModel(), CallableResult.F_VALUE));
                    }
                };
        add(assignedOrgUnits);

    }

    private CallableResult<List<AssignmentItemDto>> loadAssignments() throws Exception {
        LOGGER.debug("Loading assignments.");
        CallableResult callableResult = new CallableResult();
        List<AssignmentItemDto> list = new ArrayList<AssignmentItemDto>();
        callableResult.setValue(list);

        PrismObject<UserType> user = principalModel.getObject();
        if (user == null || user.findContainer(UserType.F_ASSIGNMENT) == null) {
            return callableResult;
        }

        Task task = createSimpleTask(OPERATION_LOAD_ASSIGNMENTS);
        OperationResult result = task.getResult();
        callableResult.setResult(result);

        PrismContainer assignments = user.findContainer(UserType.F_ASSIGNMENT);
        List<PrismContainerValue> values = assignments.getValues();
        for (PrismContainerValue assignment : values) {
            AssignmentItemDto item = createAssignmentItem(user, assignment, task, result);
            if (item != null) {
                list.add(item);
            }
        }
        result.recordSuccessIfUnknown();
        result.recomputeStatus();

        Collections.sort(list);

        LOGGER.debug("Finished assignments loading.");

        return callableResult;
    }

    private AssignmentItemDto createAssignmentItem(PrismObject<UserType> user,
                                                   PrismContainerValue assignment, Task task, OperationResult result) {
        PrismReference targetRef = assignment.findReference(AssignmentType.F_TARGET_REF);
        if (targetRef == null || targetRef.isEmpty()) {
            //account construction
            PrismContainer construction = assignment.findContainer(AssignmentType.F_CONSTRUCTION);
            String name = null;
            String description = null;
            if (construction.getValue().asContainerable() != null && !construction.isEmpty()) {
                ConstructionType constr = (ConstructionType) construction.getValue().asContainerable();
                description =  (String) construction.getPropertyRealValue(ConstructionType.F_DESCRIPTION, String.class);

                if (constr.getResourceRef() != null) {
                    ObjectReferenceType resourceRef = constr.getResourceRef();

                    PrismObject resource = WebModelServiceUtils.loadObject(ResourceType.class, resourceRef.getOid(), this, task, result);
                    name = WebComponentUtil.getName(resource);
                }
            }

            return new AssignmentItemDto(AssignmentEditorDtoType.CONSTRUCTION, name, description, null);
        }

        PrismReferenceValue refValue = targetRef.getValue();
        PrismObject value = refValue.getObject();
        if (value == null) {
            //resolve reference
            value = WebModelServiceUtils.loadObject(ObjectType.class, refValue.getOid(), this, task, result);
        }

        if (value == null) {
            //we couldn't resolve assignment details
            return new AssignmentItemDto(null, null, null, null);
        }

        String name = WebComponentUtil.getName(value);
        AssignmentEditorDtoType type = AssignmentEditorDtoType.getType(value.getCompileTimeClass());
        String relation = refValue.getRelation() != null ? refValue.getRelation().getLocalPart() : null;
        String description = null;
        if (RoleType.class.isAssignableFrom(value.getCompileTimeClass())) {
            description = (String) value.getPropertyRealValue(RoleType.F_DESCRIPTION, String.class);
        }

        return new AssignmentItemDto(type, name, description, relation);
    }

}
