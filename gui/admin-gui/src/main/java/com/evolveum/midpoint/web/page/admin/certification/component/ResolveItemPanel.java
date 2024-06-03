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
import com.evolveum.midpoint.web.component.message.FeedbackAlerts;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.page.admin.configuration.component.EmptyOnBlurAjaxFormUpdatingBehaviour;

import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationResponseType;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;

import java.io.Serial;
import java.util.Arrays;
import java.util.List;

public class ResolveItemPanel extends BasePanel implements Popupable {

    @Serial private static final long serialVersionUID = 1L;

    private static final String ID_COMMENT = "comment";
    private static final String ID_RESPONSES_PANEL = "responsesPanel";
    private static final String ID_RESPONSE_PANEL = "responsePanel";
    private static final String ID_SAVE_BUTTON = "saveButton";
    private static final String ID_CANCEL_BUTTON = "cancelButton";
    private static final String ID_FEEDBACK = "feedback";

    AccessCertificationResponseType selectedResponse = null;

    public ResolveItemPanel(String id) {
        super(id);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();

        initLayout();
    }

    private void initLayout() {
        setOutputMarkupId(true);

        List<AccessCertificationResponseType> responses = getResponses();
        ListView<AccessCertificationResponseType> responsesPanel = new ListView<>(ID_RESPONSES_PANEL, responses) {

            @Serial private static final long serialVersionUID = 1L;

            @Override
            protected void populateItem(ListItem<AccessCertificationResponseType> item) {
                ResponseSelectablePanel widget = new ResponseSelectablePanel(ID_RESPONSE_PANEL, item.getModel()) {
                    @Serial private static final long serialVersionUID = 1L;

                    @Override
                    protected void responseSelectedPerformed(AccessCertificationResponseType response, AjaxRequestTarget target) {
                        selectedResponse = response;
                        target.add(ResolveItemPanel.this);
                    }

                    protected IModel<String> getAdditionalLinkStyle(AccessCertificationResponseType response) {
                        return getItemPanelAdditionalStyle(response);
                    }
                };
                item.add(widget);
            }
        };
        responsesPanel.setOutputMarkupId(true);
        responsesPanel.add(new VisibleBehaviour(() -> CollectionUtils.isNotEmpty(responses)));
        add(responsesPanel);


        TextArea<String> comment = new TextArea<>(ID_COMMENT, Model.of(""));
        comment.setOutputMarkupId(true);
        comment.add(new EmptyOnBlurAjaxFormUpdatingBehaviour());
        add(comment);

        AjaxButton saveButton = new AjaxButton(ID_SAVE_BUTTON, createStringResource("PageBase.button.save")) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                if (selectedResponse == null) {
                    warn(getString("PageCertDecisions.message.noItemSelected"));
                    target.add(ResolveItemPanel.this);
                    return;
                }
                savePerformed(target, selectedResponse, getComment());
                getPageBase().hideMainPopup(target);
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

        FeedbackAlerts feedback = new FeedbackAlerts(ID_FEEDBACK);
        feedback.setOutputMarkupId(true);
        add(feedback);
    }

    protected void savePerformed(AjaxRequestTarget target, AccessCertificationResponseType response, String comment) {
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
        return () -> "fa fa-edit";
    }

    private IModel<String> getItemPanelAdditionalStyle(AccessCertificationResponseType response) {
        return isSelected(response) ? Model.of("active") : Model.of("");
    }

    private boolean isSelected(AccessCertificationResponseType response) {
        return response != null && response.equals(selectedResponse);
    }

    private List<AccessCertificationResponseType> getResponses() {
        return Arrays.stream(AccessCertificationResponseType.values())
                .filter(response -> response != AccessCertificationResponseType.DELEGATE)
                .toList();
    }

    private String getComment() {
        TextArea<String> comment = (TextArea<String>) get(ID_COMMENT);
        return comment.getModelObject();
    }

}
