/*
 * Copyright (c) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.simulation;

import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import com.evolveum.midpoint.authentication.api.authorization.AuthorizationAction;
import com.evolveum.midpoint.authentication.api.authorization.PageDescriptor;
import com.evolveum.midpoint.authentication.api.authorization.Url;
import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.component.MainObjectListPanel;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.web.application.CollectionInstance;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.component.form.MidpointForm;
import com.evolveum.midpoint.web.page.admin.PageAdmin;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TagType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

/**
 * Created by Viliam Repan (lazyman).
 */
@PageDescriptor(
        urls = {
                @Url(mountUrl = "/admin/tags", matchUrlForSecurity = "/admin/tags")
        },
        action = {
                @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_TAGS_ALL_URL,
                        label = "PageAdminUsers.auth.tagsAll.label",
                        description = "PageAdminUsers.auth.usersAll.description"),
                @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_TAGS_URL,
                        label = "PageTags.auth.tags.label",
                        description = "PageTags.auth.tags.description")
        })
@CollectionInstance(identifier = "allTags", applicableForType = TagType.class,
        display = @PanelDisplay(label = "PageAdmin.menu.top.tags.list", singularLabel = "ObjectType.tag", icon = GuiStyleConstants.CLASS_CIRCLE_FULL))
public class PageTags extends PageAdmin {

    private static final long serialVersionUID = 1L;

    private static final String ID_FORM = "form";
    private static final String ID_TABLE = "table";

    public PageTags() {
        this(new PageParameters());
    }

    public PageTags(PageParameters parameters) {
        super(parameters);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();

        initLayout();
    }

    private void initLayout() {
        Form form = new MidpointForm(ID_FORM);
        add(form);

        MainObjectListPanel<TagType> table = new MainObjectListPanel<>(ID_TABLE, TagType.class) {

            @Override
            protected UserProfileStorage.TableId getTableId() {
                return UserProfileStorage.TableId.PAGE_TAGS_TABLE;
            }
        };
        form.add(table);
    }
}
