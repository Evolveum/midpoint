/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.page.admin.archetype;

import com.evolveum.midpoint.authentication.api.*;
import com.evolveum.midpoint.authentication.api.util.AuthConstants;
import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.component.MainObjectListPanel;
import com.evolveum.midpoint.web.application.CollectionInstance;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.component.form.MidpointForm;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.page.admin.PageAdmin;

import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.markup.html.form.Form;

import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.web.session.UserProfileStorage.TableId;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ArchetypeType;

@PageDescriptor(
        urls = {
                @Url(mountUrl = "/admin/archetypes", matchUrlForSecurity = "/admin/archetypes")
        },
        action = {
                @AuthorizationAction(actionUri = AuthConstants.AUTH_CONFIGURATION_ALL,
                        label = AuthConstants.AUTH_CONFIGURATION_ALL_LABEL,
                        description = AuthConstants.AUTH_CONFIGURATION_ALL_DESCRIPTION),
                @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_ARCHETYPES_ALL_URL,
                        label = "PageArchetypes.auth.archetypesAll.label",
                        description = "PageArchetypes.auth.archetypesAll.description"),
                @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_ARCHETYPES_URL,
                        label = "PageArchetypes.auth.archetypes.label",
                        description = "PageArchetypes.auth.archetypes.description"),
                @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_ARCHETYPES_VIEW_URL,
                        label = "PageArchetypes.auth.archetypes.view.label",
                        description = "PageArchetypes.auth.archetypes.view.description")
        })
@CollectionInstance(identifier = "allArchetypes", applicableForType = ArchetypeType.class,
        display = @PanelDisplay(label = "PageAdmin.menu.top.archetypes.list", singularLabel = "ObjectType.archetype", icon = GuiStyleConstants.EVO_ARCHETYPE_TYPE_ICON))
public class PageArchetypes extends PageAdmin {

    private static final long serialVersionUID = 1L;

    private static final String ID_MAIN_FORM = "mainForm";
    private static final String ID_TABLE = "table";

    public PageArchetypes() {
        super();
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    private void initLayout() {
        Form mainForm = new MidpointForm(ID_MAIN_FORM);
        add(mainForm);

        MainObjectListPanel<ArchetypeType> table = new MainObjectListPanel<ArchetypeType>(ID_TABLE, ArchetypeType.class) {

            @Override
            protected TableId getTableId() {
                return UserProfileStorage.TableId.TABLE_ARCHETYPES;
            }

            @Override
            protected IColumn<SelectableBean<ArchetypeType>, String> createCheckboxColumn() {
                return null;
            }
        };
        table.setOutputMarkupId(true);
        mainForm.add(table);
    }
}
