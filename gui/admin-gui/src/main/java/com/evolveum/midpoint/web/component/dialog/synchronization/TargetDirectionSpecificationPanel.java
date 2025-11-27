/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */
package com.evolveum.midpoint.web.component.dialog.synchronization;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.smart.api.synchronization.*;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormChoiceComponentUpdatingBehavior;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Radio;
import org.apache.wicket.markup.html.form.RadioGroup;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

public class TargetDirectionSpecificationPanel extends BasePanel<TargetSynchronizationAnswers> {

    private static final String ID_TITLE = "sectionTitle";

    private static final String ID_UNMATCHED_GROUP = "unmatchedGroup";
    private static final String ID_UNMATCHED_DISABLE = "unmatchedDisable";
    private static final String ID_UNMATCHED_DELETE = "unmatchedDelete";
    private static final String ID_UNMATCHED_NOTHING = "unmatchedNothing";

    private static final String ID_DELETED_GROUP = "deletedGroup";
    private static final String ID_DELETED_REMOVE = "deletedRemove";
    private static final String ID_DELETED_NOTHING = "deletedNothing";

    private static final String ID_DISPUTED_GROUP = "disputedGroup";
    private static final String ID_DISPUTED_CASE = "disputedCase";
    private static final String ID_DISPUTED_NOTHING = "disputedNothing";

    public TargetDirectionSpecificationPanel(String id, IModel<TargetSynchronizationAnswers> model) {
        super(id, model);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();

        addTitle();
        addUnmatchedBlock();
        addDeletedBlock();
        addDisputedBlock();
    }

    private void addTitle() {
        add(new Label(ID_TITLE,
                createStringResource("TargetDirectionSpecificationPanel.title")));
    }

    private void addUnmatchedBlock() {
        IModel<UnmatchedTargetChoice> unmatchedModel =
                Model.of(getModelObject().getUnmatched());

        RadioGroup<UnmatchedTargetChoice> group =
                new RadioGroup<>(ID_UNMATCHED_GROUP, unmatchedModel);
        group.setOutputMarkupId(true);

        group.add(new AjaxFormChoiceComponentUpdatingBehavior() {
            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                getModelObject().setUnmatched(unmatchedModel.getObject());
                target.add(group);
            }
        });

        group.add(new Radio<>(ID_UNMATCHED_DISABLE, Model.of(UnmatchedTargetChoice.DISABLE_RESOURCE_OBJECT)));
        group.add(new Label(ID_UNMATCHED_DISABLE + "Label",
                getString("TargetDirectionSpecificationPanel.unmatched.disable")));

        group.add(new Radio<>(ID_UNMATCHED_DELETE, Model.of(UnmatchedTargetChoice.DELETE_RESOURCE_OBJECT)));
        group.add(new Label(ID_UNMATCHED_DELETE + "Label",
                getString("TargetDirectionSpecificationPanel.unmatched.delete")));

        group.add(new Radio<>(ID_UNMATCHED_NOTHING, Model.of(UnmatchedTargetChoice.DO_NOTHING)));
        group.add(new Label(ID_UNMATCHED_NOTHING + "Label",
                getString("TargetDirectionSpecificationPanel.unmatched.nothing")));

        add(group);
    }

    private void addDeletedBlock() {
        IModel<DeletedTargetChoice> deletedModel =
                Model.of(getModelObject().getDeleted());

        RadioGroup<DeletedTargetChoice> group =
                new RadioGroup<>(ID_DELETED_GROUP, deletedModel);
        group.setOutputMarkupId(true);

        group.add(new AjaxFormChoiceComponentUpdatingBehavior() {
            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                getModelObject().setDeleted(deletedModel.getObject());
                target.add(group);
            }
        });

        group.add(new Radio<>(ID_DELETED_REMOVE, Model.of(DeletedTargetChoice.REMOVE_BROKEN_LINK)));
        group.add(new Label(ID_DELETED_REMOVE + "Label",
                getString("TargetDirectionSpecificationPanel.deleted.remove")));

        group.add(new Radio<>(ID_DELETED_NOTHING, Model.of(DeletedTargetChoice.DO_NOTHING)));
        group.add(new Label(ID_DELETED_NOTHING + "Label",
                getString("TargetDirectionSpecificationPanel.deleted.nothing")));

        add(group);
    }

    private void addDisputedBlock() {
        IModel<DisputedTargetChoice> disputedModel =
                Model.of(getModelObject().getDisputed());

        RadioGroup<DisputedTargetChoice> group =
                new RadioGroup<>(ID_DISPUTED_GROUP, disputedModel);
        group.setOutputMarkupId(true);

        group.add(new AjaxFormChoiceComponentUpdatingBehavior() {
            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                getModelObject().setDisputed(disputedModel.getObject());
                target.add(group);
            }
        });

        group.add(new Radio<>(ID_DISPUTED_CASE, Model.of(DisputedTargetChoice.CREATE_CORRELATION_CASE)));
        group.add(new Label(ID_DISPUTED_CASE + "Label",
                getString("TargetDirectionSpecificationPanel.disputed.case")));

        group.add(new Radio<>(ID_DISPUTED_NOTHING, Model.of(DisputedTargetChoice.DO_NOTHING)));
        group.add(new Label(ID_DISPUTED_NOTHING + "Label",
                getString("TargetDirectionSpecificationPanel.disputed.nothing")));

        add(group);
    }
}
