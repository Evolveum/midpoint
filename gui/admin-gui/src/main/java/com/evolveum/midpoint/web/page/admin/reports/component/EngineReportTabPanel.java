/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.page.admin.reports.component;

import com.evolveum.midpoint.gui.api.component.DisplayNamePanel;
import com.evolveum.midpoint.gui.api.component.tabs.PanelTab;
import com.evolveum.midpoint.gui.api.prism.wrapper.*;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.component.BasicMultivalueContainerListPanel;
import com.evolveum.midpoint.gui.impl.component.MultivalueContainerDetailsPanel;
import com.evolveum.midpoint.gui.impl.component.data.column.AbstractItemWrapperColumn.ColumnType;
import com.evolveum.midpoint.gui.impl.component.data.column.PrismPropertyWrapperColumn;
import com.evolveum.midpoint.gui.impl.factory.panel.ItemRealValueModel;
import com.evolveum.midpoint.gui.impl.prism.panel.SingleContainerPanel;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.ObjectBasicPanel;
import com.evolveum.midpoint.web.component.TabbedPanel;
import com.evolveum.midpoint.web.component.prism.ItemVisibility;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.model.PrismContainerWrapperModel;
import com.evolveum.midpoint.web.model.PrismPropertyWrapperModel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.wicket.Component;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author skublik
 */

public class EngineReportTabPanel extends ObjectBasicPanel<ReportType> {

    private static final Trace LOGGER = TraceManager.getTrace(EngineReportTabPanel.class);

    private static final String ID_TABS = "tabsPanel";
    private static final String ID_LABEL = "label";

    public EngineReportTabPanel(String id, IModel<PrismObjectWrapper<ReportType>> model) {
        super(id, model);
    }

    @Override
    protected void initLayout() {
        List<ITab> tabs = createTabs();
        Label label = new Label(ID_LABEL, (IModel) () -> getEngineLabel());
        label.add(new VisibleBehaviour(() -> hasReportArchetype()));
        label.setOutputMarkupId(true);
        add(label);
        TabbedPanel tabbedPanel = WebComponentUtil.createTabPanel(ID_TABS, getPageBase(), tabs, null);
        tabbedPanel.add(new VisibleBehaviour(() -> hasReportArchetype()));
        tabbedPanel.setOutputMarkupId(true);
        add(tabbedPanel);
    }

    private String getEngineLabel() {
        String label = "";
        if (WebComponentUtil.hasArchetypeAssignment(getReport(), SystemObjectsType.ARCHETYPE_COLLECTION_REPORT.value())) {
            ItemDefinition def = getModelObject().findItemDefinition(ItemPath.create(ReportType.F_OBJECT_COLLECTION));
            return getPageBase().createStringResource(def.getDisplayName()).getString();
        } else if (WebComponentUtil.hasArchetypeAssignment(getReport(), SystemObjectsType.ARCHETYPE_DASHBOARD_REPORT.value())) {
            ItemDefinition def = getModelObject().findItemDefinition(ItemPath.create(ReportType.F_DASHBOARD));
            return getPageBase().createStringResource(def.getDisplayName()).getString();
        }
        return label;
    }

    private boolean hasReportArchetype() {
        return WebComponentUtil.hasArchetypeAssignment(getReport(), SystemObjectsType.ARCHETYPE_COLLECTION_REPORT.value())
                || WebComponentUtil.hasArchetypeAssignment(getReport(), SystemObjectsType.ARCHETYPE_DASHBOARD_REPORT.value());
    }

    private ReportType getReport() {
        return getModelObject().getObject().asObjectable();
    }

    private List<ITab> createTabs() {

        if (WebComponentUtil.hasArchetypeAssignment(getReport(), SystemObjectsType.ARCHETYPE_COLLECTION_REPORT.value())) {
            return createTabsForCollectionReports();
        } else if (WebComponentUtil.hasArchetypeAssignment(getReport(), SystemObjectsType.ARCHETYPE_DASHBOARD_REPORT.value())) {
            return createTabsForDashboardReports();
        }
        warn(getPageBase().createStringResource("PageReport.message.selectTypeOfReport").getString());
        return Collections.EMPTY_LIST;
    }

