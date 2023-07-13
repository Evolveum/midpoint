/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.panels.tables;

import static com.evolveum.midpoint.gui.api.component.mining.DataStorage.getDataStorageUsersCount;

import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.export.AbstractExportableColumn;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.gui.api.component.MainObjectListPanel;
import com.evolveum.midpoint.gui.api.component.data.provider.ISelectableDataProvider;
import com.evolveum.midpoint.gui.api.component.mining.RoleMiningFilter;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.data.column.ColumnMenuAction;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.util.FocusListInlineMenuHelper;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.component.util.SelectableBeanImpl;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentHolderType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

public class BasicTableSelector extends Panel {

    private static final String ID_ROLE_SEARCH = "role_search";
    private static final String ID_USER_SEARCH = "user_search";

    private static final String ID_DATATABLE = "datatable";
    private static final String ID_FORM = "form";

    PageBase parentPage;
    boolean searchMode = true;  //false: user   true: role

    public BasicTableSelector(String id, PageBase parentPage) {
        super(id);
        this.parentPage = parentPage;

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
        int usersSize = getDataStorageUsersCount();
        if (isSearchMode()) {
            form.add(basicRoleHelperTable(usersSize));
        } else {
            form.add(basicUserHelperTable());
        }

        searchSelector(form, usersSize);

    }

    private void searchSelector(@NotNull Form<?> mainForm, int usersCount) {

        AjaxButton roleSearch = new AjaxButton(ID_ROLE_SEARCH) {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                setSearchMode(true);
                getBasicTable().replaceWith(basicRoleHelperTable(usersCount));
                target.add(getBasicTable());

                getUserSearchLink().add(new AttributeModifier("class", " btn btn-default"));
                target.add(getUserSearchLink());

                getRoleSearchLink().add(new AttributeModifier("class", " btn btn-secondary"));
                target.add(getRoleSearchLink());
            }
        };

        roleSearch.setOutputMarkupId(true);

        if (!isSearchMode()) {
            roleSearch.add(new AttributeModifier("class", " btn btn-default"));
        } else {
            roleSearch.add(new AttributeModifier("class", " btn btn-secondary"));
        }

        mainForm.add(roleSearch);

