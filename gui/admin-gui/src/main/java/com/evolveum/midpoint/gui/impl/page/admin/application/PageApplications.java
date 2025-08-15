/*
 * Copyright (c) 2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.application;

import java.util.Collection;
import java.util.List;

import com.evolveum.midpoint.gui.api.component.data.provider.ISelectableDataProvider;
import com.evolveum.midpoint.gui.impl.component.data.column.icon.RoundedImageObjectColumn;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.page.admin.users.component.ExecuteChangeOptionsDto;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ApplicationType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;

import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.authentication.api.authorization.AuthorizationAction;
import com.evolveum.midpoint.authentication.api.authorization.PageDescriptor;
import com.evolveum.midpoint.authentication.api.authorization.Url;
import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.component.MainObjectListPanel;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.web.application.CollectionInstance;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.component.data.column.ColumnMenuAction;
import com.evolveum.midpoint.web.component.form.MidpointForm;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.util.FocusListInlineMenuHelper;
import com.evolveum.midpoint.web.page.admin.PageAdmin;
import com.evolveum.midpoint.web.session.UserProfileStorage;

import org.apache.wicket.model.Model;

@PageDescriptor(
        urls = {
                @Url(mountUrl = "/admin/applications")
        },
        action = {
        @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_APPROVALS_ALL_URL,
                label = "PageAdminApplications.auth.applicationsAll.label",
                description = "PageAdminApplications.auth.applicationsAll.description"),
        @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_APPLICATIONS_URL,
                label = "PageApplications.auth.applications.label",
                description = "PageApplications.auth.applications.description"),
        @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_APPLICATION_VIEW_URL,
                label = "PageApplications.auth.applications.view.label",
                description = "PageApplications.auth.applications.view.description")})
@CollectionInstance(identifier = "allApplications", applicableForType = ApplicationType.class,
        display = @PanelDisplay(label = "PageAdmin.menu.top.applications.list", singularLabel = "ObjectType.application", icon = GuiStyleConstants.CLASS_OBJECT_APPLICATION_ICON))
public class PageApplications extends PageAdmin {
    private static final long serialVersionUID = 1L;

    private static final String DOT_CLASS = PageApplications.class.getName() + ".";

    private static final String ID_MAIN_FORM = "mainForm";
    private static final String ID_TABLE = "table";


    public PageApplications() {
        super();
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    protected void initLayout() {

        Form mainForm = new MidpointForm(ID_MAIN_FORM);
        add(mainForm);

        MainObjectListPanel<ApplicationType> table = new MainObjectListPanel<>(ID_TABLE, ApplicationType.class) {

            @Override
            protected UserProfileStorage.TableId getTableId() {
                return UserProfileStorage.TableId.TABLE_APPLICATIONS;
            }

            @Override
            protected List<InlineMenuItem> createInlineMenu() {
                FocusListInlineMenuHelper<ApplicationType> listInlineMenuHelper = new FocusListInlineMenuHelper<>(ApplicationType.class, PageApplications.this, this) {
                    private static final long serialVersionUID = 1L;

                    protected boolean isShowConfirmationDialog(ColumnMenuAction action) {
                        return PageApplications.this.isShowConfirmationDialog(action);
                    }

                    protected IModel<String> getConfirmationMessageModel(ColumnMenuAction action, String actionName) {
                        return PageApplications.this.getConfirmationMessageModel(action, actionName);
                    }
                };
                return listInlineMenuHelper.createRowActions(getType());
            }

            @Override
            protected IColumn<SelectableBean<ApplicationType>, String> createIconColumn() {
                return new RoundedImageObjectColumn<>(Model.of(), getPageBase());
            }

            @Override
            protected ISelectableDataProvider<SelectableBean<ApplicationType>> createProvider() {
                Collection<SelectorOptions<GetOperationOptions>> options = getPageBase().getOperationOptionsBuilder()
                        // no read-only because the photo (byte[]) is provided to unknown actors
                        .item(FocusType.F_JPEG_PHOTO).retrieve()
                        .build();

                return createSelectableBeanObjectDataProvider(null, null, options);
            }
        };
        table.setOutputMarkupId(true);
        mainForm.add(table);
    }

     private IModel<String> getConfirmationMessageModel(ColumnMenuAction action, String actionName){
        return WebComponentUtil.createAbstractRoleConfirmationMessage(actionName, action, getObjectListPanel(), this);
    }

    private MainObjectListPanel<ApplicationType> getObjectListPanel() {
        return (MainObjectListPanel<ApplicationType>) get(createComponentPath(ID_MAIN_FORM, ID_TABLE));
    }

    private boolean isShowConfirmationDialog(ColumnMenuAction action){
        return action.getRowModel() != null ||
                getObjectListPanel().getSelectedObjectsCount() > 0;
    }
}
