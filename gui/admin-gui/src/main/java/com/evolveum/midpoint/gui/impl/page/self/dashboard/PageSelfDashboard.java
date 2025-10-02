/*
 * Copyright (C) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.self.dashboard;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import com.evolveum.midpoint.gui.api.PredefinedDashboardWidgetId;
import com.evolveum.midpoint.gui.api.component.result.MessagePanel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.impl.page.admin.user.UserDetailsModel;

import com.evolveum.midpoint.gui.impl.page.self.dashboard.component.DashboardSearchPanel;
import com.evolveum.midpoint.gui.impl.page.self.dashboard.component.StatisticDashboardWidget;
import com.evolveum.midpoint.model.api.authentication.CompiledGuiProfile;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.security.api.MidPointPrincipal;
import com.evolveum.midpoint.security.api.SecurityUtil;
import com.evolveum.midpoint.util.exception.SecurityViolationException;

import com.evolveum.midpoint.web.component.form.MidpointForm;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.wicket.Component;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.Form;
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

    private static final String LINK_WIDGET_IDENTIFIER = "linkWidget";


    public PageSelfDashboard() {
        initLayout();
    }

    private void initLayout() {
        MidpointForm<?> mainForm = new MidpointForm<>("mainForm");
        add(mainForm);

        DashboardSearchPanel dashboardSearchPanel = new DashboardSearchPanel(ID_SEARCH_PANEL);
        List<String> searchPanelActions = Arrays.asList(AuthorizationConstants.AUTZ_UI_USERS_ALL_URL,
                AuthorizationConstants.AUTZ_UI_USERS_URL, AuthorizationConstants.AUTZ_UI_RESOURCES_ALL_URL,
                AuthorizationConstants.AUTZ_UI_RESOURCES_URL, AuthorizationConstants.AUTZ_UI_TASKS_ALL_URL,
                AuthorizationConstants.AUTZ_UI_TASKS_URL);
        dashboardSearchPanel.add(new VisibleBehaviour(() -> {
            UserInterfaceElementVisibilityType visibility = getComponentVisibility(PredefinedDashboardWidgetId.SEARCH);
            return WebComponentUtil.getElementVisibility(visibility, searchPanelActions);
        }));
        mainForm.add(dashboardSearchPanel);

        initStatisticWidgets(mainForm);

        initPreviewWidgets(mainForm);
     }

     private void initStatisticWidgets(Form mainForm) {
        List<PreviewContainerPanelConfigurationType> statisticWidgets = getStatisticWidgetList();
         ListView<PreviewContainerPanelConfigurationType> statisticWidgetsPanel = new ListView<>(ID_STATISTIC_WIDGETS_PANEL,
                 statisticWidgets) {

             private static final long serialVersionUID = 1L;

             @Override
             protected void populateItem(ListItem<PreviewContainerPanelConfigurationType> item) {
                 StatisticDashboardWidget widget = new StatisticDashboardWidget(ID_STATISTIC_WIDGET, item.getModel());
                 widget.add(new VisibleBehaviour(() -> WebComponentUtil.getElementVisibility(item.getModelObject().getVisibility())));
                 item.add(widget);
             }
         };
         statisticWidgetsPanel.setOutputMarkupId(true);
         statisticWidgetsPanel.add(new VisibleBehaviour(() -> CollectionUtils.isNotEmpty(statisticWidgets)));
         mainForm.add(statisticWidgetsPanel);

     }

     private void initPreviewWidgets(Form mainForm) {
         UserDetailsModel userDetailsModel = new UserDetailsModel(createSelfModel(), PageSelfDashboard.this) {

             @Override
             public List<? extends ContainerPanelConfigurationType> getPanelConfigurations() {
                 return getCompiledGuiProfile().getHomePage().getWidget();
             }
         };

         List<PreviewContainerPanelConfigurationType> previewWidgets = getNonStatisticWidgetList();
         ListView<PreviewContainerPanelConfigurationType> viewWidgetsPanel = new ListView<>(ID_OBJECT_COLLECTION_VIEW_WIDGETS_PANEL, previewWidgets) {

             @Override
             protected void populateItem(ListItem<PreviewContainerPanelConfigurationType> item) {
                 Component widget = createWidget(ID_OBJECT_COLLECTION_VIEW_WIDGET, item.getModel(), userDetailsModel);
                 widget.add(new VisibleBehaviour(() -> WebComponentUtil.getElementVisibility(item.getModelObject().getVisibility())));
                 widget.add(AttributeAppender.append("class", getWidgetCssClassModel(item.getModelObject())));
                 item.add(widget);
             }
         };
         viewWidgetsPanel.setOutputMarkupId(true);
         viewWidgetsPanel.add(new VisibleBehaviour(() -> CollectionUtils.isNotEmpty(previewWidgets)));
         mainForm.add(viewWidgetsPanel);
     }

     private IModel<String> getWidgetCssClassModel(PreviewContainerPanelConfigurationType panelConfig) {
        return () -> {
            if (panelConfig == null || panelConfig.getDisplay() == null) {
                return "col-6"; //default 6
            }
            String displayType = panelConfig.getDisplay().getCssClass();
            return displayType == null ? "col-6" : displayType;
        };
     }

     private List<PreviewContainerPanelConfigurationType> getStatisticWidgetList() {
         HomePageType homePageType = getCompiledGuiProfile().getHomePage();
         List<PreviewContainerPanelConfigurationType> allWidgetList = homePageType != null ? homePageType.getWidget() : null;
         if (allWidgetList == null) {
             return null;
         }
         return allWidgetList.stream().filter(w -> LINK_WIDGET_IDENTIFIER.equals(w.getPanelType())).collect(Collectors.toList());
     }

     private List<PreviewContainerPanelConfigurationType> getNonStatisticWidgetList() {
         HomePageType homePageType = getCompiledGuiProfile().getHomePage();
         List<PreviewContainerPanelConfigurationType> allWidgetList = homePageType != null ? homePageType.getWidget() : null;
         if (allWidgetList == null) {
             return Collections.emptyList();
         }
         return allWidgetList.stream().filter(w -> w.getPanelType() != null &&
                 !LINK_WIDGET_IDENTIFIER.equals(w.getPanelType())).collect(Collectors.toList());
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
                return (PrismObject<UserType>) principal.getFocus().asPrismObject().clone();
            }
        };
    }

    private Component createWidget(String markupId, IModel<PreviewContainerPanelConfigurationType> model, UserDetailsModel userDetailsModel) {
        ContainerPanelConfigurationType config = model.getObject();
        String panelType = config.getPanelType();

        if (panelType == null && LOGGER.isDebugEnabled()) {
            //No panel defined, just grouping element, e.g. top "Assignments" in details navigation menu
            LOGGER.debug("AbstractPageObjectDetails.panelTypeUndefined {}", config.getIdentifier());
            return new WebMarkupContainer(markupId);
        }

        Class<? extends Panel> panelClass = findObjectPanel(panelType);
        if (panelClass == null) {
            //panel type defined, but no class found. Something strange happened.
            return createMessagePanel(markupId, MessagePanel.MessagePanelType.ERROR,"AbstractPageObjectDetails.panelTypeUndefined", config.getIdentifier());
        }

        Component panel = WebComponentUtil.createPanel(panelClass, markupId, userDetailsModel, config);
        if (panel == null) {
            return createMessagePanel(markupId, MessagePanel.MessagePanelType.ERROR,"AbstractPageObjectDetails.panelTypeUndefined", config.getIdentifier());
        }

        return panel;
    }

    private UserInterfaceElementVisibilityType getComponentVisibility(PredefinedDashboardWidgetId componentId) {
        CompiledGuiProfile compiledGuiProfile = getCompiledGuiProfile();
        if (compiledGuiProfile.getHomePage() == null) {
            return UserInterfaceElementVisibilityType.AUTOMATIC;
        }
        List<PreviewContainerPanelConfigurationType> widgetsList = compiledGuiProfile.getHomePage().getWidget();
        if (CollectionUtils.isEmpty(widgetsList)) {
            return UserInterfaceElementVisibilityType.VACANT;
        }
        String widgetIdentifier = componentId.getIdentifier();
        PreviewContainerPanelConfigurationType widget = widgetsList
                .stream()
                .filter(w -> widgetIdentifier.equals(w.getIdentifier()))
                .findFirst()
                .orElse(null);
        if (widget == null) {
            return UserInterfaceElementVisibilityType.VACANT;
        }
        return widget.getVisibility();
    }

    private List<RichHyperlinkType> loadLinksList() {
        return ((PageBase) getPage()).getCompiledGuiProfile().getUserDashboardLink();
    }

}
