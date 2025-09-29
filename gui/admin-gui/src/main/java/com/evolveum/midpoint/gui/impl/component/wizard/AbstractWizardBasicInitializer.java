/*
 * Copyright (C) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.component.wizard;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.component.wizard.WizardModel;
import com.evolveum.midpoint.web.component.AjaxIconButton;
import com.evolveum.midpoint.web.component.message.FeedbackAlerts;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;

public abstract class AbstractWizardBasicInitializer extends BasePanel {

    private static final String ID_TITLE_ICON = "titleIcon";
    private static final String ID_TEXT = "text";
    private static final String ID_SUBTEXT = "subText";
    private static final String ID_FEEDBACK_CONTAINER = "feedbackContainer";
    private static final String ID_FEEDBACK = "feedback";
    private static final String ID_BUTTONS = "buttons";
    private static final String ID_BUTTONS_CONTAINER = "buttonsContainer";

    public AbstractWizardBasicInitializer(String id) {
        super(id);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        onAfterSuperInitialize();
        initLayout();
    }

    protected void onAfterSuperInitialize() {
    }

    private void initLayout() {

        WebMarkupContainer titleIcon = new WebMarkupContainer(ID_TITLE_ICON);
        titleIcon.add(new VisibleBehaviour(() -> getTitleIconModel() != null && getTitleIconModel().getObject() != null));
        titleIcon.add(AttributeModifier.replace("class", getTitleIconModel()));
        add(titleIcon);

        Label mainText = new Label(ID_TEXT, getTextModel());
        mainText.add(new VisibleBehaviour(() -> getTextModel() != null && getTextModel().getObject() != null));
        add(mainText);

        Label secondaryText = new Label(ID_SUBTEXT, getSubTextModel());
        secondaryText.add(new VisibleBehaviour(() -> getSubTextModel() != null && getSubTextModel().getObject() != null));
        add(secondaryText);

        WebMarkupContainer feedbackContainer = new WebMarkupContainer(ID_FEEDBACK_CONTAINER);
        feedbackContainer.setOutputMarkupId(true);
        feedbackContainer.setOutputMarkupPlaceholderTag(true);
        add(feedbackContainer);
        feedbackContainer.add(new VisibleBehaviour(this::isFeedbackContainerVisible));
        feedbackContainer.add(AttributeAppender.append("class", getCssForWidthOfFeedbackPanel()));

        FeedbackAlerts feedbackList = new FeedbackAlerts(ID_FEEDBACK);
        feedbackList.setOutputMarkupId(true);
        feedbackList.setOutputMarkupPlaceholderTag(true);
        feedbackContainer.add(feedbackList);

        WebMarkupContainer buttonsContainer = new WebMarkupContainer(ID_BUTTONS_CONTAINER);
        buttonsContainer.setOutputMarkupId(true);
        add(buttonsContainer);

        RepeatingView buttons = new RepeatingView(ID_BUTTONS);

        AjaxIconButton back = new AjaxIconButton(
                buttons.newChildId(),
                Model.of("fas fa-arrow-left"),
                getBackLabel()) {
            @Override
            public void onClick(AjaxRequestTarget target) {
                onBackPerformed(target);
            }
        };
        back.showTitleAsLabel(true);
        back.add(new VisibleBehaviour(() -> isBackButtonVisible()));
        back.add(AttributeAppender.append("class", "text-primary"));
        buttons.add(back);

        AjaxIconButton exit = new AjaxIconButton(
                buttons.newChildId(),
                Model.of("fas fa-right-from-bracket fa-rotate-180"),
                getExitLabel()) {
            @Override
            public void onClick(AjaxRequestTarget target) {
                onExitPerformed(target);
            }
        };
        exit.showTitleAsLabel(true);
        exit.add(new VisibleBehaviour(() -> isExitButtonVisible()));
        exit.add(AttributeAppender.append("class", getExitButtonCssClass()));
        buttons.add(exit);

        addCustomButtons(buttons);
        buttonsContainer.add(buttons);

        AjaxIconButton saveButton = new AjaxIconButton(
                buttons.newChildId(),
                Model.of(getSubmitIcon()),
                getSubmitLabelModel()) {
            @Override
            public void onClick(AjaxRequestTarget target) {
                onSubmitPerformed(target);
            }
        };
        saveButton.showTitleAsLabel(true);
        saveButton.add(new VisibleBehaviour(() -> isSubmitButtonVisible()));
        saveButton.add(AttributeAppender.append("class", "btn-success"));
        buttons.add(saveButton);
    }

    protected boolean isBackButtonVisible() {
        return false;
    }

    protected String getCssForWidthOfFeedbackPanel() {
        return "col-12";
    }

    protected boolean isSubmitButtonVisible() {
        return false;
    }

    protected void onSubmitPerformed(AjaxRequestTarget target) {
    }

    protected String getExitButtonCssClass() {
        return "btn-default";
    }

    protected String getSubmitIcon() {
        return "fa fa-check";
    }

    protected IModel<String> getSubmitLabelModel() {
        return getPageBase().createStringResource("WizardPanel.submit");
    }

    protected boolean isExitButtonVisible() {
        return true;
    }

    protected WebMarkupContainer getButtonsContainer() {
        return (WebMarkupContainer) get(ID_BUTTONS_CONTAINER);
    }

    protected IModel<String> getExitLabel() {
        return getPageBase().createStringResource("WizardPanel.exit");
    }

    protected IModel<String> getBackLabel() {
        return getExitLabel();
    }

    protected void onExitPerformed(AjaxRequestTarget target) {
        getPageBase().getPageParameters().remove(WizardModel.PARAM_STEP);
    }

    protected void onBackPerformed(AjaxRequestTarget target) {
        onExitPerformed(target);
    }

    protected void addCustomButtons(RepeatingView buttons) {
    }

    protected IModel<String> getSubTextModel(){
        return getPageBase().createStringResource(getClass().getSimpleName() + ".text");
    };

    protected IModel<String> getTextModel(){
        return getPageBase().createStringResource(getClass().getSimpleName() + ".subText");
    }

    protected IModel<String> getTitleIconModel() {
        return null;
    }

    protected boolean isFeedbackContainerVisible() {
        return true;
    }

    protected WebMarkupContainer getFeedback() {
        return (WebMarkupContainer) get(ID_FEEDBACK_CONTAINER);
    }
}
