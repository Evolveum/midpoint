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

import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.page.admin.configuration.component.EmptyOnBlurAjaxFormUpdatingBehaviour;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;

import java.io.Serial;

public class CommentPanel extends BasePanel<String> implements Popupable {

    @Serial private static final long serialVersionUID = 1L;

    private static final String ID_INFORMATION_LABEL = "informationLabel";
    private static final String ID_COMMENT_LABEL = "commentLabel";
    private static final String ID_REQUIRED = "required";
    private static final String ID_COMMENT = "comment";
    private static final String ID_CONFIRM_BUTTON = "confirmButton";
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
        IModel<String> informationModel = createInformationLabelModel();
        Label informationLabel = new Label(ID_INFORMATION_LABEL, informationModel);
        informationLabel.add(new VisibleBehaviour(() -> StringUtils.isNotEmpty(informationModel.getObject())));
        add(informationLabel);

        Label commentLabel = new Label(ID_COMMENT_LABEL, createStringResource("CommentPanel.comment"));
        add(commentLabel);

        WebMarkupContainer required = new WebMarkupContainer(ID_REQUIRED);
        required.add(new VisibleBehaviour(() -> false)); //todo implement
        add(required);

        TextArea<String> comment = new TextArea<>(ID_COMMENT, getModel());
        comment.setOutputMarkupId(true);
        comment.add(new EmptyOnBlurAjaxFormUpdatingBehaviour());
        add(comment);

        AjaxButton confirmButton = new AjaxButton(ID_CONFIRM_BUTTON, createStringResource("CommentPanel.confirm")) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                savePerformed(target, CommentPanel.this.getModelObject());
            }
        };
        add(confirmButton);

        AjaxButton cancelButton = new AjaxButton(ID_CANCEL_BUTTON, createStringResource("Button.cancel")) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                cancelPerformed(target);
            }
        };
        add(cancelButton);
    }

    protected IModel<String> createInformationLabelModel() {
        return Model.of();
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

    @Override
    public IModel<String> getTitleIconClass() {
        return Model.of("fa fa-comments");
    }
}
