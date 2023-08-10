/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.panel;

import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils.ClusterObjectUtils.*;
import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils.Tools.getColorClass;
import static com.evolveum.midpoint.web.component.data.column.ColumnUtils.createStringResource;

import java.io.Serial;
import java.util.ArrayList;
import java.util.List;

import com.evolveum.midpoint.gui.impl.page.admin.AbstractObjectMainPanel;
import com.evolveum.midpoint.gui.impl.page.admin.ObjectDetailsModels;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.PageCluster;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.PageMiningOperationNew;

import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.application.PanelType;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.component.MainObjectListPanel;
import com.evolveum.midpoint.gui.api.component.data.provider.ISelectableDataProvider;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.impl.component.data.provider.SelectableBeanObjectDataProvider;
import com.evolveum.midpoint.gui.impl.component.icon.CompositedIconBuilder;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.PageMiningOperation;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.panel.details.objects.ClusterBasicDetailsPanel;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.panel.details.work.ImageDetailsPanel;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.web.component.AjaxIconButton;
import com.evolveum.midpoint.web.component.data.column.ColumnMenuAction;
import com.evolveum.midpoint.web.component.data.column.SelectableObjectNameColumn;
import com.evolveum.midpoint.web.component.menu.cog.ButtonInlineMenuItem;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItemAction;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;

@PanelType(name = "clusters")
@PanelInstance(
        identifier = "clusters",
        applicableForType = RoleAnalysisSessionType.class,
        display = @PanelDisplay(
                label = "RoleAnalysisSessionType.roleAnalysisClusterRef",
                icon = GuiStyleConstants.CLASS_CIRCLE_FULL,
                order = 10
        )
)
public class ClustersPanel extends AbstractObjectMainPanel<RoleAnalysisSessionType, ObjectDetailsModels<RoleAnalysisSessionType>> {

    private static final String ID_DATATABLE = "datatable";
    private static final String ID_FORM = "form";
    String parentOid;
    String mode;

    public ClustersPanel(String id, ObjectDetailsModels<RoleAnalysisSessionType> model, ContainerPanelConfigurationType config) {
        super(id, model, config);
    }

    public ClustersPanel(String id, String parentOid, String mode, ObjectDetailsModels<RoleAnalysisSessionType> model) {
        super(id, model, null);
        this.parentOid = parentOid;
        this.mode = mode;
    }

    @Override
    protected void initLayout() {
        this.parentOid = getObjectWrapper().getOid();
        this.mode = getPageParameterMode();
        addForm();
    }

    String getPageParameterMode() {
        PageParameters params = getPageBase().getPageParameters();
        return params.get(PageCluster.PARAMETER_MODE).toString();
    }

    public void addForm() {
        Form<?> form = new Form<>(ID_FORM);
        form.setOutputMarkupId(true);
        add(form);
        form.add(clusterTable());
    }

    private ObjectQuery getCustomizeContentQuery() {

        if (parentOid != null) {
            return ((PageBase) getPage()).getPrismContext().queryFor(RoleAnalysisClusterType.class)
                    .item(RoleAnalysisClusterType.F_ROLE_ANALYSIS_SESSION_REF)
                    .ref(parentOid, RoleAnalysisSessionType.COMPLEX_TYPE)
                    .build();
        }
        return null;
    }

