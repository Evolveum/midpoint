/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.page;

import static com.evolveum.midpoint.gui.api.component.mining.analyse.tools.jaccard.JacquardSorter.jaccSortMiningSet;
import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils.ClusterObjectUtils.cleanBeforeClustering;
import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils.MiningObjectUtils.deleteMiningObjects;
import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils.MiningObjectUtils.getMiningObject;
import static com.evolveum.midpoint.security.api.MidPointPrincipalManager.DOT_CLASS;

import java.io.Serial;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.evolveum.midpoint.gui.impl.page.admin.role.mining.details.objects.MiningBasicDetailsPanel;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.details.objects.ParentClusterBasicDetailsPanel;

import com.evolveum.midpoint.gui.impl.page.admin.role.mining.details.work.ClusterDetailsPanel;

import com.evolveum.midpoint.gui.impl.page.admin.role.mining.details.work.ImageDetailsPanel;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.export.AbstractExportableColumn;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import com.evolveum.midpoint.authentication.api.authorization.AuthorizationAction;
import com.evolveum.midpoint.authentication.api.authorization.PageDescriptor;
import com.evolveum.midpoint.authentication.api.authorization.Url;
import com.evolveum.midpoint.gui.api.component.MainObjectListPanel;
import com.evolveum.midpoint.gui.api.component.data.provider.ISelectableDataProvider;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.impl.component.data.provider.SelectableBeanObjectDataProvider;

import com.evolveum.midpoint.gui.impl.page.admin.role.mining.perform.ClusterPanel;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.perform.MiningPanel;

import com.evolveum.midpoint.model.api.AssignmentObjectRelation;
import com.evolveum.midpoint.model.api.authentication.CompiledObjectCollectionView;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.data.column.ObjectNameColumn;
import com.evolveum.midpoint.web.component.form.MidpointForm;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.page.admin.PageAdmin;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.web.session.UserProfileStorage.TableId;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ClusterType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MiningType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ParentClusterType;


@PageDescriptor(
        urls = {
                @Url(mountUrl = "/admin/mainMining", matchUrlForSecurity = "/admin/mainMining")
        },
        action = {
                @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_USERS_ALL_URL,
                        label = "PageAdminUsers.auth.usersAll.label",
                        description = "PageAdminUsers.auth.usersAll.description"),
                @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_USERS_URL,
                        label = "PageUsers.auth.users.label",
                        description = "PageUsers.auth.users.description"),
                @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_USERS_VIEW_URL,
                        label = "PageUsers.auth.users.view.label",
                        description = "PageUsers.auth.users.view.description")
        })
public class MainPageMining extends PageAdmin {
    @Serial private static final long serialVersionUID = 1L;

    private static final String ID_MAIN_FORM = "mainForm";
    private static final String ID_TABLE = "table";
    private static final String ID_TABLE_2 = "table2";

