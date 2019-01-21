/*
 * Copyright (c) 2010-2017 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.web.page.admin.valuePolicy;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.component.MainObjectListPanel;
import com.evolveum.midpoint.model.api.authentication.CompiledObjectCollectionView;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.web.application.AuthorizationAction;
import com.evolveum.midpoint.web.application.PageDescriptor;
import com.evolveum.midpoint.web.application.Url;
import com.evolveum.midpoint.web.component.form.Form;
import com.evolveum.midpoint.web.component.menu.cog.ButtonInlineMenuItem;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItemAction;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.page.admin.PageAdminObjectList;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.xml.ns._public.common.common_4.ValuePolicyType;
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
    protected void newObjectActionPerformed(AjaxRequestTarget target, CompiledObjectCollectionView collectionView) {
        navigateToNext(PageValuePolicy.class);
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
            public String getButtonIconCssClass() {
                return GuiStyleConstants.CLASS_DELETE_MENU_ITEM;
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
