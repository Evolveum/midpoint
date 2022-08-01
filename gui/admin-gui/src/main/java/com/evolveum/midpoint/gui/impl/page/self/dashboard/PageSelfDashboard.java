/*
 * Copyright (C) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.self.dashboard;

import static com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractWorkItemType.F_CREATE_TIMESTAMP;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.gui.impl.page.admin.user.UserDetailsModel;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.security.api.MidPointPrincipal;
import com.evolveum.midpoint.security.api.SecurityUtil;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.web.security.util.SecurityUtils;

import org.apache.wicket.Component;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.authentication.api.authorization.AuthorizationAction;
import com.evolveum.midpoint.authentication.api.authorization.PageDescriptor;
import com.evolveum.midpoint.authentication.api.authorization.Url;
import com.evolveum.midpoint.authentication.api.util.AuthUtil;
import com.evolveum.midpoint.cases.api.util.QueryUtils;
import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.page.self.dashboard.component.MyAccessesPreviewDataPanel;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.page.admin.cases.CaseWorkItemsPanel;
import com.evolveum.midpoint.web.page.admin.server.CasesTablePanel;
import com.evolveum.midpoint.web.page.self.PageSelf;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.wicket.model.LoadableDetachableModel;

@PageDescriptor(
        urls = {
                @Url(mountUrl = "/self", matchUrlForSecurity = "/self"),
                @Url(mountUrl = "/self/dashboard")
        },
        action = {
                @AuthorizationAction(actionUri = PageSelf.AUTH_SELF_ALL_URI,
                        label = PageSelf.AUTH_SELF_ALL_LABEL,
                        description = PageSelf.AUTH_SELF_ALL_DESCRIPTION),
                @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_SELF_DASHBOARD_URL,
                        label = "PageSelfDashboard.auth.dashboard.label",
                        description = "PageSelfDashboard.auth.dashboard.description")
        })
public class PageSelfDashboard extends PageSelf {

    private static final Trace LOGGER = TraceManager.getTrace(PageSelfDashboard.class);
    private static final String ID_SEARCH_PANEL = "searchPanel";
    private static final String ID_LINKS_PANEL = "linksPanel";
    private static final String ID_LINK_ITEM = "linkItem";
    private static final String ID_OBJECT_COLLECTION_VIEW_WIDGETS_PANEL = "objectCollectionViewWidgetsPanel";
    private static final String ID_OBJECT_COLLECTION_VIEW_WIDGET = "objectCollectionViewWidget";

    private static final String ID_TITLE = "title";



    private static final String DOT_CLASS = PageSelfDashboard.class.getName() + ".";


    public PageSelfDashboard() {
        initLayout();
    }

    private void initLayout() {
        DashboardSearchPanel dashboardSearchPanel = new DashboardSearchPanel(ID_SEARCH_PANEL);
        List<String> searchPanelActions = Arrays.asList(AuthorizationConstants.AUTZ_UI_USERS_ALL_URL,
                AuthorizationConstants.AUTZ_UI_USERS_URL, AuthorizationConstants.AUTZ_UI_RESOURCES_ALL_URL,
                AuthorizationConstants.AUTZ_UI_RESOURCES_URL, AuthorizationConstants.AUTZ_UI_TASKS_ALL_URL,
                AuthorizationConstants.AUTZ_UI_TASKS_URL);
        dashboardSearchPanel.add(new VisibleBehaviour(() -> {
//            UserInterfaceElementVisibilityType visibility = getComponentVisibility(PredefinedDashboardWidgetId.SEARCH);
//            return WebComponentUtil.getElementVisibility(visibility, searchPanelActions);
            return true;
        }));
        add(dashboardSearchPanel);

        ListView<RichHyperlinkType> linksPanel = new ListView<>(ID_LINKS_PANEL, () -> loadLinksList()) {

            private static final long serialVersionUID = 1L;

            @Override
            protected void populateItem(ListItem<RichHyperlinkType> item) {
                item.add(new DashboardLinkComponent(ID_LINK_ITEM, item.getModel()));
            }
        };
        linksPanel.setOutputMarkupId(true);
        linksPanel.add(new VisibleBehaviour(() -> {
//            UserInterfaceElementVisibilityType visibility = getComponentVisibility(PredefinedDashboardWidgetId.SHORTCUTS);
//            return WebComponentUtil.getElementVisibility(visibility);
            return true;
        }));
        add(linksPanel);


        initPreviewWidgets();
     }

     private void initPreviewWidgets() {
         //TODO compile default config + prepare config to default roles.
//         List<ContainerPanelConfigurationType> configs = new ArrayList<>();
//         configs.add(createPanelConfig("myAccesses", AssignmentType.COMPLEX_TYPE, createDisplayType("My Access", "col-md-4", GuiStyleConstants.EVO_ASSIGNMENT_ICON)));
//         configs.add(createPanelConfig("myRequests", CaseType.COMPLEX_TYPE, createDisplayType("My Requests", "col-md-8", GuiStyleConstants.EVO_CASE_OBJECT_ICON)));
//         configs.add(createPanelConfig("myWorkItems", CaseWorkItemType.COMPLEX_TYPE, createDisplayType("My Work Items", "col-md-6", GuiStyleConstants.CLASS_OBJECT_WORK_ITEM_ICON)));


         HomePageType homePageType = getCompiledGuiProfile().getHomePage();
         ListView<ContainerPanelConfigurationType> viewWidgetsPanel = new ListView<>(ID_OBJECT_COLLECTION_VIEW_WIDGETS_PANEL, () -> homePageType.getWidget()) {

             @Override
             protected void populateItem(ListItem<ContainerPanelConfigurationType> item) {
                 Component widget = createWidget(ID_OBJECT_COLLECTION_VIEW_WIDGET, item.getModel());
                 //TODO use model!
                 widget.add(AttributeAppender.append("class", item.getModelObject().getDisplay().getCssClass()));
                 item.add(widget);
             }
         };
         viewWidgetsPanel.setOutputMarkupId(true);
         viewWidgetsPanel.add(new VisibleBehaviour(() -> {
//            UserInterfaceElementVisibilityType visibility = getComponentVisibility(PredefinedDashboardWidgetId.SHORTCUTS);
//            return WebComponentUtil.getElementVisibility(visibility);
             return true;
         }));
         add(viewWidgetsPanel);
     }

     private DisplayType createDisplayType(String label, String cssClass, String icon) {
         return new DisplayType().label(label).cssClass(cssClass).beginIcon().cssClass(icon).end();
     }

     private ContainerPanelConfigurationType createPanelConfig(String panelType, QName type, DisplayType displayType) {
        ContainerPanelConfigurationType config = new ContainerPanelConfigurationType();
        config.setDisplay(displayType);
        config.setType(type);
        config.setPanelType(panelType);
        return config;
     }

    private List<RichHyperlinkType> loadLinksList() {
        return ((PageBase) getPage()).getCompiledGuiProfile().getUserDashboardLink();
    }

    private LoadableDetachableModel<PrismObject<UserType>> createSelfModel() {
        return new LoadableDetachableModel<>() {
            @Override
            protected PrismObject<UserType> load() {
                MidPointPrincipal principal;
                try {
                    principal = SecurityUtil.getPrincipal();
                } catch (SecurityViolationException e) {
                    LOGGER.error("Cannot load logged in focus");
                    return null;
                }
                return (PrismObject<UserType>) principal.getFocus().asPrismObject();
            }
        };
    }

        // TODO just a prototype, should be initialized using refrection? or factory?
    private Component createWidget(String markupId, IModel<ContainerPanelConfigurationType> model) {
        ContainerPanelConfigurationType config = model.getObject();
        String panelType = config.getPanelType();
        Class<? extends Panel> panelClass = findObjectPanel(panelType);

        UserDetailsModel userDetailsModel = new UserDetailsModel(createSelfModel(), PageSelfDashboard.this);

        Panel panel = WebComponentUtil.createPanel(panelClass, markupId, userDetailsModel, config);
        if (panel == null) {
            return new WebMarkupContainer(markupId);
        }

        return panel;


//        Class<?> type = WebComponentUtil.qnameToClass(PrismContext.get(), model.getObject().getType());
//        if (AssignmentType.class.equals(type)) {
//            return new MyAccessesPreviewDataPanel(markupId, null, model.getObject());
//        }
//
//        if (CaseType.class.equals(type)) {
//            return createMyRequestsPanel(markupId, model.getObject());
//        }
//
//        if (CaseWorkItemType.class.equals(type)) {
//            return createMyWorkItemsPanel(markupId, model.getObject());
//        }
//
//        return new WebMarkupContainer(markupId);
    }

//    private Component createMyWorkItemsPanel(String markupId, ContainerPanelConfigurationType config) {
//        CaseWorkItemsPanel workItemsPanel = new CaseWorkItemsPanel(markupId, CaseWorkItemsPanel.View.DASHBOARD, config) {
//            private static final long serialVersionUID = 1L;
//
//            @Override
//            protected ObjectFilter getCaseWorkItemsFilter() {
//                return QueryUtils.filterForNotClosedStateAndAssignees(getPrismContext().queryFor(CaseWorkItemType.class),
//                                AuthUtil.getPrincipalUser(),
//                                OtherPrivilegesLimitationType.F_APPROVAL_WORK_ITEMS, getPageBase().getRelationRegistry())
//                        .desc(F_CREATE_TIMESTAMP)
//                        .buildFilter();
//            }
//        };
//        workItemsPanel.setOutputMarkupId(true);
//        return workItemsPanel;
//    }

    private Component createMyRequestsPanel(String markupId, ContainerPanelConfigurationType config) {
        CasesTablePanel cases =  new CasesTablePanel(markupId, null, config) {

            @Override
            protected ObjectFilter getCasesFilter() {
                return QueryUtils.filterForMyRequests(getPrismContext().queryFor(CaseType.class),
                                AuthUtil.getPrincipalUser().getOid())
                        .desc(ItemPath.create(CaseType.F_METADATA, MetadataType.F_CREATE_TIMESTAMP))
                        .buildFilter();
            }

            @Override
            protected boolean isDashboard() {
                return true;
            }

            @Override
            protected UserProfileStorage.TableId getTableId() {
                return UserProfileStorage.TableId.PAGE_CASE_CHILD_CASES_TAB;
            }
        };
        cases.setDashboard(true);
        return cases;
    }


}