    protected MainObjectListPanel<?> clusterTable() {

        MainObjectListPanel<?> basicTable = new MainObjectListPanel<>(ID_DATATABLE, RoleAnalysisClusterType.class) {

            @Override
            protected boolean notContainsNameColumn(@NotNull List<IColumn<SelectableBean<RoleAnalysisClusterType>, String>> iColumns) {
                return false;
            }

            @Override
            protected ISelectableDataProvider<SelectableBean<RoleAnalysisClusterType>> createProvider() {
                SelectableBeanObjectDataProvider<RoleAnalysisClusterType> provider = createSelectableBeanObjectDataProvider(() ->
                        getCustomizeContentQuery(), null);
                provider.setEmptyListOnNullQuery(true);
                provider.setSort(null);
                provider.setDefaultCountIfNull(Integer.MAX_VALUE);
                return provider;
            }

            @Override
            protected boolean isCreateNewObjectVisible() {
                return false;
            }

            @Override
            protected List<InlineMenuItem> createInlineMenu() {
                List<InlineMenuItem> menuItems = new ArrayList<>();
                menuItems.add(ClustersPanel.this.createDeleteInlineMenu());
                return menuItems;
            }

            @Override
            protected List<IColumn<SelectableBean<RoleAnalysisClusterType>, String>> createDefaultColumns() {

                List<IColumn<SelectableBean<RoleAnalysisClusterType>, String>> columns = new ArrayList<>();

                IColumn<SelectableBean<RoleAnalysisClusterType>, String> column;

                column = new SelectableObjectNameColumn<>(createStringResource("ObjectType.name"), null, null, null) {

                    @Serial private static final long serialVersionUID = 1L;

                    @Override
                    public void onClick(AjaxRequestTarget target, IModel<SelectableBean<RoleAnalysisClusterType>> rowModel) {

                        PageBase pageBase = (PageBase) getPage();
                        OperationResult operationResult = new OperationResult("prepareObjects");
                        List<PrismObject<FocusType>> elements = new ArrayList<>();

                        List<ObjectReferenceType> elements1 = rowModel.getObject().getValue().getMember();
                        for (ObjectReferenceType objectReferenceType : elements1) {
                            elements.add(getFocusTypeObject(pageBase, objectReferenceType.getOid(), operationResult));
                        }

                        List<PrismObject<FocusType>> points = new ArrayList<>();

                        ClusterBasicDetailsPanel detailsPanel = new ClusterBasicDetailsPanel(((PageBase) getPage()).getMainPopupBodyId(),
                                Model.of("TO DO: details"), elements, points, mode) {
                            @Override
                            public void onClose(AjaxRequestTarget ajaxRequestTarget) {
                                super.onClose(ajaxRequestTarget);
                            }
                        };
                        ((PageBase) getPage()).showMainPopup(detailsPanel, target);

                    }
                };

                columns.add(column);

                column = new AbstractColumn<>(
                        createStringResource("RoleMining.cluster.table.members.count")) {

                    @Override
                    public void populateItem(Item<ICellPopulator<SelectableBean<RoleAnalysisClusterType>>> cellItem,
                            String componentId, IModel<SelectableBean<RoleAnalysisClusterType>> model) {

                        cellItem.add(new Label(componentId, model.getObject().getValue().getClusterStatistic()
                                .getMemberCount()));
                    }

                    @Override
                    public boolean isSortable() {
                        return false;
                    }

                    @Override
                    public String getSortProperty() {
                        return RoleAnalysisClusterType.F_CLUSTER_STATISTIC.toString();
                    }

                };
                columns.add(column);

                column = new AbstractColumn<>(
                        createStringResource("RoleMining.cluster.table.properties.count")) {

                    @Override
                    public void populateItem(Item<ICellPopulator<SelectableBean<RoleAnalysisClusterType>>> cellItem,
                            String componentId, IModel<SelectableBean<RoleAnalysisClusterType>> model) {

                        if (model.getObject().getValue() != null && model.getObject().getValue()
                                .getClusterStatistic().getPropertiesCount() != null) {
                            cellItem.add(new Label(componentId, model.getObject().getValue()
                                    .getClusterStatistic().getPropertiesCount()));

                        } else {
                            cellItem.add(new Label(componentId,
                                    (Integer) null));
                        }
                    }

                    @Override
                    public boolean isSortable() {
                        return false;
                    }

                    @Override
                    public String getSortProperty() {
                        return RoleAnalysisClusterType.F_CLUSTER_STATISTIC.toString();
                    }
                };
                columns.add(column);

                column = new AbstractColumn<>(
                        createStringResource("RoleMining.cluster.table.column.header.range.properties")) {

                    @Override
                    public void populateItem(Item<ICellPopulator<SelectableBean<RoleAnalysisClusterType>>> cellItem,
                            String componentId, IModel<SelectableBean<RoleAnalysisClusterType>> model) {
                        if (model.getObject().getValue() != null
                                && model.getObject().getValue().getClusterStatistic().getPropertiesMinOccupancy() != null
                                && model.getObject().getValue().getClusterStatistic().getPropertiesMaxOccupancy() != null) {
                            cellItem.add(new Label(componentId, model.getObject().getValue()
                                    .getClusterStatistic().getPropertiesMinOccupancy() + " - " + model.getObject().getValue()
                                    .getClusterStatistic().getPropertiesMaxOccupancy()));

                        } else {
                            cellItem.add(new Label(componentId,
                                    (Integer) null));
                        }
                    }

                    @Override
                    public boolean isSortable() {
                        return false;
                    }

                    @Override
                    public String getSortProperty() {
                        return RoleAnalysisClusterType.F_CLUSTER_STATISTIC.toString();
                    }
                };
                columns.add(column);

                column = new AbstractColumn<>(
                        createStringResource("RoleMining.cluster.table.column.header.mean")) {

                    @Override
                    public void populateItem(Item<ICellPopulator<SelectableBean<RoleAnalysisClusterType>>> cellItem,
                            String componentId, IModel<SelectableBean<RoleAnalysisClusterType>> model) {
                        if (model.getObject().getValue() != null && model.getObject().getValue()
                                .getClusterStatistic().getPropertiesMean() != null) {
                            cellItem.add(new Label(componentId, model.getObject().getValue()
                                    .getClusterStatistic().getPropertiesMean()));

                        } else {
                            cellItem.add(new Label(componentId,
                                    (Integer) null));
                        }
                    }

                    @Override
                    public boolean isSortable() {
                        return false;
                    }

                    @Override
                    public String getSortProperty() {
                        return RoleAnalysisClusterType.F_CLUSTER_STATISTIC.toString();
                    }
                };
                columns.add(column);

                column = new AbstractColumn<>(
                        createStringResource("RoleMining.cluster.table.column.header.reduction")) {

                    @Override
                    public void populateItem(Item<ICellPopulator<SelectableBean<RoleAnalysisClusterType>>> cellItem,
                            String componentId, IModel<SelectableBean<RoleAnalysisClusterType>> model) {

                        List<RoleAnalysisDetectionPatternType> detection = model.getObject().getValue().getDetectionPattern();
                        double maxMetric = 0;
                        if (detection != null) {
                            for (RoleAnalysisDetectionPatternType roleAnalysisDetectionType : detection) {
                                maxMetric = Math.max(maxMetric, roleAnalysisDetectionType.getClusterMetric());
                            }
                        }

                        cellItem.add(new Label(componentId,
                                maxMetric));

                    }

                    @Override
                    public boolean isSortable() {
                        return false;
                    }

                    @Override
                    public String getSortProperty() {
                        return RoleAnalysisClusterType.F_CLUSTER_STATISTIC.toString();
                    }
                };
                columns.add(column);

                column = new AbstractColumn<>(
                        createStringResource("RoleMining.cluster.table.column.header.density")) {

                    @Override
                    public void populateItem(Item<ICellPopulator<SelectableBean<RoleAnalysisClusterType>>> cellItem,
                            String componentId, IModel<SelectableBean<RoleAnalysisClusterType>> model) {

                        String pointsDensity = String.format("%.3f",
                                model.getObject().getValue().getClusterStatistic().getPropertiesDensity());

                        String colorClass = getColorClass(pointsDensity);

                        Label label = new Label(componentId, pointsDensity + " (%)");
                        label.setOutputMarkupId(true);
                        label.add(new AttributeModifier("class", colorClass));
                        label.add(AttributeModifier.append("style", "width: 100px;"));

                        cellItem.add(label);

                    }

                    @Override
                    public boolean isSortable() {
                        return false;
                    }

                    @Override
                    public String getSortProperty() {
                        return RoleAnalysisClusterType.F_CLUSTER_STATISTIC.toString();
                    }
                };
                columns.add(column);

                column = new AbstractColumn<>(
                        createStringResource("RoleMining.cluster.table.load.operation.panel")) {

                    @Override
                    public void populateItem(Item<ICellPopulator<SelectableBean<RoleAnalysisClusterType>>> cellItem,
                            String componentId, IModel<SelectableBean<RoleAnalysisClusterType>> model) {
                        if (model.getObject().getValue() != null && model.getObject().getValue()
                                .getClusterStatistic().getMemberCount() != null) {

                            AjaxIconButton ajaxButton = new AjaxIconButton(componentId, Model.of("fa fa-bars"),
                                    createStringResource("RoleMining.cluster.table.load.operation.panel")) {
                                @Override
                                public void onClick(AjaxRequestTarget ajaxRequestTarget) {
                                    String parentRef = model.getObject().getValue().getRoleAnalysisSessionRef().getOid();
                                    if (parentRef != null) {
                                        PrismObject<RoleAnalysisSessionType> getParent = getParentClusterByOid(getPageBase(),
                                                parentRef, new OperationResult("getParent"));
                                        PageParameters params = new PageParameters();
                                        String oid = model.getObject().getValue().asPrismObject().getOid();
                                        assert getParent != null;

                                        params.set(PageMiningOperation.PARENT_PARAMETER_OID, parentRef);
                                        params.set(OnePageParameterEncoder.PARAMETER, oid);

                                        ((PageBase) getPage()).navigateToNext(PageMiningOperation.class, params);

                                    }
                                }
                            };

                            ajaxButton.add(AttributeAppender.replace("class", " btn btn-default btn-sm d-flex "
                                    + "justify-content-center align-items-center"));
                            ajaxButton.add(new AttributeAppender("style", " width:40px; "));
                            ajaxButton.setOutputMarkupId(true);

                            cellItem.add(ajaxButton);

                        } else {
                            cellItem.add(new Label(componentId,
                                    (Integer) null));
                        }
                    }

                    @Override
                    public boolean isSortable() {
                        return false;
                    }

                    @Override
                    public String getSortProperty() {
                        return RoleAnalysisClusterType.F_CLUSTER_STATISTIC.toString();
                    }
                };
                columns.add(column);

                column = new AbstractColumn<>(
                        createStringResource("RoleMining.cluster.table.similar.image.popup")) {

                    @Override
                    public void populateItem(Item<ICellPopulator<SelectableBean<RoleAnalysisClusterType>>> cellItem,
                            String componentId, IModel<SelectableBean<RoleAnalysisClusterType>> model) {
                        if (model.getObject().getValue() != null && model.getObject().getValue()
                                .getClusterStatistic().getMemberCount() != null) {

                            AjaxIconButton ajaxButton = new AjaxIconButton(componentId, Model.of("fa fa-image"),
                                    createStringResource("RoleMining.cluster.table.similar.image.popup")) {
                                @Override
                                public void onClick(AjaxRequestTarget ajaxRequestTarget) {

                                    ImageDetailsPanel detailsPanel = new ImageDetailsPanel(((PageBase) getPage()).getMainPopupBodyId(),
                                            Model.of("Image"), model.getObject().getValue().asPrismObject().getOid(), mode) {
                                        @Override
                                        public void onClose(AjaxRequestTarget ajaxRequestTarget) {
                                            super.onClose(ajaxRequestTarget);
                                        }
                                    };
                                    ((PageBase) getPage()).showMainPopup(detailsPanel, ajaxRequestTarget);
                                }
                            };

                            ajaxButton.add(AttributeAppender.replace("class", " btn btn-default btn-sm d-flex "
                                    + "justify-content-center align-items-center"));
                            ajaxButton.add(new AttributeAppender("style", " width:40px; "));
                            ajaxButton.setOutputMarkupId(true);
                            cellItem.add(ajaxButton);

                        } else {
                            cellItem.add(new Label(componentId,
                                    (Integer) null));
                        }
                    }

                    @Override
                    public boolean isSortable() {
                        return false;
                    }

                    @Override
                    public String getSortProperty() {
                        return RoleAnalysisClusterType.F_CLUSTER_STATISTIC.toString();
                    }
                };
                columns.add(column);
                return columns;
            }

            @Override
            protected UserProfileStorage.TableId getTableId() {
                return UserProfileStorage.TableId.TABLE_CLUSTER;
            }

            @Override
            protected String getNothingSelectedMessage() {
                return getString("pageUsers.message.nothingSelected");
            }

            @Override
            protected String getConfirmMessageKeyForMultiObject() {
                return "pageUsers.message.confirmationMessageForMultipleObject";
            }

            @Override
            protected String getConfirmMessageKeyForSingleObject() {
                return "pageUsers.message.confirmationMessageForSingleObject";
            }
        };
        basicTable.setOutputMarkupId(true);

        return basicTable;
    }

