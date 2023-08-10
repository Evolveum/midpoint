/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.page;

import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils.ClusterObjectUtils.deleteRoleAnalysisObjects;
import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils.ClusterObjectUtils.deleteSingleRoleAnalysisSession;
import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils.Tools.getColorClass;

import java.io.Serial;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.AttributeModifier;
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
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import com.evolveum.midpoint.authentication.api.authorization.AuthorizationAction;
import com.evolveum.midpoint.authentication.api.authorization.PageDescriptor;
import com.evolveum.midpoint.authentication.api.authorization.Url;
import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.component.MainObjectListPanel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.impl.component.icon.CompositedIconBuilder;
import com.evolveum.midpoint.gui.impl.error.ErrorPanel;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.panel.ExecuteClusteringPanel;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.panel.details.objects.ParentClusterBasicDetailsPanel;
import com.evolveum.midpoint.model.api.AssignmentObjectRelation;
import com.evolveum.midpoint.model.api.authentication.CompiledObjectCollectionView;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.web.component.AjaxIconButton;
import com.evolveum.midpoint.web.component.data.column.ColumnMenuAction;
import com.evolveum.midpoint.web.component.data.column.SelectableObjectNameColumn;
import com.evolveum.midpoint.web.component.form.MidpointForm;
import com.evolveum.midpoint.web.component.menu.cog.ButtonInlineMenuItem;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItemAction;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.page.admin.PageAdmin;
import com.evolveum.midpoint.web.session.UserProfileStorage.TableId;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisSessionType;

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

    private MainObjectListPanel<RoleAnalysisSessionType> getTable() {
        return (MainObjectListPanel<RoleAnalysisSessionType>) get(createComponentPath(ID_MAIN_FORM, ID_TABLE));
    }

    private InlineMenuItem createDeleteInlineMenu() {
        return new ButtonInlineMenuItem(createStringResource("MainObjectListPanel.menu.delete")) {
            @Override
            public CompositedIconBuilder getIconCompositedBuilder() {
                return getDefaultCompositedIconBuilder(GuiStyleConstants.CLASS_ICON_TRASH);
            }

            @Override
            public InlineMenuItemAction initAction() {
                return new ColumnMenuAction<SelectableBean<RoleAnalysisSessionType>>() {
                    @Override
                    public void onClick(AjaxRequestTarget target) {

                        List<SelectableBean<RoleAnalysisSessionType>> selectedObjects = getTable().getSelectedObjects();
                        OperationResult result = new OperationResult("Delete session objects");
                        if (selectedObjects.size() == 1 && getRowModel() == null) {
                            try {
                                SelectableBean<RoleAnalysisSessionType> roleAnalysisSessionTypeSelectableBean = selectedObjects.get(0);
                                deleteSingleRoleAnalysisSession(result, roleAnalysisSessionTypeSelectableBean.getValue(),
                                        (PageBase) getPage());
                            } catch (Exception e) {
                                throw new RuntimeException(e);
                            }
                        } else if (getRowModel() != null) {
                            try {
                                IModel<SelectableBean<RoleAnalysisSessionType>> rowModel = getRowModel();
                                deleteSingleRoleAnalysisSession(result, rowModel.getObject().getValue(), (PageBase) getPage());
                            } catch (Exception e) {
                                throw new RuntimeException(e);
                            }
                        } else {
                            for (SelectableBean<RoleAnalysisSessionType> selectedObject : selectedObjects) {
                                try {
                                    String parentOid = selectedObject.getValue().asPrismObject().getOid();
                                    List<ObjectReferenceType> roleAnalysisClusterRef = selectedObject.getValue().getRoleAnalysisClusterRef();
                                    deleteRoleAnalysisObjects(result, (PageBase) getPage(), parentOid, roleAnalysisClusterRef);
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

    protected void initLayout() {

        Form<?> mainForm = new MidpointForm<>(ID_MAIN_FORM);
        add(mainForm);

        if (!isNativeRepo()) {
            mainForm.add(new ErrorPanel(ID_TABLE,
                    () -> getString("PageAdmin.menu.top.resources.templates.list.nonNativeRepositoryWarning")));
            return;
        }
        MainObjectListPanel<RoleAnalysisSessionType> table = new MainObjectListPanel<>(ID_TABLE, RoleAnalysisSessionType.class) {

            @Override
            protected List<InlineMenuItem> createInlineMenu() {
                List<InlineMenuItem> menuItems = new ArrayList<>();
                menuItems.add(MainPageMining.this.createDeleteInlineMenu());
                return menuItems;
            }

            @Override
            protected List<IColumn<SelectableBean<RoleAnalysisSessionType>, String>> createDefaultColumns() {

                List<IColumn<SelectableBean<RoleAnalysisSessionType>, String>> columns = new ArrayList<>();

                IColumn<SelectableBean<RoleAnalysisSessionType>, String> column;

                column = new SelectableObjectNameColumn<>(createStringResource("ObjectType.name"), null, null, null) {

                    @Serial private static final long serialVersionUID = 1L;

                    @Override
                    public void onClick(AjaxRequestTarget target, IModel<SelectableBean<RoleAnalysisSessionType>> rowModel) {

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

                column = new AbstractExportableColumn<>(getHeaderTitle("mode")) {

                    @Override
                    public void populateItem(Item<ICellPopulator<SelectableBean<RoleAnalysisSessionType>>> cellItem,
                            String componentId, IModel<SelectableBean<RoleAnalysisSessionType>> model) {
                        cellItem.add(new Label(componentId,
                                model.getObject().getValue() != null && model.getObject().getValue().getClusterOptions().getProcessMode() != null ?
                                        model.getObject().getValue().getClusterOptions().getProcessMode() : null));
                    }

                    @Override
                    public IModel<String> getDataModel(IModel<SelectableBean<RoleAnalysisSessionType>> rowModel) {
                        return Model.of("");
                    }

                };
                columns.add(column);

                column = new AbstractExportableColumn<>(getHeaderTitle("similarity.option")) {

                    @Override
                    public void populateItem(Item<ICellPopulator<SelectableBean<RoleAnalysisSessionType>>> cellItem,
                            String componentId, IModel<SelectableBean<RoleAnalysisSessionType>> model) {
                        RoleAnalysisSessionType value = model.getObject().getValue();
                        cellItem.add(new Label(componentId, value.getClusterOptions().getSimilarityThreshold()));
                    }

                    @Override
                    public IModel<String> getDataModel(IModel<SelectableBean<RoleAnalysisSessionType>> rowModel) {
                        return Model.of("");
                    }

                };
                columns.add(column);

                column = new AbstractExportableColumn<>(getHeaderTitle("intersection.option")) {

                    @Override
                    public void populateItem(Item<ICellPopulator<SelectableBean<RoleAnalysisSessionType>>> cellItem,
                            String componentId, IModel<SelectableBean<RoleAnalysisSessionType>> model) {
                        RoleAnalysisSessionType value = model.getObject().getValue();
                        cellItem.add(new Label(componentId, value.getClusterOptions().getMinPropertiesOverlap()));
                    }

                    @Override
                    public IModel<String> getDataModel(IModel<SelectableBean<RoleAnalysisSessionType>> rowModel) {
                        return Model.of("");
                    }

                };
                columns.add(column);

                column = new AbstractExportableColumn<>(getHeaderTitle("properties.option")) {

                    @Override
                    public void populateItem(Item<ICellPopulator<SelectableBean<RoleAnalysisSessionType>>> cellItem,
                            String componentId, IModel<SelectableBean<RoleAnalysisSessionType>> model) {
                        RoleAnalysisSessionType value = model.getObject().getValue();
                        cellItem.add(new Label(componentId,
                                "from " + value.getClusterOptions().getMinPropertiesCount()
                                        + " to "
                                        + value.getClusterOptions().getMaxPropertiesCount()));
                    }

                    @Override
                    public IModel<String> getDataModel(IModel<SelectableBean<RoleAnalysisSessionType>> rowModel) {
                        return Model.of("");
                    }

                };
                columns.add(column);

                column = new AbstractExportableColumn<>(getHeaderTitle("group.option")) {

                    @Override
                    public void populateItem(Item<ICellPopulator<SelectableBean<RoleAnalysisSessionType>>> cellItem,
                            String componentId, IModel<SelectableBean<RoleAnalysisSessionType>> model) {
                        RoleAnalysisSessionType value = model.getObject().getValue();
                        cellItem.add(new Label(componentId, value.getClusterOptions().getMinUniqueMembersCount()));
                    }

                    @Override
                    public IModel<String> getDataModel(IModel<SelectableBean<RoleAnalysisSessionType>> rowModel) {
                        return Model.of("");
                    }

                };
                columns.add(column);

                column = new AbstractExportableColumn<>(getHeaderTitle("consist")) {

                    @Override
                    public void populateItem(Item<ICellPopulator<SelectableBean<RoleAnalysisSessionType>>> cellItem,
                            String componentId, IModel<SelectableBean<RoleAnalysisSessionType>> model) {
                        cellItem.add(new Label(componentId,
                                model.getObject().getValue() != null && model.getObject().getValue().getSessionStatistic().getProcessedObjectCount() != null ?
                                        model.getObject().getValue().getSessionStatistic().getProcessedObjectCount() : null));
                    }

                    @Override
                    public IModel<String> getDataModel(IModel<SelectableBean<RoleAnalysisSessionType>> rowModel) {
                        return Model.of("");
                    }

                };
                columns.add(column);

                column = new AbstractExportableColumn<>(getHeaderTitle("density")) {

                    @Override
                    public void populateItem(Item<ICellPopulator<SelectableBean<RoleAnalysisSessionType>>> cellItem,
                            String componentId, IModel<SelectableBean<RoleAnalysisSessionType>> model) {

                        String meanDensity = new DecimalFormat("#.###")
                                .format(Math.round(model.getObject().getValue().getSessionStatistic().getMeanDensity() * 1000.0) / 1000.0);

                        String colorClass = getColorClass(meanDensity);

                        Label label = new Label(componentId, meanDensity + " (%)");
                        label.setOutputMarkupId(true);
                        label.add(new AttributeModifier("class", colorClass));
                        label.add(AttributeModifier.append("style", "width: 100px;"));

                        cellItem.add(label);

                    }

                    @Override
                    public IModel<String> getDataModel(IModel<SelectableBean<RoleAnalysisSessionType>> rowModel) {
                        return Model.of("");
                    }

                };
                columns.add(column);

                column = new AbstractColumn<>(
                        createStringResource("RoleMining.button.title.load")) {

                    @Override
                    public void populateItem(Item<ICellPopulator<SelectableBean<RoleAnalysisSessionType>>> cellItem,
                            String componentId, IModel<SelectableBean<RoleAnalysisSessionType>> model) {
                        if (model.getObject().getValue() != null && model.getObject().getValue().getName() != null) {

                            AjaxIconButton ajaxButton = new AjaxIconButton(componentId, Model.of("fa fa-bars"),
                                    createStringResource("RoleMining.cluster.table.load.operation.panel")) {
                                @Override
                                public void onClick(AjaxRequestTarget ajaxRequestTarget) {
                                    PageParameters params = new PageParameters();
                                    params.set(PageCluster.PARAMETER_MODE, model.getObject().getValue().getClusterOptions().getProcessMode());
                                    params.set(PageCluster.PARAMETER_PARENT_OID, model.getObject().getValue().getOid());

                                    ((PageBase) getPage()).navigateToNext(PageCluster.class, params);
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
                        return RoleAnalysisSessionType.F_NAME.toString();
                    }
                };
                columns.add(column);

                return columns;
            }

            @Override
            protected void newObjectPerformed(AjaxRequestTarget target, AssignmentObjectRelation relation,
                    CompiledObjectCollectionView collectionView) {

                ExecuteClusteringPanel detailsPanel = new ExecuteClusteringPanel(((PageBase) getPage()).getMainPopupBodyId(),
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

    @Override
    protected List<String> pageParametersToBeRemoved() {
        return List.of(PageBase.PARAMETER_SEARCH_BY_NAME);
    }

    protected StringResourceModel getHeaderTitle(String identifier) {
        return createStringResource("RoleMining.cluster.table.column.header." + identifier);
    }
}
