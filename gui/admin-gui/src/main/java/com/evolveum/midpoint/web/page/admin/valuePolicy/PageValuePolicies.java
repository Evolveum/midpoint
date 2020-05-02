/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.page.admin.valuePolicy;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.impl.component.icon.CompositedIconBuilder;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.web.application.AuthorizationAction;
import com.evolveum.midpoint.web.application.PageDescriptor;
import com.evolveum.midpoint.web.application.Url;
import com.evolveum.midpoint.web.component.menu.cog.ButtonInlineMenuItem;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItemAction;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.page.admin.PageAdminObjectList;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ValuePolicyType;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by matus on 9/8/2017.
 */

@PageDescriptor(
        urls = {
                @Url(mountUrl = "/admin/valuepolicies", matchUrlForSecurity = "/admin/valuepolicies" )
        },
        action = {
                @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_VALUE_POLICIES_ALL_URL,
                        label = "PageAdminValuePolicies.auth.valuePoliciesAll.label",
                        description = "PageAdminValuePolicies.auth.valuePoliciesAll.description"),
                @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_VALUE_POLICIES_URL,
                        label = "PageValuePolicies.auth.valuePolicies.label",
                        description = "PageValuePolicies.auth.valuePolicies.description")
        })

public class PageValuePolicies extends PageAdminObjectList<ValuePolicyType> {

    private static final long serialVersionUID = 1L;

    public static final String ID_MAIN_FORM = "mainForm";
    public static final String ID_VALUE_POLICIES_TABLE = "valuePoliciesTable";

    public PageValuePolicies() {
        initLayout();
    }

    @Override
    protected void objectDetailsPerformed(AjaxRequestTarget target, ValuePolicyType valuePolicy) {
        PageValuePolicies.this.valuePolicyDetailsPerformed(target, valuePolicy);
    }

    @Override
    protected List<IColumn<SelectableBean<ValuePolicyType>, String>> initColumns() {
        return PageValuePolicies.this.initValuePoliciesColumns();
    }

    @Override
    protected List<InlineMenuItem> createRowActions() {
        return PageValuePolicies.this.createInlineMenu();
    }


    @Override
    protected Class<ValuePolicyType> getType() {
        return ValuePolicyType.class;
    }

    private void valuePolicyDetailsPerformed(AjaxRequestTarget target, ValuePolicyType valuePolicy) {
        PageParameters pageParameters = new PageParameters();
        pageParameters.add(OnePageParameterEncoder.PARAMETER, valuePolicy.getOid());
        navigateToNext(PageValuePolicy.class, pageParameters);
    }

    private List<IColumn<SelectableBean<ValuePolicyType>, String>> initValuePoliciesColumns() {
        List<IColumn<SelectableBean<ValuePolicyType>, String>> columns = new ArrayList<>();

        IColumn column = new PropertyColumn(createStringResource("pageValuePolicies.table.description"), "value.description");
        columns.add(column);

        return columns;
    }


    private List<InlineMenuItem> createInlineMenu() {
        List<InlineMenuItem> menu = new ArrayList<>();
        menu.add(new ButtonInlineMenuItem(createStringResource("pageValuePolicies.button.delete")) {
            private static final long serialVersionUID = 1L;

            @Override
            public CompositedIconBuilder getIconCompositedBuilder() {
                return getDefaultCompositedIconBuilder(GuiStyleConstants.CLASS_DELETE_MENU_ITEM);
            }

            @Override
            public InlineMenuItemAction initAction() {
                return null;
            }
        });
        return menu;

    }

    @Override
    protected UserProfileStorage.TableId getTableId(){
        return UserProfileStorage.TableId.TABLE_VALUE_POLICIES;
    }
}