    private MainObjectListPanel<RoleAnalysisClusterType> getTable() {
        return (MainObjectListPanel<RoleAnalysisClusterType>) get(ID_FORM + ":" + ID_DATATABLE);
    }

    private InlineMenuItem createDeleteInlineMenu() {
        return new ButtonInlineMenuItem(createStringResource("MainObjectListPanel.menu.delete")) {
            @Override
            public CompositedIconBuilder getIconCompositedBuilder() {
                return getDefaultCompositedIconBuilder(GuiStyleConstants.CLASS_ICON_TRASH);
            }

            @Override
            public InlineMenuItemAction initAction() {
                return new ColumnMenuAction<SelectableBean<RoleAnalysisClusterType>>() {
                    @Override
                    public void onClick(AjaxRequestTarget target) {

                        List<SelectableBean<RoleAnalysisClusterType>> selectedObjects = getTable().getSelectedObjects();
                        OperationResult result = new OperationResult("Delete cluster object");
                        if (selectedObjects.size() == 1 && getRowModel() == null) {
                            try {
                                SelectableBean<RoleAnalysisClusterType> roleAnalysisSessionTypeSelectableBean = selectedObjects.get(0);
                                deleteSingleRoleAnalysisCluster(result, roleAnalysisSessionTypeSelectableBean.getValue(),
                                        (PageBase) getPage());
                            } catch (Exception e) {
                                throw new RuntimeException(e);
                            }
                        } else if (getRowModel() != null) {
                            try {
                                IModel<SelectableBean<RoleAnalysisClusterType>> rowModel = getRowModel();
                                deleteSingleRoleAnalysisCluster(result, rowModel.getObject().getValue(), (PageBase) getPage());
                            } catch (Exception e) {
                                throw new RuntimeException(e);
                            }
                        } else {
                            for (SelectableBean<RoleAnalysisClusterType> selectedObject : selectedObjects) {
                                try {
                                    RoleAnalysisClusterType roleAnalysisClusterType = selectedObject.getValue();
                                    deleteSingleRoleAnalysisCluster(result, roleAnalysisClusterType, (PageBase) getPage());
                                } catch (Exception e) {
                                    throw new RuntimeException(e);
                                }
                            }
                        }

                        getTable().refreshTable(target);
                    }
                };
            }

            @Override
            public IModel<String> getConfirmationMessageModel() {
                String actionName = createStringResource("MainObjectListPanel.message.deleteAction").getString();
                return getTable().getConfirmationMessageModel((ColumnMenuAction<?>) getAction(), actionName);
            }
        };
    }

}