    public MainPageMining(PageParameters params) {
        super(params);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    protected void initLayout() {

        add(getDeleteClustersTypeButton());
        add(getDeleteMiningTypeButton());
        Form<?> mainForm = new MidpointForm<>(ID_MAIN_FORM);
        add(mainForm);

        MainObjectListPanel<ParentClusterType> table = new MainObjectListPanel<>(ID_TABLE, ParentClusterType.class) {

            @Override
            protected List<IColumn<SelectableBean<ParentClusterType>, String>> createDefaultColumns() {

                List<IColumn<SelectableBean<ParentClusterType>, String>> columns = new ArrayList<>();

                IColumn<SelectableBean<ParentClusterType>, String> column;

                column = new ObjectNameColumn<>(createStringResource("ObjectType.name")) {

                    @Serial private static final long serialVersionUID = 1L;

                    @Override
                    public void onClick(AjaxRequestTarget target, IModel<SelectableBean<ParentClusterType>> rowModel) {

                        ParentClusterBasicDetailsPanel detailsPanel = new ParentClusterBasicDetailsPanel(((PageBase) getPage()).getMainPopupBodyId(),
                                Model.of("TO DO: details"), rowModel) {
                            @Override
                            public void onClose(AjaxRequestTarget ajaxRequestTarget) {
                                super.onClose(ajaxRequestTarget);
                            }
                        };
                        ((PageBase) getPage()).showMainPopup(detailsPanel, target);
                    }
                };

                columns.add(column);

                column = new AbstractExportableColumn<>(
                        createStringResource("Density")) {

                    @Override
                    public void populateItem(Item<ICellPopulator<SelectableBean<ParentClusterType>>> cellItem,
                            String componentId, IModel<SelectableBean<ParentClusterType>> model) {

                        cellItem.add(new Label(componentId, Model.of(model.getObject().getValue().getDensity())));

                    }

                    @Override
                    public IModel<String> getDataModel(IModel<SelectableBean<ParentClusterType>> rowModel) {
                        return Model.of("");
                    }

                    @Override
                    public String getCssClass() {
                        return "col-md-2 col-lg-1";
                    }
                };
                columns.add(column);

                column = new AbstractExportableColumn<>(
                        createStringResource("Consist")) {

                    @Override
                    public void populateItem(Item<ICellPopulator<SelectableBean<ParentClusterType>>> cellItem,
                            String componentId, IModel<SelectableBean<ParentClusterType>> model) {
                        cellItem.add(new Label(componentId,
                                model.getObject().getValue() != null && model.getObject().getValue().getConsist() != null ?
                                        model.getObject().getValue().getConsist() : null));
                    }

                    @Override
                    public IModel<String> getDataModel(IModel<SelectableBean<ParentClusterType>> rowModel) {
                        return Model.of("");
                    }

                    @Override
                    public String getCssClass() {
                        return "col-md-2 col-lg-1";
                    }
                };
                columns.add(column);

                column = new AbstractColumn<>(
                        createStringResource("Load")) {

                    @Override
                    public void populateItem(Item<ICellPopulator<SelectableBean<ParentClusterType>>> cellItem,
                            String componentId, IModel<SelectableBean<ParentClusterType>> model) {
                        if (model.getObject().getValue() != null && model.getObject().getValue().getIdentifier() != null) {

                            AjaxButton ajaxButton = new AjaxButton(componentId,
                                    Model.of(String.valueOf(model.getObject().getValue().getIdentifier()))) {
                                @Override
                                public void onClick(AjaxRequestTarget ajaxRequestTarget) {
                                    PageParameters params = new PageParameters();
                                    params.set("IDENTIFIER", model.getObject().getValue().getIdentifier());
                                    ((PageBase) getPage()).navigateToNext(PageCluster.class, params);
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
            protected void newObjectPerformed(AjaxRequestTarget target, AssignmentObjectRelation relation, CompiledObjectCollectionView collectionView) {

                ClusterPanel detailsPanel = new ClusterPanel(((PageBase) getPage()).getMainPopupBodyId(),
                        Model.of("New cluster")) {
                    @Override
                    public void onClose(AjaxRequestTarget ajaxRequestTarget) {
                        super.onClose(ajaxRequestTarget);
                    }
                };
                ((PageBase) getPage()).showMainPopup(detailsPanel, target);
            }

            @Override
            protected TableId getTableId() {
                return TableId.TABLE_USERS;
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
        table.setOutputMarkupId(true);
        mainForm.add(table);
        mainForm.add(miningTable().setOutputMarkupId(true));

    }

    public AjaxButton getDeleteClustersTypeButton() {
        AjaxButton ajaxLinkAssign = new AjaxButton("id_delete_clusters_set", Model.of("Delete Clusters Objects")) {
            @Override
            public void onClick(AjaxRequestTarget target) {
                OperationResult result = new OperationResult("Delete clusters objects");
                try {
                    cleanBeforeClustering(result, ((PageBase) getPage()), null);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }

            }
        };
        ajaxLinkAssign.setOutputMarkupId(true);
        return ajaxLinkAssign;
    }

    public AjaxButton getDeleteMiningTypeButton() {
        AjaxButton ajaxLinkAssign = new AjaxButton("id_delete_mining_set", Model.of("Delete Mining Objects")) {
            @Override
            public void onClick(AjaxRequestTarget target) {
                OperationResult result = new OperationResult("Delete miningType objects");
                try {
                    deleteMiningObjects(result, (PageBase) getPage());
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }

            }
        };
        ajaxLinkAssign.setOutputMarkupId(true);
        return ajaxLinkAssign;
    }

    private ObjectQuery getCustomizeContentQuery() {
        return ((PageBase) getPage()).getPrismContext().queryFor(MiningType.class).not()
                .item(MiningType.F_SIMILAR_GROUPS_COUNT).eq(0)
                .build();
    }

    protected MainObjectListPanel<?> miningTable() {

        MainObjectListPanel<?> basicTable = new MainObjectListPanel<>(ID_TABLE_2, MiningType.class) {

            @Override
            protected void newObjectPerformed(AjaxRequestTarget target, AssignmentObjectRelation relation, CompiledObjectCollectionView collectionView) {
                MiningPanel detailsPanel = new MiningPanel(((PageBase) getPage()).getMainPopupBodyId(),
                        Model.of("Update mining objects")) {
                    @Override
                    public void onClose(AjaxRequestTarget ajaxRequestTarget) {
                        super.onClose(ajaxRequestTarget);
                    }
                };
                ((PageBase) getPage()).showMainPopup(detailsPanel, target);
            }

            @Override
            protected ISelectableDataProvider<SelectableBean<MiningType>> createProvider() {
                SelectableBeanObjectDataProvider<MiningType> provider = createSelectableBeanObjectDataProvider(() ->
                        getCustomizeContentQuery(), null);
                provider.setEmptyListOnNullQuery(true);
                provider.setSort(null);
                provider.setDefaultCountIfNull(Integer.MAX_VALUE);
                return provider;
            }

            @Override
            protected List<IColumn<SelectableBean<MiningType>, String>> createDefaultColumns() {

                List<IColumn<SelectableBean<MiningType>, String>> columns = new ArrayList<>();

                IColumn<SelectableBean<MiningType>, String> column;

                column = new ObjectNameColumn<>(createStringResource("ObjectType.name")) {

                    @Serial private static final long serialVersionUID = 1L;

                    @Override
                    public void onClick(AjaxRequestTarget target, IModel<SelectableBean<MiningType>> rowModel) {

                        MiningBasicDetailsPanel detailsPanel = new MiningBasicDetailsPanel(((PageBase) getPage()).getMainPopupBodyId(),
                                Model.of("TO DO: details"), rowModel) {
                            @Override
                            public void onClose(AjaxRequestTarget ajaxRequestTarget) {
                                super.onClose(ajaxRequestTarget);
                            }
                        };
                        ((PageBase) getPage()).showMainPopup(detailsPanel, target);

                    }
                };

                columns.add(column);

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

                                    List<String> similarGroupsId = model.getObject().getValue().getSimilarGroupsId();
                                    String string = DOT_CLASS + "getMiningTypeObject";
                                    OperationResult result = new OperationResult(string);

                                    List<PrismObject<MiningType>> miningTypeList = new ArrayList<>();
                                    Set<String> rolesOid = new HashSet<>();
                                    for (String groupOid : similarGroupsId) {
                                        PrismObject<MiningType> miningObject = getMiningObject(getPageBase(), groupOid, result);

                                        miningTypeList.add(miningObject);
                                        rolesOid.addAll(miningObject.asObjectable().getRoles());
                                    }
                                    String targetValue = model.getObject().getValue().asPrismObject().getOid();
                                    if (targetValue != null) {
                                        PrismObject<MiningType> miningObject = getMiningObject(getPageBase(), targetValue, result);
                                        miningTypeList.add(miningObject);
                                        rolesOid.addAll(miningObject.asObjectable().getRoles());
                                    }
                                    List<PrismObject<MiningType>> jaccSortMiningSet = jaccSortMiningSet(miningTypeList);

                                    Map<String, Long> roleCountMap = rolesOid.stream()
                                            .collect(Collectors.toMap(Function.identity(),
                                                    role -> jaccSortMiningSet.stream()
                                                            .filter(miningType -> miningType.asObjectable().getRoles().contains(role))
                                                            .count()));

                                    List<String> sortedRolePrismObjectList = roleCountMap.entrySet().stream()
                                            .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                                            .map(Map.Entry::getKey)
                                            .toList();
                                    ClusterDetailsPanel detailsPanel = new ClusterDetailsPanel(((PageBase) getPage()).getMainPopupBodyId(),
                                            Model.of("Groups"), jaccSortMiningSet, sortedRolePrismObjectList, model.getObject().getValue().asPrismObject().getOid()) {
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

                                    List<String> similarGroupsId = model.getObject().getValue().getSimilarGroupsId();
                                    String string = DOT_CLASS + "getMiningTypeObject";
                                    OperationResult result = new OperationResult(string);
                                    List<PrismObject<MiningType>> miningTypeList = new ArrayList<>();
                                    Set<String> rolesOid = new HashSet<>();
                                    for (String groupOid : similarGroupsId) {
                                        PrismObject<MiningType> miningObject = getMiningObject(getPageBase(), groupOid, result);

                                        miningTypeList.add(miningObject);
                                        rolesOid.addAll(miningObject.asObjectable().getRoles());
                                    }
                                    List<PrismObject<MiningType>> jaccSortMiningSet = jaccSortMiningSet(miningTypeList);

                                    List<String> sortedRoles = new ArrayList<>();
                                    String identifier = model.getObject().getValue().getIdentifier();
                                    if (identifier == null || !identifier.equals("outliers")) {


                                        Map<String, Long> roleCountMap = rolesOid.stream()
                                                .collect(Collectors.toMap(Function.identity(),
                                                        role -> jaccSortMiningSet.stream()
                                                                .filter(miningType -> miningType.asObjectable().getRoles().contains(role))
                                                                .count()));

                                        sortedRoles = roleCountMap.entrySet().stream()
                                                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                                                .map(Map.Entry::getKey)
                                                .toList();
                                    }

                                    ImageDetailsPanel detailsPanel = new ImageDetailsPanel(((PageBase) getPage()).getMainPopupBodyId(),
                                            Model.of("Image"), jaccSortMiningSet,
                                            sortedRoles, model.getObject().getValue().asPrismObject().getOid(),
                                            model.getObject().getValue().getIdentifier()) {
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

    @Override
    protected List<String> pageParametersToBeRemoved() {
        return List.of(PageBase.PARAMETER_SEARCH_BY_NAME);
    }
}
