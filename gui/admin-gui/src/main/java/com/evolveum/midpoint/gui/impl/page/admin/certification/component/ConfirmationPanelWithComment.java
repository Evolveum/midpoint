/*
 * Copyright (c) 2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.certification.component;

import com.evolveum.midpoint.web.component.dialog.ConfirmationPanel;
import com.evolveum.midpoint.web.component.dialog.Popupable;

import com.evolveum.midpoint.web.page.admin.configuration.component.EmptyOnBlurAjaxFormUpdatingBehaviour;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.model.IModel;

import java.io.Serial;

public class ConfirmationPanelWithComment extends ConfirmationPanel implements Popupable {

    @Serial private static final long serialVersionUID = 1L;

    private static final String ID_COMMENT_LABEL = "commentLabel";
    private static final String ID_COMMENT = "comment";

    String comment;

    public ConfirmationPanelWithComment(String id, IModel<String> messageModel) {
        super(id, messageModel);
    }

    @Override
    protected void initLayout() {
        super.initLayout();

        Label commentLabel = new Label(ID_COMMENT_LABEL, createStringResource("ResponseConfirmationPanel.comment"));
        add(commentLabel);

        TextArea<String> commentPanel = new TextArea<>(ID_COMMENT, new IModel<String>() {

            @Serial private static final long serialVersionUID = 1L;

            @Override
            public void setObject(String object) {
                comment = object;
            }

            @Override
            public String getObject() {
                return comment;
            }
        });
        commentPanel.setOutputMarkupId(true);
        commentPanel.add(new EmptyOnBlurAjaxFormUpdatingBehaviour());
        add(commentPanel);
    }

    public void yesPerformed(AjaxRequestTarget target) {
        yesPerformedWithComment(target, comment);
    }

    protected void yesPerformedWithComment(AjaxRequestTarget target, String comment) {
        // to be overridden
    }

}
