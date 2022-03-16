/*
 * Copyright (c) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.messagetemplate;

import com.evolveum.midpoint.authentication.api.authorization.AuthorizationAction;
import com.evolveum.midpoint.authentication.api.authorization.PageDescriptor;
import com.evolveum.midpoint.authentication.api.authorization.Url;
import com.evolveum.midpoint.authentication.api.util.AuthConstants;
import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.component.MainObjectListPanel;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.web.application.CollectionInstance;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.component.data.column.CheckBoxHeaderColumn;
import com.evolveum.midpoint.web.component.form.MidpointForm;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.page.admin.PageAdmin;
import com.evolveum.midpoint.web.session.UserProfileStorage.TableId;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ArchetypeType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.MessageTemplateType;

import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.markup.html.form.Form;

import java.util.Arrays;
import java.util.List;

@PageDescriptor(
        urls = {
                @Url(mountUrl = "/admin/messageTemplates")
        },
        action = {
                @AuthorizationAction(actionUri = AuthConstants.AUTH_CONFIGURATION_ALL,
                        label = AuthConstants.AUTH_CONFIGURATION_ALL_LABEL,
                        description = AuthConstants.AUTH_CONFIGURATION_ALL_DESCRIPTION),
                @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_MESSAGE_TEMPLATES_ALL_URL,
                        label = "PageMessageTemplates.auth.archetypesAll.label",
                        description = "PageMessageTemplates.auth.archetypesAll.description"),
                @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_MESSAGE_TEMPLATES_URL,
                        label = "PageMessageTemplates.auth.messageTemplates.label",
                        description = "PageMessageTemplates.auth.messageTemplates.description"),
                @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_MESSAGE_TEMPLATES_VIEW_URL,
                        label = "PageMessageTemplates.auth.messageTemplates.view.label",
                        description = "PageMessageTemplates.auth.messageTemplates.view.description")
        })
@CollectionInstance(identifier = "allMessageTemplates", applicableForType = MessageTemplateType.class,
        display = @PanelDisplay(label = "PageAdmin.menu.top.messageTemplates.list", singularLabel = "ObjectType.messageTemplate", icon = GuiStyleConstants.EVO_MESSAGE_TEMPLATE_TYPE_ICON))
public class PageMessageTemplates extends PageAdmin {

    private static final long serialVersionUID = 1L;

    private static final String ID_MAIN_FORM = "mainForm";
    private static final String ID_TABLE = "table";

    public PageMessageTemplates() {
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

        MainObjectListPanel<MessageTemplateType> table = new MainObjectListPanel<>(ID_TABLE, MessageTemplateType.class) {

            @Override
            protected TableId getTableId() {
                return TableId.TABLE_MESSAGE_TEMPLATES;
            }

            @Override
            protected IColumn<SelectableBean<MessageTemplateType>, String> createCheckboxColumn() {
                return new CheckBoxHeaderColumn<>();
            }

            @Override
            protected List<InlineMenuItem> createInlineMenu() {
                return Arrays.asList(createDeleteInlineMenu());
            }

            @Override
            protected String getConfirmMessageKeyForMultiObject() {
                return "pageMessageTemplates.message.confirmationMessageForMultipleObject";
            }

            @Override
            protected String getConfirmMessageKeyForSingleObject() {
                return "pageMessageTemplates.message.confirmationMessageForSingleObject";
            }
        };
        table.setOutputMarkupId(true);
        mainForm.add(table);
    }
}
