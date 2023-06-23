/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.page;

import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils.ClusterObjectUtils.cleanBeforeClustering;

import java.io.Serial;
import java.util.ArrayList;
import java.util.List;

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
import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.component.MainObjectListPanel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.impl.component.icon.CompositedIconBuilder;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.details.objects.ParentClusterBasicDetailsPanel;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.perform.ClusterPanel;
import com.evolveum.midpoint.model.api.AssignmentObjectRelation;
import com.evolveum.midpoint.model.api.authentication.CompiledObjectCollectionView;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.data.column.ColumnMenuAction;
import com.evolveum.midpoint.web.component.data.column.ObjectNameColumn;
import com.evolveum.midpoint.web.component.form.MidpointForm;
import com.evolveum.midpoint.web.component.menu.cog.ButtonInlineMenuItem;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItemAction;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.page.admin.PageAdmin;
import com.evolveum.midpoint.web.session.UserProfileStorage.TableId;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ArchetypeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ClusterType;
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

    public MainPageMining(PageParameters params) {
        super(params);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    private MainObjectListPanel<ParentClusterType> getTable() {
        return (MainObjectListPanel<ParentClusterType>) get(createComponentPath(ID_MAIN_FORM, ID_TABLE));
    }

    private InlineMenuItem createDeleteInlineMenu() {
        return new ButtonInlineMenuItem(createStringResource("MainObjectListPanel.menu.delete")) {
            @Override
            public CompositedIconBuilder getIconCompositedBuilder() {
                return getDefaultCompositedIconBuilder(GuiStyleConstants.CLASS_ICON_TRASH);
            }

            @Override
            public InlineMenuItemAction initAction() {
                return new ColumnMenuAction<SelectableBean<ArchetypeType>>() {
                    @Override
                    public void onClick(AjaxRequestTarget target) {

                        List<SelectableBean<ParentClusterType>> selectedObjects = getTable().getSelectedObjects();
                        OperationResult result = new OperationResult("Delete clusters objects");
                        if (selectedObjects == null || selectedObjects.size() == 0) {
                            try {
                                cleanBeforeClustering(result, ((PageBase) getPage()), null);
                            } catch (Exception e) {
                                throw new RuntimeException(e);
                            }
                        } else {

                            for (SelectableBean<ParentClusterType> selectedObject : selectedObjects) {
                                try {
                                    cleanBeforeClustering(result, ((PageBase) getPage()),
                                            selectedObject.getValue().getIdentifier());
                                } catch (Exception e) {
                                    throw new RuntimeException(e);
                                }
                            }
                        }

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

    protected void initLayout() {

        add(getDeleteClustersTypeButton());
        Form<?> mainForm = new MidpointForm<>(ID_MAIN_FORM);
        add(mainForm);

        MainObjectListPanel<ParentClusterType> table = new MainObjectListPanel<>(ID_TABLE, ParentClusterType.class) {

            @Override
            protected List<InlineMenuItem> createInlineMenu() {
                List<InlineMenuItem> menuItems = new ArrayList<>();
                menuItems.add(MainPageMining.this.createDeleteInlineMenu());
                return menuItems;
            }

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
                                    params.set(PageCluster.PARAMETER_IDENTIFIER, model.getObject().getValue().getIdentifier());
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
            protected void newObjectPerformed(AjaxRequestTarget target, AssignmentObjectRelation relation,
                    CompiledObjectCollectionView collectionView) {

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
                return getString("pageMining.message.nothingSelected");
            }

            @Override
            protected String getConfirmMessageKeyForMultiObject() {
                return "pageMining.message.confirmationMessageForMultipleObject";
            }

            @Override
            protected String getConfirmMessageKeyForSingleObject() {
                return "pagemMining.message.confirmationMessageForSingleObject";
            }
        };
        table.setOutputMarkupId(true);
        mainForm.add(table);

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

    @Override
    protected List<String> pageParametersToBeRemoved() {
        return List.of(PageBase.PARAMETER_SEARCH_BY_NAME);
    }
}
