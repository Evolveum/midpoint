/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.page.admin.server.handlers;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.web.component.data.column.LinkIconPanel;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.page.admin.server.handlers.dto.LiveSyncHandlerDto;
import com.evolveum.midpoint.web.page.admin.server.handlers.dto.ResourceRelatedHandlerDto;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;

/**
 * @author mederly
 */
public class LiveSyncHandlerPanel extends ResourceRelatedHandlerPanel<LiveSyncHandlerDto> {

    private static final long serialVersionUID = 1L;

    private static final String ID_TOKEN_CONTAINER = "tokenContainer";
    private static final String ID_TOKEN = "token";
    private static final String ID_DELETE_TOKEN = "deleteToken";

        private static final String ID_TOKEN_RETRY_CONTAINER = "retryUnhandledErrContainer";
    private static final String ID_TOKEN_RETRY_CHECKBOX_CONTAINER = "retryUnhandledErrCheckboxContainer";
    private static final String ID_TOKEN_RETRY_CHECKBOX = "retryUnhandledErrCheckbox";

    public LiveSyncHandlerPanel(String id, IModel<LiveSyncHandlerDto> handlerDtoModel, PageBase parentPage) {
        super(id, handlerDtoModel, parentPage);
        initLayout(parentPage);
    }

    private void initLayout(final PageBase parentPage) {
        WebMarkupContainer tokenContainer = new WebMarkupContainer(ID_TOKEN_CONTAINER);
        tokenContainer.add(new VisibleEnableBehaviour() {
            @Override
            public boolean isVisible() {
                return true;    //TODO
            }
        });
        tokenContainer.add(new Label(ID_TOKEN, new PropertyModel<>(getModel(), LiveSyncHandlerDto.F_TOKEN)));

        LinkIconPanel deleteTokenPanel = new LinkIconPanel(ID_DELETE_TOKEN, new Model("fa fa-fw fa-trash-o fa-lg text-danger"), createStringResource("LiveSyncHandlerPanel.deleteToken")) {
            @Override
            protected void onClickPerformed(AjaxRequestTarget target) {
//                parentPage.getController().deleteSyncTokenPerformed(target);
            }
        };
//        deleteTokenPanel.add(new VisibleEnableBehaviour() {
//            @Override
//            public boolean isVisible() {
//                return !parentPage.isEdit() && getModelObject().hasToken();        // TODO ... and security
//            }
//        });
        deleteTokenPanel.setRenderBodyOnly(true);
        tokenContainer.add(deleteTokenPanel);
        add(tokenContainer);

                WebMarkupContainer retryContainer = new WebMarkupContainer(ID_TOKEN_RETRY_CONTAINER);
        add(retryContainer);
        WebMarkupContainer retryCheckboxContainer = new WebMarkupContainer(ID_TOKEN_RETRY_CHECKBOX_CONTAINER);
        retryContainer.add(retryCheckboxContainer);
        CheckBox retryCheckbox = new CheckBox(ID_TOKEN_RETRY_CHECKBOX, new PropertyModel<>(getModel(), ResourceRelatedHandlerDto.F_TOKEN_RETRY_UNHANDLED_ERR));
        retryCheckbox.add(enabledIfEdit);
        retryCheckboxContainer.add(retryCheckbox);
    }

}
