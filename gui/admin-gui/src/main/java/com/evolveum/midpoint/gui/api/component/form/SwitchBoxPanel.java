/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.api.component.form;

import java.io.Serial;

import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.model.IModel;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.web.component.prism.InputPanel;
import com.evolveum.midpoint.web.component.util.EnableBehaviour;
import com.evolveum.midpoint.web.page.admin.configuration.component.EmptyOnChangeAjaxFormUpdatingBehavior;

public class SwitchBoxPanel extends InputPanel {
    @Serial private static final long serialVersionUID = 1L;

    private static final String ID_CHECK = "switch";

    private final IModel<Boolean> checkboxModel;

    public SwitchBoxPanel(
            @NotNull String id,
            @NotNull IModel<Boolean> checkboxModel) {
        super(id);
        this.checkboxModel = checkboxModel;
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();

        add(AttributeAppender.append("class", getComponentCssClass()));

        CheckBox check = new CheckBox(ID_CHECK, checkboxModel);
        check.add(new EmptyOnChangeAjaxFormUpdatingBehavior());
        check.setOutputMarkupId(true);
        check.add(new EnableBehaviour(this::isCheckboxEnabled));
        add(check);
    }

    public String getComponentCssClass() {
        return "d-flex flex-row gap-3";
    }

    public CheckBox getPanelComponent() {
        return (CheckBox) get(ID_CHECK);
    }

    public boolean getValue() {
        Boolean val = getPanelComponent().getModelObject();
        if (val == null) {
            return false;
        }

        return val;
    }

    protected boolean isCheckboxEnabled() {
        return true;
    }

    public FormComponent<?> getBaseFormComponent() {
        return getPanelComponent();
    }

    @Override
    public void renderHead(IHeaderResponse response) {
        super.renderHead(response);

        response.render(OnDomReadyHeaderItem.forScript("$(\"[data-bootstrap-switch]\").bootstrapSwitch({\n"
                + "  onSwitchChange: function(e, state) { \n"
                + "    $('#" + getPanelComponent().getMarkupId() + "').trigger('change');\n"
                + "  }\n"
                + "});"));
    }
}
