/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.web.component.dialog.steper;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.web.component.AjaxIconButton;
import com.evolveum.midpoint.web.component.dialog.Popupable;

import com.evolveum.midpoint.web.component.util.VisibleBehaviour;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serial;

/**
 * Generic multi-step popup wizard component.
 *
 * <p>Displays a sequence of {@link PopupStep steps} and provides
 * navigation controls for moving between them. Only visible steps
 * participate in navigation and step numbering.</p>
 *
 * <p>The panel supports step-specific customization of button labels,
 * icons, and styling, while providing common handling for Back, Next,
 * Finish, and Cancel actions.</p>
 *
 * <p>This component is intended as a reusable foundation for wizard-like
 * workflows, confirmation flows, configuration dialogs, and task-driven
 * user interactions.</p>
 */
public class PopupStepperPanel extends BasePanel<PopupStepperModel> implements Popupable {
    @Serial private static final long serialVersionUID = 1L;

    private static final String ID_STEP = "step";
    private static final String ID_BUTTONS = "buttons";
    private static final String ID_BACK = "back";
    private static final String ID_BACK_CONTAINER = "backContainer";
    private static final String ID_EXIT_CONTAINER = "exitContainer";
    private static final String ID_EXIT = "exit";
    private static final String ID_CUSTOM_BUTTONS_CONTAINER = "customButtonsContainer";
    private static final String ID_BUTTONS_REPEATER = "buttons";
    private static final String ID_CLOSE = "close";

    private Fragment footer;
    private Fragment title;

    public PopupStepperPanel(String id, IModel<PopupStepperModel> model) {
        super(id, model);
        this.setOutputMarkupId(true);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();

        getModelObject().init();

        WebMarkupContainer stepContainer = new WebMarkupContainer(ID_STEP);
        stepContainer.setOutputMarkupId(true);
        add(stepContainer);

        initFooter();
        replaceStep(null);
    }

    private void replaceStep(AjaxRequestTarget target) {
        PopupStep activeStep = getModelObject().getActiveStep();

        Component stepPanel = activeStep.getPanel();
        stepPanel.setOutputMarkupId(true);

        addOrReplace(stepPanel);

        if (target != null) {
            target.add(this);
            target.add(footer);
            target.add(title);
        }
    }

    private void initFooter() {
        footer = new Fragment(Popupable.ID_FOOTER, ID_BUTTONS, this);
        footer.setOutputMarkupId(true);
        footer.add(AttributeModifier.append("class", "wizard-actions-strip"));

        initBackButton(footer);
        initExitButton(footer);
        initCustomButtons(footer);

        add(footer);
    }

