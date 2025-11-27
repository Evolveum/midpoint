/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.web.component.dialog.synchronization;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.smart.api.synchronization.DeletedSourceChoice;
import com.evolveum.midpoint.smart.api.synchronization.SourceSynchronizationAnswers;
import com.evolveum.midpoint.smart.api.synchronization.UnmatchedSourceChoice;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormChoiceComponentUpdatingBehavior;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Radio;
import org.apache.wicket.markup.html.form.RadioGroup;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

public class SourceDirectionSpecificationPanel extends BasePanel<SourceSynchronizationAnswers> {

    private static final String ID_TITLE = "sectionTitle";

    private static final String ID_UNMATCHED_GROUP = "unmatchedGroup";
    private static final String ID_UNMATCHED_ADD_FOCUS = "unmatchedAddFocus";
    private static final String ID_UNMATCHED_NOTHING = "unmatchedNothing";

    private static final String ID_DELETED_GROUP = "deletedGroup";
    private static final String ID_DELETED_DELETE = "deletedDelete";
    private static final String ID_DELETED_DISABLE = "deletedDisable";
    private static final String ID_DELETED_REMOVE_LINK = "deletedRemoveLink";
    private static final String ID_DELETED_NOTHING = "deletedNothing";

    public SourceDirectionSpecificationPanel(String id, IModel<SourceSynchronizationAnswers> model) {
        super(id, model);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();

        addTitle();
        addUnmatchedSection();
        addDeletedSection();
    }

    private void addTitle() {
        add(new Label(ID_TITLE,
                createStringResource("SourceDirectionSpecificationPanel.title"))
                .setOutputMarkupId(true));
    }

    private void addUnmatchedSection() {
        IModel<UnmatchedSourceChoice> unmatchedModel = Model.of(getModelObject().getUnmatched());

        RadioGroup<UnmatchedSourceChoice> group =
                new RadioGroup<>(ID_UNMATCHED_GROUP, unmatchedModel);
        group.setOutputMarkupId(true);

        group.add(new AjaxFormChoiceComponentUpdatingBehavior() {
            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                getModelObject().setUnmatched(unmatchedModel.getObject());
                target.add(group);
            }
        });

        group.add(new Radio<>(ID_UNMATCHED_ADD_FOCUS, Model.of(UnmatchedSourceChoice.ADD_FOCUS)));
        group.add(new Label(ID_UNMATCHED_ADD_FOCUS + "Label",
                getString("SourceDirectionSpecificationPanel.unmatched.addFocus")));

        group.add(new Radio<>(ID_UNMATCHED_NOTHING, Model.of(UnmatchedSourceChoice.DO_NOTHING)));
        group.add(new Label(ID_UNMATCHED_NOTHING + "Label",
                getString("SourceDirectionSpecificationPanel.unmatched.doNothing")));

        add(group);
    }

    private void addDeletedSection() {
        IModel<DeletedSourceChoice> deletedModel = Model.of(getModelObject().getDeleted());

        RadioGroup<DeletedSourceChoice> group =
                new RadioGroup<>(ID_DELETED_GROUP, deletedModel);
        group.setOutputMarkupId(true);

        group.add(new AjaxFormChoiceComponentUpdatingBehavior() {
            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                getModelObject().setDeleted(deletedModel.getObject());
                target.add(group);
            }
        });

        group.add(new Radio<>(ID_DELETED_DELETE, Model.of(DeletedSourceChoice.DELETE_FOCUS)));
        group.add(new Label(ID_DELETED_DELETE + "Label",
                getString("SourceDirectionSpecificationPanel.deleted.delete")));

        group.add(new Radio<>(ID_DELETED_DISABLE, Model.of(DeletedSourceChoice.DISABLE_FOCUS)));
        group.add(new Label(ID_DELETED_DISABLE + "Label",
                getString("SourceDirectionSpecificationPanel.deleted.disable")));

        group.add(new Radio<>(ID_DELETED_REMOVE_LINK, Model.of(DeletedSourceChoice.REMOVE_BROKEN_LINK)));
        group.add(new Label(ID_DELETED_REMOVE_LINK + "Label",
                getString("SourceDirectionSpecificationPanel.deleted.removeLink")));

        group.add(new Radio<>(ID_DELETED_NOTHING, Model.of(DeletedSourceChoice.DO_NOTHING)));
        group.add(new Label(ID_DELETED_NOTHING + "Label",
                getString("SourceDirectionSpecificationPanel.deleted.doNothing")));

        add(group);
    }
}
