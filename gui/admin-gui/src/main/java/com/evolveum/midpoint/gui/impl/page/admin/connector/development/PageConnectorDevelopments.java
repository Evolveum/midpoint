/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.page.admin.connector.development;

import com.evolveum.midpoint.authentication.api.authorization.AuthorizationAction;
import com.evolveum.midpoint.authentication.api.authorization.PageDescriptor;
import com.evolveum.midpoint.authentication.api.authorization.Url;
import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.component.MainObjectListPanel;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.web.application.CollectionInstance;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.component.form.MidpointForm;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.page.admin.PageAdmin;
import com.evolveum.midpoint.web.page.admin.services.PageServices;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnectorDevelopmentType;

import org.apache.wicket.markup.html.form.Form;

import java.util.ArrayList;
import java.util.List;

@PageDescriptor(
        urls = {
                @Url(mountUrl = "/admin/connectorDevelopments")
        },
        action = {
                @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_CONNECTOR_DEVELOPMENTS_ALL_URL,
                        label = "PageAdminConnectorDevelopments.auth.connectorDevelopmentsAll.label",
                        description = "PageAdminConnectorDevelopments.auth.connectorDevelopmentsAll.description"),
                @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_CONNECTOR_DEVELOPMENTS_URL,
                        label = "PageConnectorDevelopments.auth.connectorDevelopments.label",
                        description = "PageConnectorDevelopments.auth.connectorDevelopments.description")})
//                @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_CONNECTOR_DEVELOPMENTS_VIEW_URL,
//                        label = "PageConnectorDevelopments.auth.connectorDevelopments.view.label",
//                        description = "PageConnectorDevelopments.auth.connectorDevelopments.view.description")})
@CollectionInstance(identifier = "allConnectorDevelopments", applicableForType = ConnectorDevelopmentType.class,
        display = @PanelDisplay(label = "PageAdmin.menu.top.connectorDevelopments.list", singularLabel = "ObjectType.connectorDevelopment", icon = GuiStyleConstants.CLASS_OBJECT_CONNECTOR_ICON))
public class PageConnectorDevelopments extends PageAdmin {
    private static final long serialVersionUID = 1L;

    private static final String DOT_CLASS = PageServices.class.getName() + ".";

    private static final String ID_MAIN_FORM = "mainForm";
    private static final String ID_TABLE = "table";


    public PageConnectorDevelopments() {
        super();
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    protected void initLayout() {

        Form<?> mainForm = new MidpointForm<>(ID_MAIN_FORM);
        add(mainForm);

        MainObjectListPanel<ConnectorDevelopmentType> table = new MainObjectListPanel<>(ID_TABLE, ConnectorDevelopmentType.class) {

            @Override
            protected UserProfileStorage.TableId getTableId() {
                return UserProfileStorage.TableId.TABLE_CONNECTOR_DEVELOPMENTS;
            }

            @Override
            protected boolean isDuplicationSupported() {
                return false;
            }

            @Override
            protected List<InlineMenuItem> createInlineMenu() {
                List<InlineMenuItem> menu = new ArrayList<>();
                menu.add(createDeleteInlineMenu());
                return menu;
            }

            @Override
            protected String getNothingSelectedMessage() {
                return getString("PageConnectorDevelopments.message.nothingSelected");
            }

            @Override
            protected String getConfirmMessageKeyForMultiObject() {
                return "PageConnectorDevelopments.message.confirmationMessageForMultipleObject";
            }

            @Override
            protected String getConfirmMessageKeyForSingleObject() {
                return "PageConnectorDevelopments.message.confirmationMessageForSingleObject";
            }

        };
        table.setOutputMarkupId(true);
        mainForm.add(table);
    }
}
