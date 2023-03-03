/*
 * Copyright (c) 2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.page.admin.shadows;

import java.util.Arrays;
import java.util.List;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.model.IModel;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.DisplayableChoiceRenderer;
import com.evolveum.midpoint.prism.impl.DisplayableValueImpl;
import com.evolveum.midpoint.schema.TaskExecutionMode;
import com.evolveum.midpoint.util.DisplayableValue;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.dialog.Popupable;
import com.evolveum.midpoint.web.component.dialog.SimplePopupable;

public class ChooseTaskExecutionModePopup extends SimplePopupable<DisplayableValue<TaskExecutionMode>> {

    private static final String ID_BUTTONS = "buttons";
    private static final String ID_DROPDOWN = "dropdown";
    private static final String ID_CANCEL_BUTTON = "cancel";
    private static final String ID_SELECT_BUTTON = "select";

    private Fragment footer;

    public ChooseTaskExecutionModePopup(String id) {
        super(id, null, 400, 200, PageBase.createStringResourceStatic("ChooseTaskExecutionModePopup.title"));

        initLayout();
    }

    @Override
    public IModel<DisplayableValue<TaskExecutionMode>> createModel() {
        return new LoadableModel<>(false) {
            @Override
            protected DisplayableValue<TaskExecutionMode> load() {
                return createValue(TaskExecutionMode.SIMULATED_PRODUCTION);
            }
        };
    }

    @Override
    public Component getContent() {
        return this;
    }

    @Override
    public @NotNull Component getFooter() {
        return footer;
    }

    private DisplayableValue<TaskExecutionMode> createValue(TaskExecutionMode mode) {
        return new DisplayableValueImpl<>(mode, "TaskExecutionMode." + mode);
    }

    private void initLayout() {
        IModel<List<DisplayableValue<TaskExecutionMode>>> choices = () -> Arrays.asList(
                createValue(TaskExecutionMode.SIMULATED_DEVELOPMENT),
                createValue(TaskExecutionMode.SIMULATED_PRODUCTION));

        DropDownChoice<DisplayableValue<TaskExecutionMode>> dropdown = new DropDownChoice<>(ID_DROPDOWN, getModel(), choices, new DisplayableChoiceRenderer<>());
        dropdown.add(new AjaxFormComponentUpdatingBehavior("change") {
            @Override
            protected void onUpdate(AjaxRequestTarget target) {

            }
        });
        add(dropdown);

        footer = initFooter();
    }

    private Fragment initFooter() {
        Fragment footer = new Fragment(Popupable.ID_FOOTER, ID_BUTTONS, this);

        AjaxButton cancelButton = new AjaxButton(ID_CANCEL_BUTTON) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                onCancelPerformed(target);
            }
        };
        footer.add(cancelButton);

        AjaxButton selectButton = new AjaxButton(ID_SELECT_BUTTON) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                onSelectPerformed(target, ChooseTaskExecutionModePopup.this.getModelObject().getValue());
            }
        };
        footer.add(selectButton);

        return footer;
    }

    protected void onCancelPerformed(AjaxRequestTarget target) {
        getPageBase().hideMainPopup(target);
    }

    protected void onSelectPerformed(AjaxRequestTarget target, TaskExecutionMode mode) {

    }
}
