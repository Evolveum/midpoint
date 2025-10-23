/*
 * Copyright (c) 2022 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.api.component.wizard;

import java.io.Serial;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;

/**
 * Created by Viliam Repan (lazyman).
 */
public class WizardPanel extends BasePanel implements WizardListener {

    @Serial private static final long serialVersionUID = 1L;

    private static final String ID_HEADER = "header";
    private static final String ID_STEPS = "steps";
    private static final String ID_STEP = "step";
    private static final String ID_LINE = "line";
    private static final String ID_CONTENT_HEADER = "contentHeader";
    public static final String ID_CONTENT_BODY = "contentBody";
    private static final String ID_STEP_STATUS = "stepStatus";

    private final WizardModel wizardModel;
    private boolean enableBackTitleModel = false;

    public WizardPanel(String id, WizardModel wizardModel, boolean enableBackTitleModel) {
        super(id);

        this.wizardModel = wizardModel;
        this.wizardModel.setPanel(this);
        this.enableBackTitleModel = enableBackTitleModel;

        wizardModel.addWizardListener(this);

        initLayout();
    }

    public WizardPanel(String id, WizardModel wizardModel) {
        this(id, wizardModel, false);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();

        this.wizardModel.init(getPage());
    }

    @Override
    protected void onComponentTag(ComponentTag tag) {
        checkComponentTag(tag, "div");

        super.onComponentTag(tag);
    }

    @Override
    protected void onBeforeRender() {
        super.onBeforeRender();

        String stepId = wizardModel.getActiveStep().getStepId();
        if (StringUtils.isNotEmpty(stepId)) {
            PageParameters params = getPage().getPageParameters();
            params.set(WizardModel.PARAM_STEP, stepId);
        }
    }

    @Override
    public void renderHead(IHeaderResponse response) {
        super.renderHead(response);

        response.render(OnDomReadyHeaderItem.forScript(
                "MidPointTheme.updatePageUrlParameter('" + WizardModel.PARAM_STEP + "', '" + wizardModel.getActiveStep().getStepId() + "');"));

        String stepStatusId = get(ID_HEADER + ":" + ID_STEP_STATUS).getMarkupId();
        String stepPage = getString("WizardPanel.stepStatus", wizardModel.getActiveStep().getTitle().getObject());
        response.render(OnDomReadyHeaderItem.forScript(
                String.format("MidPointTheme.updateStatusMessage('%s', '%s', %d)", stepStatusId, stepPage, 400)));
    }

    @Override
    public void onStepChanged(WizardStep newStep) {
        WizardStep step = getActiveStep();
        ((Component)step).add(AttributeAppender.append("class", () -> getActiveStep().appendCssToWizard()));

        addOrReplace((Component) step);
    }

    private IModel<List<IModel<String>>> createStepsModel() {
        return () -> wizardModel.getSteps().stream().filter(
                s -> BooleanUtils.isTrue(
                        s.isStepVisible().getObject())).map(WizardStep::getTitle).collect(Collectors.toList());
    }

    private void initLayout() {
        add(AttributeAppender.prepend("class", "bs-stepper"));
        add(AttributeAppender.append("class", () -> "w-100"));

        WebMarkupContainer header = new WebMarkupContainer(ID_HEADER);
        header.add(new BehaviourDelegator(() -> getActiveStep().getStepsBehaviour()));
        header.add(AttributeAppender.append("class", getCssForStepsHeader()));
        header.setOutputMarkupId(true);
        add(header);

        WebMarkupContainer stepStatus = new WebMarkupContainer(ID_STEP_STATUS, Model.of(""));
        stepStatus.setOutputMarkupId(true);
        header.add(stepStatus);

        IModel<List<IModel<String>>> stepsModel = createStepsModel();
        WizardHeaderStepHelper wizardHeaderStepHelper = new WizardHeaderStepHelper(wizardModel.getActiveStepIndex(),
                stepsModel.getObject().size(), WizardPanel.this);
        ListView<IModel<String>> steps = new ListView<>(ID_STEPS, stepsModel) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            protected void populateItem(ListItem<IModel<String>> listItem) {
                WizardHeaderStepPanel step = new WizardHeaderStepPanel(
                        ID_STEP, listItem.getIndex(), listItem.getModelObject(), wizardHeaderStepHelper);
                // todo fix, if steps are invisible index might shift?
                listItem.add(step);

                WebMarkupContainer line = new WebMarkupContainer(ID_LINE);
                // hide last "line"
                line.add(new VisibleBehaviour(() -> listItem.getIndex() < getModelObject().size() - 1));
                listItem.add(line);
            }
        };
        header.add(steps);

        WizardHeader contentHeader = new WizardHeader(ID_CONTENT_HEADER, wizardModel) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            protected Component createHeaderContent(String id) {
                return getActiveStep().createHeaderContent(id);
            }

            @Override
            protected void onBackPerformed(AjaxRequestTarget target) {
                boolean executeDefaultBehaviour = wizardModel.getActiveStep().onBackPerformed(target);
                if (!executeDefaultBehaviour) {
                    return;
                }

                if (!wizardModel.hasPrevious()) {
                    getPageBase().redirectBack();
                } else {
                    wizardModel.previous();
                }

                target.add(WizardPanel.this);
            }

            @Override
            protected void onNextPerformed(AjaxRequestTarget target) {
                boolean executeDefaultBehaviour = wizardModel.getActiveStep().onNextPerformed(target);
                if (!executeDefaultBehaviour) {
                    return;
                }

                wizardModel.next();

                target.add(WizardPanel.this);
            }

            @Override
            protected @NotNull VisibleEnableBehaviour getNextVisibilityBehaviour() {
                return wizardModel.getActiveStep().getNextBehaviour();
            }

            @Override
            protected IModel<String> createBackTitleModel() {
                if (enableBackTitleModel) {
                    return () -> {
                        WizardStep step = wizardModel.findPreviousStep();
                        return step != null ? getString("WizardHeader.backTo", step.getTitle().getObject()) : getString("WizardHeader.back");
                    };
                }
                return createStringResource("WizardHeader.back");
            }
        };
        contentHeader.add(new BehaviourDelegator(() -> getActiveStep().getHeaderBehaviour()));
        contentHeader.setOutputMarkupId(true);
        add(contentHeader);

        add(new WebMarkupContainer(ID_CONTENT_BODY));
    }

    private String getCssForStepsHeader() {
        int steps = wizardModel.getSteps().size();
        if (steps == 2) {
            return "col-xxl-5 col-xl-7 col-lg-9 col-md-9 col-sm-12 m-auto";
        }

        if (steps == 3) {
            return "col-xxl-7 col-xl-10 col-12 m-auto";
        }

        if (steps == 4) {
            return "col-xxl-10 col-12 m-auto";
        }

        if (steps >= 5) {
            return "col-12 m-auto";
        }

        return "";
    }

    public WizardStep getActiveStep() {
        return wizardModel.getActiveStep();
    }

    public Component getHeader() {
        return get(ID_CONTENT_HEADER);
    }
}
