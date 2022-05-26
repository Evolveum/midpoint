/*
 * Copyright (c) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.api.component.wizard;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.border.Border;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;

/**
 * Created by Viliam Repan (lazyman).
 */
public class WizardBorder extends Border {

    private static final long serialVersionUID = 1L;

    private static final String ID_HEADER = "header";
    private static final String ID_STEPS = "steps";
    private static final String ID_STEP = "step";
    private static final String ID_LINE = "line";

    private static final String ID_CONTENT_HEADER = "contentHeader";

    private List<WizardPanel> steps;

    private IModel<Wizard> model;

    public WizardBorder(String id) {
        super(id);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();

        initLayout();
    }

    @Override
    protected void onComponentTag(ComponentTag tag) {
        checkComponentTag(tag, "div");

        super.onComponentTag(tag);
    }

    protected List<WizardPanel> createSteps() {
        return new ArrayList<>();
    }

    private void initLayout() {
        steps = createSteps();
        for (int i = 0; i < steps.size(); i++) {
            WizardPanel step = steps.get(i);
            if (!(step instanceof Component)) {
                continue;
            }

            Component comp = (Component) step;
            comp.add(createWizardStepVisibleBehaviour(i));
            add(comp);
        }

        model = Model.of(new Wizard(steps.size()));

        add(AttributeAppender.prepend("class", "bs-stepper"));

        WebMarkupContainer header = new WebMarkupContainer(ID_HEADER);
        header.setOutputMarkupId(true);
        addToBorder(header);

        ListView<IModel<String>> steps = new ListView<>(ID_STEPS, () -> this.steps.stream().map(s -> s.getTitle()).collect(Collectors.toList())) {

            @Override
            protected void populateItem(ListItem<IModel<String>> listItem) {
                WizardStepPanel step = new WizardStepPanel(ID_STEP, listItem.getIndex(), listItem.getModelObject());
                step.add(AttributeAppender.append("class", () -> model.getObject().getActiveStepIndex() == listItem.getIndex() ? "active" : null));
                listItem.add(step);

                WebMarkupContainer line = new WebMarkupContainer(ID_LINE);
                // hide last "line"
                line.add(new VisibleBehaviour(() -> listItem.getIndex() < WizardBorder.this.steps.size() - 1));
                listItem.add(line);
            }
        };
        header.add(steps);

        IModel<String> currentPanelTitle = () -> getCurrentPanel().getTitle().getObject();
        IModel<String> nextPanelTitle = () -> {
            WizardPanel next = getNextPanel();
            return next != null ? getNextPanel().getTitle().getObject() : null;
        };
        WizardHeader contentHeader = new WizardHeader(ID_CONTENT_HEADER, currentPanelTitle, nextPanelTitle) {

            @Override
            protected void onBackPerformed(AjaxRequestTarget target) {
                previousStep();
            }

            @Override
            protected void onNextPerformed(AjaxRequestTarget target) {
                nextStep();
            }
        };
        contentHeader.add(new VisibleEnableBehaviour() {

            @Override
            public boolean isVisible() {
                VisibleEnableBehaviour b = getCurrentPanel().getHeaderBehaviour();
                return b != null ? b.isVisible() : true;
            }

            @Override
            public boolean isEnabled() {
                VisibleEnableBehaviour b = getCurrentPanel().getHeaderBehaviour();
                return b != null ? b.isEnabled() : true;
            }
        });
        contentHeader.setOutputMarkupId(true);
        addToBorder(contentHeader);
    }

    public VisibleBehaviour createWizardStepVisibleBehaviour(int index) {
        return new VisibleBehaviour(() -> model.getObject().getActiveStepIndex() == index);
    }

    public WizardPanel getCurrentPanel() {
        return steps.get(model.getObject().getActiveStepIndex());
    }

    public WizardPanel getNextPanel() {
        int nextIndex = model.getObject().getActiveStepIndex() + 1;
        if (steps.size() <= nextIndex) {
            return null;
        }

        return steps.get(nextIndex);
    }

    public void previousStep() {
        model.getObject().previousStep();
    }

    public void nextStep() {
        model.getObject().nextStep();
    }
}
