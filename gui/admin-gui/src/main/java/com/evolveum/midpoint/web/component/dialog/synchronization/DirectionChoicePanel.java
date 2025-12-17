/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.web.component.dialog.synchronization;

import com.evolveum.midpoint.gui.api.component.BasePanel;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormChoiceComponentUpdatingBehavior;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Radio;
import org.apache.wicket.markup.html.form.RadioGroup;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.jetbrains.annotations.NotNull;

public class DirectionChoicePanel extends BasePanel<DirectionChoicePanel.DirectionSelection> {

    private static final String ID_SELECT_TITLE = "selectTitle";

    private static final String ID_RADIO_GROUP = "radioGroup";
    private static final String ID_SOURCE_CONTAINER = "sourceContainer";
    private static final String ID_TARGET_CONTAINER = "targetContainer";
    private static final String ID_SOURCE_RADIO = "sourceRadio";
    private static final String ID_TARGET_RADIO = "targetRadio";

    public enum DirectionSelection {
        SOURCE,
        TARGET,
        NONE
    }

    public DirectionChoicePanel(String id, IModel<DirectionSelection> directionSelectionIModel) {
        super(id, directionSelectionIModel);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();

        addSelectTitle();
        addDirectionSelector();
    }

    private void addSelectTitle() {
        Label selectTitle = new Label(
                ID_SELECT_TITLE,
                createStringResource("ConfigureSynchronizationConfirmationPanel.selectTitle")
        );
        selectTitle.setOutputMarkupId(true);
        add(selectTitle);
    }

    private void addDirectionSelector() {
        RadioGroup<DirectionSelection> group = createRadioGroup();
        add(group);

        group.add(createSourceOption());
        group.add(createTargetOption());
    }

    private @NotNull RadioGroup<DirectionSelection> createRadioGroup() {
        RadioGroup<DirectionSelection> group = new RadioGroup<>(ID_RADIO_GROUP, getModel());
        group.setOutputMarkupId(true);

        group.add(new AjaxFormChoiceComponentUpdatingBehavior() {
            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                onDirectionChange(target);
                target.add(group);
            }
        });

        return group;
    }

    private @NotNull WebMarkupContainer createSourceOption() {
        WebMarkupContainer source = new WebMarkupContainer(ID_SOURCE_CONTAINER);

        source.add(new Radio<>(ID_SOURCE_RADIO, Model.of(DirectionSelection.SOURCE)));

        source.add(new Label("sourceLabel",
                getString("DirectionChoicePanel.source.title")));
        source.add(new Label("sourceDesc",
                getString("DirectionChoicePanel.source.desc")));

        source.add(AttributeModifier.append("class",
                () -> DirectionSelection.SOURCE.equals(getModel().getObject())
                        ? " border-primary bg-light"
                        : ""));

        source.setOutputMarkupId(true);
        return source;
    }

    private @NotNull WebMarkupContainer createTargetOption() {
        WebMarkupContainer target = new WebMarkupContainer(ID_TARGET_CONTAINER);

        target.add(new Radio<>(ID_TARGET_RADIO, Model.of(DirectionSelection.TARGET)));

        target.add(new Label("targetLabel",
                getString("DirectionChoicePanel.target.title")));
        target.add(new Label("targetDesc",
                getString("DirectionChoicePanel.target.desc")));

        target.add(AttributeModifier.append("class",
                () -> DirectionSelection.TARGET.equals(getModel().getObject())
                        ? " border-primary bg-light"
                        : ""));

        target.setOutputMarkupId(true);
        return target;
    }

    protected void onDirectionChange(AjaxRequestTarget target) {

    }
}
