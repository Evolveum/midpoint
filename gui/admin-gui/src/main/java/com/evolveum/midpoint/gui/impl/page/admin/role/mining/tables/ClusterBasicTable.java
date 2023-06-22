/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.tables;

import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils.MiningObjectUtils.getMiningObject;
import static com.evolveum.midpoint.security.api.MidPointPrincipalManager.DOT_CLASS;

import java.io.Serial;
import java.util.ArrayList;
import java.util.List;

import com.evolveum.midpoint.gui.impl.page.admin.role.mining.details.objects.ClusterBasicDetailsPanel;

import com.evolveum.midpoint.gui.impl.page.admin.role.mining.details.work.ClusterDetailsPanel;

import com.evolveum.midpoint.gui.impl.page.admin.role.mining.details.work.ImageDetailsPanel;

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

import com.evolveum.midpoint.gui.api.component.MainObjectListPanel;
import com.evolveum.midpoint.gui.api.component.data.provider.ISelectableDataProvider;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.impl.component.data.provider.SelectableBeanObjectDataProvider;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.data.column.ObjectNameColumn;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ClusterType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MiningType;

public class ClusterBasicTable extends Panel {

    private static final String ID_DATATABLE = "datatable";
    private static final String ID_FORM = "form";
    String identifier;

    public ClusterBasicTable(String id, String identifier) {
        super(id);
        this.identifier = identifier;
        System.out.println(identifier);
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
                .item(ClusterType.F_SIMILAR_GROUPS_COUNT).eq(0)
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

                        ClusterBasicDetailsPanel detailsPanel = new ClusterBasicDetailsPanel(((PageBase) getPage()).getMainPopupBodyId(),
                                Model.of("TO DO: details"),rowModel) {
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
                        createStringResource("Cluster")) {

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
                        createStringResource("Density")) {

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
                                model.getObject().getValue() != null && model.getObject().getValue().getMembersCount() != null ?
                                        model.getObject().getValue().getMembersCount() : null));
                    }

                    @Override
                    public boolean isSortable() {
                        return true;
                    }

                    @Override
                    public String getSortProperty() {
                        return ClusterType.F_MEMBERS_COUNT.toString();
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
                        if (model.getObject().getValue() != null && model.getObject().getValue().getRolesCount() != null) {
                            cellItem.add(new Label(componentId, model.getObject().getValue().getRolesCount()));

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
                        return ClusterType.F_ROLES_COUNT.toString();
                    }
                };
                columns.add(column);

                column = new AbstractColumn<>(
                        createStringResource("Min roles")) {

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
                        createStringResource("Max roles")) {

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
                        createStringResource("Mean")) {

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
                        if (model.getObject().getValue() != null && model.getObject().getValue().getSimilarGroupsCount() != null) {

                            AjaxButton ajaxButton = new AjaxButton(componentId,
                                    Model.of(String.valueOf(model.getObject().getValue().getSimilarGroupsCount()))) {
                                @Override
                                public void onClick(AjaxRequestTarget ajaxRequestTarget) {
                                    long startTime = System.currentTimeMillis();

                                    List<String> similarGroupsId = model.getObject().getValue().getSimilarGroupsId();
                                    String string = DOT_CLASS + "getMiningTypeObject";
                                    OperationResult result = new OperationResult(string);
                                    List<PrismObject<MiningType>> miningTypeList = new ArrayList<>();

                                    for (String groupOid : similarGroupsId) {
                                        PrismObject<MiningType> miningObject = getMiningObject(getPageBase(), groupOid, result);

                                        miningTypeList.add(miningObject);
                                    }

                                    List<String> roles = model.getObject().getValue().getRoles();

                                    long endTime = System.currentTimeMillis();
                                    long elapsedTime = endTime - startTime;
                                    double elapsedSeconds = elapsedTime / 1000.0;
                                    System.out.println("Elapsed time: " + elapsedSeconds + " seconds (prepare table data))");
                                    ClusterDetailsPanel detailsPanel = new ClusterDetailsPanel(((PageBase) getPage()).getMainPopupBodyId(),
                                            Model.of("Groups"), miningTypeList, roles, null) {
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

                            if (model.getObject().getValue().getSimilarGroupsCount() > 500) {
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
                        return ClusterType.F_SIMILAR_GROUPS_COUNT.toString();
                    }
                };
                columns.add(column);

                column = new AbstractColumn<>(
                        createStringResource("RoleMining.cluster.table.similar.image.popup")) {

                    @Override
                    public void populateItem(Item<ICellPopulator<SelectableBean<ClusterType>>> cellItem,
                            String componentId, IModel<SelectableBean<ClusterType>> model) {
                        if (model.getObject().getValue() != null && model.getObject().getValue().getSimilarGroupsCount() != null) {

                            AjaxButton ajaxButton = new AjaxButton(componentId,
                                    Model.of("popup")) {
                                @Override
                                public void onClick(AjaxRequestTarget ajaxRequestTarget) {
                                    long startTime = System.currentTimeMillis();

                                    List<String> similarGroupsId = model.getObject().getValue().getSimilarGroupsId();
                                    String string = DOT_CLASS + "getMiningTypeObject";
                                    OperationResult result = new OperationResult(string);
                                    List<PrismObject<MiningType>> miningTypeList = new ArrayList<>();
                                    List<String> rolesOid = model.getObject().getValue().getRoles();
                                    for (String groupOid : similarGroupsId) {
                                        PrismObject<MiningType> miningObject = getMiningObject(getPageBase(), groupOid, result);

                                        miningTypeList.add(miningObject);
                                    }

                                    long endTime = System.currentTimeMillis();
                                    long elapsedTime = endTime - startTime;
                                    double elapsedSeconds = elapsedTime / 1000.0;
                                    System.out.println("Elapsed time: " + elapsedSeconds + " seconds (prepare table data))");

                                    ImageDetailsPanel detailsPanel = new ImageDetailsPanel(((PageBase) getPage()).getMainPopupBodyId(),
                                            Model.of("Image"), miningTypeList, rolesOid, null, model.getObject().getValue().getIdentifier()) {
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
                        return ClusterType.F_SIMILAR_GROUPS_COUNT.toString();
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
