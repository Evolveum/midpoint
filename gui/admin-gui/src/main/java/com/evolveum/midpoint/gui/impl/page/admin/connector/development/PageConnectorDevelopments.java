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
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.web.application.CollectionInstance;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.component.data.column.ColumnMenuAction;
import com.evolveum.midpoint.web.component.form.MidpointForm;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItemAction;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.page.admin.PageAdmin;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnectorDevelopmentType;


import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.IModel;

import java.io.Serial;
import java.util.ArrayList;
import java.util.List;

@PageDescriptor(
        urls = {
                @Url(mountUrl = "/admin/connectorDevelopments")
        },
        action = {
                @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_CONNECTOR_WIZARD_URL,
                        label = "PageAdminConnectorDevelopments.auth.connectorGenerator.label",
                        description = "PageAdminConnectorDevelopments.auth.connectorGenerator.description")})
@CollectionInstance(identifier = "allConnectorDevelopments", applicableForType = ConnectorDevelopmentType.class,
        display = @PanelDisplay(label = "PageAdmin.menu.top.connectorDevelopments.list", singularLabel = "ObjectType.connectorDevelopment", icon = GuiStyleConstants.CLASS_OBJECT_CONNECTOR_ICON))
public class PageConnectorDevelopments extends PageAdmin {
    private static final long serialVersionUID = 1L;

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

        MainObjectListPanel<ConnectorDevelopmentType> table = new MainObjectListPanel<ConnectorDevelopmentType>(ID_TABLE, ConnectorDevelopmentType.class) {

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

            IModel<String> getDeleteMessageModel(ColumnMenuAction action, String actionName) {
                return new IModel<String>() {
                    private static final long serialVersionUID = 1L;
                    @Override
                    public String getObject() {
                        String confirmation;
                        switch (getSelectedObjectsCount()) {
                            case 1:
                                ConnectorDevelopmentType first = getSelectedObjects().get(0).getValue();
                                String name = WebComponentUtil
                                        .getName(first);
                                confirmation = createStringResource("PageConnectorDevelopment.message.deleteSingleConfirm", name).getString();
                                break;
                            default:
                                confirmation = createStringResource("PageConnectorDevelopment.message.deleteMultipleConfirm", getSelectedObjectsCount()).getString();
                                break;
                        }
                        String note = createStringResource("PageConnectors.message.deleteConnectorNote").getString();
                        return confirmation + " " + note;
                    }
                };
            }

            @Override
            public InlineMenuItem createDeleteInlineMenu() {
                return new InlineMenuItem(createStringResource("MainObjectListPanel.menu.delete")) {
                    @Serial private static final long serialVersionUID = 1L;

                    @Override
                    public InlineMenuItemAction initAction() {
                        return new ColumnMenuAction<SelectableBean<ConnectorDevelopmentType>>() {
                            @Override
                            public void onClick(AjaxRequestTarget target) {
                                deleteConfirmedPerformed(target, getRowModel());
                            }
                        };
                    }

                    @Override
                    public IModel<String> getConfirmationMessageModel() {
                        String actionName = createStringResource("MainObjectListPanel.message.deleteAction").getString();
                        return getDeleteMessageModel((ColumnMenuAction) getAction(), actionName);
                    }
                };
            }
        };
        table.setOutputMarkupId(true);
        mainForm.add(table);
    }
}
