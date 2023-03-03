/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.api.component.form;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.attributes.AjaxRequestAttributes;
import org.apache.wicket.ajax.markup.html.form.AjaxCheckBox;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.web.component.util.EnableBehaviour;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;

/**
 * Checkbox that is supposed to be used in forms - checkbox with label.
 *
 * @author lazyman
 * @author Radovan Semancik
 */
public class CheckBoxPanel extends Panel {

    private static final long serialVersionUID = 1L;

    private static final String ID_CHECK = "check";
    private static final String ID_LABEL = "label";

    private IModel<Boolean> checkboxModel;
    private IModel<String> labelModel;
    private IModel<String> tooltipModel;

    public CheckBoxPanel(String id, IModel<Boolean> checkboxModel) {
        this(id, checkboxModel, null);
    }

    public CheckBoxPanel(String id, IModel<Boolean> checkboxModel, IModel<String> labelModel) {
        this(id, checkboxModel, labelModel, null);
    }

    public CheckBoxPanel(String id, IModel<Boolean> checkboxModel, IModel<String> labelModel, IModel<String> tooltipModel) {
        super(id);
        this.checkboxModel = checkboxModel;
        this.labelModel = labelModel;
        this.tooltipModel = tooltipModel;
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();

        add(AttributeAppender.append("class", "form-check"));

        AjaxCheckBox check = createCheckbox();
        check.setOutputMarkupId(true);

        check.add(new EnableBehaviour(() -> isCheckboxEnabled()));
        add(check);

        Component label = createLabel(ID_LABEL, labelModel, check);
        add(label);

        if (tooltipModel != null) {
            add(new AttributeModifier("title", tooltipModel));
        }
    }

    protected Component createLabel(String id, IModel<String> model, AjaxCheckBox check) {
        Label label = new Label(ID_LABEL, labelModel);
        label.add(AttributeModifier.replace("for", (IModel<String>) () -> check.getMarkupId()));
        label.add(new VisibleBehaviour(() -> labelModel != null));

        return label;
    }

    protected AjaxCheckBox createCheckbox() {
        return new AjaxCheckBox(ID_CHECK, checkboxModel) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                CheckBoxPanel.this.onUpdate(target);
            }

            @Override
            protected void updateAjaxAttributes(AjaxRequestAttributes attributes) {
                CheckBoxPanel.this.updateAjaxAttributes(attributes);
            }
        };
    }

    public AjaxCheckBox getPanelComponent() {
        return (AjaxCheckBox) get(ID_CHECK);
    }

    protected void updateAjaxAttributes(AjaxRequestAttributes attributes) {
    }

    public void onUpdate(AjaxRequestTarget target) {
    }

    public boolean getValue() {
        Boolean val = getPanelComponent().getModelObject();
        if (val == null) {
            return false;
        }

        return val.booleanValue();
    }

    protected boolean isCheckboxEnabled() {
        return true;
    }

    public IModel<Boolean> getCheckboxModel() {
        return checkboxModel;
    }
}
