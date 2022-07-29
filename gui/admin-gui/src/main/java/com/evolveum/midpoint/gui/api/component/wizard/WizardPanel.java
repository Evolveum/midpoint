/*
 * Copyright (c) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.api.component.wizard;

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
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;

/**
 * Created by Viliam Repan (lazyman).
 */
public class WizardPanel extends BasePanel implements WizardListener {

    private static final long serialVersionUID = 1L;

    private static final String ID_HEADER = "header";
    private static final String ID_STEPS = "steps";
    private static final String ID_STEP = "step";
    private static final String ID_LINE = "line";
    private static final String ID_CONTENT_HEADER = "contentHeader";
    public static final String ID_CONTENT_BODY = "contentBody";

    private WizardModel wizardModel;

    public WizardPanel(String id, WizardModel wizardModel) {
        super(id);

        this.wizardModel = wizardModel;
        this.wizardModel.setPanel(this);

        wizardModel.addWizardListener(this);

        initLayout();
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();

        this.wizardModel.init(getPage());
    }

    public WizardModel getWizardModel() {
        return wizardModel;
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
    }

    @Override
    public void onStepChanged(WizardStep newStep) {
        WizardStep step = getActiveStep();
        step.init(wizardModel);

        addOrReplace((Component) step);
    }

    private IModel<List<IModel<String>>> createStepsModel() {
        return () -> wizardModel.getSteps().stream().filter(s -> BooleanUtils.isTrue(s.isStepVisible().getObject())).map(s -> s.getTitle()).collect(Collectors.toList());
    }

    private void initLayout() {
        add(AttributeAppender.prepend("class", "bs-stepper"));
        add(AttributeAppender.append("class", () -> getActiveStep().appendCssToWizard()));

        WebMarkupContainer header = new WebMarkupContainer(ID_HEADER);
        header.add(new BehaviourDelegator(() -> getActiveStep().getStepsBehaviour()));
        header.setOutputMarkupId(true);
        add(header);

        ListView<IModel<String>> steps = new ListView<>(ID_STEPS, createStepsModel()) {

            @Override
            protected void populateItem(ListItem<IModel<String>> listItem) {
                WizardHeaderStepPanel step = new WizardHeaderStepPanel(ID_STEP, listItem.getIndex(), listItem.getModelObject());
                // todo fix, if steps are invisible index might shift?
                step.add(AttributeAppender.append("class", () -> wizardModel.getActiveStepIndex() == listItem.getIndex() ? "active" : null));
                listItem.add(step);

                WebMarkupContainer line = new WebMarkupContainer(ID_LINE);
                // hide last "line"
                line.add(new VisibleBehaviour(() -> listItem.getIndex() < getModelObject().size() - 1));
                listItem.add(line);
            }
        };
        header.add(steps);

        WizardHeader contentHeader = new WizardHeader(ID_CONTENT_HEADER, wizardModel) {

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

                if (wizardModel.getActiveStepIndex() == 0) {
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
        };
        contentHeader.add(new BehaviourDelegator(() -> getActiveStep().getHeaderBehaviour()));
        contentHeader.setOutputMarkupId(true);
        add(contentHeader);

        add(new WebMarkupContainer(ID_CONTENT_BODY));
    }

    public WizardStep getActiveStep() {
        return wizardModel.getActiveStep();
    }

    public Component getHeader() {
        return get(ID_CONTENT_HEADER);
    }
}
