/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.page.admin.objectCollection;

import com.evolveum.midpoint.authentication.api.authorization.AuthorizationAction;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.authentication.api.authorization.PageDescriptor;
import com.evolveum.midpoint.authentication.api.authorization.Url;
import com.evolveum.midpoint.authentication.api.util.AuthConstants;
import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.component.MainObjectListPanel;
import com.evolveum.midpoint.web.application.*;
import com.evolveum.midpoint.web.component.data.column.ColumnUtils;
import com.evolveum.midpoint.web.component.form.MidpointForm;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.page.admin.PageAdmin;
import com.evolveum.midpoint.web.session.UserProfileStorage;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectCollectionType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectTemplateType;

import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.markup.html.form.Form;

import java.util.ArrayList;
import java.util.List;

/**
 * @author skublik
 */
@PageDescriptor(
        urls = {
                @Url(mountUrl = "/admin/objectCollections", matchUrlForSecurity = "/admin/objectCollections")
        },
        action = {
        @AuthorizationAction(actionUri = AuthConstants.AUTH_CONFIGURATION_ALL,
                label = AuthConstants.AUTH_CONFIGURATION_ALL_LABEL,
                description = AuthConstants.AUTH_CONFIGURATION_ALL_DESCRIPTION),
        @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_OBJECT_COLLECTIONS_ALL_URL,
                label = "PageObjectCollections.auth.objectCollectionAll.label",
                description = "PageObjectCollections.auth.objectCollectionAll.description"),
        @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_OBJECT_COLLECTIONS_URL,
                label = "PageObjectCollections.auth.objectCollections.label",
                description = "PageObjectCollections.auth.objectCollections.description")
})
@CollectionInstance(identifier = "allObjectCollections", applicableForType = ObjectCollectionType.class,
        display = @PanelDisplay(label = "PageAdmin.menu.top.objectCollections.list", singularLabel = "ObjectType.objectCollection", icon = GuiStyleConstants.CLASS_OBJECT_COLLECTION_ICON))
public class PageObjectCollections extends PageAdmin{

    private static final long serialVersionUID = 1L;

    private static final String ID_MAIN_FORM = "mainForm";
    private static final String ID_TABLE = "table";

    public PageObjectCollections() {
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

        MainObjectListPanel<ObjectCollectionType> table = new MainObjectListPanel<ObjectCollectionType>(ID_TABLE, ObjectCollectionType.class) {

            @Override
            protected UserProfileStorage.TableId getTableId() {
                return UserProfileStorage.TableId.TABLE_OBJECTS_COLLECTION;
            }

            @Override
            protected List<InlineMenuItem> createInlineMenu() {
                List<InlineMenuItem> menu = new ArrayList<>();
                menu.add(createDeleteInlineMenu());
                return menu;
            }

            @Override
            protected List<IColumn<SelectableBean<ObjectCollectionType>, String>> createDefaultColumns() {
                return ColumnUtils.getDefaultObjectColumns();
            }

            @Override
            protected String getNothingSelectedMessage() {
                return getString("pageObjectCollections.message.nothingSelected");
            }

            @Override
            protected String getConfirmMessageKeyForMultiObject() {
                return "pageObjectCollections.message.confirmationMessageForMultipleObject";
            }

            @Override
            protected String getConfirmMessageKeyForSingleObject() {
                return "pageObjectCollections.message.confirmationMessageForSingleObject";
            }
        };
        table.setOutputMarkupId(true);
        mainForm.add(table);
    }

    private MainObjectListPanel<ObjectTemplateType> getTable() {
        return (MainObjectListPanel<ObjectTemplateType>) get(createComponentPath(ID_MAIN_FORM, ID_TABLE));
    }
}
