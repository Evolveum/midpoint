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

import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisClusterType;

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
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisSessionType;

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
            protected ISelectableDataProvider<SelectableBean<RoleAnalysisClusterType>> createProvider() {
                SelectableBeanObjectDataProvider<RoleAnalysisClusterType> provider = createSelectableBeanObjectDataProvider(() ->
                        getCustomizeContentQuery(), null);
                provider.setEmptyListOnNullQuery(true);
                provider.setSort(null);
                provider.setDefaultCountIfNull(Integer.MAX_VALUE);
                return provider;
            }

            @Override
            protected List<IColumn<SelectableBean<RoleAnalysisClusterType>, String>> createDefaultColumns() {

                List<IColumn<SelectableBean<RoleAnalysisClusterType>, String>> columns = new ArrayList<>();

                IColumn<SelectableBean<RoleAnalysisClusterType>, String> column;

                column = new ObjectNameColumn<>(createStringResource("ObjectType.name")) {

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
                        createStringResource("RoleMining.cluster.table.column.header.density")) {

                    @Override
                    public void populateItem(Item<ICellPopulator<SelectableBean<RoleAnalysisClusterType>>> cellItem,
                            String componentId, IModel<SelectableBean<RoleAnalysisClusterType>> model) {

                        String pointsDensity = String.format("%.3f",
                                model.getObject().getValue().getClusterStatistic().getPropertiesDensity());

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
                        return false;
                    }

                    @Override
                    public String getSortProperty() {
                        return RoleAnalysisClusterType.F_CLUSTER_STATISTIC.toString();
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

                    @Override
                    public String getCssClass() {
                        return "col-md-2 col-lg-1";
                    }
                };
                columns.add(column);

                column = new AbstractColumn<>(
                        createStringResource("RoleMining.cluster.table.roles.count")) {

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
                    public String getCssClass() {
                        return "col-md-2 col-lg-1";
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
                        createStringResource("RoleMining.cluster.table.column.header.min.roles")) {

                    @Override
                    public void populateItem(Item<ICellPopulator<SelectableBean<RoleAnalysisClusterType>>> cellItem,
                            String componentId, IModel<SelectableBean<RoleAnalysisClusterType>> model) {
                        if (model.getObject().getValue() != null && model.getObject().getValue()
                                .getClusterStatistic().getPropertiesMinOccupation() != null) {
                            cellItem.add(new Label(componentId, model.getObject().getValue()
                                    .getClusterStatistic().getPropertiesMinOccupation()));

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
                        return false;
                    }

                    @Override
                    public String getSortProperty() {
                        return RoleAnalysisClusterType.F_CLUSTER_STATISTIC.toString();
                    }
                };
                columns.add(column);

                column = new AbstractColumn<>(
                        createStringResource("RoleMining.cluster.table.column.header.max.roles")) {

                    @Override
                    public void populateItem(Item<ICellPopulator<SelectableBean<RoleAnalysisClusterType>>> cellItem,
                            String componentId, IModel<SelectableBean<RoleAnalysisClusterType>> model) {
                        if (model.getObject().getValue() != null && model.getObject().getValue()
                                .getClusterStatistic().getPropertiesMaxOccupation() != null) {
                            cellItem.add(new Label(componentId, model.getObject().getValue()
                                    .getClusterStatistic().getPropertiesMaxOccupation()));

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
                    public String getCssClass() {
                        return "col-md-2 col-lg-1";
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
                        createStringResource("RoleMining.cluster.table.similar.groups.count")) {

                    @Override
                    public void populateItem(Item<ICellPopulator<SelectableBean<RoleAnalysisClusterType>>> cellItem,
                            String componentId, IModel<SelectableBean<RoleAnalysisClusterType>> model) {
                        if (model.getObject().getValue() != null && model.getObject().getValue()
                                .getClusterStatistic().getMemberCount() != null) {

                            AjaxButton ajaxButton = new AjaxButton(componentId,
                                    Model.of(String.valueOf(model.getObject().getValue()
                                            .getClusterStatistic().getMemberCount()))) {
                                @Override
                                public void onClick(AjaxRequestTarget ajaxRequestTarget) {
                                    String parentRef = model.getObject().getValue().getRoleAnalysisSessionRef().getOid();
                                    if (parentRef != null) {
                                        PrismObject<RoleAnalysisSessionType> getParent = getParentClusterByOid(getPageBase(),
                                                parentRef, new OperationResult("getParent"));
                                        PageParameters params = new PageParameters();
                                        String oid = model.getObject().getValue().asPrismObject().getOid();
                                        assert getParent != null;
                                        String processMode = getParent.asObjectable().getClusterOptions().getProcessMode().value();
                                        String searchMode = getParent.asObjectable().getPatternDetectionOption().getSearchMode().value();
                                        Integer elementsCount = model.getObject().getValue().getClusterStatistic().getMemberCount();
                                        Integer pointsCount = model.getObject().getValue().getClusterStatistic().getPropertiesCount();
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

}
