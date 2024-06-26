/*
 * Copyright (c) 2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.page.admin.certification.component;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.dialog.Popupable;

import com.evolveum.midpoint.web.page.admin.configuration.component.EmptyOnBlurAjaxFormUpdatingBehaviour;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.StringResourceModel;

import java.io.Serial;

//todo not sure if it is possible just to add comment to cert work item,
// may be will be removed, or transformed to response + comment panel
public class CommentPanel extends BasePanel<String> implements Popupable {

    @Serial private static final long serialVersionUID = 1L;

    private static final String ID_COMMENT = "comment";
    private static final String ID_SAVE_BUTTON = "saveButton";
    private static final String ID_CANCEL_BUTTON = "cancelButton";

    public CommentPanel(String id, IModel<String> model) {
        super(id, model);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();

        initLayout();
    }

    private void initLayout() {
        TextArea<String> comment = new TextArea<>(ID_COMMENT, getModel());
        comment.setOutputMarkupId(true);
        comment.add(new EmptyOnBlurAjaxFormUpdatingBehaviour());
        add(comment);

        AjaxButton saveButton = new AjaxButton(ID_SAVE_BUTTON, createStringResource("Button.save")) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                savePerformed(target, CommentPanel.this.getModelObject());
            }
        };
        add(saveButton);

        AjaxButton cancelButton = new AjaxButton(ID_CANCEL_BUTTON, createStringResource("Button.cancel")) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                cancelPerformed(target);
            }
        };
        add(cancelButton);
    }

    protected void savePerformed(AjaxRequestTarget target, String comment) {
    }

    private void cancelPerformed(AjaxRequestTarget target) {
        getPageBase().hideMainPopup(target);
    }

    @Override
    public int getWidth() {
        return 600;
    }

    @Override
    public int getHeight() {
        return 400;
    }

    @Override
    public String getWidthUnit() {
        return "px";
    }

    @Override
    public String getHeightUnit() {
        return "px";
    }

    @Override
    public StringResourceModel getTitle() {
        return createStringResource("CommentPanel.title");
    }

    @Override
    public Component getContent() {
        return this;
    }
}
