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
import com.evolveum.midpoint.web.component.input.QNameChoiceRenderer;
import com.evolveum.midpoint.web.component.wizard.resource.dto.SynchronizationActionTypeDto;
import com.evolveum.midpoint.web.page.self.dto.AssignmentViewType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.apache.commons.lang.StringUtils;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.OnChangeAjaxBehavior;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.EnumChoiceRenderer;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.util.ListModel;

import javax.xml.namespace.QName;
import java.util.ArrayList;
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
    private static final String ID_VIEW_TYPE = "type";
    private static final String ID_BUTTON_PANEL = "buttonPanel";
    private static final String DOT_CLASS = PageAssignmentShoppingKart.class.getName() + ".";
    private static final String OPERATION_LOAD_ROLE_CATALOG_REFERENCE = DOT_CLASS + "loadRoleCatalogReference";
    private static final Trace LOGGER = TraceManager.getTrace(PageAssignmentShoppingKart.class);

    private IModel<AssignmentViewType> viewModel;
    private AssignmentViewType currentViewType = AssignmentViewType.ROLE_CATALOG_VIEW;
    private String catalogOid = null;

    public PageAssignmentShoppingKart() {
        initLayout();
    }

    private void initLayout() {
        Form mainForm = new org.apache.wicket.markup.html.form.Form(ID_MAIN_FORM);
        add(mainForm);

        catalogOid = getRoleCatalogOid();
        viewModel = new IModel<AssignmentViewType>() {
            @Override
            public AssignmentViewType getObject() {
                return currentViewType;
            }

            @Override
            public void setObject(AssignmentViewType assignmentViewType) {
                currentViewType = assignmentViewType;
            }

            @Override
            public void detach() {

            }
        };
        initButtonPanel(mainForm);

        mainForm.add(initMainPanel());

    }

    private PageBase getPageBase() {
        return (PageBase) getPage();
    }

    private String getRoleCatalogOid() {
        Task task = getPageBase().createAnonymousTask(OPERATION_LOAD_ROLE_CATALOG_REFERENCE);
        OperationResult result = task.getResult();

        PrismObject<SystemConfigurationType> config;
        try {
            config = getPageBase().getModelService().getObject(SystemConfigurationType.class,
                    SystemObjectsType.SYSTEM_CONFIGURATION.value(), null, task, result);
        } catch (ObjectNotFoundException | SchemaException | SecurityViolationException
                | CommunicationException | ConfigurationException e) {
            LOGGER.error("Error getting system configuration: {}", e.getMessage(), e);
            return null;
        }
        if (config != null && config.asObjectable().getRoleManagement() != null &&
                config.asObjectable().getRoleManagement().getRoleCatalogRef() != null) {
            return config.asObjectable().getRoleManagement().getRoleCatalogRef().getOid();
        }
        return "";
    }

    private void initButtonPanel(Form mainForm) {
        WebMarkupContainer buttonPanel = new WebMarkupContainer(ID_BUTTON_PANEL);
        buttonPanel.setOutputMarkupId(true);
        mainForm.add(buttonPanel);

        DropDownChoice<AssignmentViewType> viewSelect = new DropDownChoice(ID_VIEW_TYPE, viewModel, new ListModel(createAssignableTypesList()),
                new EnumChoiceRenderer<AssignmentViewType>(this));
        viewSelect.add(new OnChangeAjaxBehavior() {

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                QName viewTypeClass = getViewTypeClass();
                Component panel;
                if (viewTypeClass != null) {
                    panel = new AssignmentCatalogPanel(ID_MAIN_PANEL, viewTypeClass, PageAssignmentShoppingKart.this);
                    panel.setOutputMarkupId(true);
                } else {
                    panel = initMainPanel();
                }
                ((Form) PageAssignmentShoppingKart.this.get(ID_MAIN_FORM)).addOrReplace(panel);
                target.add(get(ID_MAIN_FORM));
            }
        });
        viewSelect.setOutputMarkupId(true);
        buttonPanel.add(viewSelect);

    }

    public static List<AssignmentViewType> createAssignableTypesList() {
        List<AssignmentViewType> focusTypeList = new ArrayList<>();

        focusTypeList.add(AssignmentViewType.ROLE_CATALOG_VIEW);
        focusTypeList.add(AssignmentViewType.ORG_TYPE);
        focusTypeList.add(AssignmentViewType.ROLE_TYPE);
        focusTypeList.add(AssignmentViewType.SERVICE_TYPE);

        return focusTypeList;
    }

    private QName getViewTypeClass() {
        if (AssignmentViewType.ORG_TYPE.equals(currentViewType)) {
            return OrgType.COMPLEX_TYPE;
        } else if (AssignmentViewType.ROLE_TYPE.equals(currentViewType)) {
            return RoleType.COMPLEX_TYPE;
        } else if (AssignmentViewType.SERVICE_TYPE.equals(currentViewType)) {
            return ServiceType.COMPLEX_TYPE;
        }
        return null;
    }

    private Component initMainPanel(){
        if (StringUtils.isEmpty(catalogOid)) {
            Label panel = new Label(ID_MAIN_PANEL, createStringResource("PageAssignmentShoppingKart.roleCatalogIsNotConfigured"));
            panel.setOutputMarkupId(true);
            return panel;
        } else {
            AssignmentCatalogPanel panel = new AssignmentCatalogPanel(ID_MAIN_PANEL, catalogOid, PageAssignmentShoppingKart.this);
            panel.setOutputMarkupId(true);
            return panel;
        }
    }
}
