/*
 * Copyright (c) 2016 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.evolveum.midpoint.web.page.self;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.application.AuthorizationAction;
import com.evolveum.midpoint.web.application.PageDescriptor;
import com.evolveum.midpoint.web.component.assignment.AssignmentCatalogPanel;
import com.evolveum.midpoint.web.page.self.dto.AssignmentViewType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.apache.commons.lang.StringUtils;
import org.apache.poi.util.StringUtil;
import org.apache.wicket.Component;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by honchar.
 */
@PageDescriptor(url = {"/self/assignmentShoppingCart"}, action = {
        @AuthorizationAction(actionUri = PageSelf.AUTH_SELF_ALL_URI,
                label = PageSelf.AUTH_SELF_ALL_LABEL,
                description = PageSelf.AUTH_SELF_ALL_DESCRIPTION),
        @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_SELF_ASSIGNMENT_SHOP_KART_URL,
                label = "PageAssignmentShoppingKart.auth.requestAssignment.label",
                description = "PageAssignmentShoppingKart.auth.requestAssignment.description")})
public class PageAssignmentShoppingKart extends PageSelf {
    private static final long serialVersionUID = 1L;

    private static final String ID_MAIN_PANEL = "mainPanel";
    private static final String ID_MAIN_FORM = "mainForm";
    private static final String DOT_CLASS = PageAssignmentShoppingKart.class.getName() + ".";
    private static final String OPERATION_LOAD_ROLE_CATALOG_REFERENCE = DOT_CLASS + "loadRoleCatalogReference";
    private static final String OPERATION_GET_ASSIGNMENT_VIEW_LIST = DOT_CLASS + "getAssignmentViewList";
    private static final Trace LOGGER = TraceManager.getTrace(PageAssignmentShoppingKart.class);

    private String catalogOid = null;
    private boolean isFirstInit = true;
    private RoleManagementConfigurationType roleManagementConfigurationType;

    public PageAssignmentShoppingKart() {
        initLayout();
    }

    private void initLayout() {
        roleManagementConfigurationType = getRoleManagementConfigurationType();

        Form mainForm = new org.apache.wicket.markup.html.form.Form(ID_MAIN_FORM);
        add(mainForm);

        catalogOid = getRoleCatalogOid();
        mainForm.add(initMainPanel());

    }

    private PageBase getPageBase() {
        return (PageBase) getPage();
    }

    private String getRoleCatalogOid() {
        Task task = getPageBase().createAnonymousTask(OPERATION_LOAD_ROLE_CATALOG_REFERENCE);
        OperationResult result = task.getResult();

        SystemConfigurationType config;
        try {
            config = getPageBase().getModelInteractionService().getSystemConfiguration(result);
        } catch (ObjectNotFoundException | SchemaException e) {
            LOGGER.error("Error getting system configuration: {}", e.getMessage(), e);
            return null;
        }
        if (config != null && config.getRoleManagement() != null &&
                config.getRoleManagement().getRoleCatalogRef() != null) {
            return config.getRoleManagement().getRoleCatalogRef().getOid();
        }
        return "";
    }

    private Component initMainPanel() {
        List<AssignmentViewType> viewTypeList = getAssignmentViewList();
        AssignmentViewType defaultViewType = getDefaultAssignmentViewType();
        if (viewTypeList == null || viewTypeList.size() == 0){
            viewTypeList = new ArrayList<>(Arrays.asList(AssignmentViewType.values()));
        }
        if (defaultViewType != null && !viewTypeList.contains(defaultViewType)) {
            viewTypeList.add(defaultViewType);
        } else if (defaultViewType == null) {
            if (viewTypeList.size() == 1) {
                defaultViewType = viewTypeList.get(0);
            } else {
                if (StringUtils.isEmpty(catalogOid) && AssignmentViewType.ROLE_CATALOG_VIEW.getUri().equals(viewTypeList.get(0).getUri())) {
                    defaultViewType = viewTypeList.get(1);
                } else {
                    defaultViewType = viewTypeList.get(0);
                }
            }
        }
                AssignmentCatalogPanel panel = new AssignmentCatalogPanel(ID_MAIN_PANEL, catalogOid, defaultViewType, viewTypeList, PageAssignmentShoppingKart.this);
                panel.setOutputMarkupId(true);
                return panel;
    }

    private RoleManagementConfigurationType getRoleManagementConfigurationType() {
        OperationResult result = new OperationResult(OPERATION_GET_ASSIGNMENT_VIEW_LIST);
        SystemConfigurationType config = null;
        try {
            config = getModelInteractionService().getSystemConfiguration(result);
        } catch (ObjectNotFoundException | SchemaException e) {
            LOGGER.error("Error getting system configuration: {}", e.getMessage(), e);
            return null;
        }
        if (config != null) {
            return config.getRoleManagement();
        }
        return null;
    }

    private List<AssignmentViewType> getAssignmentViewList() {
        List<AssignmentViewType> assignmentViewTypes = new ArrayList<>();
        if (roleManagementConfigurationType != null
                && roleManagementConfigurationType.getRoleCatalogCollections() != null
                && roleManagementConfigurationType.getRoleCatalogCollections().getCollection() != null) {
            for (ObjectCollectionUseType collection :
                    roleManagementConfigurationType.getRoleCatalogCollections().getCollection()){
                AssignmentViewType viewType = AssignmentViewType.getViewType(collection);
                if (viewType != null){
                    assignmentViewTypes.add(viewType);
                }
            }
        }
        return assignmentViewTypes;
    }

    private AssignmentViewType getDefaultAssignmentViewType(){
        if (roleManagementConfigurationType == null){
            return null;
        }
        return AssignmentViewType.getViewType(roleManagementConfigurationType.getDefaultCollection());
    }
}