    private List<ITab> createTabsForDashboardReports() {
        List<ITab> tabs = new ArrayList<>();

        tabs.add(new PanelTab(createStringResource("Basic")) {

            @Override
            public WebMarkupContainer createPanel(String panelId) {
                return new SingleContainerPanel(panelId, PrismContainerWrapperModel.fromContainerWrapper(
                        getModel(), ItemPath.create(ReportType.F_DASHBOARD)), DashboardReportEngineConfigurationType.COMPLEX_TYPE) {
                    @Override
                    protected ItemVisibility getVisibility(ItemWrapper itemWrapper) {
                        if (!ItemPath.create(ReportType.F_DASHBOARD, DashboardReportEngineConfigurationType.F_DASHBOARD_REF).equivalent(itemWrapper.getPath())
                                && !ItemPath.create(ReportType.F_DASHBOARD, DashboardReportEngineConfigurationType.F_SHOW_ONLY_WIDGETS_TABLE).equivalent(itemWrapper.getPath())) {
                            return ItemVisibility.HIDDEN;
                        }
                        return super.getVisibility(itemWrapper);
                    }
                };
            }
        });

        tabs.add(new PanelTab(createStringResource("DashboardReportEngineConfigurationType.view")) {

            @Override
            public WebMarkupContainer createPanel(String panelId) {
                return new SingleContainerPanel(panelId, PrismContainerWrapperModel.fromContainerWrapper(
                        getModel(), ItemPath.create(ReportType.F_DASHBOARD, DashboardReportEngineConfigurationType.F_VIEW)), GuiObjectListViewType.COMPLEX_TYPE);
            }
        });

        return tabs;
    }

    private List<ITab> createTabsForCollectionReports() {
        List<ITab> tabs = new ArrayList<>();

        tabs.add(new PanelTab(createStringResource("Basic")) {

            @Override
            public WebMarkupContainer createPanel(String panelId) {
                return new SingleContainerPanel(panelId, PrismContainerWrapperModel.fromContainerWrapper(
                        getModel(), ItemPath.create(ReportType.F_OBJECT_COLLECTION)), ObjectCollectionReportEngineConfigurationType.COMPLEX_TYPE) {
                    @Override
                    protected ItemVisibility getVisibility(ItemWrapper itemWrapper) {
                        if (!ItemPath.create(ReportType.F_OBJECT_COLLECTION, ObjectCollectionReportEngineConfigurationType.F_CONDITION).equivalent(itemWrapper.getPath())
                                && !ItemPath.create(ReportType.F_OBJECT_COLLECTION, ObjectCollectionReportEngineConfigurationType.F_USE_ONLY_REPORT_VIEW).equivalent(itemWrapper.getPath())) {
                            return ItemVisibility.HIDDEN;
                        }
                        return super.getVisibility(itemWrapper);
                    }
                };
            }
        });

        tabs.add(new PanelTab(createStringResource("ObjectCollectionReportEngineConfigurationType.collection")) {

            @Override
            public WebMarkupContainer createPanel(String panelId) {
                return new SingleContainerPanel(panelId, PrismContainerWrapperModel.fromContainerWrapper(
                        getModel(), ItemPath.create(ReportType.F_OBJECT_COLLECTION, ObjectCollectionReportEngineConfigurationType.F_COLLECTION)), GuiObjectListViewType.COMPLEX_TYPE);
            }
        });

        tabs.add(new PanelTab(createStringResource("ObjectCollectionReportEngineConfigurationType.view")) {

            @Override
            public WebMarkupContainer createPanel(String panelId) {
                RepeatingView panel = new RepeatingView(panelId);
                PrismPropertyWrapperModel<ReportType, Object> propertyModel = PrismPropertyWrapperModel.fromContainerWrapper(getModel(),
                        ItemPath.create(ReportType.F_OBJECT_COLLECTION, ObjectCollectionReportEngineConfigurationType.F_VIEW, GuiObjectListViewType.F_TYPE));
                try {
                    Panel propertyPanel = getPageBase().initItemPanel(panel.newChildId(), propertyModel.getObject().getTypeName(), propertyModel, null);
                    panel.add(propertyPanel);
                } catch (SchemaException e) {
                    LOGGER.error("Couldn't create panel for type element in view");
                }
                panel.add(createObjectListForColumns(panel.newChildId()));
                return panel;
            }
        });

        tabs.add(new PanelTab(createStringResource("ObjectCollectionReportEngineConfigurationType.parameter")) {

            @Override
            public WebMarkupContainer createPanel(String panelId) {
                return createObjectListForParameters(panelId);
            }
        });

        tabs.add(new PanelTab(createStringResource("ObjectCollectionReportEngineConfigurationType.subreport")) {

            @Override
            public WebMarkupContainer createPanel(String panelId) {
                return createObjectListForSubreports(panelId);
            }
        });

        return tabs;
    }