    private void initBackButton(WebMarkupContainer footer) {
        WebMarkupContainer backContainer = new WebMarkupContainer(ID_BACK_CONTAINER);
        backContainer.add(new VisibleBehaviour(() -> getModelObject().hasPrevious()));
        footer.add(backContainer);

        AjaxIconButton back = new AjaxIconButton(
                ID_BACK,
                getBackIcon(),
                getBackLabel()) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                PopupStep step = PopupStepperPanel.this.getModelObject().getActiveStep();

                if (step instanceof BasicPopupStepPanel<?> panel && !panel.onBackPerformed(target)) {
                    return;
                }

                PopupStepperPanel.this.getModelObject().previous();
                replaceStep(target);
            }
        };
        back.showTitleAsLabel(true);
        back.add(new VisibleBehaviour(() -> getModelObject().hasPrevious()));
        backContainer.add(back);
    }

    private void initExitButton(@NotNull WebMarkupContainer footer) {
        WebMarkupContainer exitContainer = new WebMarkupContainer(ID_EXIT_CONTAINER);
        exitContainer.add(new VisibleBehaviour(this::isExitButtonVisible));
        footer.add(exitContainer);

        AjaxIconButton exit = new AjaxIconButton(
                ID_EXIT,
                Model.of("fas fa-right-from-bracket fa-rotate-180"),
                getExitLabel()) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                getPageBase().hideMainPopup(target);
                onCancelPerformed(target);
            }
        };
        exit.showTitleAsLabel(true);
        exit.add(new VisibleBehaviour(this::isExitButtonVisible));
        exitContainer.add(exit);
    }

    private void initCustomButtons(WebMarkupContainer footer) {
        WebMarkupContainer customButtonsContainer = new WebMarkupContainer(ID_CUSTOM_BUTTONS_CONTAINER);
        footer.add(customButtonsContainer);

        RepeatingView buttons = new RepeatingView(ID_BUTTONS_REPEATER);
        customButtonsContainer.add(buttons);

        addCustomButtons(buttons);
        initNextOrFinishButton(buttons);

        customButtonsContainer.add(new VisibleBehaviour(() -> hasVisibleButtons(buttons)));
    }

    private void initNextOrFinishButton(@NotNull RepeatingView buttons) {
        AjaxIconButton nextOrFinish = new AjaxIconButton(
                buttons.newChildId(),
                () -> getModelObject().hasNext()
                        ? getNextIcon().getObject()
                        : getFinishIcon().getObject(),
                () -> getModelObject().hasNext()
                        ? getNextLabel().getObject()
                        : getFinishLabel().getObject()) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                PopupStep step = PopupStepperPanel.this.getModelObject().getActiveStep();

                if (PopupStepperPanel.this.getModelObject().hasNext()) {
                    if (step instanceof BasicPopupStepPanel<?> panel && !panel.onNextPerformed(target)) {
                        return;
                    }

                    PopupStepperPanel.this.getModelObject().next();
                    replaceStep(target);
                    return;
                }

                if (step instanceof BasicPopupStepPanel<?> panel && !panel.onSubmitPerformed(target)) {
                    return;
                }

                onSubmitPerformed(target);
                getPageBase().hideMainPopup(target);
            }
        };

        nextOrFinish.showTitleAsLabel(true);
        nextOrFinish.add(AttributeModifier.append("class", () ->
                getModelObject().hasNext() ? getNextCssClass() : getFinishCssClass()));
        buttons.add(nextOrFinish);
    }

    protected boolean isExitButtonVisible() {
        return true;
    }

    protected void addCustomButtons(RepeatingView buttons) {
    }

    private boolean hasVisibleButtons(@NotNull RepeatingView repeater) {
        for (Component child : repeater) {
            child.configure();
            if (child.isVisibleInHierarchy()
                    && child.isVisibilityAllowed()
                    && child.isVisible()) {
                return true;
            }
        }
        return false;
    }

    protected void onCancelPerformed(AjaxRequestTarget target) {
    }

    protected void onSubmitPerformed(AjaxRequestTarget target) {
    }

    @Override
    public @NotNull Component getFooter() {
        return footer;
    }

    @Override
    public IModel<String> getTitle() {
        return createStringResource("PopupStepperPanel.title");
    }

    @Override
    public IModel<String> getTitleIconClass() {
        return Model.of("fas fa-info-circle");
    }

    private static final String ID_TITLE = "title";
    private static final String ID_TITLE_TEXT = "titleText";
    private static final String ID_STEP_INDICATOR = "stepIndicator";

    @Override
    public @Nullable Component getTitleComponent() {

        title = new Fragment(Popupable.ID_TITLE, ID_TITLE, this);
        title.setOutputMarkupId(true);

        title.add(new Label(ID_TITLE_TEXT, getTitle()));
        title.add(new Label(ID_STEP_INDICATOR, () -> createStringResource("PopupStepperPanel.title.steper",
                getModelObject().getActiveStepNumber(), getModelObject().getVisibleStepCount()).getString()));

        title.add(AttributeModifier.append("class", "w-100"));

        title.add(new AjaxLink<Void>(ID_CLOSE) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                getPageBase().hideMainPopup(target);
                onCancelPerformed(target);
            }
        });

        return title;
    }

    @Override
    public Component getContent() {
        return this;
    }

    @Override
    public int getWidth() {
        return 50;
    }

    @Override
    public int getHeight() {
        return 50;
    }

    @Override
    public String getWidthUnit() {
        return "%";
    }

    @Override
    public String getHeightUnit() {
        return "%";
    }

    private PopupStep getActiveStep() {
        return getModelObject().getActiveStep();
    }

    private IModel<String> orDefault(IModel<String> value, IModel<String> defaultValue) {
        return value != null ? value : defaultValue;
    }

    private String orDefault(String value, String defaultValue) {
        return value != null ? value : defaultValue;
    }

    protected IModel<String> getExitLabel() {
        return orDefault(
                getActiveStep().getExitLabel(),
                createStringResource("PopupStepperPanel.cancel"));
    }

    protected IModel<String> getBackLabel() {
        return orDefault(
                getActiveStep().getBackLabel(),
                createStringResource("PopupStepperPanel.back"));
    }

    protected IModel<String> getNextLabel() {
        return orDefault(
                getActiveStep().getNextLabel(),
                createStringResource("PopupStepperPanel.next"));
    }

    protected IModel<String> getFinishLabel() {
        return orDefault(
                getActiveStep().getFinishLabel(),
                createStringResource("PopupStepperPanel.finish"));
    }

    protected IModel<String> getBackIcon() {
        return orDefault(
                getActiveStep().getBackIcon(),
                Model.of("fas fa-arrow-left"));
    }

    protected IModel<String> getNextIcon() {
        return orDefault(
                getActiveStep().getNextIcon(),
                Model.of("fa fa-arrow-right"));
    }

    protected IModel<String> getFinishIcon() {
        return orDefault(
                getActiveStep().getFinishIcon(),
                Model.of("fa fa-check"));
    }

    protected String getNextCssClass() {
        return orDefault(
                getActiveStep().getNextCssClass(),
                "btn-primary");
    }

    protected String getFinishCssClass() {
        return orDefault(
                getActiveStep().getFinishCssClass(),
                "btn-success");
    }

}
