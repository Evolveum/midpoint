/*
 * Copyright (C) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.component.wizard;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.behavior.Behavior;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.component.wizard.WizardModelBasic;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.AjaxIconButton;
import com.evolveum.midpoint.web.component.message.FeedbackAlerts;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;

public abstract class AbstractWizardBasicInitializer extends BasePanel<String> {

    public static final String ID_FEEDBACK_CONTAINER = "feedbackContainer";

    private static final String ID_TITLE_ICON = "titleIcon";
    private static final String ID_TEXT = "text";
    private static final String ID_SUBTEXT = "subText";
    private static final String ID_SUBTEXT_MORE = "subTextMore";
    private static final String ID_FEEDBACK = "feedback";
    private static final String ID_BUTTONS = "buttons";
    private static final String ID_BUTTONS_CONTAINER = "buttonsContainer";

    private static final String ID_BACK_CONTAINER = "backContainer";
    private static final String ID_BACK = "back";
    private static final String ID_EXIT_CONTAINER = "exitContainer";
    private static final String ID_EXIT = "exit";

    private static final String ID_CUSTOM_BUTTONS_CONTAINER = "customButtonsContainer";

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

        IModel<String> subTextMoreModel = getSubTextMoreModel();

        AjaxButton subTextMore = new AjaxButton(
                ID_SUBTEXT_MORE, createStringResource("AbstractWizardBasicInitializer.subTextMore")) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                getPageBase().showRightSidebarHelp(target, subTextMoreModel);
            }
        };
        subTextMore.add(new VisibleBehaviour(() ->
                subTextMoreModel != null && StringUtils.isNotEmpty(subTextMoreModel.getObject())));
        add(subTextMore);

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

        initBottomActionButtons();
    }

    private void initBottomActionButtons() {
        Fragment buttonFragment = new Fragment(ID_BUTTONS_CONTAINER, "buttonFragment", this);
        buttonFragment.setOutputMarkupId(true);
        buttonFragment.add(AttributeModifier.append("class", "wizard-actions-strip"));
        buttonFragment.add(getButtonContainerAdditionalCssClassBehavior());

        initBackSection(buttonFragment);
        initExitSection(buttonFragment);
        initCustomButton(buttonFragment);

        add(buttonFragment);
    }

    private void initCustomButton(@NotNull WebMarkupContainer fragment) {
        WebMarkupContainer customButtonsContainer = new WebMarkupContainer(ID_CUSTOM_BUTTONS_CONTAINER);
        fragment.add(customButtonsContainer);

        RepeatingView buttonsRepeater = new RepeatingView(ID_BUTTONS);
        customButtonsContainer.add(buttonsRepeater);
        addCustomButtons(buttonsRepeater);
        initSaveButton(buttonsRepeater);
        customButtonsContainer.add(new VisibleBehaviour(() -> hasVisibleButtons(buttonsRepeater)));
    }
    private boolean hasVisibleButtons(RepeatingView repeater) {
        if (!repeater.iterator().hasNext()) {
            return false; // no buttons at all
        }

        for (Component child : repeater) {
            child.configure();
            if (child.isVisibleInHierarchy() && child.isVisibilityAllowed() && child.isVisible()) {
                return true; // at least one visible
            }
        }
        return false;
    }

    private void initExitSection(@NotNull WebMarkupContainer fragment) {
        WebMarkupContainer exitContainer = new WebMarkupContainer(ID_EXIT_CONTAINER);
        exitContainer.add(new VisibleBehaviour(this::isExitButtonVisible));
        fragment.add(exitContainer);

        AjaxIconButton exit = new AjaxIconButton(
                ID_EXIT,
                Model.of("fas fa-right-from-bracket fa-rotate-180"),
                getExitLabel()) {
            @Override
            public void onClick(AjaxRequestTarget target) {
                onExitPerformed(target);
            }
        };
        exit.showTitleAsLabel(true);
        exit.add(new VisibleBehaviour(this::isExitButtonVisible));
        exit.add(AttributeAppender.append("class", getExitButtonCssClass()));
        exitContainer.add(exit);
    }

    private void initBackSection(@NotNull WebMarkupContainer fragment) {
        WebMarkupContainer backContainer = new WebMarkupContainer(ID_BACK_CONTAINER);
        backContainer.add(new VisibleBehaviour(this::isBackButtonVisible));
        fragment.add(backContainer);

        AjaxIconButton back = new AjaxIconButton(
                ID_BACK,
                Model.of("fas fa-arrow-left"),
                getBackLabel()) {
            @Override
            public void onClick(AjaxRequestTarget target) {
                onBackPerformed(target);
            }
        };
        back.showTitleAsLabel(true);
        back.add(new VisibleBehaviour(this::isBackButtonVisible));
        back.add(AttributeAppender.append("class", getBackButtonCssClass()));
        backContainer.add(back);
    }

    private void initSaveButton(@NotNull RepeatingView buttonsRepeater) {
        AjaxIconButton saveButton = new AjaxIconButton(
                buttonsRepeater.newChildId(),
                Model.of(getSubmitIcon()),
                getSubmitLabelModel()) {
            @Override
            public void onClick(AjaxRequestTarget target) {
                onSubmitPerformed(target);
            }
        };
        saveButton.showTitleAsLabel(true);
        saveButton.add(new VisibleBehaviour(this::isSubmitButtonVisible));
        saveButton.add(AttributeAppender.append("class", getSubmitButtonCssClass()));
        buttonsRepeater.add(saveButton);
    }

    /**
     * This is the same on two places, not sure why, see BasicWizardStepPanel.
     */
    protected IModel<String> getSubTextMoreModel() {
        Class<?> clazz = getClass();
        if (clazz.isAnonymousClass()) {
            clazz = clazz.getSuperclass();
        }

        String key = clazz.getSimpleName() + ".subText.moreContent";

        return new StringResourceModel(key)
                .setDefaultValue("");
    }

    private Behavior getButtonContainerAdditionalCssClassBehavior() {
        return AttributeAppender.append("class", () ->
                getButtonContainerAdditionalCssClass() + (isOnlyChildCentered() ? " only-child-centered" : ""));
    }

    protected String getButtonContainerAdditionalCssClass() {
        return "col-12 col-sm-8";
    }

    protected boolean isOnlyChildCentered() {
        return false;
    }

    protected @NotNull String getBackButtonCssClass() {
        return "";
    }

    protected String getSubmitButtonCssClass() {
        return "btn-success";
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
        getPageBase().getPageParameters().remove(WizardModelBasic.PARAM_STEP);
    }

    protected void onBackPerformed(AjaxRequestTarget target) {
        onExitPerformed(target);
    }

    protected void addCustomButtons(RepeatingView buttons) {
    }

    protected IModel<String> getSubTextModel() {
        return getPageBase().createStringResource(getClass().getSimpleName() + ".text");
    }

    protected IModel<String> getTextModel() {
        return getPageBase().createStringResource(getClass().getSimpleName() + ".subText");
    }

    protected IModel<String> getTitleIconModel() {
        return null;
    }

    protected boolean isFeedbackContainerVisible() {
        return true;
    }

    public WebMarkupContainer getFeedback() {
        return (WebMarkupContainer) get(ID_FEEDBACK_CONTAINER);
    }
}
