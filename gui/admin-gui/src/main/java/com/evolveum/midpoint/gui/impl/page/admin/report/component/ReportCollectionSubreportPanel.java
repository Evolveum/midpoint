/*
 * Copyright (c) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.report.component;

import com.evolveum.midpoint.gui.api.component.DisplayNamePanel;
import com.evolveum.midpoint.gui.api.prism.ItemStatus;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.impl.component.BasicMultivalueContainerListPanel;
import com.evolveum.midpoint.gui.impl.component.MultivalueContainerDetailsPanel;
import com.evolveum.midpoint.gui.impl.component.MultivalueContainerListPanelWithDetailsPanel;
import com.evolveum.midpoint.gui.impl.component.data.column.AbstractItemWrapperColumn;
import com.evolveum.midpoint.gui.impl.component.data.column.PrismPropertyWrapperColumn;
import com.evolveum.midpoint.gui.impl.factory.panel.ItemRealValueModel;
import com.evolveum.midpoint.gui.impl.page.admin.AbstractObjectMainPanel;
import com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.AssignmentHolderDetailsModel;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.application.PanelType;
import com.evolveum.midpoint.web.model.PrismContainerWrapperModel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.model.IModel;

import java.util.ArrayList;
import java.util.List;

@PanelType(name = "reportCollectionSubreport")
@PanelInstance(identifier = "reportCollectionSubreport", applicableForOperation = OperationTypeType.MODIFY,
        display = @PanelDisplay(label = "ObjectCollectionReportEngineConfigurationType.subreport", order = 100))
public class ReportCollectionSubreportPanel extends AbstractObjectMainPanel<ReportType, AssignmentHolderDetailsModel<ReportType>> {

    private static final String ID_SUBREPORTS_TABLE = "subreportsTable";

    public ReportCollectionSubreportPanel(String id, AssignmentHolderDetailsModel<ReportType> model, ContainerPanelConfigurationType config) {
        super(id, model, config);
    }

    @Override
    protected void initLayout() {
        add(createObjectListForSubreports());
    }

    private WebMarkupContainer createObjectListForSubreports() {
        return new BasicMultivalueContainerListPanel<>(ID_SUBREPORTS_TABLE, SubreportParameterType.class) {

            @Override
            protected MultivalueContainerDetailsPanel<SubreportParameterType> getMultivalueContainerDetailsPanel(ListItem<PrismContainerValueWrapper<SubreportParameterType>> item) {
                return createDetailsPanelForSubreport(item);
            }

            @Override
            protected String getContainerNameForNewButton() {
                return getPageBase().createStringResource("ObjectCollectionReportEngineConfigurationType.subreport").getString();
            }

            @Override
            protected IModel<PrismContainerWrapper<SubreportParameterType>> getContainerModel() {
                return PrismContainerWrapperModel.fromContainerWrapper(
                        getObjectWrapperModel(), ItemPath.create(ReportType.F_OBJECT_COLLECTION, ObjectCollectionReportEngineConfigurationType.F_SUBREPORT));
            }

            @Override
            protected List<IColumn<PrismContainerValueWrapper<SubreportParameterType>, String>> createDefaultColumns() {
                return createColumnsForSubreports();
            }
        };
    }

    private MultivalueContainerDetailsPanel<SubreportParameterType> createDetailsPanelForSubreport(ListItem<PrismContainerValueWrapper<SubreportParameterType>> item) {
        return new MultivalueContainerDetailsPanel<>(MultivalueContainerListPanelWithDetailsPanel.ID_ITEM_DETAILS, item.getModel(), true) {
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

    private List<IColumn<PrismContainerValueWrapper<SubreportParameterType>, String>> createColumnsForSubreports() {
        PrismContainerWrapperModel<ReportType, SubreportParameterType> containerModel = PrismContainerWrapperModel.fromContainerWrapper(
                getObjectWrapperModel(), ItemPath.create(ReportType.F_OBJECT_COLLECTION, ObjectCollectionReportEngineConfigurationType.F_SUBREPORT));
        List<IColumn<PrismContainerValueWrapper<SubreportParameterType>, String>> columns = new ArrayList<>();

        columns.add(new PrismPropertyWrapperColumn<SubreportParameterType, String>(
                containerModel, SubreportParameterType.F_NAME, AbstractItemWrapperColumn.ColumnType.VALUE, getPageBase()) {
            @Override
            public String getCssClass() {
                return "mp-w-sm-3 mp-w-md-2";
            }
        });

        columns.add(new PrismPropertyWrapperColumn<SubreportParameterType, String>(
                containerModel, SubreportParameterType.F_TYPE, AbstractItemWrapperColumn.ColumnType.VALUE, getPageBase()) {
            @Override
            public String getCssClass() {
                return "mp-w-md-3";
            }
        });

        columns.add(new PrismPropertyWrapperColumn<SubreportParameterType, String>(
                containerModel, SubreportParameterType.F_EXPRESSION, AbstractItemWrapperColumn.ColumnType.EXISTENCE_OF_VALUE, getPageBase()) {
            @Override
            public String getCssClass() {
                return "mp-w-sm-3 mp-w-md-2";
            }
        });

        return columns;
    }
}
