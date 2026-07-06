/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.web.component.dialog.steper;

import com.evolveum.midpoint.gui.api.component.BasePanel;

import com.evolveum.midpoint.web.component.util.VisibleBehaviour;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.jetbrains.annotations.NotNull;

import java.io.Serial;

/**
 * Base implementation of {@link PopupStep} backed by a Wicket panel.
 *
 * <p>Provides a common layout consisting of an optional title, subtitle,
 * and content area. Subclasses typically override
 * {@link #createContentPanel(String)} to provide the step-specific content
 * and optionally customize navigation callbacks.</p>
 *
 * <p>The step participates in the {@link PopupStepperPanel} lifecycle and can
 * intercept navigation actions using {@link #onNextPerformed(AjaxRequestTarget)},
 * {@link #onBackPerformed(AjaxRequestTarget)}, and
 * {@link #onSubmitPerformed(AjaxRequestTarget)}.</p>
 *
 * @param <T> model object type associated with the step
 */
public abstract class BasicPopupStepPanel<T> extends BasePanel<T> implements PopupStep {
    @Serial private static final long serialVersionUID = 1L;

    private static final String ID_TITLE = "title";
    private static final String ID_SUBTITLE = "subTitle";
    private static final String ID_CONTENT = "content";
    public static final String ID_STEP = "step";

    private PopupStepperModel stepperModel;

    public BasicPopupStepPanel() {
        super(ID_STEP);
    }

    public BasicPopupStepPanel(IModel<T> model) {
        super(ID_STEP, model);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();

        add(createLabel(ID_TITLE, getTitle()));
        add(createLabel(ID_SUBTITLE, getSubTitle()));

        WebMarkupContainer content = createContentPanel(ID_CONTENT);
        content.setOutputMarkupId(true);
        add(content);

        initLayout();
    }

    private @NotNull Label createLabel(String id, IModel<String> model) {
        Label label = new Label(id, model);
        label.add(new VisibleBehaviour(() ->
                model != null && model.getObject() != null));
        return label;
    }

    protected WebMarkupContainer createContentPanel(String id) {
        return new WebMarkupContainer(id);
    }

    protected void initLayout() {
    }

    @Override
    public void init(PopupStepperModel model) {
        this.stepperModel = model;
    }

    protected PopupStepperModel getStepperModel() {
        return stepperModel;
    }

    @Override
    public Component getPanel() {
        return this;
    }

    /**
     * Called before the stepper navigates to the next visible step.
     *
     * @return {@code true} to continue navigation, {@code false} to stay
     * on the current step
     */
    public boolean onNextPerformed(AjaxRequestTarget target) {
        return true;
    }

    /**
     * Called before the stepper navigates to the previous visible step.
     *
     * @return {@code true} to continue navigation, {@code false} to stay
     * on the current step
     */
    public boolean onBackPerformed(AjaxRequestTarget target) {        return true;
    }

    /**
     * Called when the current step is the last visible step and the user
     * presses the finish button.
     *
     * @return {@code true} to complete the workflow, {@code false} to stay
     * on the current step
     */
    public boolean onSubmitPerformed(AjaxRequestTarget target) {        return true;
    }

    protected void refreshStepper(AjaxRequestTarget target) {
        getStepperModel().refresh(target);
    }

    public Component getContentPanel() {
        return get(ID_CONTENT);
    }

}
