/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.page.admin.objectCollection;

import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.web.application.AuthorizationAction;
import com.evolveum.midpoint.web.application.PageDescriptor;
import com.evolveum.midpoint.web.component.data.column.ColumnUtils;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.page.admin.PageAdminObjectList;
import com.evolveum.midpoint.web.page.admin.configuration.PageAdminConfiguration;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectCollectionType;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import java.util.ArrayList;
import java.util.List;

/**
 * @author skublik
 */
@PageDescriptor(
        url = "/admin/objectCollections", action = {
        @AuthorizationAction(actionUri = PageAdminConfiguration.AUTH_CONFIGURATION_ALL,
                label = PageAdminConfiguration.AUTH_CONFIGURATION_ALL_LABEL,
                description = PageAdminConfiguration.AUTH_CONFIGURATION_ALL_DESCRIPTION),
        @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_OBJECT_COLLECTIONS_ALL_URL,
                label = "PageObjectCollections.auth.objectCollectionAll.label",
                description = "PageObjectCollections.auth.objectCollectionAll.description"),
        @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_OBJECT_COLLECTIONS_URL,
                label = "PageObjectCollections.auth.objectsCollection.label",
                description = "PageObjectCollections.auth.objectsCollection.description")
})
public class PageObjectCollections extends PageAdminObjectList<ObjectCollectionType> {

    private static final long serialVersionUID = 1L;

    public PageObjectCollections() {
        super();
    }

    @Override
    protected void objectDetailsPerformed(AjaxRequestTarget target, ObjectCollectionType collection) {
        PageParameters pageParameters = new PageParameters();
        pageParameters.add(OnePageParameterEncoder.PARAMETER, collection.getOid());
        navigateToNext(PageObjectCollection.class, pageParameters);
    }

    @Override
    protected Class<ObjectCollectionType> getType() {
        return ObjectCollectionType.class;
    }

    @Override
    protected List<IColumn<SelectableBean<ObjectCollectionType>, String>> initColumns() {

        return ColumnUtils.getDefaultObjectColumns();
    }

    @Override
    protected List<InlineMenuItem> createRowActions() {
        List<InlineMenuItem> menu = new ArrayList<>();
        return menu;
    }

    @Override
    protected UserProfileStorage.TableId getTableId() {
        return UserProfileStorage.TableId.TABLE_OBJECTS_COLLECTION;
    }

}
