/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.tables;

import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils.ClusterObjectUtils.getFocusObject;
import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils.ClusterObjectUtils.getParentById;

import java.io.Serial;
import java.util.ArrayList;
import java.util.List;

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
import com.evolveum.midpoint.xml.ns._public.common.common_3.ClusterType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ParentClusterType;

public class ClusterOperationPanel extends Panel {

    private static final String ID_DATATABLE = "datatable";
    private static final String ID_FORM = "form";
    String identifier;
    String mode;

    public ClusterOperationPanel(String id, String identifier, String mode) {
        super(id);
        this.identifier = identifier;
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
        if (identifier != null) {
            return ((PageBase) getPage()).getPrismContext().queryFor(ClusterType.class)
                    .item(ClusterType.F_IDENTIFIER).eq(identifier)
                    .build();
        }
        return ((PageBase) getPage()).getPrismContext().queryFor(ClusterType.class).not()
                .item(ClusterType.F_ELEMENT_COUNT).eq(0)
                .build();
    }

    protected MainObjectListPanel<?> clusterTable() {

        MainObjectListPanel<?> basicTable = new MainObjectListPanel<>(ID_DATATABLE, ClusterType.class) {

            @Override
            protected ISelectableDataProvider<SelectableBean<ClusterType>> createProvider() {
                SelectableBeanObjectDataProvider<ClusterType> provider = createSelectableBeanObjectDataProvider(() ->
                        getCustomizeContentQuery(), null);
                provider.setEmptyListOnNullQuery(true);
                provider.setSort(null);
                provider.setDefaultCountIfNull(Integer.MAX_VALUE);
                return provider;
            }

            @Override
            protected List<IColumn<SelectableBean<ClusterType>, String>> createDefaultColumns() {

                List<IColumn<SelectableBean<ClusterType>, String>> columns = new ArrayList<>();

                IColumn<SelectableBean<ClusterType>, String> column;

                column = new ObjectNameColumn<>(createStringResource("ObjectType.name")) {

                    @Serial private static final long serialVersionUID = 1L;

                    @Override
                    public void onClick(AjaxRequestTarget target, IModel<SelectableBean<ClusterType>> rowModel) {

                        PageBase pageBase = (PageBase) getPage();
                        OperationResult operationResult = new OperationResult("prepareObjects");
                        List<PrismObject<FocusType>> elements = new ArrayList<>();

                        List<String> elements1 = rowModel.getObject().getValue().getElements();
                        for (String s : elements1) {
                            elements.add(getFocusObject(pageBase, s, operationResult));
                        }

                        List<PrismObject<FocusType>> points = new ArrayList<>();

                        List<String> points1 = rowModel.getObject().getValue().getPoints();
                        for (String s : points1) {
                            points.add(getFocusObject(pageBase, s, operationResult));
                        }

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
                        createStringResource("RoleMining.cluster.table.column.header.cluster")) {

                    @Override
                    public void populateItem(Item<ICellPopulator<SelectableBean<ClusterType>>> cellItem,
                            String componentId, IModel<SelectableBean<ClusterType>> model) {
                        if (model.getObject().getValue() != null && model.getObject().getValue().getIdentifier() != null) {
                            cellItem.add(new Label(componentId, model.getObject().getValue().getIdentifier()));

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
                        return ClusterType.F_IDENTIFIER.toString();
                    }
                };
                columns.add(column);

                column = new AbstractColumn<>(
                        createStringResource("RoleMining.cluster.table.column.header.density")) {

                    @Override
                    public void populateItem(Item<ICellPopulator<SelectableBean<ClusterType>>> cellItem,
                            String componentId, IModel<SelectableBean<ClusterType>> model) {
                        if (model.getObject().getValue() != null && model.getObject().getValue().getDensity() != null) {
                            cellItem.add(new Label(componentId, model.getObject().getValue().getDensity()));

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
                        return ClusterType.F_DENSITY.toString();
                    }
                };
                columns.add(column);

                column = new AbstractColumn<>(
                        createStringResource("RoleMining.cluster.table.members.count")) {

                    @Override
                    public void populateItem(Item<ICellPopulator<SelectableBean<ClusterType>>> cellItem,
                            String componentId, IModel<SelectableBean<ClusterType>> model) {
                        cellItem.add(new Label(componentId,
                                model.getObject().getValue() != null && model.getObject().getValue().getElementCount() != null ?
                                        model.getObject().getValue().getElementCount() : null));
                    }

                    @Override
                    public boolean isSortable() {
                        return true;
                    }

                    @Override
                    public String getSortProperty() {
                        return ClusterType.F_ELEMENT_COUNT.toString();
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
                    public void populateItem(Item<ICellPopulator<SelectableBean<ClusterType>>> cellItem,
                            String componentId, IModel<SelectableBean<ClusterType>> model) {
                        if (model.getObject().getValue() != null && model.getObject().getValue().getPointCount() != null) {
                            cellItem.add(new Label(componentId, model.getObject().getValue().getPointCount()));

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
                        return ClusterType.F_POINT_COUNT.toString();
                    }
                };
                columns.add(column);

                column = new AbstractColumn<>(
                        createStringResource("RoleMining.cluster.table.column.header.min.roles")) {

                    @Override
                    public void populateItem(Item<ICellPopulator<SelectableBean<ClusterType>>> cellItem,
                            String componentId, IModel<SelectableBean<ClusterType>> model) {
                        if (model.getObject().getValue() != null && model.getObject().getValue().getMinOccupation() != null) {
                            cellItem.add(new Label(componentId, model.getObject().getValue().getMinOccupation()));

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
                        return ClusterType.F_MIN_OCCUPATION.toString();
                    }
                };
                columns.add(column);

                column = new AbstractColumn<>(
                        createStringResource("RoleMining.cluster.table.column.header.max.roles")) {

                    @Override
                    public void populateItem(Item<ICellPopulator<SelectableBean<ClusterType>>> cellItem,
                            String componentId, IModel<SelectableBean<ClusterType>> model) {
                        if (model.getObject().getValue() != null && model.getObject().getValue().getMaxOccupation() != null) {
                            cellItem.add(new Label(componentId, model.getObject().getValue().getMaxOccupation()));

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
                        return ClusterType.F_MAX_OCCUPATION.toString();
                    }
                };
                columns.add(column);

                column = new AbstractColumn<>(
                        createStringResource("RoleMining.cluster.table.column.header.mean")) {

                    @Override
                    public void populateItem(Item<ICellPopulator<SelectableBean<ClusterType>>> cellItem,
                            String componentId, IModel<SelectableBean<ClusterType>> model) {
                        if (model.getObject().getValue() != null && model.getObject().getValue().getMean() != null) {
                            cellItem.add(new Label(componentId, model.getObject().getValue().getMean()));

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
                        return ClusterType.F_MEAN.toString();
                    }
                };
                columns.add(column);
                column = new AbstractColumn<>(
                        createStringResource("RoleMining.cluster.table.similar.groups.count")) {

                    @Override
                    public void populateItem(Item<ICellPopulator<SelectableBean<ClusterType>>> cellItem,
                            String componentId, IModel<SelectableBean<ClusterType>> model) {
                        if (model.getObject().getValue() != null && model.getObject().getValue().getElementCount() != null) {

                            AjaxButton ajaxButton = new AjaxButton(componentId,
                                    Model.of(String.valueOf(model.getObject().getValue().getElementCount()))) {
                                @Override
                                public void onClick(AjaxRequestTarget ajaxRequestTarget) {

                                    PrismObject<ParentClusterType> getParent = getParentById(getPageBase(),
                                            model.getObject().getValue().getIdentifier(), new OperationResult("getParent"));
                                    PageParameters params = new PageParameters();
                                    params.set(PageMiningOperation.PARAMETER_OID, model.getObject().getValue().asPrismObject().getOid());
                                    params.set(PageMiningOperation.PARAMETER_MODE, getParent.asObjectable().getMode());
                                    ((PageBase) getPage()).navigateToNext(PageMiningOperation.class, params);

                                }
                            };

                            ajaxButton.add(AttributeAppender.replace("class", " btn btn-primary btn-sm d-flex "
                                    + "justify-content-center align-items-center"));
                            ajaxButton.add(new AttributeAppender("style", " width:100px; height:20px"));
                            ajaxButton.setOutputMarkupId(true);

                            if (model.getObject().getValue().getElementCount() > 500) {
                                ajaxButton.setEnabled(false);
                            }
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
                        return ClusterType.F_ELEMENT_COUNT.toString();
                    }
                };
                columns.add(column);

                column = new AbstractColumn<>(
                        createStringResource("RoleMining.cluster.table.similar.image.popup")) {

                    @Override
                    public void populateItem(Item<ICellPopulator<SelectableBean<ClusterType>>> cellItem,
                            String componentId, IModel<SelectableBean<ClusterType>> model) {
                        if (model.getObject().getValue() != null && model.getObject().getValue().getElementCount() != null) {

                            AjaxButton ajaxButton = new AjaxButton(componentId,
                                    Model.of("popup")) {
                                @Override
                                public void onClick(AjaxRequestTarget ajaxRequestTarget) {

                                    ImageDetailsPanel detailsPanel = new ImageDetailsPanel(((PageBase) getPage()).getMainPopupBodyId(),
                                            Model.of("Image"), model.getObject().getValue(), mode) {
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
                        return ClusterType.F_ELEMENT_COUNT.toString();
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
