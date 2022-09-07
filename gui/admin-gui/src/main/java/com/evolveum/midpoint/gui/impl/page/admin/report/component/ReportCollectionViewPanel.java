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
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.component.BasicMultivalueContainerListPanel;
import com.evolveum.midpoint.gui.impl.component.MultivalueContainerDetailsPanel;
import com.evolveum.midpoint.gui.impl.component.MultivalueContainerListPanelWithDetailsPanel;
import com.evolveum.midpoint.gui.impl.component.data.column.AbstractItemWrapperColumn;
import com.evolveum.midpoint.gui.impl.component.data.column.PrismPropertyWrapperColumn;
import com.evolveum.midpoint.gui.impl.factory.panel.ItemRealValueModel;
import com.evolveum.midpoint.gui.impl.page.admin.AbstractObjectMainPanel;
import com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.AssignmentHolderDetailsModel;
import com.evolveum.midpoint.gui.impl.prism.panel.ItemPanelSettingsBuilder;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.application.PanelType;
import com.evolveum.midpoint.web.model.PrismContainerWrapperModel;
import com.evolveum.midpoint.web.model.PrismPropertyWrapperModel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.wicket.Component;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import java.util.ArrayList;
import java.util.List;

@PanelType(name = "reportCollectionView")
@PanelInstance(identifier = "reportCollectionView", applicableForOperation = OperationTypeType.MODIFY,
        display = @PanelDisplay(label = "ObjectCollectionReportEngineConfigurationType.view", order = 80))
public class ReportCollectionViewPanel extends AbstractObjectMainPanel<ReportType, AssignmentHolderDetailsModel<ReportType>> {

    private static final Trace LOGGER = TraceManager.getTrace(ReportCollectionViewPanel.class);
    private static final String ID_TYPE = "type";
    private static final String ID_COLUMNS_TABLE = "columnsTable";

    public ReportCollectionViewPanel(String id, AssignmentHolderDetailsModel<ReportType> model, ContainerPanelConfigurationType config) {
        super(id, model, config);
    }

    @Override
    protected void initLayout() {
        PrismPropertyWrapperModel<ReportType, Object> propertyModel = PrismPropertyWrapperModel.fromContainerWrapper(
                getObjectWrapperModel(),
                ItemPath.create(ReportType.F_OBJECT_COLLECTION, ObjectCollectionReportEngineConfigurationType.F_VIEW, GuiObjectListViewType.F_TYPE)
        );
        try {
            Panel propertyPanel = getPageBase().initItemPanel(
                    ID_TYPE, propertyModel.getObject().getTypeName(), propertyModel, new ItemPanelSettingsBuilder().build());
            add(propertyPanel);
        } catch (SchemaException e) {
            LOGGER.error("Couldn't create panel for type element in view");
        }
        add(createObjectListForColumns());
    }

    private WebMarkupContainer createObjectListForColumns() {
        return new BasicMultivalueContainerListPanel<>(ID_COLUMNS_TABLE, GuiObjectColumnType.class) {

            @Override
            protected MultivalueContainerDetailsPanel<GuiObjectColumnType> getMultivalueContainerDetailsPanel(
                    ListItem<PrismContainerValueWrapper<GuiObjectColumnType>> item) {
                return createDetailsPanelForColumn(item);
            }

            @Override
            protected String getContainerNameForNewButton() {
                return getPageBase().createStringResource("GuiObjectListViewType.column").getString();
            }

            @Override
            protected IModel<PrismContainerWrapper<GuiObjectColumnType>> getContainerModel() {
                return PrismContainerWrapperModel.fromContainerWrapper(
                        getObjectWrapperModel(), ItemPath.create(ReportType.F_OBJECT_COLLECTION, ObjectCollectionReportEngineConfigurationType.F_VIEW, GuiObjectListViewType.F_COLUMN));
            }

            @Override
            protected Component createHeader(String headerId) {
                Label label = new Label(headerId, getPageBase().createStringResource("EngineReportTabPanel.columns"));
                label.add(AttributeAppender.append("style", "padding-bottom:10px; font-size: 16px; font-weight: 600; color: #3c8dbc;"));
                return label;
            }

            @Override
            protected List<IColumn<PrismContainerValueWrapper<GuiObjectColumnType>, String>> createDefaultColumns() {
                return createColumnsForViewTable();
            }
        };
    }

    private MultivalueContainerDetailsPanel<GuiObjectColumnType> createDetailsPanelForColumn(
            ListItem<PrismContainerValueWrapper<GuiObjectColumnType>> item) {
        return new MultivalueContainerDetailsPanel<>(MultivalueContainerListPanelWithDetailsPanel.ID_ITEM_DETAILS, item.getModel(), true) {
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

    private List<IColumn<PrismContainerValueWrapper<GuiObjectColumnType>, String>> createColumnsForViewTable() {
        PrismContainerWrapperModel<ReportType, GuiObjectColumnType> containerModel = PrismContainerWrapperModel.fromContainerWrapper(
                getObjectWrapperModel(),
                ItemPath.create(ReportType.F_OBJECT_COLLECTION, ObjectCollectionReportEngineConfigurationType.F_VIEW, GuiObjectListViewType.F_COLUMN)
        );
        List<IColumn<PrismContainerValueWrapper<GuiObjectColumnType>, String>> columns = new ArrayList<>();

        columns.add(new PrismPropertyWrapperColumn<GuiObjectColumnType, String>(
                containerModel, GuiObjectColumnType.F_NAME, AbstractItemWrapperColumn.ColumnType.VALUE, getPageBase()) {
            @Override
            public String getCssClass() {
                return "mp-w-sm-3 mp-w-md-2";
            }
        });

        columns.add(new PrismPropertyWrapperColumn<GuiObjectColumnType, String>(
                containerModel, GuiObjectColumnType.F_PATH, AbstractItemWrapperColumn.ColumnType.VALUE, getPageBase()) {
            @Override
            public String getCssClass() {
                return "mp-w-md-3";
            }
        });

        PrismContainerDefinition<Containerable> def = containerModel.getObject().getComplexTypeDefinition().findContainerDefinition(GuiObjectColumnType.F_DISPLAY);
        columns.add(new PrismPropertyWrapperColumn(Model.of(def), DisplayType.F_LABEL, AbstractItemWrapperColumn.ColumnType.VALUE, getPageBase()) {
            @Override
            public IModel<?> getDataModel(IModel rowModel) {
                return PrismPropertyWrapperModel.fromContainerValueWrapper(rowModel, ItemPath.create(GuiObjectColumnType.F_DISPLAY, DisplayType.F_LABEL));
            }
        });

        return columns;
    }
}
