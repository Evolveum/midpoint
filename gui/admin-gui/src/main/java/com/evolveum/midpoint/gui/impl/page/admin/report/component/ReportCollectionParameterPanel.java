/*
 * Copyright (c) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.report.component;

import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.gui.api.component.DisplayNamePanel;
import com.evolveum.midpoint.gui.api.component.tabs.PanelTab;
import com.evolveum.midpoint.gui.api.prism.wrapper.ItemWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.component.BasicMultivalueContainerListPanel;
import com.evolveum.midpoint.gui.impl.component.MultivalueContainerDetailsPanel;
import com.evolveum.midpoint.gui.impl.component.MultivalueContainerListPanelWithDetailsPanel;
import com.evolveum.midpoint.gui.impl.component.data.column.AbstractItemWrapperColumn;
import com.evolveum.midpoint.gui.impl.component.data.column.PrismPropertyWrapperColumn;
import com.evolveum.midpoint.gui.impl.factory.panel.ItemRealValueModel;
import com.evolveum.midpoint.gui.impl.page.admin.AbstractObjectMainPanel;
import com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.AssignmentHolderDetailsModel;
import com.evolveum.midpoint.gui.impl.prism.panel.SingleContainerPanel;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.application.PanelType;
import com.evolveum.midpoint.web.component.prism.ItemVisibility;
import com.evolveum.midpoint.web.model.PrismContainerWrapperModel;
import com.evolveum.midpoint.web.model.PrismPropertyWrapperModel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

@PanelType(name = "reportCollectionParameter")
@PanelInstance(identifier = "reportCollectionParameter", applicableForOperation = OperationTypeType.MODIFY,
        display = @PanelDisplay(label = "ObjectCollectionReportEngineConfigurationType.parameter", order = 90))
public class ReportCollectionParameterPanel extends AbstractObjectMainPanel<ReportType, AssignmentHolderDetailsModel<ReportType>> {

    private static final String ID_PARAMETERS_TABLE = "parametersTable";

    public ReportCollectionParameterPanel(String id, AssignmentHolderDetailsModel<ReportType> model, ContainerPanelConfigurationType config) {
        super(id, model, config);
    }

    @Override
    protected void initLayout() {
        add(createObjectListForParameters());
    }

    private WebMarkupContainer createObjectListForParameters() {
        return new BasicMultivalueContainerListPanel<>(ID_PARAMETERS_TABLE, SearchFilterParameterType.class) {

            @Override
            protected MultivalueContainerDetailsPanel<SearchFilterParameterType> getMultivalueContainerDetailsPanel(ListItem<PrismContainerValueWrapper<SearchFilterParameterType>> item) {
                return createDetailsPanelForParameter(item);
            }

            @Override
            protected String getContainerNameForNewButton() {
                return getString("ObjectCollectionReportEngineConfigurationType.parameter");
            }

            @Override
            protected IModel<PrismContainerWrapper<SearchFilterParameterType>> getContainerModel() {
                return PrismContainerWrapperModel.fromContainerWrapper(
                        getObjectWrapperModel(), ItemPath.create(ReportType.F_OBJECT_COLLECTION, ObjectCollectionReportEngineConfigurationType.F_PARAMETER));
            }

            @Override
            protected List<IColumn<PrismContainerValueWrapper<SearchFilterParameterType>, String>> createDefaultColumns() {
                return createColumnsForParameters();
            }
        };
    }

    private MultivalueContainerDetailsPanel<SearchFilterParameterType> createDetailsPanelForParameter(ListItem<PrismContainerValueWrapper<SearchFilterParameterType>> item) {
        return new MultivalueContainerDetailsPanel<>(MultivalueContainerListPanelWithDetailsPanel.ID_ITEM_DETAILS, item.getModel(), true) {
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
                            protected ItemVisibility getVisibility(ItemWrapper wrapper) {
                                ItemPath parentPath = wrapper.getParent().getPath();
                                if (ItemPath.create(parentPath, DisplayType.F_LABEL).equivalent(wrapper.getPath())
                                        || ItemPath.create(parentPath, DisplayType.F_HELP).equivalent(wrapper.getPath())) {
                                    return super.getVisibility(wrapper);
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
                getObjectWrapperModel(), ItemPath.create(ReportType.F_OBJECT_COLLECTION, ObjectCollectionReportEngineConfigurationType.F_PARAMETER));
        List<IColumn<PrismContainerValueWrapper<SearchFilterParameterType>, String>> columns = new ArrayList<>();

        columns.add(new PrismPropertyWrapperColumn<SearchFilterParameterType, String>(
                containerModel, SearchFilterParameterType.F_NAME, AbstractItemWrapperColumn.ColumnType.VALUE, getPageBase()) {
            @Override
            public String getCssClass() {
                return "mp-w-sm-3 mp-w-md-2";
            }
        });

        columns.add(new PrismPropertyWrapperColumn<SearchFilterParameterType, String>(
                containerModel, SearchFilterParameterType.F_TYPE, AbstractItemWrapperColumn.ColumnType.VALUE, getPageBase()) {
            @Override
            public String getCssClass() {
                return "mp-w-md-3";
            }
        });

        PrismContainerDefinition<Containerable> def = containerModel.getObject().getComplexTypeDefinition().findContainerDefinition(SearchFilterParameterType.F_DISPLAY);
        columns.add(new PrismPropertyWrapperColumn(Model.of(def), DisplayType.F_LABEL, AbstractItemWrapperColumn.ColumnType.VALUE, getPageBase()) {
            @Override
            public IModel<?> getDataModel(IModel rowModel) {
                return PrismPropertyWrapperModel.fromContainerValueWrapper(rowModel, ItemPath.create(SearchFilterParameterType.F_DISPLAY, DisplayType.F_LABEL));
            }
        });
        return columns;
    }
}
