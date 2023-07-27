/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.tables;

import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.tables.Tools.getColorClass;
import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils.ClusterObjectUtils.getFocusTypeObject;
import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils.ClusterObjectUtils.getParentClusterByOid;

import java.io.Serial;
import java.util.ArrayList;
import java.util.List;

import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisSessionType;

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

import com.evolveum.midpoint.gui.api.component.MainObjectListPanel;
import com.evolveum.midpoint.gui.api.component.data.provider.ISelectableDataProvider;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.impl.component.data.provider.SelectableBeanObjectDataProvider;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.details.objects.ClusterBasicDetailsPanel;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.details.work.ImageDetailsPanel;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.PageMiningOperation;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.data.column.ObjectNameColumn;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisCluster;

public class PageClusters extends Panel {

    private static final String ID_DATATABLE = "datatable";
    private static final String ID_FORM = "form";
    String parentOid;
    String mode;

    public PageClusters(String id, String parentOid, String mode) {
        super(id);
        this.parentOid = parentOid;
        this.mode = mode;
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        addForm();
    }

    public void addForm() {
        Form<?> form = new Form<>(ID_FORM);
        form.setOutputMarkupId(true);
        add(form);
        form.add(clusterTable());
    }

    private ObjectQuery getCustomizeContentQuery() {

        if (parentOid != null) {
            return ((PageBase) getPage()).getPrismContext().queryFor(RoleAnalysisCluster.class)
                    .item(RoleAnalysisCluster.F_PARENT_REF).eq(parentOid)
                    .build();
        }
        return null;
    }

