/*
 * Copyright (C) 2016-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.page.self;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.web.application.Url;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.component.tabs.PanelTab;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.application.AuthorizationAction;
import com.evolveum.midpoint.web.application.PageDescriptor;
import com.evolveum.midpoint.web.component.TabbedPanel;
import com.evolveum.midpoint.web.component.assignment.AssignmentsUtil;
import com.evolveum.midpoint.web.component.form.MidpointForm;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.page.self.dto.AssignmentViewType;
import com.evolveum.midpoint.web.session.RoleCatalogStorage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Created by honchar.
 */
@PageDescriptor(
        urls = {
                @Url(mountUrl = "/self/assignmentShoppingCart")
        },
        action = {
        @AuthorizationAction(actionUri = PageSelf.AUTH_SELF_ALL_URI,
                label = PageSelf.AUTH_SELF_ALL_LABEL,
                description = PageSelf.AUTH_SELF_ALL_DESCRIPTION),
        @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_SELF_REQUESTS_ASSIGNMENTS_URL,
                label = "PageAssignmentShoppingCart.auth.requestAssignment.label",
                description = "PageAssignmentShoppingCart.auth.requestAssignment.description") })
public class PageAssignmentShoppingCart extends PageSelf {
    private static final long serialVersionUID = 1L;

    private static final String ID_MAIN_FORM = "mainForm";
    private static final String ID_VIEWS_TAB_PANEL = "viewsTabPanel";
    private static final String DOT_CLASS = PageAssignmentShoppingCart.class.getName() + ".";

    private static final String OPERATION_GET_ASSIGNMENT_VIEW_LIST = DOT_CLASS + "getRoleCatalogViewsList";
    private static final String OPERATION_LOAD_ASSIGNMENTS_LIMIT = DOT_CLASS + "loadAssignmentsLimit";
    private static final Trace LOGGER = TraceManager.getTrace(PageAssignmentShoppingCart.class);

    private IModel<RoleManagementConfigurationType> roleManagementConfigModel;

    public PageAssignmentShoppingCart() {
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initModels();
        getRoleCatalogStorage().setAssignmentRequestLimit(
                AssignmentsUtil.loadAssignmentsLimit(new OperationResult(OPERATION_LOAD_ASSIGNMENTS_LIMIT), this));

        setOutputMarkupId(true);

        Form mainForm = new MidpointForm(ID_MAIN_FORM);
        add(mainForm);

        List<ITab> tabs = getTabsList();
        TabbedPanel tabbedPanel = new TabbedPanel<ITab>(ID_VIEWS_TAB_PANEL, tabs) {

            private static final long serialVersionUID = 1L;

            @Override
            public TabbedPanel<ITab> setSelectedTab(int index) {
                getRoleCatalogStorage().setDefaultTabIndex(index);
                return super.setSelectedTab(index);
            }
        };
        tabbedPanel.setOutputMarkupId(true);

        int defaultSelectedTabIndex = getDefaultViewTypeIndex();
        if (getRoleCatalogStorage().getDefaultTabIndex() > 0 && getRoleCatalogStorage().getDefaultTabIndex() < tabs.size()) {
            tabbedPanel.setSelectedTab(getRoleCatalogStorage().getDefaultTabIndex());
        } else if (defaultSelectedTabIndex < tabs.size()) {
            tabbedPanel.setSelectedTab(defaultSelectedTabIndex);
        }
        mainForm.add(tabbedPanel);
    }

