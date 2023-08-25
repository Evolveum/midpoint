/*
 * Copyright (c) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.page.admin.shadows;

import com.evolveum.midpoint.authentication.api.authorization.AuthorizationAction;
import com.evolveum.midpoint.authentication.api.authorization.PageDescriptor;

import com.evolveum.midpoint.authentication.api.authorization.Url;

import com.evolveum.midpoint.gui.api.component.data.provider.ISelectableDataProvider;
import com.evolveum.midpoint.gui.impl.component.data.provider.RepositoryShadowBeanObjectDataProvider;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.web.component.util.SelectableBean;

import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.model.api.authentication.CompiledObjectCollectionView;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.web.application.CollectionInstance;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.component.form.MidpointForm;
import com.evolveum.midpoint.web.page.admin.PageAdmin;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

import java.util.Collection;

@PageDescriptor(
        urls = {
                @Url(mountUrl = "/admin/shadows", matchUrlForSecurity = "/admin/shadows")
        },
        action = {
                @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_USERS_ALL_URL,
                        label = "PageShadows.auth.shadowsAll.label",
                        description = "PageShadows.auth.shadowsAll.description"),
                @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_USERS_URL,
                        label = "PageShadows.auth.shadows.label",
                        description = "PageShadows.auth.shadows.description"),
                @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_USERS_VIEW_URL,
                        label = "PageShadows.auth.shadows.view.label",
                        description = "PageShadows.auth.shadows.view.description")
        })
@CollectionInstance(identifier = "allShadows", applicableForType = ShadowType.class,
        display = @PanelDisplay(label = "PageAdmin.menu.top.shadows.list", singularLabel = "ObjectType.shadow", icon = GuiStyleConstants.CLASS_SHADOW_ICON_ACCOUNT))
public class PageShadows extends PageAdmin {

    private static final String ID_MAIN_FORM = "mainForm";
    private static final String ID_TABLE = "table";

    public PageShadows() {
        this(null);
    }

    public PageShadows(PageParameters params) {
        super(params);
    }


    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    protected void initLayout() {

        Form mainForm = new MidpointForm(ID_MAIN_FORM);
        add(mainForm);

        ShadowTablePanel table = new ShadowTablePanel(ID_TABLE, null) {

            @Override
            protected UserProfileStorage.TableId getTableId() {
                return null;
            }

            @Override
            public CompiledObjectCollectionView getObjectCollectionView() {
                return super.getObjectCollectionView();
            }

            @Override
            protected boolean isNewObjectButtonEnabled() {
                return false;
            }

            @Override
            protected ISelectableDataProvider<SelectableBean<ShadowType>> createProvider() {
                return new RepositoryShadowBeanObjectDataProvider(this, getSearchModel(), null) {

                    @Override
                    public Collection<SelectorOptions<GetOperationOptions>> getOptions() {
                        return getObjectCollectionView().getOptions();
                    }
                };
            }
        };
        table.setOutputMarkupId(true);
        mainForm.add(table);
    }

}