    private WebMarkupContainer createObjectListForParameters(String panelId) {
        return new BasicMultivalueContainerListPanel<>(panelId, SearchFilterParameterType.class) {

            @Override
            protected MultivalueContainerDetailsPanel<SearchFilterParameterType> getMultivalueContainerDetailsPanel(ListItem<PrismContainerValueWrapper<SearchFilterParameterType>> item) {
                return createDetailsPanelForParameter(ID_ITEM_DETAILS, item);
            }

            @Override
            protected String getContainerNameForNewButton() {
                return EngineReportTabPanel.this.createStringResource("ObjectCollectionReportEngineConfigurationType.parameter").getString();
            }

            @Override
            protected IModel<PrismContainerWrapper<SearchFilterParameterType>> getContainerModel() {
                return PrismContainerWrapperModel.fromContainerWrapper(
                        EngineReportTabPanel.this.getModel(), ItemPath.create(ReportType.F_OBJECT_COLLECTION, ObjectCollectionReportEngineConfigurationType.F_PARAMETER));
            }

            @Override
            protected List<IColumn<PrismContainerValueWrapper<SearchFilterParameterType>, String>> createDefaultColumns() {
                return createColumnsForParameters();
            }
        };
    }

    private MultivalueContainerDetailsPanel<SearchFilterParameterType> createDetailsPanelForParameter(String panelId, ListItem<PrismContainerValueWrapper<SearchFilterParameterType>> item) {
        return new MultivalueContainerDetailsPanel<>(panelId, item.getModel(), true) {
            @Override
            protected DisplayNamePanel<SearchFilterParameterType> createDisplayNamePanel(String displayNamePanelId) {
                return new DisplayNamePanel<>(displayNamePanelId, new ItemRealValueModel<>(getModel())){
                    @Override
                    protected IModel<String> createHeaderModel() {
                        return () -> {
                            SearchFilterParameterType parameter = getModelObject();
                            String name = parameter.getName();
                            if (parameter.getDisplay() != null && parameter.getDisplay().getLabel() != null){
                                name = WebComponentUtil.getTranslatedPolyString(parameter.getDisplay().getLabel());
                            }
                            return name;
                        };
                    }

                    @Override
                    protected String createImageModel() {
                        return "fa fa-sliders";
                    }
                };
            }

            @Override
            protected ItemVisibility getBasicTabVisibity(ItemWrapper<?, ?> itemWrapper) {
                if (ItemPath.create(itemWrapper.getParent().getPath(), SearchFilterParameterType.F_DISPLAY).equivalent(itemWrapper.getPath())) {
                    return ItemVisibility.HIDDEN;
                }
                return super.getBasicTabVisibity(itemWrapper);
            }

            @Override
            protected @NotNull List<ITab> createTabs() {
                List<ITab> tabs = new ArrayList<>();
                tabs.add(new PanelTab(createStringResource("SearchFilterParameterType.display")) {
                    @Override
                    public WebMarkupContainer createPanel(String panelId) {
                        return new SingleContainerPanel<>(panelId,
                                PrismContainerWrapperModel.fromContainerValueWrapper(getModel(), SearchFilterParameterType.F_DISPLAY), DisplayType.COMPLEX_TYPE) {

                            @Override
                            protected ItemVisibility getVisibility(ItemWrapper itemWrapper) {
                                if (ItemPath.create(itemWrapper.getParent().getPath(), DisplayType.F_LABEL).equivalent(itemWrapper.getPath())
                                        || ItemPath.create(itemWrapper.getParent().getPath(), DisplayType.F_HELP).equivalent(itemWrapper.getPath())) {
                                    return super.getVisibility(itemWrapper);
                                }
                                return ItemVisibility.HIDDEN;
                            }
                        };
                    }
                });
                return tabs;
            }
        };
    }

