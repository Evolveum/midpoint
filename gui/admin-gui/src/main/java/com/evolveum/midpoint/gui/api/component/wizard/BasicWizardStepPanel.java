/*
 * Copyright (c) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.api.component.wizard;

import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.web.component.AjaxSubmitButton;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

/**
 * @author lskublik
 */
public class BasicWizardStepPanel<T> extends WizardStepPanel<T> {

    private static final long serialVersionUID = 1L;

    private static final String ID_TEXT = "text";
    private static final String ID_SUBTEXT = "subText";
    private static final String ID_BACK = "back";
    private static final String ID_EXIT = "exit";

    private static final String ID_CUSTOM_BUTTONS = "customButtons";

    private static final String ID_SUBMIT = "submit";
    private static final String ID_SUBMIT_LABEL = "submitLabel";
    private static final String ID_NEXT = "next";
    private static final String ID_NEXT_LABEL = "nextLabel";

    public BasicWizardStepPanel() {
    }

    public BasicWizardStepPanel(IModel<T> model) {
        super(model);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();

        initLayout();
    }

    private void initLayout() {
        Label mainText = new Label(ID_TEXT, getTextModel());
        mainText.add(new VisibleBehaviour(() -> getTextModel().getObject() != null));
        add(mainText);

        Label secondaryText = new Label(ID_SUBTEXT, getSubTextModel());
        secondaryText.add(new VisibleBehaviour(() -> getSubTextModel().getObject() != null));
        add(secondaryText);

        AjaxLink back = new AjaxLink<>(ID_BACK) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                onBackPerformed(target);
            }
        };
        back.add(getBackBehaviour());
        back.setOutputMarkupId(true);
        back.setOutputMarkupPlaceholderTag(true);
        WebComponentUtil.addDisabledClassBehavior(back);
        add(back);

        AjaxLink exit = new AjaxLink<>(ID_EXIT) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                onExitPerformed(target);
            }
        };
        exit.add(getExitVisibility());
        exit.setOutputMarkupId(true);
        exit.setOutputMarkupPlaceholderTag(true);
        WebComponentUtil.addDisabledClassBehavior(exit);
        add(exit);

        RepeatingView customButtons = new RepeatingView(ID_CUSTOM_BUTTONS);
        add(customButtons);
        initCustomButtons(customButtons);

        AjaxSubmitButton submit = new AjaxSubmitButton(ID_SUBMIT) {

            @Override
            public void onSubmit(AjaxRequestTarget target) {
                onSubmitPerformed(target);
            }

            @Override
            protected void onError(AjaxRequestTarget target) {
                updateFeedbackPanels(target);
            }
        };
        submit.add(new VisibleEnableBehaviour(
                () -> isSubmitVisible(),
                () -> isSubmitEnable()));
        submit.setOutputMarkupId(true);
        submit.setOutputMarkupPlaceholderTag(true);
        WebComponentUtil.addDisabledClassBehavior(submit);
        add(submit);

        Label submitLabel = new Label(ID_SUBMIT_LABEL, getSubmitLabelModel());
        submit.add(submitLabel);

        AjaxSubmitButton next = new AjaxSubmitButton(ID_NEXT) {

            @Override
            public void onSubmit(AjaxRequestTarget target) {
                onNextPerformed(target);
            }

            @Override
            protected void onError(AjaxRequestTarget target) {
                updateFeedbackPanels(target);
            }
        };
        next.add(getNextBehaviour());
        next.setOutputMarkupId(true);
        next.setOutputMarkupPlaceholderTag(true);
        WebComponentUtil.addDisabledClassBehavior(next);
        add(next);

        Label nextLabel = new Label(ID_NEXT_LABEL, getNextLabelModel());
        next.add(nextLabel);
    }

    protected void initCustomButtons(RepeatingView customButtons) {
    }

    protected boolean isSubmitEnable() {
        return true;
    }

    protected IModel<String> getSubmitLabelModel() {
        return getPageBase().createStringResource("WizardPanel.submit");
    }

    @Override
    public VisibleEnableBehaviour getNextBehaviour() {
        return new VisibleEnableBehaviour(() -> !isSubmitVisible());
    }

    protected boolean isSubmitVisible() {
      return getWizard().getNextPanel() == null;
    }

    private VisibleBehaviour getExitVisibility() {
        return new VisibleBehaviour(() -> isExitButtonVisible());
    }

    protected boolean isExitButtonVisible() {
        return false;
    }

    protected void onExitPerformed(AjaxRequestTarget target) {
    }

    protected IModel<String> getNextLabelModel() {
        return () -> {
            WizardStep step = getWizard().getNextPanel();
            return step != null ? step.getTitle().getObject() : null;
        };
    }

    protected void updateFeedbackPanels(AjaxRequestTarget target) {
    }

    protected AjaxSubmitButton getNext() {
        return (AjaxSubmitButton) get(ID_NEXT);
    }

    protected AjaxLink getBack() {
        return (AjaxLink) get(ID_BACK);
    }

    protected AjaxSubmitButton getSubmit() {
        return (AjaxSubmitButton) get(ID_SUBMIT);
    }

    protected IModel<String> getTextModel() {
        return Model.of();
    }

    protected IModel<String> getSubTextModel() {
        return Model.of();
    }

    public boolean onNextPerformed(AjaxRequestTarget target) {
        WizardModel model = getWizard();
        if (model.hasNext()) {
            model.next();
            target.add(model.getPanel());
        }

        return false;
    }

    protected void onSubmitPerformed(AjaxRequestTarget target) {
        onExitPerformed(target);
    }

    public boolean onBackPerformed(AjaxRequestTarget target) {
        WizardModel model = getWizard();
        if (model.hasPrevious()) {
            model.previous();
            target.add(model.getPanel());
        }

        return false;
    }

    @Override
    public VisibleEnableBehaviour getHeaderBehaviour() {
        return VisibleEnableBehaviour.ALWAYS_INVISIBLE;
    }
}
