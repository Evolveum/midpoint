/*
 * Copyright (c) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.page.admin.reports.component;

import com.evolveum.midpoint.gui.api.component.DisplayNamePanel;
import com.evolveum.midpoint.gui.api.component.tabs.PanelTab;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.prism.wrapper.ItemWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismObjectWrapper;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.component.BasicMultivalueContainerListPanel;
import com.evolveum.midpoint.gui.impl.component.MultivalueContainerDetailsPanel;
import com.evolveum.midpoint.gui.impl.component.data.column.AbstractItemWrapperColumn;
import com.evolveum.midpoint.gui.impl.component.data.column.PrismPropertyWrapperColumn;
import com.evolveum.midpoint.gui.impl.factory.panel.ItemRealValueModel;
import com.evolveum.midpoint.gui.impl.prism.panel.SingleContainerPanel;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.AjaxSubmitButton;
import com.evolveum.midpoint.web.component.ObjectBasicPanel;
import com.evolveum.midpoint.web.component.objectdetails.AbstractObjectMainPanel;
import com.evolveum.midpoint.web.component.prism.ItemVisibility;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.model.PrismContainerWrapperModel;
import com.evolveum.midpoint.web.model.PrismPropertyWrapperModel;
import com.evolveum.midpoint.web.page.admin.PageAdminObjectDetails;
import com.evolveum.midpoint.web.page.admin.reports.PageReport;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
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

import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author skublik
 */

public class ReportMainPanel extends AbstractObjectMainPanel<ReportType> {
    private static final long serialVersionUID = 1L;

    private static final Trace LOGGER = TraceManager.getTrace(ReportMainPanel.class);

    private static final String ID_SAVE_AND_RUN = "saveAndRun";


    public ReportMainPanel(String id, LoadableModel<PrismObjectWrapper<ReportType>> objectModel, PageAdminObjectDetails<ReportType> parentPage) {
        super(id, objectModel, parentPage);
    }

    @Override
    protected List<ITab> createTabs(PageAdminObjectDetails<ReportType> parentPage) {
        return getTabs(parentPage);
    }

    @Override
    protected void initLayoutButtons(PageAdminObjectDetails<ReportType> parentPage) {
        super.initLayoutButtons(parentPage);
        initLayoutSaveAndRunButton(parentPage);
    }

