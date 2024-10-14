/*
 * Copyright (C) 2020-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.component.menu;

import java.io.Serial;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.gui.impl.page.admin.certification.*;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.page.PageRoleAnalysis;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.page.PageRoleAnalysisSession;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.page.mining.PageRoleSuggestions;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.page.outlier.PageOutliers;
import com.evolveum.midpoint.model.api.authentication.GuiProfiledPrincipal;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.wicket.Page;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.image.ExternalImage;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.util.string.StringValue;

import com.evolveum.midpoint.cases.api.util.QueryUtils;
import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.GuiDisplayTypeUtil;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.gui.impl.page.admin.AbstractPageObjectDetails;
import com.evolveum.midpoint.gui.impl.page.admin.cases.PageCase;
import com.evolveum.midpoint.gui.impl.page.admin.simulation.PageSimulationResult;
import com.evolveum.midpoint.gui.impl.page.admin.simulation.PageSimulationResults;
import com.evolveum.midpoint.gui.impl.page.admin.systemconfiguration.page.PageBaseSystemConfiguration;
import com.evolveum.midpoint.gui.impl.page.self.PageRequestAccess;
import com.evolveum.midpoint.gui.impl.page.self.dashboard.PageSelfDashboard;
import com.evolveum.midpoint.model.api.AccessCertificationService;
import com.evolveum.midpoint.model.api.authentication.CompiledDashboardType;
import com.evolveum.midpoint.model.api.authentication.CompiledGuiProfile;
import com.evolveum.midpoint.model.api.authentication.CompiledObjectCollectionView;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.builder.S_FilterEntryOrEmpty;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.application.PageMounter;
import com.evolveum.midpoint.web.component.menu.*;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.page.admin.cases.PageCaseWorkItem;
import com.evolveum.midpoint.web.page.admin.cases.PageCaseWorkItemsAll;
import com.evolveum.midpoint.web.page.admin.cases.PageCaseWorkItemsAllocatedToMe;
import com.evolveum.midpoint.web.page.admin.cases.PageWorkItemsClaimable;
import com.evolveum.midpoint.web.page.admin.configuration.*;
import com.evolveum.midpoint.web.page.admin.home.PageDashboardConfigurable;
import com.evolveum.midpoint.web.page.admin.home.PageDashboardInfo;
import com.evolveum.midpoint.web.page.admin.orgs.PageOrgTree;
import com.evolveum.midpoint.web.page.admin.reports.PageAuditLogViewer;
import com.evolveum.midpoint.web.page.admin.reports.PageCreatedReports;
import com.evolveum.midpoint.web.page.admin.resources.PageConnectorHosts;
import com.evolveum.midpoint.web.page.admin.resources.PageImportResource;
import com.evolveum.midpoint.web.page.admin.server.PageNodes;
import com.evolveum.midpoint.web.page.admin.server.PageTasksCertScheduling;
import com.evolveum.midpoint.web.page.admin.workflow.PageAttorneySelection;
import com.evolveum.midpoint.web.page.admin.workflow.PageWorkItemsAttorney;
import com.evolveum.midpoint.web.page.self.PageSelfConsents;
import com.evolveum.midpoint.web.security.MidPointApplication;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

public class LeftMenuPanel extends BasePanel<Void> {

    private static final String ID_MENU = "menu";
    private static final String ID_LOGO = "logo";
    private static final String ID_CUSTOM_LOGO = "customLogo";
    private static final String ID_CUSTOM_LOGO_IMG_SRC = "customLogoImgSrc";
    private static final String ID_CUSTOM_LOGO_IMG_CSS = "customLogoImgCss";

    private static final Trace LOGGER = TraceManager.getTrace(LeftMenuPanel.class);

    private static final String DOT_CLASS = LeftMenuPanel.class.getName() + ".";

    private static final String OPERATION_LOAD_WORK_ITEM_COUNT = DOT_CLASS + "loadWorkItemCount";
    private static final String OPERATION_LOAD_CERT_WORK_ITEM_COUNT = DOT_CLASS + "loadCertificationWorkItemCount";

    private final LoadableModel<String> workItemCountModel;
    private final LoadableModel<String> certWorkItemCountModel;
    private final LoadableModel<List<SideBarMenuItem>> sideBarMenuModel;

    public LeftMenuPanel(String id) {
        super(id);

        sideBarMenuModel = new LoadableModel<>(false) {

            private static final long serialVersionUID = 1L;

            @Override
            protected List<SideBarMenuItem> load() {
                return createMenuItems();
            }
        };

        workItemCountModel = new LoadableModel<>(false) {
            private static final long serialVersionUID = 1L;

            @Override
            protected String load() {
                try {
                    Task task = getPageBase().createSimpleTask(OPERATION_LOAD_WORK_ITEM_COUNT);
                    S_FilterEntryOrEmpty q = getPrismContext().queryFor(CaseWorkItemType.class);
                    ObjectQuery query = QueryUtils.filterForCaseAssignees(q, getPageBase().getPrincipal())
                            .and()
                            .item(CaseWorkItemType.F_CLOSE_TIMESTAMP)
                            .isNull()
                            .build();
                    Integer workItemCount = getPageBase().getModelService().countContainers(CaseWorkItemType.class, query, null, task, task.getResult());
                    if (workItemCount == null || workItemCount == 0) {
                        return null;
                    }
                    return workItemCount.toString();
                } catch (Exception e) {
                    LoggingUtils.logExceptionAsWarning(LOGGER, "Couldn't load work item count", e);
                    return null;
                }
            }
        };
        certWorkItemCountModel = new LoadableModel<>(false) {
            private static final long serialVersionUID = 1L;

            @Override
            protected String load() {
                try {
                    AccessCertificationService acs = getPageBase().getCertificationService();
                    Task task = getPageBase().createSimpleTask(OPERATION_LOAD_CERT_WORK_ITEM_COUNT);
                    OperationResult result = task.getResult();
                    int openCertWorkItems = acs.countOpenWorkItems(getPrismContext().queryFactory().createQuery(), true, null, task, result);
                    if (openCertWorkItems == 0) {
                        return null;
                    }
                    return Integer.toString(openCertWorkItems);
                } catch (Exception e) {
                    LoggingUtils.logExceptionAsWarning(LOGGER, "Couldn't load certification work item count", e);
                    return null;
                }
            }
        };
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    private void initLayout() {
        AjaxLink<String> logo = new AjaxLink<>(ID_LOGO) {

            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                Class<? extends Page> page = MidPointApplication.get().getHomePage();
                setResponsePage(page);
            }
        };
        logo.add(new VisibleEnableBehaviour(() -> !isCustomLogoVisible(), () -> getPageBase().isLogoLinkEnabled()));
        logo.add(AttributeAppender.append("class", () -> WebComponentUtil.getMidPointSkin().getNavbarCss()));
        add(logo);

        IModel<IconType> logoModel = new IModel<>() {

            private static final long serialVersionUID = 1L;

            @Override
            public IconType getObject() {
                DeploymentInformationType info = MidPointApplication.get().getDeploymentInfo();
                if (info == null | info.getLogo() == null) {
                    return new IconType();
                }

                return info.getLogo();
            }
        };

        AjaxLink<String> customLogo = new AjaxLink<>(ID_CUSTOM_LOGO) {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                //TODO may be this should lead to customerUrl ?
                Class<? extends Page> page = MidPointApplication.get().getHomePage();
                setResponsePage(page);
            }
        };
        customLogo.add(AttributeAppender.append("class", () -> WebComponentUtil.getMidPointSkin().getNavbarCss()));
        customLogo.add(AttributeAppender.append("class", () -> {
            IconType icon = logoModel.getObject();
            return StringUtils.isNotEmpty(icon.getImageUrl()) ? logoModel.getObject().getCssClass() : null;
        }));
        customLogo.add(new VisibleBehaviour(() -> isCustomLogoVisible()));
        add(customLogo);

        ExternalImage customLogoImgSrc = new ExternalImage(ID_CUSTOM_LOGO_IMG_SRC) {

            @Override
            protected void buildSrcAttribute(ComponentTag tag, IModel<?> srcModel) {
                tag.put("src", WebComponentUtil.getIconUrlModel(logoModel.getObject()).getObject());
            }
        };
        customLogoImgSrc.add(new VisibleBehaviour(() -> StringUtils.isNotEmpty(logoModel.getObject().getImageUrl())));
        customLogo.add(customLogoImgSrc);

        WebMarkupContainer customLogoImgCss = new WebMarkupContainer(ID_CUSTOM_LOGO_IMG_CSS);
        customLogoImgCss.add(new VisibleBehaviour(() ->
                StringUtils.isEmpty(logoModel.getObject().getImageUrl()) && StringUtils.isNotEmpty(logoModel.getObject().getCssClass())));
        customLogoImgCss.add(AttributeAppender.append("class", () -> logoModel.getObject().getCssClass()));
        customLogo.add(customLogoImgCss);

        logo.add(PageBase.createHeaderColorStyleModel(false));
        customLogo.add(PageBase.createHeaderColorStyleModel(false));

        SideBarMenuPanel sidebarMenu = new SideBarMenuPanel(ID_MENU, sideBarMenuModel);
        add(sidebarMenu);
    }

    private boolean isCustomLogoVisible() {
        DeploymentInformationType info = MidPointApplication.get().getDeploymentInfo();
        if (info == null || info.getLogo() == null) {
            return false;
        }

        IconType logo = info.getLogo();
        return StringUtils.isNotEmpty(logo.getImageUrl()) || StringUtils.isNotEmpty(logo.getCssClass());
    }

    protected List<SideBarMenuItem> createMenuItems() {
        List<SideBarMenuItem> menus = new ArrayList<>();

        boolean experimentalFeaturesEnabled = WebModelServiceUtils.isEnableExperimentalFeature(getPageBase());

        SideBarMenuItem menu = createSelfServiceMenu(experimentalFeaturesEnabled);
        addSidebarMenuItem(menus, menu);

        menu = createMainNavigationMenu(experimentalFeaturesEnabled);
        addSidebarMenuItem(menus, menu);

        menu = createConfigurationMenu(experimentalFeaturesEnabled);
        addSidebarMenuItem(menus, menu);

        menu = createAdditionalMenu(experimentalFeaturesEnabled);
        addSidebarMenuItem(menus, menu);

        return menus;
    }

    private void addSidebarMenuItem(List<SideBarMenuItem> menus, SideBarMenuItem menu) {
        if (menu.isEmpty()) {
            return;
        }

        menus.add(menu);
    }

    private SideBarMenuItem createSelfServiceMenu(boolean experimentalFeaturesEnabled) {
        SideBarMenuItem menu = new SideBarMenuItem("PageAdmin.menu.selfService", experimentalFeaturesEnabled);
        menu.addMainMenuItem(createMainMenuItem("PageAdmin.menu.selfDashboard", GuiStyleConstants.CLASS_ICON_DASHBOARD,
                PageSelfDashboard.class));
        PageParameters pageParameters = new PageParameters();
        pageParameters.add(OnePageParameterEncoder.PARAMETER, WebModelServiceUtils.getLoggedInFocusOid());
        menu.addMainMenuItem(createMainMenuItem("PageAdmin.menu.profile", GuiStyleConstants.CLASS_ICON_PROFILE,
                WebComponentUtil.resolveSelfPage(), pageParameters));
        menu.addMainMenuItem(createMainMenuItem("PageAdmin.menu.credentials", GuiStyleConstants.CLASS_ICON_CREDENTIALS,
                com.evolveum.midpoint.gui.impl.page.self.credentials.PageSelfCredentials.class));
        if (WebModelServiceUtils.getLoggedInFocus() instanceof UserType) {
            menu.addMainMenuItem(createMainMenuItem("PageRequestAccess.title", GuiStyleConstants.CLASS_ICON_REQUEST_ACCESS,
                    PageRequestAccess.class));
        }
        menu.addMainMenuItem(createMainMenuItem("PageAdmin.menu.consent", GuiStyleConstants.CLASS_ICON_CONSENT,
                PageSelfConsents.class));
        return menu;
    }

    private SideBarMenuItem createMainNavigationMenu(boolean experimentalFeaturesEnabled) {
        SideBarMenuItem menu = new SideBarMenuItem("PageAdmin.menu.mainNavigation", experimentalFeaturesEnabled);
        menu.addMainMenuItem(createHomeItems());
        menu.addMainMenuItem(createUsersItems());
        menu.addMainMenuItem(createOrganizationsMenu());
        menu.addMainMenuItem(createRolesMenu());
        menu.addMainMenuItem(createServicesItems());
        menu.addMainMenuItem(createPoliciesItems());
        menu.addMainMenuItem(createResourcesItems());
        if (getPageBase().getCaseManager().isEnabled()) {
            menu.addMainMenuItem(createWorkItemsItems());
        }
        menu.addMainMenuItem(createCertificationItems());
        menu.addMainMenuItem(createServerTasksItems());
        menu.addMainMenuItem(createNodesItems());
        menu.addMainMenuItem(createReportsItems());
        menu.addMainMenuItem(createSimulationItems());
        menu.addMainMenuItem(createAuditItems());

        return menu;
    }

    private MainMenuItem createHomeItems() {
        MainMenuItem homeMenu = createMainMenuItem("PageAdmin.menu.dashboard", GuiStyleConstants.CLASS_DASHBOARD_ICON);
        homeMenu.addMenuItem(new MenuItem("PageAdmin.menu.dashboard.info", PageDashboardInfo.class));

        List<CompiledDashboardType> dashboards = getPageBase().getCompiledGuiProfile().getConfigurableDashboards();

        for (CompiledDashboardType prismObject : dashboards) {
            MenuItem dashboardMenu = createDashboardMenuItem(prismObject);
            homeMenu.addMenuItem(dashboardMenu);
        }

        return homeMenu;
    }

    private MenuItem createDashboardMenuItem(CompiledDashboardType dashboard) {
        Validate.notNull(dashboard, "Dashboard object is null");

        if (!WebComponentUtil.getElementVisibility(dashboard.getVisibility())) {
            return null;
        }

        String label = getDashboardLabel(dashboard);

        StringValue dashboardOidParam = getPageBase().getPageParameters().get(OnePageParameterEncoder.PARAMETER);
        boolean active = false;
        if (dashboardOidParam != null) {
            active = dashboard.getOid().equals(dashboardOidParam.toString());
        }

        return new MenuItem(label, PageDashboardConfigurable.class, createDashboardPageParameters(dashboard), active);

    }

    private String getDashboardLabel(CompiledDashboardType dashboard) {
        String label = null;
        PolyStringType displayType = WebComponentUtil.getCollectionLabel(dashboard.getDisplay());
        if (displayType != null) {
            label = WebComponentUtil.getTranslatedPolyString(displayType);
        }
        if (StringUtils.isBlank(label)) {
            label = WebComponentUtil.getTranslatedPolyString(dashboard.getName());
        }
        return label;
    }

    private PageParameters createDashboardPageParameters(CompiledDashboardType dashboard) {
        PageParameters pageParameters = new PageParameters();
        pageParameters.add(OnePageParameterEncoder.PARAMETER, dashboard.getOid());
        return pageParameters;
    }

    private MainMenuItem createUsersItems() {
        MainMenuItem userMenu = createMainMenuItem("PageAdmin.menu.top.users", GuiStyleConstants.CLASS_OBJECT_USER_ICON_COLORED);
        createBasicAssignmentHolderMenuItems(userMenu, PageTypes.USER);
        return userMenu;
    }

    //TODO AuthorizationConstants.AUTZ_UI_ORG_STRUCT_URL
    private MainMenuItem createOrganizationsMenu() {
        MainMenuItem organizationMenu = createMainMenuItem("PageAdmin.menu.top.orgs", GuiStyleConstants.CLASS_OBJECT_ORG_ICON_COLORED
        );

        MenuItem orgTree = new MenuItem("PageAdmin.menu.top.orgs.tree",
                GuiStyleConstants.CLASS_OBJECT_ORG_ICON, PageOrgTree.class);
        organizationMenu.addMenuItem(orgTree);

        createBasicAssignmentHolderMenuItems(organizationMenu, PageTypes.ORG);

        return organizationMenu;

    }

    private MainMenuItem createRolesMenu() {
        MainMenuItem roleMenu = createMainMenuItem("PageAdmin.menu.top.roles", GuiStyleConstants.CLASS_OBJECT_ROLE_ICON_COLORED
        );
        createBasicAssignmentHolderMenuItems(roleMenu, PageTypes.ROLE);

        roleMenu.addMenuItemAtIndex(new MenuItem("PageRoleAnalysis.menu.title",
                GuiStyleConstants.CLASS_ROLE_ANALYSIS_SESSION_ICON,
                PageRoleAnalysis.class), 1);
        roleMenu.addMenuItem(new MenuItem("PageRoleAnalysisSession.menu.title",
                GuiStyleConstants.CLASS_PLUS_CIRCLE,
                PageRoleAnalysisSession.class));
        roleMenu.addMenuItem(new MenuItem("PageAdmin.menu.top.outliers",
                GuiStyleConstants.CLASS_OUTLIER_ICON,
                PageOutliers.class));
        roleMenu.addMenuItem(new MenuItem("PageAdmin.menu.role.suggestions",
                GuiStyleConstants.CLASS_OBJECT_ROLE_ICON,
                PageRoleSuggestions.class));

        return roleMenu;
    }

    private MainMenuItem createServicesItems() {
        MainMenuItem serviceMenu = createMainMenuItem("PageAdmin.menu.top.services", GuiStyleConstants.CLASS_OBJECT_SERVICE_ICON_COLORED);
        createBasicAssignmentHolderMenuItems(serviceMenu, PageTypes.SERVICE);
        return serviceMenu;
    }

    private MainMenuItem createPoliciesItems() {
        MainMenuItem serviceMenu = createMainMenuItem("PageAdmin.menu.top.policies", GuiStyleConstants.CLASS_OBJECT_POLICY_ICON_COLORED);
        createBasicAssignmentHolderMenuItems(serviceMenu, PageTypes.POLICY);
        return serviceMenu;
    }

    private MainMenuItem createResourcesItems() {
        MainMenuItem resourceMenu = createMainMenuItem(
                "PageAdmin.menu.top.resources", GuiStyleConstants.CLASS_OBJECT_RESOURCE_ICON_COLORED);

        createBasicAssignmentHolderMenuItems(resourceMenu, PageTypes.RESOURCE);

        resourceMenu.addMenuItem(new MenuItem("PageAdmin.menu.top.resources.import", PageImportResource.class));
        resourceMenu.addMenuItem(new MenuItem("PageAdmin.menu.top.connectorHosts.list", PageConnectorHosts.class));
        return resourceMenu;
    }

    private MainMenuItem createWorkItemsItems() {
        MainMenuItem casesMenu = new MainMenuItem("PageAdmin.menu.top.cases", GuiStyleConstants.EVO_CASE_THICK_ICON) {

            @Override
            public String getBubbleLabel() {
                return workItemCountModel.getObject();
            }
        };
        createBasicAssignmentHolderMenuItems(casesMenu, PageTypes.CASE);
        casesMenu.addMenuItem(new MenuItem("PageAdmin.menu.top.caseWorkItems.listAll", GuiStyleConstants.CLASS_OBJECT_WORK_ITEM_ICON, PageCaseWorkItemsAll.class));

        casesMenu.addMenuItem(new MenuItem("PageAdmin.menu.top.caseWorkItems.list", PageCaseWorkItemsAllocatedToMe.class));
        casesMenu.addMenuItem(new MenuItem("PageAdmin.menu.top.workItems.selectAttorney", PageAttorneySelection.class));

        createFocusPageViewMenu(casesMenu, "PageAdmin.menu.top.workItems.listAttorney", PageWorkItemsAttorney.class);

        casesMenu.addMenuItem(new MenuItem("PageWorkItemsClaimable.title", PageWorkItemsClaimable.class));

        createFocusPageViewMenu(casesMenu, "PageAdmin.menu.top.case.view", PageCase.class);
        createFocusPageViewMenu(casesMenu, "PageAdmin.menu.top.caseWorkItems.view", PageCaseWorkItem.class);

        return casesMenu;
    }

    private MainMenuItem createCertificationItems() {
        MainMenuItem certificationMenu = new MainMenuItem("PageAdmin.menu.top.certification", "fa fa-certificate"
        ) {

            private static final long serialVersionUID = 1L;

            @Override
            public String getBubbleLabel() {
                return certWorkItemCountModel.getObject();
            }
        };

        certificationMenu.addMenuItem(new MenuItem("PageAdmin.menu.top.certification.definitions", PageCertDefinitions.class));
        certificationMenu.addMenuItem(new MenuItem("PageAdmin.menu.top.certification.campaigns", PageCertCampaigns.class));

        if (hasNamedCertificationCollectionForTask()) {
            MenuItem menu = new MenuItem("PageAdmin.menu.top.certification.scheduling", PageTasksCertScheduling.class);
            certificationMenu.addMenuItem(menu);
        }

//        if (isFullyAuthorized()) {  // workaround for MID-5917
        certificationMenu.addMenuItem(new MenuItem("PageAdmin.menu.top.certification.certificationActiveCampaigns", PageActiveCampaigns.class) {

            @Serial private static final long serialVersionUID = 1L;

            @Override
            public boolean isMenuActive(WebPage page) {
                boolean isMenuActive = super.isMenuActive(page);
                return isMenuActive || classMatches(PageCertItems.class);
            }
        });

        boolean certItemsMenusEnabled = WebComponentUtil.isCertItemsMenusEnabled(getPageBase());
        if (certItemsMenusEnabled) {
            certificationMenu.addMenuItem(new MenuItem("PageAdmin.menu.top.certification.certificationItems", PageCertItems.class));
        }

//        }
        certificationMenu.addMenuItem(new MenuItem("PageAdmin.menu.top.certification.myCertificationActiveCampaigns", PageMyActiveCampaigns.class) {

            @Serial private static final long serialVersionUID = 1L;

            @Override
            public boolean isMenuActive(WebPage page) {
                boolean isMenuActive = super.isMenuActive(page);
                return isMenuActive || classMatches(PageMyCertItems.class);
            }
        });

        if (certItemsMenusEnabled) {
            certificationMenu.addMenuItem(new MenuItem("PageAdmin.menu.top.certification.myCertificationItems", PageMyCertItems.class));
        }

        MenuItem newCertificationMenu = new MenuItem("PageAdmin.menu.top.certification.newDefinition", GuiStyleConstants.CLASS_PLUS_CIRCLE, PageCertDefinition.class);
        certificationMenu.addMenuItem(newCertificationMenu);

        return certificationMenu;
    }

    private boolean hasNamedCertificationCollectionForTask() {
        GuiProfiledPrincipal principal = getPageBase().getPrincipal();
        if (principal == null) {
            return false;
        }

        return principal.getCompiledGuiProfile().findObjectCollectionView(TaskType.COMPLEX_TYPE, PageTasksCertScheduling.COLLECTION_NAME) != null;
    }

    private MainMenuItem createServerTasksItems() {
        MainMenuItem tasksMenu = createMainMenuItem("PageAdmin.menu.top.serverTasks", GuiStyleConstants.CLASS_OBJECT_TASK_ICON_COLORED);
        createBasicAssignmentHolderMenuItems(tasksMenu, PageTypes.TASK);
        return tasksMenu;
    }

    private MainMenuItem createNodesItems() {
        MainMenuItem nodesMenu = createMainMenuItem("PageAdmin.menu.top.nodes", GuiStyleConstants.CLASS_OBJECT_NODE_ICON_COLORED);
        nodesMenu.addMenuItem(new MenuItem("PageAdmin.menu.top.nodes.list", PageNodes.class));
        return nodesMenu;
    }

    private MainMenuItem createReportsItems() {
        MainMenuItem reportMenu = createMainMenuItem("PageAdmin.menu.top.reports", GuiStyleConstants.CLASS_REPORT_ICON);
        createBasicAssignmentHolderMenuItems(reportMenu, PageTypes.REPORT);
        reportMenu.addMenuItem(new MenuItem("PageAdmin.menu.top.reports.created", PageCreatedReports.class));
        return reportMenu;
    }

    private MainMenuItem createAuditItems() {
        return createMainMenuItem("PageAuditLogViewer.menuName", GuiStyleConstants.CLASS_AUDIT, PageAuditLogViewer.class);
    }

    private MainMenuItem createSimulationItems() {
        MainMenuItem menu = createMainMenuItem("PageAdmin.menu.top.simulationResults", GuiStyleConstants.CLASS_SIMULATION_RESULT);
        menu.addMenuItem(new MenuItem("PageAdmin.menu.top.simulationResults.list", PageSimulationResults.class));

        boolean editActive = classMatches(PageSimulationResult.class);
        if (editActive) {
            MenuItem edit = new MenuItem("PageAdmin.menu.top.simulationResults.view", PageSimulationResult.class);
            edit.setDynamic(true);
            menu.addMenuItem(edit);
        }

        return menu;
    }

    private MainMenuItem createMarkItems() {
        MainMenuItem menu = createMainMenuItem("PageAdmin.menu.top.marks", GuiStyleConstants.CLASS_MARK);
        createBasicAssignmentHolderMenuItems(menu, PageTypes.MARK);

        return menu;
    }

    private MainMenuItem createSchemaItems() {
        MainMenuItem menu = createMainMenuItem("PageAdmin.menu.top.schemas", GuiStyleConstants.CLASS_ICON_RESOURCE_SCHEMA);
        createBasicAssignmentHolderMenuItems(menu, PageTypes.SCHEMA);

        return menu;
    }

    private SideBarMenuItem createConfigurationMenu(boolean experimentalFeaturesEnabled) {
        SideBarMenuItem item = new SideBarMenuItem("PageAdmin.menu.top.configuration", experimentalFeaturesEnabled) {
            @Override
            public boolean isVisible() {
                return true;
            }
        };
        item.addMainMenuItem(createArchetypesItems());
        item.addMainMenuItem(createMessageTemplatesItems());
        item.addMainMenuItem(createObjectsCollectionItems());
        item.addMainMenuItem(createObjectTemplatesItems());
        item.addMainMenuItem(createMarkItems());
        item.addMainMenuItem(createSchemaItems());
        item.addMainMenuItem(createMainMenuItem("PageAdmin.menu.top.configuration.actions", "fa fa-bullseye", PageAction.class));
        item.addMainMenuItem(createMainMenuItem("PageAdmin.menu.top.configuration.importObject", "fa fa-upload", PageImportObject.class));
        item.addMainMenuItem(createRepositoryObjectsMenu());

        createSystemConfigurationMenu(item);

        item.addMainMenuItem(createMainMenuItem("PageAdmin.menu.top.configuration.internals", "fa fa-archive", PageInternals.class));
        item.addMainMenuItem(createMainMenuItem("PageAdmin.menu.top.configuration.repoQuery", GuiStyleConstants.CLASS_ICON_SEARCH_FLIP + " flip-icon-margin", PageRepositoryQuery.class));
        item.addMainMenuItem(createMainMenuItem("PageAdmin.menu.top.configuration.evaluateMapping", "fa fa-cog", PageEvaluateMapping.class));
        item.addMainMenuItem(createMainMenuItem("PageAdmin.menu.top.configuration.authorizationPlayground", "fa fa-cog", PageAuthorizationPlayground.class));
        item.addMainMenuItem(createMainMenuItem("PageAdmin.menu.top.configuration.about", "fa fa-info-circle", PageAbout.class));
        return item;
    }

    private SideBarMenuItem createAdditionalMenu(boolean experimentalFeaturesEnabled) {
        SideBarMenuItem menu = new SideBarMenuItem("PageAdmin.menu.additional", experimentalFeaturesEnabled);

        CompiledGuiProfile userProfile = getPageBase().getCompiledGuiProfile();
        List<RichHyperlinkType> menuList = userProfile.getAdditionalMenuLink();
        if (CollectionUtils.isEmpty(menuList)) {
            return menu;
        }

        Map<String, Class<? extends WebPage>> urlClassMap = PageMounter.getUrlClassMap();
        if (MapUtils.isEmpty(urlClassMap)) {
            return menu;
        }

        for (RichHyperlinkType link : menuList) {
            if (StringUtils.isBlank(link.getTargetUrl())) {
                continue;
            }

            //noinspection unchecked
            AdditionalMenuItem item = new AdditionalMenuItem(link,
                    (Class<? extends PageBase>) urlClassMap.get(link.getTargetUrl()));
            menu.addMainMenuItem(item);
        }
        return menu;
    }

    private void createBasicAssignmentHolderMenuItems(MainMenuItem mainMenuItem, PageTypes pageDesc) {
        String label = "PageAdmin.menu.top." + pageDesc.getIdentifier() + ".list";
        String icon = pageDesc.getIcon();
        Class<? extends PageBase> page = pageDesc.getListClass();
        boolean isDefaultViewVisible = true;

        CompiledObjectCollectionView defaultView = getPageBase().getCompiledGuiProfile().findAllApplicableObjectCollectionViews(pageDesc.getTypeName()).stream()
                .filter(view -> view.isDefaultView()).findFirst().orElse(null);

        if (defaultView != null) {
            isDefaultViewVisible = WebComponentUtil.getElementVisibility(defaultView.getVisibility());
            if (isDefaultViewVisible) {
                DisplayType viewDisplayType = defaultView.getDisplay();

                PolyStringType display = WebComponentUtil.getCollectionLabel(viewDisplayType);
                if (StringUtils.isNotEmpty(getLabelTranslationKey(display))) {
                    label = getLabelTranslationKey(display);
                }

                String iconClass = GuiDisplayTypeUtil.getIconCssClass(viewDisplayType);
                if (StringUtils.isNotEmpty(iconClass)) {
                    icon = iconClass;
                }
            }
        }

        if (isDefaultViewVisible) {
            mainMenuItem.addMenuItem(createObjectListPageMenuItem(label, icon, page));
        }

        addCollectionsMenuItems(mainMenuItem, pageDesc.getTypeName(), pageDesc.getListClass());

        if (PageTypes.CASE != pageDesc) {
            createFocusPageNewEditMenu(mainMenuItem, "PageAdmin.menu.top." + pageDesc.getIdentifier() + ".new",
                    "PageAdmin.menu.top." + pageDesc.getIdentifier() + ".edit", getDetailsPage(pageDesc));
        }
    }

    private String getLabelTranslationKey(PolyStringType label) {
        if (label == null || label.getTranslation() == null) {
            return null;
        }
        return label.getTranslation().getKey();
    }

    private Class<? extends PageBase> getDetailsPage(PageTypes pageDesc) {
        return pageDesc.getDetailsPage();
    }

    private boolean isEditForAdminObjectDetails() {
        PageBase pageBase = getPageBase();
        if (pageBase instanceof AbstractPageObjectDetails) {
            AbstractPageObjectDetails<?, ?> page = (AbstractPageObjectDetails<?, ?>) pageBase;
            return page.isEditObject();
        }
        return false;
    }

    private void createFocusPageNewEditMenu(MainMenuItem mainMenuItem, String newKey, String editKey,
            final Class<? extends PageBase> newPageClass) {

        boolean addActive = classMatches(newPageClass) && !isEditForAdminObjectDetails();// && !isEditForResourceWizzard();
        MenuItem newMenu = new MenuItem(newKey,
                GuiStyleConstants.CLASS_PLUS_CIRCLE, newPageClass, null, addActive);
        mainMenuItem.addMenuItem(newMenu);

        boolean editActive = classMatches(newPageClass) && (isEditForAdminObjectDetails());// || isEditForResourceWizzard());
        if (editActive) {
            MenuItem edit = new MenuItem(editKey, newPageClass);
            edit.setDynamic(true);
            mainMenuItem.addMenuItem(edit);
        }
    }

    private boolean classMatches(Class<? extends PageBase> page) {
        return getPageBase().getClass().equals(page);
    }

    private void createFocusPageViewMenu(MainMenuItem mainMenuItem, String viewKey, final Class<? extends PageBase> newPageType) {
        boolean editActive = classMatches(newPageType);
        if (editActive) {
            MenuItem editMenuItem = new MenuItem(viewKey, newPageType);
            editMenuItem.setDynamic(true);
            mainMenuItem.addMenuItem(editMenuItem);
        }
    }

    private MainMenuItem createMessageTemplatesItems() {
        MainMenuItem item = new MainMenuItem("PageAdmin.menu.top.messageTemplates", GuiStyleConstants.EVO_MESSAGE_TEMPLATE_TYPE_ICON);
        createBasicAssignmentHolderMenuItems(item, PageTypes.MESSAGE_TEMPLATES);
        return item;
    }

    private MainMenuItem createArchetypesItems() {
        MainMenuItem item = new MainMenuItem("PageAdmin.menu.top.archetypes", GuiStyleConstants.EVO_ARCHETYPE_TYPE_ICON);
        createBasicAssignmentHolderMenuItems(item, PageTypes.ARCHETYPE);
        return item;
    }

    private MainMenuItem createObjectsCollectionItems() {
        MainMenuItem item = new MainMenuItem("PageAdmin.menu.top.objectCollections", GuiStyleConstants.CLASS_OBJECT_COLLECTION_ICON);
        createBasicAssignmentHolderMenuItems(item, PageTypes.OBJECT_COLLECTION);
        return item;
    }

    private MainMenuItem createObjectTemplatesItems() {
        MainMenuItem item = new MainMenuItem("PageAdmin.menu.top.objectTemplates", GuiStyleConstants.CLASS_OBJECT_TEMPLATE_ICON);
        createBasicAssignmentHolderMenuItems(item, PageTypes.OBJECT_TEMPLATE);
        return item;
    }

    private MainMenuItem createRepositoryObjectsMenu() {
        MainMenuItem repositoryObjectsMenu = createMainMenuItem("PageAdmin.menu.top.configuration.repositoryObjects", "fa fa-file-alt");
        repositoryObjectsMenu.addMenuItem(new MenuItem("PageAdmin.menu.top.configuration.repositoryObjectsList", PageDebugList.class));
        boolean editActive = classMatches(PageDebugView.class);
        if (editActive) {
            MenuItem editMenuItem = new MenuItem("PageAdmin.menu.top.configuration.repositoryObjectView", PageDebugView.class);
            editMenuItem.setDynamic(true);
            repositoryObjectsMenu.addMenuItem(editMenuItem);
        }
        return repositoryObjectsMenu;
    }

    private MenuItem createObjectListPageMenuItem(String key, String iconClass, Class<? extends PageBase> menuItemPage) {

        return new MenuItem(key, iconClass, menuItemPage) {
            @Override
            public boolean isMenuActive(WebPage page) {
                PageParameters pageParameters = getPageBase().getPageParameters();
                if (!page.getClass().equals(this.getPageClass()) || pageParameters != null && pageParameters.get(PageBase.PARAMETER_OBJECT_COLLECTION_NAME) != null
                        && StringUtils.isNotEmpty(pageParameters.get(PageBase.PARAMETER_OBJECT_COLLECTION_NAME).toString())
                        && !pageParameters.get(PageBase.PARAMETER_OBJECT_COLLECTION_NAME).toString().equals("null")) {
                    return false;
                } else {
                    return super.isMenuActive(page);
                }
            }
        };
    }

    private void addCollectionsMenuItems(MainMenuItem mainMenuItem, QName type, Class<? extends PageBase> redirectToPage) {
        List<CompiledObjectCollectionView> objectViews = getPageBase().getCompiledGuiProfile().findAllApplicableObjectCollectionViews(type);

        List<MenuItem> items = objectViews.stream().map(objectView -> {
            if (objectView.isDefaultView() || !WebComponentUtil.getElementVisibility(objectView.getVisibility())) {
                return null;
            }

            OperationTypeType operationTypeType = objectView.getApplicableForOperation();
            if (operationTypeType != null && operationTypeType != OperationTypeType.MODIFY) {
                return null;
            }

            if (objectView.isDefaultView()) {
                return null;
            }
            DisplayType viewDisplayType = objectView.getDisplay();

            PageParameters pageParameters = new PageParameters();
            pageParameters.add(PageBase.PARAMETER_OBJECT_COLLECTION_NAME, objectView.getViewIdentifier());

            String label = "MenuItem.noName";
            PolyStringType display = WebComponentUtil.getCollectionLabel(viewDisplayType);
            if (display != null) {
                label = WebComponentUtil.getTranslatedPolyString(display);
            }

            String iconClass = GuiDisplayTypeUtil.getIconCssClass(viewDisplayType);
            MenuItem menuItem = new MenuItem(label,
                    StringUtils.isEmpty(iconClass) ? BaseMenuItem.DEFAULT_ICON : iconClass, redirectToPage, pageParameters, isObjectCollectionMenuActive(objectView));
            menuItem.setDisplayOrder(objectView.getDisplayOrder());

            return menuItem;
        }).filter(i -> i != null).collect(Collectors.toList());

        items.sort(
                Comparator.comparing(MenuItem::getDisplayOrder, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(MenuItem::getNameModel, Comparator.nullsLast(Comparator.naturalOrder())));

        items.forEach(i -> mainMenuItem.addCollectionMenuItem(i));
    }

    private boolean isObjectCollectionMenuActive(CompiledObjectCollectionView objectView) {
        PageParameters params = getPageBase().getPageParameters();
        if (params == null) {
            return false;
        }
        StringValue collectionNameParam = params.get(PageBase.PARAMETER_OBJECT_COLLECTION_NAME);
        if (collectionNameParam.isEmpty()) {
            return false;
        }

        return collectionNameParam.toString().equals(objectView.getViewIdentifier());
    }

    private void createSystemConfigurationMenu(SideBarMenuItem item) {
        MainMenuItem system = new MainMenuItem(
                "PageAdmin.menu.top.configuration.basic",
                GuiStyleConstants.CLASS_SYSTEM_CONFIGURATION_ICON,
                com.evolveum.midpoint.gui.impl.page.admin.systemconfiguration.PageSystemConfiguration.class) {
            @Override
            public boolean isVisible() {
                return true;
            }
        };
        PageBase page = getPageBase();
        if (page != null && PageBaseSystemConfiguration.class.isAssignableFrom(page.getClass())) {

            // title key is not nice - model should be sent there...
            MenuItem menuItem = new MenuItem(page.getClass().getSimpleName() + ".title", page.getClass(), new PageParameters());
            system.addMenuItem(menuItem);
        }
        item.addMainMenuItem(system);
    }

    private MainMenuItem createMainMenuItem(String key, String icon) {
        return new MainMenuItem(key, icon);
    }

    private MainMenuItem createMainMenuItem(String key, String icon, Class<? extends PageBase> page) {
        return new MainMenuItem(key, icon, page);
    }

    private MainMenuItem createMainMenuItem(String key, String icon, Class<? extends PageBase> page, PageParameters params) {
        return new MainMenuItem(key, icon, page, params);
    }

    public List<SideBarMenuItem> getItems() {
        SideBarMenuPanel sideBarMenuPanel = (SideBarMenuPanel) get(ID_MENU);
        return sideBarMenuPanel.getModelObject();
    }
}
