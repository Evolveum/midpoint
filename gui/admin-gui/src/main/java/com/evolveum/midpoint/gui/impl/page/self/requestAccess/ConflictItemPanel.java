/*
 * Copyright (c) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.self.requestAccess;

import java.util.Objects;

import com.evolveum.midpoint.gui.impl.util.DetailsPageUtil;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormChoiceComponentUpdatingBehavior;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Radio;
import org.apache.wicket.markup.html.form.RadioGroup;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.ResourceModel;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.gui.api.component.Badge;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.page.admin.simulation.visualization.CardOutlineLeftPanel;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.AjaxSubmitButton;
import com.evolveum.midpoint.web.component.form.MidpointForm;
import com.evolveum.midpoint.web.component.util.EnableBehaviour;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;

/**
 * Created by Viliam Repan (lazyman).
 */
public class ConflictItemPanel extends CardOutlineLeftPanel<Conflict> {

    private static final long serialVersionUID = 1L;

    private static final String ID_LINK1 = "link1";
    private static final String ID_LINK2 = "link2";
    private static final String ID_MESSAGE = "message";
    private static final String ID_FIX_CONFLICT = "fixConflict";
    private static final String ID_OPTIONS = "options";
    private static final String ID_OPTION = "option";
    private static final String ID_LABEL = "label";
    private static final String ID_RADIO = "radio";
    private static final String ID_STATE = "state";
    private static final String ID_OPTION1 = "option1";
    private static final String ID_OPTION2 = "option2";
    private static final String ID_FORM = "form";
    private static final String ID_ACTION = "action";

    private IModel<ConflictItem> selectedOption;

    public ConflictItemPanel(String id, IModel<Conflict> model) {
        super(id, model);

        initModels();
        initLayout();
    }

    private void initModels() {
        selectedOption = new IModel<>() {

            @Override
            public ConflictItem getObject() {
                Conflict conflict = getModelObject();
                if (conflict.getToBeRemoved() == null) {
                    return null;
                }

                return Objects.equals(conflict.getToBeRemoved(), conflict.getAdded()) ?
                        conflict.getExclusion() :
                        conflict.getAdded();
            }

            @Override
            public void setObject(ConflictItem toKeep) {
                Conflict conflict = getModelObject();
                if (toKeep == null) {
                    conflict.setToBeRemoved(null);
                    return;
                }

                ConflictItem toRemove = Objects.equals(toKeep, conflict.getAdded()) ?
                        conflict.getExclusion() :
                        conflict.getAdded();

                conflict.setToBeRemoved(toRemove);
            }
        };
    }

    @Override
    protected IModel<String> createCardOutlineCssModel() {
        return () -> {
            Conflict c = getModelObject();
            switch (c.getState()) {
                case SKIPPED:
                    return "card-outline-left-secondary";
                case SOLVED:
                    return "card-outline-left-success";
            }

            return c.isWarning() ? "card-outline-left-warning" : "card-outline-left-danger";
        };
    }

    @Override
    protected @NotNull IModel<String> createTitleModel() {
        return () -> {
            String poiName = WebComponentUtil.getName(getModelObject().getPersonOfInterest());

            String shortMsg = getModelObject().getShortMessage();
            if (shortMsg != null) {
                return getString("ConflictItemPanel.conflictTitle", shortMsg, poiName);
            }

            return getString("ConflictItemPanel.defaultConflictTitle", poiName);
        };
    }

    @Override
    protected @NotNull IModel<Badge> createBadgeModel() {
        return () -> {
            Conflict c = getModelObject();
            Badge b = new Badge();
            b.setCssClass(c.isWarning() ? Badge.State.WARNING : Badge.State.DANGER);

            String key = c.isWarning() ? "ConflictItemPanel.badgeWarning" : "ConflictItemPanel.badgeFatalConflict";
            b.setText(getString(key));

            return b;
        };
    }

    private void initLayout() {
        AjaxButton link1 = new AjaxButton(ID_LINK1, () -> getModelObject().getAdded().getDisplayName()) {
            @Override
            public void onClick(AjaxRequestTarget target) {
                navigateToObject(ConflictItemPanel.this.getModelObject().getAdded().getAssignment());
            }
        };
        add(link1);

        AjaxButton link2 = new AjaxButton(ID_LINK2, () -> getModelObject().getExclusion().getDisplayName()) {
            @Override
            public void onClick(AjaxRequestTarget target) {
                navigateToObject(ConflictItemPanel.this.getModelObject().getExclusion().getAssignment());
            }
        };
        add(link2);

        Label message = new Label(ID_MESSAGE, () -> getModelObject().getMessage());
        add(message);

        MidpointForm form = new MidpointForm(ID_FORM);
        add(form);

        RadioGroup options = new RadioGroup(ID_OPTIONS, selectedOption);
        options.add(new EnableBehaviour(() -> getModelObject().getState() != ConflictState.SOLVED));
        options.add(new AjaxFormChoiceComponentUpdatingBehavior() {

            private static final long serialVersionUID = 1L;

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                target.add(getFixConflictButton());
            }
        });
        form.add(options);

        Fragment option1 = createOption(ID_OPTION1, () -> getModelObject().getAdded());
        options.add(option1);

        Fragment option2 = createOption(ID_OPTION2, () -> getModelObject().getExclusion());
        options.add(option2);

        AjaxSubmitButton fixConflict = new AjaxSubmitButton(ID_FIX_CONFLICT) {

            private static final long serialVersionUID = 1L;

            @Override
            protected void onSubmit(AjaxRequestTarget target) {
                fixConflictPerformed(target, selectedOption);
            }
        };
        fixConflict.add(new EnableBehaviour(() -> getModelObject().getState() != ConflictState.SOLVED && selectedOption.getObject() != null));
        fixConflict.setOutputMarkupId(true);
        WebComponentUtil.addDisabledClassBehavior(fixConflict);
        add(fixConflict);
    }

    private Component getFixConflictButton() {
        return get(ID_FIX_CONFLICT);
    }

    private Fragment createOption(String id, IModel<ConflictItem> item) {
        Fragment option = new Fragment(id, ID_OPTION, this);
        option.add(AttributeAppender.append("class", "form-check"));

        Label action = new Label(ID_ACTION, new ResourceModel("ConflictItemPanel.iWantToKeep"));
        action.setOutputMarkupId(true);
        option.add(action);

        Label label = new Label(ID_LABEL, () -> item.getObject().getDisplayName());
        label.setOutputMarkupId(true);
        option.add(label);

        Label state = new Label(ID_STATE, () -> item.getObject().isExisting() ?
                getString("ConflictItemPanel.existingAssignment") : getString("ConflictItemPanel.newAssignment"));
        state.setOutputMarkupId(true);
        option.add(state);

        Radio radio = new Radio(ID_RADIO, item);
        radio.add(AttributeAppender.append("aria-labelledby", action.getMarkupId() + " " + label.getMarkupId() + " " + state.getMarkupId()));
        option.add(radio);

        return option;
    }

    private void navigateToObject(AssignmentType assignment) {
        if (assignment == null || assignment.getTargetRef() == null) {
            return;
        }

        ObjectReferenceType ref = assignment.getTargetRef();

        DetailsPageUtil.dispatchToObjectDetailsPage(ref, this, true);
    }

    protected void fixConflictPerformed(AjaxRequestTarget target, IModel<ConflictItem> assignmentToKeep) {

    }
}
