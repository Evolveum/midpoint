/*
 * Copyright (c) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.self.requestAccess;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormChoiceComponentUpdatingBehavior;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Radio;
import org.apache.wicket.markup.html.form.RadioGroup;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import com.evolveum.midpoint.gui.api.component.Badge;
import com.evolveum.midpoint.gui.api.component.BadgePanel;
import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.AjaxSubmitButton;
import com.evolveum.midpoint.web.component.form.MidpointForm;
import com.evolveum.midpoint.web.component.util.EnableBehaviour;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;

import java.util.Objects;

/**
 * Created by Viliam Repan (lazyman).
 */
public class ConflictItemPanel extends BasePanel<Conflict> {

    private static final long serialVersionUID = 1L;

    private static final String ID_BADGE = "badge";
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
    private static final String ID_TITLE = "title";

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

    private void initLayout() {
        add(AttributeAppender.append("class", "card conflict-item"));
        add(AttributeAppender.append("class", () -> {
            Conflict c = getModelObject();
            switch (c.getState()) {
                case SKIPPED:
                    return "conflict-item-secondary";
                case SOLVED:
                    return "conflict-item-success";
            }

            return c.isWarning() ? "conflict-item-warning" : "conflict-item-danger";
        }));

        Label title = new Label(ID_TITLE, () -> getString("ConflictItemPanel.duplicationConflict",
                WebComponentUtil.getName(getModelObject().getPersonOfInterest())));
        add(title);

        BadgePanel badge = new BadgePanel(ID_BADGE, () -> {
            Conflict c = getModelObject();
            Badge b = new Badge();
            b.setCssClass(c.isWarning() ? Badge.State.WARNING : Badge.State.DANGER);

            String key = c.isWarning() ? "ConflictItemPanel.badgeWarning" : "ConflictItemPanel.badgeFatalConflict";
            b.setText(getString(key));

            return b;
        });
        add(badge);

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

        Radio radio = new Radio(ID_RADIO, item);
        option.add(radio);

        Label label = new Label(ID_LABEL, () -> item.getObject().getDisplayName());
        option.add(label);

        Label state = new Label(ID_STATE, () -> item.getObject().isExisting() ?
                getString("ConflictItemPanel.existingAssignment") : getString("ConflictItemPanel.newAssignment"));
        option.add(state);

        return option;
    }

    private void navigateToObject(AssignmentType assignment) {
        if (assignment == null || assignment.getTargetRef() == null) {
            return;
        }

        ObjectReferenceType ref = assignment.getTargetRef();

        WebComponentUtil.dispatchToObjectDetailsPage(ref, this, true);
    }

    protected void fixConflictPerformed(AjaxRequestTarget target, IModel<ConflictItem> assignmentToKeep) {

    }
}