    private List<IColumn<PrismContainerValueWrapper<SearchFilterParameterType>, String>> createColumnsForParameters() {
        PrismContainerWrapperModel<ReportType, SearchFilterParameterType> containerModel = PrismContainerWrapperModel.fromContainerWrapper(
                getModel(), ItemPath.create(ReportType.F_OBJECT_COLLECTION, ObjectCollectionReportEngineConfigurationType.F_PARAMETER));
        List<IColumn<PrismContainerValueWrapper<SearchFilterParameterType>, String>> columns = new ArrayList<>();

        columns.add(new PrismPropertyWrapperColumn<SearchFilterParameterType, String>(
                containerModel, SearchFilterParameterType.F_NAME, ColumnType.VALUE, getPageBase()){
            @Override
            public String getCssClass() {
                return "col-sm-3 col-md-2";
            }
        });

        columns.add(new PrismPropertyWrapperColumn<SearchFilterParameterType, String>(
                containerModel, SearchFilterParameterType.F_TYPE, ColumnType.VALUE, getPageBase()){
            @Override
            public String getCssClass() {
                return "col-md-3";
            }
        });

        PrismContainerDefinition<Containerable> def = containerModel.getObject().getComplexTypeDefinition().findContainerDefinition(SearchFilterParameterType.F_DISPLAY);
        columns.add(new PrismPropertyWrapperColumn(Model.of(def), DisplayType.F_LABEL, ColumnType.VALUE, getPageBase()){
            @Override
            public IModel<?> getDataModel(IModel rowModel) {
                return PrismPropertyWrapperModel.fromContainerValueWrapper(rowModel, ItemPath.create(SearchFilterParameterType.F_DISPLAY, DisplayType.F_LABEL));
            }
        });
        return columns;
    }

    private WebMarkupContainer createObjectListForSubreports(String panelId) {
        return new BasicMultivalueContainerListPanel<>(panelId, SubreportParameterType.class) {

            @Override
            protected MultivalueContainerDetailsPanel<SubreportParameterType> getMultivalueContainerDetailsPanel(ListItem<PrismContainerValueWrapper<SubreportParameterType>> item) {
                return createDetailsPanelForSubreport(ID_ITEM_DETAILS, item);
            }

            @Override
            protected String getContainerNameForNewButton() {
                return getPageBase().createStringResource("ObjectCollectionReportEngineConfigurationType.subreport").getString();
            }

            @Override
            protected IModel<PrismContainerWrapper<SubreportParameterType>> getContainerModel() {
                return PrismContainerWrapperModel.fromContainerWrapper(
                        EngineReportTabPanel.this.getModel(), ItemPath.create(ReportType.F_OBJECT_COLLECTION, ObjectCollectionReportEngineConfigurationType.F_SUBREPORT));
            }

            @Override
            protected List<IColumn<PrismContainerValueWrapper<SubreportParameterType>, String>> createDefaultColumns() {
                return createColumnsForSubreports();
            }
        };
    }

    private MultivalueContainerDetailsPanel<SubreportParameterType> createDetailsPanelForSubreport(String panelId, ListItem<PrismContainerValueWrapper<SubreportParameterType>> item) {
        return new MultivalueContainerDetailsPanel<>(panelId, item.getModel(), true) {
            @Override
            protected DisplayNamePanel<SubreportParameterType> createDisplayNamePanel(String displayNamePanelId) {
                return new DisplayNamePanel<>(displayNamePanelId, new ItemRealValueModel<>(getModel())){
                    @Override
                    protected IModel<String> createHeaderModel() {
                        return () -> getModelObject().getName();
                    }

                    @Override
                    protected String createImageModel() {
                        return "fa fa-sliders";
                    }
                };
            }
        };
    }

    private List<IColumn<PrismContainerValueWrapper<SubreportParameterType>, String>> createColumnsForSubreports() {
        PrismContainerWrapperModel<ReportType, SubreportParameterType> containerModel = PrismContainerWrapperModel.fromContainerWrapper(
                getModel(), ItemPath.create(ReportType.F_OBJECT_COLLECTION, ObjectCollectionReportEngineConfigurationType.F_SUBREPORT));
        List<IColumn<PrismContainerValueWrapper<SubreportParameterType>, String>> columns = new ArrayList<>();

        columns.add(new PrismPropertyWrapperColumn<SubreportParameterType, String>(
                containerModel, SubreportParameterType.F_NAME, ColumnType.VALUE, getPageBase()){
            @Override
            public String getCssClass() {
                return "col-sm-3 col-md-2";
            }
        });

        columns.add(new PrismPropertyWrapperColumn<SubreportParameterType, String>(
                containerModel, SubreportParameterType.F_TYPE, ColumnType.VALUE, getPageBase()){
            @Override
            public String getCssClass() {
                return "col-md-3";
            }
        });

        columns.add(new PrismPropertyWrapperColumn<SubreportParameterType, String>(
                containerModel, SubreportParameterType.F_EXPRESSION, ColumnType.EXISTENCE_OF_VALUE, getPageBase()){
            @Override
            public String getCssClass() {
                return "col-sm-3 col-md-2";
            }
        });

        return columns;
    }