    private void initLayoutSaveAndRunButton(PageAdminObjectDetails<ReportType> parentPage) {
        AjaxSubmitButton saveAndRunButton = new AjaxSubmitButton(ID_SAVE_AND_RUN, parentPage.createStringResource("pageReport.button.saveAndRun")) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onSubmit(AjaxRequestTarget target) {
                ((PageReport) getDetailsPage()).saveAndRunPerformed(target);
            }

            @Override
            protected void onError(AjaxRequestTarget target) {
                target.add(parentPage.getFeedbackPanel());
            }
        };
        saveAndRunButton.add(getVisibilityForSaveAndRunButton());
        saveAndRunButton.setOutputMarkupId(true);
        saveAndRunButton.setOutputMarkupPlaceholderTag(true);
        getMainForm().add(saveAndRunButton);
    }

    private VisibleEnableBehaviour getVisibilityForSaveAndRunButton() {
        return new VisibleEnableBehaviour() {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isVisible() {
                return !getObjectWrapper().isReadOnly() &&
                        !getDetailsPage().isForcedPreview();
            }

            @Override
            public boolean isEnabled() {
                return !getExecuteChangeOptionsDto().isSaveInBackground();
            }
        };
    }

    @Override
    public void reloadSavePreviewButtons(AjaxRequestTarget target) {
        super.reloadSavePreviewButtons(target);
        target.add(ReportMainPanel.this.get(ID_MAIN_FORM).get(ID_SAVE_AND_RUN));

    }

    private List<ITab> getTabs(PageAdminObjectDetails<ReportType> parentPage) {
        List<ITab> tabs = new ArrayList<>();
        tabs.add(new PanelTab(parentPage.createStringResource("pageReport.basic.title")) {

            private static final long serialVersionUID = 1L;

            @Override
            public WebMarkupContainer createPanel(String panelId) {
                return new ObjectBasicPanel<ReportType>(panelId, getObjectModel()) {
                    @Override
                    protected QName getType() {
                        return ReportType.COMPLEX_TYPE;
                    }

                    @Override
                    protected ItemVisibility getBasicTabVisibility(ItemWrapper<?, ?> itemWrapper) {
                        if (itemWrapper.getPath().isSubPathOrEquivalent(ItemPath.create(ItemPath.EMPTY_PATH, ReportType.F_LIFECYCLE_STATE))
                                || itemWrapper.getPath().isSubPathOrEquivalent(ItemPath.create(ItemPath.EMPTY_PATH, ReportType.F_DIAGNOSTIC_INFORMATION))
                                || itemWrapper.getPath().isSubPathOrEquivalent(ItemPath.create(ItemPath.EMPTY_PATH, ReportType.F_DEFAULT_SCRIPT_CONFIGURATION))
                                || itemWrapper.getPath().isSubPathOrEquivalent(ItemPath.create(ItemPath.EMPTY_PATH, ReportType.F_POST_REPORT_SCRIPT))) {
                            return ItemVisibility.HIDDEN;
                        }
                        return ItemVisibility.AUTO;
                    }
                };
            }
        });
        tabs.add(new PanelTab(parentPage.createStringResource("pageReport.engine.title")) {

            private static final long serialVersionUID = 1L;

            @Override
            public WebMarkupContainer createPanel(String panelId) {
                return new EngineReportTabPanel(panelId, getObjectModel());
            }

            @Override
            public boolean isVisible() {
                return hasDefinitionFor(ReportType.F_OBJECT_COLLECTION)
                        || hasDefinitionFor(ReportType.F_DASHBOARD) ;
            }
        });
        tabs.addAll(createTabsForCollectionReports(parentPage));
        tabs.addAll(createTabsForDashboardReports(parentPage));

        tabs.add(new PanelTab(parentPage.createStringResource("pageReport.export.title")) {

            private static final long serialVersionUID = 1L;

            @Override
            public WebMarkupContainer createPanel(String panelId) {
                PrismContainerWrapperModel<ReportType, Containerable> model = PrismContainerWrapperModel.fromContainerWrapper(getObjectModel(), ReportType.F_FILE_FORMAT);
                return new SingleContainerPanel(panelId, model, FileFormatConfigurationType.COMPLEX_TYPE) {
                    @Override
                    protected ItemVisibility getVisibility(ItemWrapper itemWrapper) {
                        if (itemWrapper.getPath().isSubPathOrEquivalent(ItemPath.create(ReportType.F_FILE_FORMAT, FileFormatConfigurationType.F_HTML))) {
                            return ItemVisibility.HIDDEN;
                        }
                        return ItemVisibility.AUTO;
                    }
                };
            }
        });

        return tabs;
    }

    private boolean hasDefinitionFor(ItemPath path){
        return getObjectModel().getObject().findItemDefinition(path) != null;
    }

    private List<ITab> createTabsForDashboardReports(PageAdminObjectDetails<ReportType> parentPage) {
        List<ITab> tabs = new ArrayList<>();

        tabs.add(new PanelTab(parentPage.createStringResource("DashboardReportEngineConfigurationType.view")) {

            @Override
            public WebMarkupContainer createPanel(String panelId) {
                return new SingleContainerPanel(panelId, PrismContainerWrapperModel.fromContainerWrapper(
                        getObjectModel(), ItemPath.create(ReportType.F_DASHBOARD, DashboardReportEngineConfigurationType.F_VIEW)), GuiObjectListViewType.COMPLEX_TYPE);
            }

            @Override
            public boolean isVisible() {
                return hasDefinitionFor(ItemPath.create(ReportType.F_DASHBOARD, DashboardReportEngineConfigurationType.F_VIEW));
            }
        });

        return tabs;
    }

    private List<ITab> createTabsForCollectionReports(PageAdminObjectDetails<ReportType> parentPage) {
        List<ITab> tabs = new ArrayList<>();

        tabs.add(new PanelTab(parentPage.createStringResource("ObjectCollectionReportEngineConfigurationType.collection")) {

            @Override
            public WebMarkupContainer createPanel(String panelId) {
                return new SingleContainerPanel(panelId, PrismContainerWrapperModel.fromContainerWrapper(
                        getObjectModel(), ItemPath.create(ReportType.F_OBJECT_COLLECTION, ObjectCollectionReportEngineConfigurationType.F_COLLECTION)), GuiObjectListViewType.COMPLEX_TYPE);
            }

            @Override
            public boolean isVisible() {
                return hasDefinitionFor(ItemPath.create(ReportType.F_OBJECT_COLLECTION, ObjectCollectionReportEngineConfigurationType.F_COLLECTION));
            }
        });

        tabs.add(new PanelTab(parentPage.createStringResource("ObjectCollectionReportEngineConfigurationType.view")) {

            @Override
            public WebMarkupContainer createPanel(String panelId) {
                RepeatingView panel = new RepeatingView(panelId);
                PrismPropertyWrapperModel<ReportType, Object> propertyModel = PrismPropertyWrapperModel.fromContainerWrapper(getObjectModel(),
                        ItemPath.create(ReportType.F_OBJECT_COLLECTION, ObjectCollectionReportEngineConfigurationType.F_VIEW, GuiObjectListViewType.F_TYPE));
                try {
                    Panel propertyPanel = parentPage.initItemPanel(panel.newChildId(), propertyModel.getObject().getTypeName(), propertyModel, null);
                    panel.add(propertyPanel);
                } catch (SchemaException e) {
                    LOGGER.error("Couldn't create panel for type element in view");
                }
                panel.add(createObjectListForColumns(panel.newChildId(), parentPage));
                return panel;
            }

            @Override
            public boolean isVisible() {
                return hasDefinitionFor(ItemPath.create(ReportType.F_OBJECT_COLLECTION, ObjectCollectionReportEngineConfigurationType.F_VIEW));
            }
        });

        tabs.add(new PanelTab(parentPage.createStringResource("ObjectCollectionReportEngineConfigurationType.parameter")) {

            @Override
            public WebMarkupContainer createPanel(String panelId) {
                return createObjectListForParameters(panelId, parentPage);
            }

            @Override
            public boolean isVisible() {
                return hasDefinitionFor(ItemPath.create(ReportType.F_OBJECT_COLLECTION, ObjectCollectionReportEngineConfigurationType.F_PARAMETER));
            }
        });

        tabs.add(new PanelTab(parentPage.createStringResource("ObjectCollectionReportEngineConfigurationType.subreport")) {

            @Override
            public WebMarkupContainer createPanel(String panelId) {
                return createObjectListForSubreports(panelId, parentPage);
            }

            @Override
            public boolean isVisible() {
                return hasDefinitionFor(ItemPath.create(ReportType.F_OBJECT_COLLECTION, ObjectCollectionReportEngineConfigurationType.F_SUBREPORT));
            }
        });

        return tabs;
    }

    private WebMarkupContainer createObjectListForParameters(String panelId, PageAdminObjectDetails<ReportType> parentPage) {
        return new BasicMultivalueContainerListPanel<>(panelId, SearchFilterParameterType.class) {

            @Override
            protected MultivalueContainerDetailsPanel<SearchFilterParameterType> getMultivalueContainerDetailsPanel(ListItem<PrismContainerValueWrapper<SearchFilterParameterType>> item) {
                return createDetailsPanelForParameter(ID_ITEM_DETAILS, item);
            }

            @Override
            protected String getContainerNameForNewButton() {
                return getPageBase().createStringResource("ObjectCollectionReportEngineConfigurationType.parameter").getString();
            }

            @Override
            protected IModel<PrismContainerWrapper<SearchFilterParameterType>> getContainerModel() {
                return PrismContainerWrapperModel.fromContainerWrapper(
                        getObjectModel(), ItemPath.create(ReportType.F_OBJECT_COLLECTION, ObjectCollectionReportEngineConfigurationType.F_PARAMETER));
            }

            @Override
            protected List<IColumn<PrismContainerValueWrapper<SearchFilterParameterType>, String>> createDefaultColumns() {
                return createColumnsForParameters(parentPage);
            }
        };
    }

    private MultivalueContainerDetailsPanel<SearchFilterParameterType> createDetailsPanelForParameter(String panelId, ListItem<PrismContainerValueWrapper<SearchFilterParameterType>> item) {
        return new MultivalueContainerDetailsPanel<>(panelId, item.getModel(), true) {
            @Override
            protected DisplayNamePanel<SearchFilterParameterType> createDisplayNamePanel(String displayNamePanelId) {
                return new DisplayNamePanel<>(displayNamePanelId, new ItemRealValueModel<>(getModel())) {
                    @Override
                    protected IModel<String> createHeaderModel() {
                        return () -> {
                            SearchFilterParameterType parameter = getModelObject();
                            String name = parameter.getName();
                            if (parameter.getDisplay() != null && parameter.getDisplay().getLabel() != null) {
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

    private List<IColumn<PrismContainerValueWrapper<SearchFilterParameterType>, String>> createColumnsForParameters(PageAdminObjectDetails<ReportType> parentPage) {
        PrismContainerWrapperModel<ReportType, SearchFilterParameterType> containerModel = PrismContainerWrapperModel.fromContainerWrapper(
                getObjectModel(), ItemPath.create(ReportType.F_OBJECT_COLLECTION, ObjectCollectionReportEngineConfigurationType.F_PARAMETER));
        List<IColumn<PrismContainerValueWrapper<SearchFilterParameterType>, String>> columns = new ArrayList<>();

        columns.add(new PrismPropertyWrapperColumn<SearchFilterParameterType, String>(
                containerModel, SearchFilterParameterType.F_NAME, AbstractItemWrapperColumn.ColumnType.VALUE, parentPage) {
            @Override
            public String getCssClass() {
                return "col-sm-3 col-md-2";
            }
        });

        columns.add(new PrismPropertyWrapperColumn<SearchFilterParameterType, String>(
                containerModel, SearchFilterParameterType.F_TYPE, AbstractItemWrapperColumn.ColumnType.VALUE, parentPage) {
            @Override
            public String getCssClass() {
                return "col-md-3";
            }
        });

        PrismContainerDefinition<Containerable> def = containerModel.getObject().getComplexTypeDefinition().findContainerDefinition(SearchFilterParameterType.F_DISPLAY);
        columns.add(new PrismPropertyWrapperColumn(Model.of(def), DisplayType.F_LABEL, AbstractItemWrapperColumn.ColumnType.VALUE, parentPage) {
            @Override
            public IModel<?> getDataModel(IModel rowModel) {
                return PrismPropertyWrapperModel.fromContainerValueWrapper(rowModel, ItemPath.create(SearchFilterParameterType.F_DISPLAY, DisplayType.F_LABEL));
            }
        });
        return columns;
    }

    private WebMarkupContainer createObjectListForSubreports(String panelId, PageAdminObjectDetails<ReportType> parentPage) {
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
                        ReportMainPanel.this.getObjectModel(), ItemPath.create(ReportType.F_OBJECT_COLLECTION, ObjectCollectionReportEngineConfigurationType.F_SUBREPORT));
            }

            @Override
            protected List<IColumn<PrismContainerValueWrapper<SubreportParameterType>, String>> createDefaultColumns() {
                return createColumnsForSubreports(parentPage);
            }
        };
    }

    private MultivalueContainerDetailsPanel<SubreportParameterType> createDetailsPanelForSubreport(String panelId, ListItem<PrismContainerValueWrapper<SubreportParameterType>> item) {
        return new MultivalueContainerDetailsPanel<>(panelId, item.getModel(), true) {
            @Override
            protected DisplayNamePanel<SubreportParameterType> createDisplayNamePanel(String displayNamePanelId) {
                return new DisplayNamePanel<>(displayNamePanelId, new ItemRealValueModel<>(getModel())) {
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

    private List<IColumn<PrismContainerValueWrapper<SubreportParameterType>, String>> createColumnsForSubreports(PageAdminObjectDetails<ReportType> parentPage) {
        PrismContainerWrapperModel<ReportType, SubreportParameterType> containerModel = PrismContainerWrapperModel.fromContainerWrapper(
                getObjectModel(), ItemPath.create(ReportType.F_OBJECT_COLLECTION, ObjectCollectionReportEngineConfigurationType.F_SUBREPORT));
        List<IColumn<PrismContainerValueWrapper<SubreportParameterType>, String>> columns = new ArrayList<>();

        columns.add(new PrismPropertyWrapperColumn<SubreportParameterType, String>(
                containerModel, SubreportParameterType.F_NAME, AbstractItemWrapperColumn.ColumnType.VALUE, parentPage) {
            @Override
            public String getCssClass() {
                return "col-sm-3 col-md-2";
            }
        });

        columns.add(new PrismPropertyWrapperColumn<SubreportParameterType, String>(
                containerModel, SubreportParameterType.F_TYPE, AbstractItemWrapperColumn.ColumnType.VALUE, parentPage) {
            @Override
            public String getCssClass() {
                return "col-md-3";
            }
        });

        columns.add(new PrismPropertyWrapperColumn<SubreportParameterType, String>(
                containerModel, SubreportParameterType.F_EXPRESSION, AbstractItemWrapperColumn.ColumnType.EXISTENCE_OF_VALUE, parentPage) {
            @Override
            public String getCssClass() {
                return "col-sm-3 col-md-2";
            }
        });

        return columns;
    }

    private WebMarkupContainer createObjectListForColumns(String panelId, PageAdminObjectDetails<ReportType> parentPage) {
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
                        ReportMainPanel.this.getObjectModel(), ItemPath.create(ReportType.F_OBJECT_COLLECTION, ObjectCollectionReportEngineConfigurationType.F_VIEW, GuiObjectListViewType.F_COLUMN));
            }

            @Override
            protected Component createHeader(String headerId) {
                Label label = new Label(headerId, getPageBase().createStringResource("EngineReportTabPanel.columns"));
                label.add(AttributeAppender.append("style", "padding-bottom:10px; font-size: 16px; font-weight: 600; color: #3c8dbc;"));
                return label;
            }

            @Override
            protected List<IColumn<PrismContainerValueWrapper<GuiObjectColumnType>, String>> createDefaultColumns() {
                return createColumnsForColumns(parentPage);
            }
        };
    }

    private MultivalueContainerDetailsPanel<GuiObjectColumnType> createDetailsPanelForColumn(String panelId, ListItem<PrismContainerValueWrapper<GuiObjectColumnType>> item) {
        return new MultivalueContainerDetailsPanel<>(panelId, item.getModel(), true) {
            @Override
            protected DisplayNamePanel<GuiObjectColumnType> createDisplayNamePanel(String displayNamePanelId) {
                return new DisplayNamePanel<>(displayNamePanelId, new ItemRealValueModel<>(getModel())) {
                    @Override
                    protected IModel<String> createHeaderModel() {
                        return () -> {
                            GuiObjectColumnType column = getModelObject();
                            String name = column.getName();
                            if (column.getDisplay() != null && column.getDisplay().getLabel() != null) {
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

    private List<IColumn<PrismContainerValueWrapper<GuiObjectColumnType>, String>> createColumnsForColumns(PageAdminObjectDetails<ReportType> parentPage) {
        PrismContainerWrapperModel<ReportType, GuiObjectColumnType> containerModel = PrismContainerWrapperModel.fromContainerWrapper(
                getObjectModel(), ItemPath.create(ReportType.F_OBJECT_COLLECTION, ObjectCollectionReportEngineConfigurationType.F_VIEW, GuiObjectListViewType.F_COLUMN));
        List<IColumn<PrismContainerValueWrapper<GuiObjectColumnType>, String>> columns = new ArrayList<>();

        columns.add(new PrismPropertyWrapperColumn<GuiObjectColumnType, String>(
                containerModel, GuiObjectColumnType.F_NAME, AbstractItemWrapperColumn.ColumnType.VALUE, parentPage) {
            @Override
            public String getCssClass() {
                return "col-sm-3 col-md-2";
            }
        });

        columns.add(new PrismPropertyWrapperColumn<GuiObjectColumnType, String>(
                containerModel, GuiObjectColumnType.F_PATH, AbstractItemWrapperColumn.ColumnType.VALUE, parentPage) {
            @Override
            public String getCssClass() {
                return "col-md-3";
            }
        });

        PrismContainerDefinition<Containerable> def = containerModel.getObject().getComplexTypeDefinition().findContainerDefinition(GuiObjectColumnType.F_DISPLAY);
        columns.add(new PrismPropertyWrapperColumn(Model.of(def), DisplayType.F_LABEL, AbstractItemWrapperColumn.ColumnType.VALUE, parentPage) {
            @Override
            public IModel<?> getDataModel(IModel rowModel) {
                return PrismPropertyWrapperModel.fromContainerValueWrapper(rowModel, ItemPath.create(GuiObjectColumnType.F_DISPLAY, DisplayType.F_LABEL));
            }
        });

        return columns;
    }
}
