/*
 * Copyright (c) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.policy;

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
import com.evolveum.midpoint.xml.ns._public.common.common_3.PolicyType;

import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.IModel;

import java.util.List;

@PageDescriptor(
        urls = {
                @Url(mountUrl = "/admin/policies")
        },
        action = {
        @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_POLICIES_ALL_URL,
                label = "PageAdminPolicies.auth.policiesAll.label",
                description = "PageAdminPolicies.auth.policiesAll.description"),
        @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_POLICIES_URL,
                label = "PagePolicies.auth.policies.label",
                description = "PagePolicies.auth.policies.description"),
        @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_POLICIES_VIEW_URL,
                label = "PagePolicies.auth.policies.view.label",
                description = "PagePolicies.auth.policies.view.description")})
@CollectionInstance(identifier = "allPolicies", applicableForType = PolicyType.class,
        display = @PanelDisplay(label = "PageAdmin.menu.top.policies.list", singularLabel = "ObjectType.policy", icon = GuiStyleConstants.CLASS_OBJECT_POLICY_ICON))
public class PagePolicies extends PageAdmin {
    private static final long serialVersionUID = 1L;

    private static final String DOT_CLASS = PagePolicies.class.getName() + ".";

    private static final String ID_MAIN_FORM = "mainForm";
    private static final String ID_TABLE = "table";


    public PagePolicies() {
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

        MainObjectListPanel<PolicyType> table = new MainObjectListPanel<>(ID_TABLE, PolicyType.class) {

            @Override
            protected UserProfileStorage.TableId getTableId() {
                return UserProfileStorage.TableId.TABLE_POLICIES;
            }

            @Override
            protected List<InlineMenuItem> createInlineMenu() {
                FocusListInlineMenuHelper<PolicyType> listInlineMenuHelper = new FocusListInlineMenuHelper<>(PolicyType.class, PagePolicies.this, this) {
                    private static final long serialVersionUID = 1L;

                    protected boolean isShowConfirmationDialog(ColumnMenuAction action) {
                        return PagePolicies.this.isShowConfirmationDialog(action);
                    }

                    protected IModel<String> getConfirmationMessageModel(ColumnMenuAction action, String actionName) {
                        return PagePolicies.this.getConfirmationMessageModel(action, actionName);
                    }
                };
                return listInlineMenuHelper.createRowActions(getType());
            }

        };
        table.setOutputMarkupId(true);
        mainForm.add(table);
    }

     private IModel<String> getConfirmationMessageModel(ColumnMenuAction action, String actionName){
        return WebComponentUtil.createAbstractRoleConfirmationMessage(actionName, action, getObjectListPanel(), this);
    }

    private MainObjectListPanel<PolicyType> getObjectListPanel() {
        return (MainObjectListPanel<PolicyType>) get(createComponentPath(ID_MAIN_FORM, ID_TABLE));
    }

    private boolean isShowConfirmationDialog(ColumnMenuAction action){
        return action.getRowModel() != null ||
                getObjectListPanel().getSelectedObjectsCount() > 0;
    }
}
