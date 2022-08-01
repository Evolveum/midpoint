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

    protected IModel<String> getTextModel() {
        return Model.of();
    }

    protected IModel<String> getSubTextModel() {
        return Model.of();
    }

    public boolean onNextPerformed(AjaxRequestTarget target) {
        getWizard().next();
        target.add(getWizard().getPanel());

        return false;
    }

    public boolean onBackPerformed(AjaxRequestTarget target) {
        int index = getWizard().getActiveStepIndex();
        if (index > 0) {
            getWizard().previous();
            target.add(getWizard().getPanel());
        }

        return false;
    }

    @Override
    public VisibleEnableBehaviour getHeaderBehaviour() {
        return VisibleEnableBehaviour.ALWAYS_INVISIBLE;
    }
}
