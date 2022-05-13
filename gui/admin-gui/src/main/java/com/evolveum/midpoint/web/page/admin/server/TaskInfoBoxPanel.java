/*
 * Copyright (c) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.page.admin.server;

import com.evolveum.midpoint.gui.impl.component.box.InfoBoxPanel;

import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.request.component.IRequestablePage;

import com.evolveum.midpoint.gui.api.model.ReadOnlyModel;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.web.page.admin.server.dto.ActivityInfoBoxDto;
import com.evolveum.midpoint.web.util.TooltipBehavior;

public class TaskInfoBoxPanel extends InfoBoxPanel<ActivityInfoBoxDto> {

    private static final String ID_DURATION = "duration";
    private static final String ID_ERROR_MESSAGE = "errorMessage";

    public TaskInfoBoxPanel(String id, IModel<ActivityInfoBoxDto> model) {
        super(id, model, null);
    }

    @Override
    protected void customInitLayout(WebMarkupContainer parentInfoBox, IModel<ActivityInfoBoxDto> model, Class<? extends IRequestablePage> linkPage) {

        Label duration = new Label(ID_DURATION, new ReadOnlyModel<>(() -> model != null && model.getObject() != null ?
                WebComponentUtil.formatDurationWordsForLocal(model.getObject().getDuration(), true, true, getPageBase()) : ""));
        parentInfoBox.add(duration);

        Label errorMessage = new Label(ID_ERROR_MESSAGE, new PropertyModel<>(model, ActivityInfoBoxDto.F_ERROR_MESSAGE));
        parentInfoBox.add(errorMessage);
        errorMessage.add(AttributeAppender.append("title", new PropertyModel<>(model, ActivityInfoBoxDto.F_ERROR_MESSAGE)));
        errorMessage.add(new TooltipBehavior() {
            @Override
            public String getDataPlacement() {
                return "bottom";
            }
        });


        if (linkPage != null) {
            add(new AjaxEventBehavior("click") {
                private static final long serialVersionUID = 1L;

                @Override
                protected void onEvent(AjaxRequestTarget target) {
                    setResponsePage(linkPage);
                }
            });
        }
    }
}