    protected MainObjectListPanel<?> clusterTable() {

        MainObjectListPanel<?> basicTable = new MainObjectListPanel<>(ID_DATATABLE, RoleAnalysisCluster.class) {

            @Override
            protected ISelectableDataProvider<SelectableBean<RoleAnalysisCluster>> createProvider() {
                SelectableBeanObjectDataProvider<RoleAnalysisCluster> provider = createSelectableBeanObjectDataProvider(() ->
                        getCustomizeContentQuery(), null);
                provider.setEmptyListOnNullQuery(true);
                provider.setSort(null);
                provider.setDefaultCountIfNull(Integer.MAX_VALUE);
                return provider;
            }

            @Override
            protected List<IColumn<SelectableBean<RoleAnalysisCluster>, String>> createDefaultColumns() {

                List<IColumn<SelectableBean<RoleAnalysisCluster>, String>> columns = new ArrayList<>();

                IColumn<SelectableBean<RoleAnalysisCluster>, String> column;

                column = new ObjectNameColumn<>(createStringResource("ObjectType.name")) {

                    @Serial private static final long serialVersionUID = 1L;

                    @Override
                    public void onClick(AjaxRequestTarget target, IModel<SelectableBean<RoleAnalysisCluster>> rowModel) {

                        PageBase pageBase = (PageBase) getPage();
                        OperationResult operationResult = new OperationResult("prepareObjects");
                        List<PrismObject<FocusType>> elements = new ArrayList<>();

                        List<String> elements1 = rowModel.getObject().getValue().getElements();
                        for (String s : elements1) {
                            elements.add(getFocusTypeObject(pageBase, s, operationResult));
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
                        createStringResource("RoleMining.cluster.table.column.header.density")) {

                    @Override
                    public void populateItem(Item<ICellPopulator<SelectableBean<RoleAnalysisCluster>>> cellItem,
                            String componentId, IModel<SelectableBean<RoleAnalysisCluster>> model) {

                        String pointsDensity = model.getObject().getValue().getPointsDensity();
                        String colorClass = getColorClass(pointsDensity);

                        Label label = new Label(componentId, pointsDensity);
                        label.setOutputMarkupId(true);
                        label.add(new AttributeModifier("class", colorClass));
                        label.add(AttributeModifier.append("style", "width: 100px;"));

                        cellItem.add(label);

                    }

                    @Override
                    public String getCssClass() {
                        return "col-md-2 col-lg-1";
                    }

                    @Override
                    public boolean isSortable() {
                        return true;
                    }

                    @Override
                    public String getSortProperty() {
                        return RoleAnalysisCluster.F_POINTS_DENSITY.toString();
                    }
                };
                columns.add(column);

                column = new AbstractColumn<>(
                        createStringResource("RoleMining.cluster.table.members.count")) {

                    @Override
                    public void populateItem(Item<ICellPopulator<SelectableBean<RoleAnalysisCluster>>> cellItem,
                            String componentId, IModel<SelectableBean<RoleAnalysisCluster>> model) {

                        cellItem.add(new Label(componentId,
                                model.getObject().getValue() != null && model.getObject().getValue().getElementsCount() != null ?
                                        model.getObject().getValue().getElementsCount() : null));
                    }

                    @Override
                    public boolean isSortable() {
                        return true;
                    }

                    @Override
                    public String getSortProperty() {
                        return RoleAnalysisCluster.F_ELEMENTS_COUNT.toString();
                    }

                    @Override
                    public String getCssClass() {
                        return "col-md-2 col-lg-1";
                    }
                };
                columns.add(column);

                column = new AbstractColumn<>(
                        createStringResource("RoleMining.cluster.table.roles.count")) {

                    @Override
                    public void populateItem(Item<ICellPopulator<SelectableBean<RoleAnalysisCluster>>> cellItem,
                            String componentId, IModel<SelectableBean<RoleAnalysisCluster>> model) {
                        if (model.getObject().getValue() != null && model.getObject().getValue().getPointsCount() != null) {
                            cellItem.add(new Label(componentId, model.getObject().getValue().getPointsCount()));

                        } else {
                            cellItem.add(new Label(componentId,
                                    (Integer) null));
                        }
                    }

                    @Override
                    public String getCssClass() {
                        return "col-md-2 col-lg-1";
                    }

                    @Override
                    public boolean isSortable() {
                        return true;
                    }

                    @Override
                    public String getSortProperty() {
                        return RoleAnalysisCluster.F_POINTS_COUNT.toString();
                    }
                };
                columns.add(column);

                column = new AbstractColumn<>(
                        createStringResource("RoleMining.cluster.table.column.header.min.roles")) {

                    @Override
                    public void populateItem(Item<ICellPopulator<SelectableBean<RoleAnalysisCluster>>> cellItem,
                            String componentId, IModel<SelectableBean<RoleAnalysisCluster>> model) {
                        if (model.getObject().getValue() != null && model.getObject().getValue().getPointsMinOccupation() != null) {
                            cellItem.add(new Label(componentId, model.getObject().getValue().getPointsMinOccupation()));

                        } else {
                            cellItem.add(new Label(componentId,
                                    (Integer) null));
                        }
                    }

                    @Override
                    public String getCssClass() {
                        return "col-md-2 col-lg-1";
                    }

                    @Override
                    public boolean isSortable() {
                        return true;
                    }

                    @Override
                    public String getSortProperty() {
                        return RoleAnalysisCluster.F_POINTS_MIN_OCCUPATION.toString();
                    }
                };
                columns.add(column);

                column = new AbstractColumn<>(
                        createStringResource("RoleMining.cluster.table.column.header.max.roles")) {

                    @Override
                    public void populateItem(Item<ICellPopulator<SelectableBean<RoleAnalysisCluster>>> cellItem,
                            String componentId, IModel<SelectableBean<RoleAnalysisCluster>> model) {
                        if (model.getObject().getValue() != null && model.getObject().getValue().getPointsMaxOccupation() != null) {
                            cellItem.add(new Label(componentId, model.getObject().getValue().getPointsMaxOccupation()));

                        } else {
                            cellItem.add(new Label(componentId,
                                    (Integer) null));
                        }
                    }

                    @Override
                    public String getCssClass() {
                        return "col-md-2 col-lg-1";
                    }

                    @Override
                    public boolean isSortable() {
                        return true;
                    }

                    @Override
                    public String getSortProperty() {
                        return RoleAnalysisCluster.F_POINTS_MAX_OCCUPATION.toString();
                    }
                };
                columns.add(column);

                column = new AbstractColumn<>(
                        createStringResource("RoleMining.cluster.table.column.header.mean")) {

                    @Override
                    public void populateItem(Item<ICellPopulator<SelectableBean<RoleAnalysisCluster>>> cellItem,
                            String componentId, IModel<SelectableBean<RoleAnalysisCluster>> model) {
                        if (model.getObject().getValue() != null && model.getObject().getValue().getPointsMean() != null) {
                            cellItem.add(new Label(componentId, model.getObject().getValue().getPointsMean()));

                        } else {
                            cellItem.add(new Label(componentId,
                                    (Integer) null));
                        }
                    }

                    @Override
                    public String getCssClass() {
                        return "col-md-2 col-lg-1";
                    }

                    @Override
                    public boolean isSortable() {
                        return true;
                    }

                    @Override
                    public String getSortProperty() {
                        return RoleAnalysisCluster.F_POINTS_MEAN.toString();
                    }
                };
                columns.add(column);
                column = new AbstractColumn<>(
                        createStringResource("RoleMining.cluster.table.similar.groups.count")) {

                    @Override
                    public void populateItem(Item<ICellPopulator<SelectableBean<RoleAnalysisCluster>>> cellItem,
                            String componentId, IModel<SelectableBean<RoleAnalysisCluster>> model) {
                        if (model.getObject().getValue() != null && model.getObject().getValue().getElementsCount() != null) {

                            AjaxButton ajaxButton = new AjaxButton(componentId,
                                    Model.of(String.valueOf(model.getObject().getValue().getElementsCount()))) {
                                @Override
                                public void onClick(AjaxRequestTarget ajaxRequestTarget) {
                                    String parentRef = model.getObject().getValue().getParentRef();
                                    if (parentRef != null) {
                                        PrismObject<RoleAnalysisSessionType> getParent = getParentClusterByOid(getPageBase(),
                                                parentRef, new OperationResult("getParent"));
                                        PageParameters params = new PageParameters();
                                        String oid = model.getObject().getValue().asPrismObject().getOid();
                                        assert getParent != null;
                                        String processMode = getParent.asObjectable().getClusterOptions().getProcessMode().value();
                                        String searchMode = getParent.asObjectable().getPatternDetectionOptions().getSearchMode().value();
                                        Integer elementsCount = model.getObject().getValue().getElementsCount();
                                        Integer pointsCount = model.getObject().getValue().getPointsCount();
                                        int max = Math.max(elementsCount, pointsCount);

                                        params.set(PageMiningOperation.PARAMETER_OID, oid);
                                        params.set(PageMiningOperation.PARAMETER_MODE, processMode);
                                        params.set(PageMiningOperation.PARAMETER_SEARCH_MODE, searchMode);
                                        params.set(PageMiningOperation.PARAMETER_SORT, max);

                                        ((PageBase) getPage()).navigateToNext(PageMiningOperation.class, params);

                                    }
                                }
                            };

                            ajaxButton.add(AttributeAppender.replace("class", " btn btn-primary btn-sm d-flex "
                                    + "justify-content-center align-items-center"));
                            ajaxButton.add(new AttributeAppender("style", " width:100px; height:20px"));
                            ajaxButton.setOutputMarkupId(true);

//                            if (model.getObject().getValue().getElementCount() > 500) {
//                                ajaxButton.setEnabled(false);
//                            }
                            cellItem.add(ajaxButton);

                        } else {
                            cellItem.add(new Label(componentId,
                                    (Integer) null));
                        }
                    }

                    @Override
                    public String getCssClass() {
                        return "col-md-2 col-lg-1";
                    }

                    @Override
                    public boolean isSortable() {
                        return true;
                    }

                    @Override
                    public String getSortProperty() {
                        return RoleAnalysisCluster.F_ELEMENTS_COUNT.toString();
                    }
                };
                columns.add(column);

                column = new AbstractColumn<>(
                        createStringResource("RoleMining.cluster.table.similar.image.popup")) {

                    @Override
                    public void populateItem(Item<ICellPopulator<SelectableBean<RoleAnalysisCluster>>> cellItem,
                            String componentId, IModel<SelectableBean<RoleAnalysisCluster>> model) {
                        if (model.getObject().getValue() != null && model.getObject().getValue().getElementsCount() != null) {

                            AjaxButton ajaxButton = new AjaxButton(componentId,
                                    Model.of("popup")) {
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

                            ajaxButton.add(AttributeAppender.replace("class", " btn btn-primary btn-sm d-flex "
                                    + "justify-content-center align-items-center"));
                            ajaxButton.add(new AttributeAppender("style", " width:100px; height:20px"));
                            ajaxButton.setOutputMarkupId(true);
                            cellItem.add(ajaxButton);

                        } else {
                            cellItem.add(new Label(componentId,
                                    (Integer) null));
                        }
                    }

                    @Override
                    public String getCssClass() {
                        return "col-md-2 col-lg-1";
                    }

                    @Override
                    public boolean isSortable() {
                        return true;
                    }

                    @Override
                    public String getSortProperty() {
                        return RoleAnalysisCluster.F_ELEMENTS_COUNT.toString();
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

}