        AjaxButton userSearch = new AjaxButton(ID_USER_SEARCH) {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                setSearchMode(false);
                getBasicTable().replaceWith(basicUserHelperTable());
                target.add(getBasicTable());

                getRoleSearchLink().add(new AttributeModifier("class", " btn btn-default"));
                target.add(getRoleSearchLink());

                getUserSearchLink().add(new AttributeModifier("class", " btn btn-secondary"));
                target.add(getUserSearchLink());
            }

        };
        userSearch.setOutputMarkupId(true);

        if (!isSearchMode()) {
            userSearch.add(new AttributeModifier("class", " btn btn-secondary"));
        } else {
            userSearch.add(new AttributeModifier("class", " btn btn-default"));
        }

        mainForm.add(userSearch);
    }

    protected MainObjectListPanel<?> basicUserHelperTable() {

        MainObjectListPanel<?> basicTable = new MainObjectListPanel<>(ID_DATATABLE, UserType.class) {

            @Override
            protected List<InlineMenuItem> createInlineMenu() {
                FocusListInlineMenuHelper<UserType> helper = new FocusListInlineMenuHelper<>(
                        UserType.class, parentPage, this) {

                    private static final long serialVersionUID = 1L;

                    @Override
                    protected boolean isShowConfirmationDialog(ColumnMenuAction<?> action) {
                        return BasicTableSelector.this.getBasicTable().getSelectedObjectsCount() > 0;
                    }

                    @Override
                    protected IModel<String> getConfirmationMessageModel(ColumnMenuAction<?> action, String actionName) {
                        return BasicTableSelector.this.getBasicTable().getConfirmationMessageModel(action, actionName);
                    }
                };

                return helper.createRowActions(UserType.class);
            }

            @Override
            protected ISelectableDataProvider<SelectableBean<UserType>> createProvider() {
                return super.createProvider();
            }

            @Override
            protected List<IColumn<SelectableBean<UserType>, String>> createDefaultColumns() {

                List<IColumn<SelectableBean<UserType>, String>> columns = new ArrayList<>();

                IColumn<SelectableBean<UserType>, String> column = new PropertyColumn<>(
                        createStringResource("UserType.givenName"),
                        SelectableBeanImpl.F_VALUE + ".givenName");
                columns.add(column);

                column = new AbstractExportableColumn<>(
                        createStringResource("UserType.assignments.count")) {

                    @Override
                    public void populateItem(Item<ICellPopulator<SelectableBean<UserType>>> cellItem,
                            String componentId, IModel<SelectableBean<UserType>> model) {
                        cellItem.add(new Label(componentId,
                                model.getObject().getValue() != null && model.getObject().getValue().getAssignment() != null ?
                                        model.getObject().getValue().getAssignment().size() : null));
                    }

                    @Override
                    public IModel<String> getDataModel(IModel<SelectableBean<UserType>> rowModel) {
                        return Model.of("");
                    }

                    @Override
                    public String getCssClass() {
                        return "col-md-2 col-lg-1";
                    }
                };
                columns.add(column);

                column = new AbstractExportableColumn<>(
                        createStringResource("UserType.assignments.roles.count")) {
                    @Override
                    public String getSortProperty() {
                        return super.getSortProperty();
                    }

                    @Override
                    public void populateItem(Item<ICellPopulator<SelectableBean<UserType>>> cellItem,
                            String componentId, IModel<SelectableBean<UserType>> model) {
                        if (model.getObject().getValue() != null && model.getObject().getValue().getRoleMembershipRef() != null) {
                            AssignmentHolderType object = model.getObject().getValue();
                            cellItem.add(new Label(componentId, new RoleMiningFilter().getRoleObjectReferenceTypes(object).size()));

                        } else {
                            cellItem.add(new Label(componentId,
                                    (Integer) null));
                        }
                    }

                    @Override
                    public IModel<String> getDataModel(IModel<SelectableBean<UserType>> rowModel) {
                        return Model.of("");
                    }

                    @Override
                    public String getCssClass() {
                        return "col-md-2 col-lg-1";
                    }
                };
                columns.add(column);
                return columns;
            }

            @Override
            protected UserProfileStorage.TableId getTableId() {
                return UserProfileStorage.TableId.TABLE_USERS;
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

    protected MainObjectListPanel<?> basicRoleHelperTable(int usersCount) {

        MainObjectListPanel<?> basicTable = new MainObjectListPanel<>(ID_DATATABLE, RoleType.class) {
            @Override
            public List<SelectableBean<RoleType>> isAnythingSelected(
                    AjaxRequestTarget target, IModel<SelectableBean<RoleType>> selectedObject) {
                return super.isAnythingSelected(target, selectedObject);
            }

            @Override
            protected List<InlineMenuItem> createInlineMenu() {
                FocusListInlineMenuHelper<RoleType> helper = new FocusListInlineMenuHelper<>(
                        RoleType.class, parentPage, this) {

                    private static final long serialVersionUID = 1L;

                    @Override
                    protected boolean isShowConfirmationDialog(ColumnMenuAction<?> action) {
                        return BasicTableSelector.this.getBasicTable().getSelectedObjectsCount() > 0;
                    }

                    @Override
                    protected IModel<String> getConfirmationMessageModel(ColumnMenuAction<?> action, String actionName) {
                        return BasicTableSelector.this.getBasicTable().getConfirmationMessageModel(action, actionName);
                    }
                };

                return helper.createRowActions(RoleType.class);
            }

            @Override
            protected List<IColumn<SelectableBean<RoleType>, String>> createDefaultColumns() {

                List<IColumn<SelectableBean<RoleType>, String>> columns = new ArrayList<>();

                IColumn<SelectableBean<RoleType>, String> column;

                column = new AbstractExportableColumn<>(
                        createStringResource("RoleType.assignments.count")) {
                    @Override
                    public String getSortProperty() {
                        return super.getSortProperty();
                    }

                    @Override
                    public void populateItem(Item<ICellPopulator<SelectableBean<RoleType>>> cellItem,
                            String componentId, IModel<SelectableBean<RoleType>> model) {
                        if (model.getObject().getValue() != null && model.getObject().getValue().getAssignment() != null) {
                            int object = model.getObject().getValue().getAssignment().size();
                            cellItem.add(new Label(componentId, object));
                        } else {
                            cellItem.add(new Label(componentId, 0));
                        }
                    }

                    @Override
                    public IModel<String> getDataModel(IModel<SelectableBean<RoleType>> rowModel) {
                        return Model.of("");
                    }

                    @Override
                    public String getCssClass() {
                        return "col-md-2 col-lg-1";
                    }
                };
                columns.add(column);

                column = new AbstractExportableColumn<>(
                        createStringResource("RoleType.members.count")) {

                    @Override
                    public void populateItem(Item<ICellPopulator<SelectableBean<RoleType>>> cellItem,
                            String componentId, IModel<SelectableBean<RoleType>> model) {
                        RoleType value = model.getObject().getValue();

                        int membersCount = new RoleMiningFilter().getRoleMembersCount(getPageBase(), value.getOid());

                        double percentage = (100.00 / usersCount) * (double) membersCount;
                        cellItem.add(new Label(componentId,
                                membersCount + " -> " + Math.round(percentage * 100.0) / 100.0 + "%"));

                    }

                    @Override
                    public IModel<String> getDataModel(IModel<SelectableBean<RoleType>> rowModel) {
                        return Model.of(" ");
                    }

                    @Override
                    public String getCssClass() {
                        return "col-md-2 col-lg-1";
                    }
                };
                columns.add(column);

                column = new AbstractExportableColumn<>(
                        createStringResource("PageRoleEditor.label.riskLevel"),
                        RoleType.F_RISK_LEVEL.getLocalPart()) {

                    @Override
                    public void populateItem(Item<ICellPopulator<SelectableBean<RoleType>>> cellItem,
                            String componentId, IModel<SelectableBean<RoleType>> model) {
                        try {
                            cellItem.add(new Label(componentId,
                                    model.getObject().getValue().getRiskLevel() != null ?
                                            model.getObject().getValue().getRiskLevel() : null));
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    }

                    @Override
                    public IModel<String> getDataModel(IModel<SelectableBean<RoleType>> rowModel) {
                        return Model.of(" ");
                    }

                    @Override
                    public String getCssClass() {
                        return "col-md-2 col-lg-1";
                    }
                };

                columns.add(column);
                return columns;

            }

            @Override
            protected UserProfileStorage.TableId getTableId() {
                return UserProfileStorage.TableId.TABLE_USERS;
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

    private boolean isSearchMode() {
        return searchMode;
    }

    private void setSearchMode(boolean searchMode) {
        this.searchMode = searchMode;
    }

    protected AjaxButton getUserSearchLink() {
        return (AjaxButton) get(((PageBase) getPage()).createComponentPath(ID_FORM, ID_USER_SEARCH));
    }

    protected AjaxButton getRoleSearchLink() {
        return (AjaxButton) get(((PageBase) getPage()).createComponentPath(ID_FORM, ID_ROLE_SEARCH));
    }

    protected MainObjectListPanel<?> getBasicTable() {
        return (MainObjectListPanel<?>) get(((PageBase) getPage()).createComponentPath(ID_FORM, ID_DATATABLE));
    }
}