    private WebMarkupContainer createObjectListForColumns(String panelId) {
        return new BasicMultivalueContainerListPanel<>(panelId, GuiObjectColumnType.class) {

            @Override
            protected MultivalueContainerDetailsPanel<GuiObjectColumnType> getMultivalueContainerDetailsPanel(ListItem<PrismContainerValueWrapper<GuiObjectColumnType>> item) {
                return createDetailsPanelForColumn(ID_ITEM_DETAILS, item);
            }

            @Override
            protected String getContainerNameForNewButton() {
                return getPageBase().createStringResource("GuiObjectListViewType.column").getString();
            }

            @Override
            protected IModel<PrismContainerWrapper<GuiObjectColumnType>> getContainerModel() {
                return PrismContainerWrapperModel.fromContainerWrapper(
                        EngineReportTabPanel.this.getModel(), ItemPath.create(ReportType.F_OBJECT_COLLECTION, ObjectCollectionReportEngineConfigurationType.F_VIEW, GuiObjectListViewType.F_COLUMN));
            }

            @Override
            protected Component createHeader(String headerId) {
                Label label = new Label(headerId, getPageBase().createStringResource("EngineReportTabPanel.columns"));
                label.add(AttributeAppender.append("style", "padding-bottom:10px; font-size: 16px; font-weight: 600; color: #3c8dbc;"));
                return label;
            }

            @Override
            protected List<IColumn<PrismContainerValueWrapper<GuiObjectColumnType>, String>> createDefaultColumns() {
                return createColumnsForColumns();
            }
        };
    }

    private MultivalueContainerDetailsPanel<GuiObjectColumnType> createDetailsPanelForColumn(String panelId, ListItem<PrismContainerValueWrapper<GuiObjectColumnType>> item) {
        return new MultivalueContainerDetailsPanel<>(panelId, item.getModel(), true) {
            @Override
            protected DisplayNamePanel<GuiObjectColumnType> createDisplayNamePanel(String displayNamePanelId) {
                return new DisplayNamePanel<>(displayNamePanelId, new ItemRealValueModel<>(getModel())){
                    @Override
                    protected IModel<String> createHeaderModel() {
                        return () -> {
                            GuiObjectColumnType column = getModelObject();
                            String name = column.getName();
                            if (column.getDisplay() != null && column.getDisplay().getLabel() != null){
                                name = WebComponentUtil.getTranslatedPolyString(column.getDisplay().getLabel());
                            }
                            return name;
                        };
                    }

                    @Override
                    protected String createImageModel() {
                        return "fa fa-columns";
                    }
                };
            }
        };
    }

    private List<IColumn<PrismContainerValueWrapper<GuiObjectColumnType>, String>> createColumnsForColumns() {
        PrismContainerWrapperModel<ReportType, GuiObjectColumnType> containerModel = PrismContainerWrapperModel.fromContainerWrapper(
                getModel(), ItemPath.create(ReportType.F_OBJECT_COLLECTION, ObjectCollectionReportEngineConfigurationType.F_VIEW, GuiObjectListViewType.F_COLUMN));
        List<IColumn<PrismContainerValueWrapper<GuiObjectColumnType>, String>> columns = new ArrayList<>();

        columns.add(new PrismPropertyWrapperColumn<GuiObjectColumnType, String>(
                containerModel, GuiObjectColumnType.F_NAME, ColumnType.VALUE, getPageBase()){
            @Override
            public String getCssClass() {
                return "col-sm-3 col-md-2";
            }
        });

        columns.add(new PrismPropertyWrapperColumn<GuiObjectColumnType, String>(
                containerModel, GuiObjectColumnType.F_PATH, ColumnType.VALUE, getPageBase()){
            @Override
            public String getCssClass() {
                return "col-md-3";
            }
        });

        PrismContainerDefinition<Containerable> def = containerModel.getObject().getComplexTypeDefinition().findContainerDefinition(GuiObjectColumnType.F_DISPLAY);
        columns.add(new PrismPropertyWrapperColumn(Model.of(def), DisplayType.F_LABEL, ColumnType.VALUE, getPageBase()){
            @Override
            public IModel<?> getDataModel(IModel rowModel) {
                return PrismPropertyWrapperModel.fromContainerValueWrapper(rowModel, ItemPath.create(GuiObjectColumnType.F_DISPLAY, DisplayType.F_LABEL));
            }
        });

        return columns;
    }
}
