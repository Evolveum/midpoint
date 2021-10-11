/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.page.admin.users;

import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.web.application.AuthorizationAction;
import com.evolveum.midpoint.web.application.PageDescriptor;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.page.admin.PageAdmin;
import com.evolveum.midpoint.web.page.admin.reports.component.AceEditorPanel;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.IModel;

/**
 * Created by honchar.
 */
@PageDescriptor(url = "/admin/xmlDataReview", action = {
        @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_USERS_ALL_URL,
                label = "PageAdminUsers.auth.usersAll.label",
                description = "PageAdminUsers.auth.usersAll.description"),
        @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_USER_HISTORY_XML_REVIEW_URL,
                label = "PageUser.auth.userHistoryXmlReview.label",
                description = "PageUser.auth.userHistoryXmlReview.description")})
public class PageXmlDataReview extends PageAdmin {

    private static final String ID_ACE_EDITOR_CONTAINER = "aceEditorContainer";
    private static final String ID_ACE_EDITOR_PANEL = "aceEditorPanel";
    private static final String ID_BUTTON_BACK = "back";

    public PageXmlDataReview(IModel<String> title, IModel<String> data){
        initLayout(title, data);
    }

    private void initLayout(IModel<String> title, IModel<String> data){
        WebMarkupContainer container = new WebMarkupContainer(ID_ACE_EDITOR_CONTAINER);
        container.setOutputMarkupId(true);
        add(container);

        AceEditorPanel aceEditorPanel = new AceEditorPanel(ID_ACE_EDITOR_PANEL, title, data);
        aceEditorPanel.getEditor().setReadonly(true);
        aceEditorPanel.setOutputMarkupId(true);
        container.add(aceEditorPanel);

        AjaxButton back = new AjaxButton(ID_BUTTON_BACK, createStringResource("PageBase.button.back")) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                redirectBack();
            }

        };
        add(back);
    }

}
