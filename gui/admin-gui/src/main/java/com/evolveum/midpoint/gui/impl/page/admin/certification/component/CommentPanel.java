/*
 * Copyright (c) 2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.certification.component;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.dialog.Popupable;

import com.evolveum.midpoint.web.component.message.FeedbackAlerts;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.page.admin.configuration.component.EmptyOnBlurAjaxFormUpdatingBehaviour;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.feedback.ComponentFeedbackMessageFilter;
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
    private static final String ID_FEEDBACK_PANEL = "feedbackPanel";

    public CommentPanel(String id, IModel<String> model) {
        super(id, model);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();

        initLayout();
    }

    private void initLayout() {
        setOutputMarkupId(true);

        IModel<String> informationModel = createInformationLabelModel();
        Label informationLabel = new Label(ID_INFORMATION_LABEL, informationModel);
        informationLabel.add(new VisibleBehaviour(() -> StringUtils.isNotEmpty(informationModel.getObject())));
        add(informationLabel);

        Label commentLabel = new Label(ID_COMMENT_LABEL, createStringResource("CommentPanel.comment"));
        add(commentLabel);

        WebMarkupContainer required = new WebMarkupContainer(ID_REQUIRED);
        required.add(new VisibleBehaviour(this::isCommentRequired));
        add(required);

        TextArea<String> commentArea = new TextArea<>(ID_COMMENT, getModel());
        commentArea.setOutputMarkupId(true);
        commentArea.add(new EmptyOnBlurAjaxFormUpdatingBehaviour());
        commentArea.setRequired(isCommentRequired());
        add(commentArea);

        FeedbackAlerts feedback = new FeedbackAlerts(ID_FEEDBACK_PANEL);
        feedback.setOutputMarkupId(true);
        feedback.setOutputMarkupPlaceholderTag(true);
        feedback.setFilter(new ComponentFeedbackMessageFilter(commentArea));
        add(feedback);

        AjaxButton confirmButton = new AjaxButton(ID_CONFIRM_BUTTON, createStringResource("CommentPanel.confirm")) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                String comment = CommentPanel.this.getModelObject();
                if (isCommentRequired() && StringUtils.isEmpty(comment)) {
                    commentArea.error(getString("CommentPanel.comment.required"));
                    target.add(getCommentPanelFeedbackPanel());
                    return;
                }
                savePerformed(target, comment);
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

    protected boolean isCommentRequired() {
        return false;
    }

    private Component getCommentPanelFeedbackPanel() {
        return get(ID_FEEDBACK_PANEL);
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
