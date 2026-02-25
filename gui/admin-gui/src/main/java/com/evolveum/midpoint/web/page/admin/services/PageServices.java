/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.web.page.admin.services;

import java.io.Serial;
import java.util.List;

import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.component.MainObjectListPanel;
import com.evolveum.midpoint.gui.api.component.data.provider.ISelectableDataProvider;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.web.component.data.column.ColumnMenuAction;
import com.evolveum.midpoint.web.component.form.MidpointForm;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.util.FocusListInlineMenuHelper;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.component.util.SerializableSupplier;
import com.evolveum.midpoint.web.page.admin.PageAdmin;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ServiceType;

/**
 * @author katkav
 * @author lazyman
 */
public abstract class PageServices extends PageAdmin {

    @Serial private static final long serialVersionUID = 1L;

    private static final String ID_MAIN_FORM = "mainForm";
    private static final String ID_TABLE = "table";

    public PageServices() {
        super();
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();

        initLayout();
    }

    protected void initLayout() {
        Form<?> mainForm = new MidpointForm<>(ID_MAIN_FORM);
        add(mainForm);

        MainObjectListPanel<ServiceType> table = new MainObjectListPanel<>(ID_TABLE, ServiceType.class) {

            @Override
            protected UserProfileStorage.TableId getTableId() {
                return PageServices.this.getTableId();
            }

            @Override
            protected List<InlineMenuItem> createInlineMenu() {
                FocusListInlineMenuHelper<ServiceType> listInlineMenuHelper =
                        new FocusListInlineMenuHelper<>(ServiceType.class, PageServices.this, this) {

                            @Serial private static final long serialVersionUID = 1L;

                            protected boolean isShowConfirmationDialog(ColumnMenuAction<?> action) {
                                return PageServices.this.isShowConfirmationDialog(action);
                            }

                            protected IModel<String> getConfirmationMessageModel(ColumnMenuAction<?> action, String actionName) {
                                return PageServices.this.getConfirmationMessageModel(action, actionName);
                            }
                        };
                return listInlineMenuHelper.createRowActions(getType());
            }

            @Override
            protected ISelectableDataProvider<SelectableBean<ServiceType>> createProvider() {
                return createSelectableBeanObjectDataProvider(createObjectQuerySupplier(), null);
            }

            @Override
            protected List<IColumn<SelectableBean<ServiceType>, String>> createDefaultColumns() {
                List<IColumn<SelectableBean<ServiceType>, String>> columns = PageServices.this.createDefaultColumns();
                if (columns != null) {
                    return columns;
                }

                return super.createDefaultColumns();
            }
        };
        table.setOutputMarkupId(true);
        mainForm.add(table);
    }

    private IModel<String> getConfirmationMessageModel(ColumnMenuAction<?> action, String actionName) {
        return WebComponentUtil.createAbstractRoleConfirmationMessage(actionName, action, getObjectListPanel(), this);
    }

    @SuppressWarnings("unchecked")
    private MainObjectListPanel<ServiceType> getObjectListPanel() {
        return (MainObjectListPanel<ServiceType>) get(createComponentPath(ID_MAIN_FORM, ID_TABLE));
    }

    private boolean isShowConfirmationDialog(ColumnMenuAction<?> action) {
        return action.getRowModel() != null ||
                getObjectListPanel().getSelectedObjectsCount() > 0;
    }

    protected abstract UserProfileStorage.TableId getTableId();

    protected SerializableSupplier<ObjectQuery> createObjectQuerySupplier() {
        return null;
    }

    protected List<IColumn<SelectableBean<ServiceType>, String>> createDefaultColumns() {
        return null;
    }
}
