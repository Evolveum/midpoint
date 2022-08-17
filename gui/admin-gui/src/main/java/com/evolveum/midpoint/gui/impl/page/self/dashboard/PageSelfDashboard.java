/*
 * Copyright (C) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.self.dashboard;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import com.evolveum.midpoint.gui.api.PredefinedDashboardWidgetId;
import com.evolveum.midpoint.gui.impl.page.admin.user.UserDetailsModel;

import com.evolveum.midpoint.gui.impl.page.self.dashboard.component.DashboardSearchPanel;
import com.evolveum.midpoint.gui.impl.page.self.dashboard.component.StatisticDashboardWidget;
import com.evolveum.midpoint.model.api.authentication.CompiledGuiProfile;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.security.api.MidPointPrincipal;
import com.evolveum.midpoint.security.api.SecurityUtil;
import com.evolveum.midpoint.util.exception.SecurityViolationException;

import org.apache.commons.collections4.CollectionUtils;
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
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.page.self.PageSelf;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;

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
    private static final String ID_STATISTIC_WIDGETS_PANEL = "statisticWidgetsPanel";
    private static final String ID_STATISTIC_WIDGET = "statisticWidget";
    private static final String ID_OBJECT_COLLECTION_VIEW_WIDGETS_PANEL = "objectCollectionViewWidgetsPanel";
    private static final String ID_OBJECT_COLLECTION_VIEW_WIDGET = "objectCollectionViewWidget";

    private static final String STATISTIC_WIDGET_IDENTIFIER = "statisticWidget";


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
            UserInterfaceElementVisibilityType visibility = getComponentVisibility(PredefinedDashboardWidgetId.SEARCH);
            return WebComponentUtil.getElementVisibility(visibility, searchPanelActions);
        }));
        add(dashboardSearchPanel);

        initStatisticWidgets();

        initPreviewWidgets();
     }

     private void initStatisticWidgets() {
         ListView<ContainerPanelConfigurationType> linksPanel = new ListView<>(ID_STATISTIC_WIDGETS_PANEL, this::getStatisticWidgetList) {

             private static final long serialVersionUID = 1L;

             @Override
             protected void populateItem(ListItem<ContainerPanelConfigurationType> item) {
                 StatisticDashboardWidget widget = new StatisticDashboardWidget(ID_STATISTIC_WIDGET, item.getModel());
                 widget.add(new VisibleBehaviour(() -> WebComponentUtil.getElementVisibility(item.getModelObject().getVisibility())));
                 item.add(widget);
             }
         };
         linksPanel.setOutputMarkupId(true);
         linksPanel.add(new VisibleBehaviour(() -> {
             UserInterfaceElementVisibilityType visibility = getComponentVisibility(PredefinedDashboardWidgetId.SHORTCUTS);
             return WebComponentUtil.getElementVisibility(visibility);
         }));
         add(linksPanel);
     }

     private void initPreviewWidgets() {
         ListView<ContainerPanelConfigurationType> viewWidgetsPanel = new ListView<>(ID_OBJECT_COLLECTION_VIEW_WIDGETS_PANEL, this::getNonStatisticWidgetList) {

             @Override
             protected void populateItem(ListItem<ContainerPanelConfigurationType> item) {
                 Component widget = createWidget(ID_OBJECT_COLLECTION_VIEW_WIDGET, item.getModel());
                 widget.add(new VisibleBehaviour(() -> WebComponentUtil.getElementVisibility(item.getModelObject().getVisibility())));
                 widget.add(AttributeAppender.append("class", getWidgetCssClassModel(item.getModelObject())));
                 item.add(widget);
             }
         };
         viewWidgetsPanel.setOutputMarkupId(true);
         viewWidgetsPanel.add(new VisibleBehaviour(() -> {
            UserInterfaceElementVisibilityType visibility = getComponentVisibility(PredefinedDashboardWidgetId.PREVIEW_WIDGETS);
            return getCompiledGuiProfile().getHomePage() != null && WebComponentUtil.getElementVisibility(visibility);
         }));
         add(viewWidgetsPanel);
     }

     private IModel<String> getWidgetCssClassModel(ContainerPanelConfigurationType panelConfig) {
        if (panelConfig == null || panelConfig.getDisplay() == null) {
            return Model.of();
        }
        return Model.of(panelConfig.getDisplay().getCssClass());
     }

     private List<ContainerPanelConfigurationType> getStatisticWidgetList() {
         HomePageType homePageType = getCompiledGuiProfile().getHomePage();
         List<ContainerPanelConfigurationType> allWidgetList = homePageType != null ? homePageType.getWidget() : null;
         if (allWidgetList == null) {
             return null;
         }
         return allWidgetList.stream().filter(w -> STATISTIC_WIDGET_IDENTIFIER.equals(w.getPanelType())).collect(Collectors.toList());
     }

     private List<ContainerPanelConfigurationType> getNonStatisticWidgetList() {
         HomePageType homePageType = getCompiledGuiProfile().getHomePage();
         List<ContainerPanelConfigurationType> allWidgetList = homePageType != null ? homePageType.getWidget() : null;
         if (allWidgetList == null) {
             return null;
         }
         return allWidgetList.stream().filter(w -> !STATISTIC_WIDGET_IDENTIFIER.equals(w.getPanelType())).collect(Collectors.toList());
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
    }

    private UserInterfaceElementVisibilityType getComponentVisibility(PredefinedDashboardWidgetId componentId) {
        CompiledGuiProfile compiledGuiProfile = getCompiledGuiProfile();
        if (compiledGuiProfile.getUserDashboard() == null) {
            return UserInterfaceElementVisibilityType.AUTOMATIC;
        }
        List<DashboardWidgetType> widgetsList = compiledGuiProfile.getUserDashboard().getWidget();
        if (CollectionUtils.isEmpty(widgetsList)) {
            return UserInterfaceElementVisibilityType.VACANT;
        }
        HomePageType homePage = compiledGuiProfile.getHomePage();
        List<ContainerPanelConfigurationType> containerPanelWidgets = homePage == null ? null : compiledGuiProfile.getHomePage().getWidget();
        if (CollectionUtils.isEmpty(containerPanelWidgets)) {
            return UserInterfaceElementVisibilityType.VACANT;
        }
        DashboardWidgetType widget = compiledGuiProfile.findUserDashboardWidget(componentId.getUri());
        if (widget == null || widget.getVisibility() == null) {
            return UserInterfaceElementVisibilityType.HIDDEN;
        } else {
            return widget.getVisibility();
        }
    }

}