    private void initModels() {
        roleManagementConfigModel = new LoadableModel<RoleManagementConfigurationType>(false) {
            @Override
            protected RoleManagementConfigurationType load() {
                OperationResult result = new OperationResult(OPERATION_GET_ASSIGNMENT_VIEW_LIST);
                SystemConfigurationType config;
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
        };
    }

    private int getDefaultViewTypeIndex() {
        RoleManagementConfigurationType roleConfig = roleManagementConfigModel.getObject();

        if (roleConfig == null || roleConfig.getDefaultCollection() == null || roleConfig.getDefaultCollection().getCollectionUri() == null) {
            return 0;
        }
        List<AssignmentViewType> viewTypes = Arrays.asList(AssignmentViewType.values());
        for (AssignmentViewType viewType : viewTypes) {
            if (viewType.getUri().equals(roleConfig.getDefaultCollection().getCollectionUri())) {
                return viewTypes.indexOf(viewType);
            }
        }
        return 0;
    }

    private List<ITab> getTabsList() {
        List<ITab> tabs = new ArrayList<>();

        String roleCatalogOid = getRoleCatalogOid();
        if (StringUtils.isNotEmpty(roleCatalogOid)) {

            tabs.add(new PanelTab(createStringResource("AssignmentViewType.ROLE_CATALOG_VIEW"),
                    getTabVisibleBehaviour(SchemaConstants.OBJECT_COLLECTION_ROLE_CATALOG_URI)) {

                private static final long serialVersionUID = 1L;

                @Override
                public WebMarkupContainer createPanel(String panelId) {
                    return new RoleCatalogTabPanel(panelId, roleManagementConfigModel.getObject(), roleCatalogOid);
                }
            });
        }
        tabs.add(new PanelTab(createStringResource("AssignmentViewType.ROLE_TYPE"),
                getTabVisibleBehaviour(SchemaConstants.OBJECT_COLLECTION_ALL_ROLES_URI)) {

            private static final long serialVersionUID = 1L;

            @Override
            public WebMarkupContainer createPanel(String panelId) {
                return new AbstractShoppingCartTabPanel<RoleType>(panelId, roleManagementConfigModel.getObject()) {
                    private static final long serialVersionUID = 1L;

                    @Override
                    protected QName getQueryType() {
                        return RoleType.COMPLEX_TYPE;
                    }

                };
            }
        });
        tabs.add(new PanelTab(createStringResource("AssignmentViewType.ORG_TYPE"),
                getTabVisibleBehaviour(SchemaConstants.OBJECT_COLLECTION_ALL_ORGS_URI)) {

            private static final long serialVersionUID = 1L;

            @Override
            public WebMarkupContainer createPanel(String panelId) {
                return new AbstractShoppingCartTabPanel<OrgType>(panelId, roleManagementConfigModel.getObject()) {
                    private static final long serialVersionUID = 1L;

                    @Override
                    protected QName getQueryType() {
                        return OrgType.COMPLEX_TYPE;
                    }

                };
            }
        });
        tabs.add(new PanelTab(createStringResource("AssignmentViewType.SERVICE_TYPE"),
                getTabVisibleBehaviour(SchemaConstants.OBJECT_COLLECTION_ALL_SERVICES_URI)) {

            private static final long serialVersionUID = 1L;

            @Override
            public WebMarkupContainer createPanel(String panelId) {
                return new AbstractShoppingCartTabPanel<ServiceType>(panelId, roleManagementConfigModel.getObject()) {
                    private static final long serialVersionUID = 1L;

                    @Override
                    protected QName getQueryType() {
                        return ServiceType.COMPLEX_TYPE;
                    }
                };
            }
        });
        tabs.add(new PanelTab(createStringResource("AssignmentViewType.USER_TYPE"),
                getTabVisibleBehaviour(SchemaConstants.OBJECT_COLLECTION_USER_ASSIGNMENTS_URI)) {

            private static final long serialVersionUID = 1L;

            @Override
            public WebMarkupContainer createPanel(String panelId) {
                return new UserViewTabPanel(panelId, roleManagementConfigModel.getObject());
            }
        });
        return tabs;
    }

    private VisibleEnableBehaviour getTabVisibleBehaviour(String viewType) {
        return new VisibleEnableBehaviour() {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isVisible() {
                if (SchemaConstants.OBJECT_COLLECTION_ROLE_CATALOG_URI.equals(viewType)) {
                    return !StringUtils.isEmpty(getRoleCatalogOid());
                }

                RoleManagementConfigurationType config = roleManagementConfigModel.getObject();
                if (config == null || config.getRoleCatalogCollections() == null) {
                    return true;
                }

                List<ObjectCollectionUseType> viewsList = config.getRoleCatalogCollections().getCollection();
                if (viewsList.isEmpty()) {
                    return true;
                }
                for (ObjectCollectionUseType view : viewsList) {
                    if (viewType.equals(view.getCollectionUri())) {
                        return true;
                    }
                }
                return false;
            }
        };
    }

    private RoleCatalogStorage getRoleCatalogStorage() {
        return getSessionStorage().getRoleCatalog();
    }

    private String getRoleCatalogOid() {
        RoleManagementConfigurationType config = roleManagementConfigModel.getObject();
        return config == null || config.getRoleCatalogRef() == null ? null : config.getRoleCatalogRef().getOid();
    }
}
