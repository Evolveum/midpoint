/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.panels.tables;

import java.util.ArrayList;
import java.util.List;

import com.evolveum.midpoint.gui.impl.page.admin.role.panels.details.ImageDetailsPanel;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.export.AbstractExportableColumn;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import com.evolveum.midpoint.gui.api.component.MainObjectListPanel;
import com.evolveum.midpoint.gui.api.component.data.provider.ISelectableDataProvider;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.impl.page.admin.role.panels.details.ClusterDetailsPanel;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MiningType;

public class ClusterTable extends Panel {

    private static final String ID_DATATABLE = "datatable";
    private static final String ID_FORM = "form";

    public ClusterTable(String id) {
        super(id);
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
        form.add(basicUserHelperTable());
    }

    protected MainObjectListPanel<?> basicUserHelperTable() {

        MainObjectListPanel<?> basicTable = new MainObjectListPanel<>(ID_DATATABLE, MiningType.class) {

            @Override
            protected ISelectableDataProvider<SelectableBean<MiningType>> createProvider() {
                return super.createProvider();
            }

            @Override
            protected List<IColumn<SelectableBean<MiningType>, String>> createDefaultColumns() {

                List<IColumn<SelectableBean<MiningType>, String>> columns = new ArrayList<>();

                IColumn<SelectableBean<MiningType>, String> column;

                column = new AbstractExportableColumn<>(
                        createStringResource("RoleMining.cluster.table.members.count")) {

                    @Override
                    public void populateItem(Item<ICellPopulator<SelectableBean<MiningType>>> cellItem,
                            String componentId, IModel<SelectableBean<MiningType>> model) {
                        cellItem.add(new Label(componentId,
                                model.getObject().getValue() != null && model.getObject().getValue().getMembersCount() != null ?
                                        model.getObject().getValue().getMembersCount() : null));
                    }

                    @Override
                    public IModel<String> getDataModel(IModel<SelectableBean<MiningType>> rowModel) {
                        return Model.of("");
                    }

                    @Override
                    public boolean isSortable() {
                        return true;
                    }

                    @Override
                    public String getSortProperty() {
                        return MiningType.F_MEMBERS_COUNT.toString();
                    }

                    @Override
                    public String getCssClass() {
                        return "col-md-2 col-lg-1";
                    }
                };
                columns.add(column);

                column = new AbstractExportableColumn<>(
                        createStringResource("RoleMining.cluster.table.roles.count")) {

                    @Override
                    public void populateItem(Item<ICellPopulator<SelectableBean<MiningType>>> cellItem,
                            String componentId, IModel<SelectableBean<MiningType>> model) {
                        if (model.getObject().getValue() != null && model.getObject().getValue().getRolesCount() != null) {
                            cellItem.add(new Label(componentId, model.getObject().getValue().getRolesCount()));

                        } else {
                            cellItem.add(new Label(componentId,
                                    (Integer) null));
                        }
                    }

                    @Override
                    public IModel<String> getDataModel(IModel<SelectableBean<MiningType>> rowModel) {
                        return Model.of("");
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
                        return MiningType.F_ROLES_COUNT.toString();
                    }
                };
                columns.add(column);

                column = new AbstractExportableColumn<>(
                        createStringResource("RoleMining.cluster.table.similar.groups.count")) {

                    @Override
                    public void populateItem(Item<ICellPopulator<SelectableBean<MiningType>>> cellItem,
                            String componentId, IModel<SelectableBean<MiningType>> model) {
                        if (model.getObject().getValue() != null && model.getObject().getValue().getSimilarGroupsCount() != null) {
//                            cellItem.add(new Label(componentId, model.getObject().getValue().getSimilarGroupsCount()));

                            AjaxButton ajaxButton = new AjaxButton(componentId,
                                    Model.of(String.valueOf(model.getObject().getValue().getSimilarGroupsCount()))) {
                                @Override
                                public void onClick(AjaxRequestTarget ajaxRequestTarget) {
                                    ClusterDetailsPanel detailsPanel = new ClusterDetailsPanel(((PageBase) getPage()).getMainPopupBodyId(),
                                            Model.of("Groups"), model.getObject().getValue().getSimilarGroupsId(),model.getObject().getValue()) {
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
                    public IModel<String> getDataModel(IModel<SelectableBean<MiningType>> rowModel) {
                        return Model.of("");
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
                        return MiningType.F_SIMILAR_GROUPS_COUNT.toString();
                    }
                };
                columns.add(column);


                column = new AbstractExportableColumn<>(
                        createStringResource("RoleMining.cluster.table.similar.image.popup")) {

                    @Override
                    public void populateItem(Item<ICellPopulator<SelectableBean<MiningType>>> cellItem,
                            String componentId, IModel<SelectableBean<MiningType>> model) {
                        if (model.getObject().getValue() != null && model.getObject().getValue().getSimilarGroupsCount() != null) {

                            AjaxButton ajaxButton = new AjaxButton(componentId,
                                    Model.of("popup")) {
                                @Override
                                public void onClick(AjaxRequestTarget ajaxRequestTarget) {
                                    ImageDetailsPanel detailsPanel = new ImageDetailsPanel(((PageBase) getPage()).getMainPopupBodyId(),
                                            Model.of("Image"), model.getObject().getValue().getSimilarGroupsId(),model.getObject().getValue()) {
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
                    public IModel<String> getDataModel(IModel<SelectableBean<MiningType>> rowModel) {
                        return Model.of("");
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
                        return MiningType.F_SIMILAR_GROUPS_COUNT.toString();
                    }
                };
                columns.add(column);
                return columns;
            }

            @Override
            protected UserProfileStorage.TableId getTableId() {
                return UserProfileStorage.TableId.TABLE_MINING;
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
