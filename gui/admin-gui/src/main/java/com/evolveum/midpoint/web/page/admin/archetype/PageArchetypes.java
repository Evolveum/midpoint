/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.page.admin.archetype;

import java.util.ArrayList;
import java.util.List;

import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.page.admin.configuration.PageAdminConfiguration;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.web.application.AuthorizationAction;
import com.evolveum.midpoint.web.application.PageDescriptor;
import com.evolveum.midpoint.web.component.data.column.ColumnUtils;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.page.admin.PageAdminObjectList;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.web.session.UserProfileStorage.TableId;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ArchetypeType;

@PageDescriptor(
        url = "/admin/archetypes", action = {
                @AuthorizationAction(actionUri = PageAdminConfiguration.AUTH_CONFIGURATION_ALL,
                        label = PageAdminConfiguration.AUTH_CONFIGURATION_ALL_LABEL,
                        description = PageAdminConfiguration.AUTH_CONFIGURATION_ALL_DESCRIPTION),
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
public class PageArchetypes extends PageAdminObjectList<ArchetypeType> {

    private static final long serialVersionUID = 1L;

    public PageArchetypes() {
        super();
    }

    @Override
    protected void objectDetailsPerformed(AjaxRequestTarget target, ArchetypeType archetype) {
        PageParameters pageParameters = new PageParameters();
        pageParameters.add(OnePageParameterEncoder.PARAMETER, archetype.getOid());
        navigateToNext(PageArchetype.class, pageParameters);
    }

    @Override
    protected Class<ArchetypeType> getType() {
        return ArchetypeType.class;
    }

    @Override
    protected List<IColumn<SelectableBean<ArchetypeType>, String>> initColumns() {
        return ColumnUtils.getDefaultArchetypeColumns();
    }

    @Override
    protected List<InlineMenuItem> createRowActions() {
        List<InlineMenuItem> menu = new ArrayList<>();
        return menu;
    }

    @Override
    protected TableId getTableId() {
        return UserProfileStorage.TableId.TABLE_ARCHETYPES;
    }

}
