/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.api.component.form;

import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.web.component.prism.InputPanel;
import com.evolveum.midpoint.web.component.util.EnableBehaviour;
import com.evolveum.midpoint.web.page.admin.configuration.component.EmptyOnChangeAjaxFormUpdatingBehavior;

import java.io.Serial;

public class ToggleCheckBoxPanel extends InputPanel {
    @Serial private static final long serialVersionUID = 1L;

    private static final String ID_CHECK = "check";
    private static final String ID_LABEL = "label";
    private static final String ID_DESCRIPTION = "description";

    private final IModel<Boolean> checkboxModel;
    private final IModel<String> labelModel;
    private final IModel<String> descriptionModel;

    public ToggleCheckBoxPanel(String id,
            IModel<Boolean> checkboxModel,
            IModel<String> labelModel,
            IModel<String> descriptionModel) {
        super(id);
        this.checkboxModel = checkboxModel;
        this.labelModel = labelModel;
        this.descriptionModel = descriptionModel;
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();

        CheckBox check = new CheckBox(ID_CHECK, checkboxModel);
        check.add(new EmptyOnChangeAjaxFormUpdatingBehavior());
        check.setOutputMarkupId(true);
        check.add(new EnableBehaviour(this::isCheckboxEnabled));
        add(check);

        Label label = new Label(ID_LABEL, labelModel);
        add(label);

        Label description = new Label(ID_DESCRIPTION, descriptionModel);
        add(description);

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

    public FormComponent getBaseFormComponent() {
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
