/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.api.component.form;

import com.evolveum.midpoint.gui.api.util.GuiDisplayTypeUtil;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.web.component.prism.InputPanel;
import com.evolveum.midpoint.web.component.util.EnableBehaviour;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;

import com.evolveum.midpoint.web.page.admin.configuration.component.EmptyOnBlurAjaxFormUpdatingBehaviour;
import com.evolveum.midpoint.web.page.admin.configuration.component.EmptyOnChangeAjaxFormUpdatingBehavior;
import com.evolveum.midpoint.xml.ns._public.common.common_3.DisplayType;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.OnChangeAjaxBehavior;
import org.apache.wicket.ajax.markup.html.form.AjaxCheckBox;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.model.IModel;

public class ToggleCheckBoxPanel extends InputPanel {
    private static final long serialVersionUID = 1L;

    private static final String ID_CHECK = "check";
    private static final String ID_LABEL = "label";
    private static final String ID_ICON = "icon";

    private IModel<Boolean> checkboxModel;
    private IModel<DisplayType> displayModel;

    public ToggleCheckBoxPanel(String id, IModel<Boolean> checkboxModel) {
        this(id, checkboxModel, null);
    }

    public ToggleCheckBoxPanel(String id, IModel<Boolean> checkboxModel, IModel<DisplayType> displayModel) {
        super(id);
        this.checkboxModel = checkboxModel;
        this.displayModel = displayModel;
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();

        CheckBox check = new CheckBox(ID_CHECK, getCheckboxModel());
        check.add(new OnChangeAjaxBehavior() {
            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                System.out.println("ToggleCheckBoxPanel.onUpdate");
            }
        });
//        check.add(new EmptyOnChangeAjaxFormUpdatingBehavior());
        check.setOutputMarkupId(true);
        check.add(new EnableBehaviour(this::isCheckboxEnabled));
        add(check);

        Label label = new Label(ID_LABEL, getLabelModel());
//        label.add(AttributeModifier.replace("for", (IModel<String>) check::getMarkupId));
        add(label);

        WebMarkupContainer icon = new WebMarkupContainer(ID_ICON);
//        icon.add(AttributeModifier.append("class", this::getIconModel));
        icon.add(new VisibleBehaviour(() -> notNullModel(getIconModel())));
        add(icon);

//        add(new AttributeModifier("title", this::getTooltipModel));
    }

    private IModel<String> getLabelModel() {
        return () -> {
            if (displayModel == null) {
                return null;
            }
            return WebComponentUtil.getTranslatedPolyString(GuiDisplayTypeUtil.getLabel(displayModel.getObject()));
        };
    }

    private IModel<String> getTooltipModel() {
        return () -> {
            if (displayModel == null) {
                return null;
            }
            return GuiDisplayTypeUtil.getHelp(displayModel.getObject());
        };
    }

    private IModel<String> getIconModel() {
        return () -> {
            if (displayModel == null) {
                return null;
            }
            return GuiDisplayTypeUtil.getIconCssClass(displayModel.getObject());
        };
    }

    private boolean notNullModel(IModel<?> model) {
        return model != null && model.getObject() != null;
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

    public IModel<Boolean> getCheckboxModel() {
        return checkboxModel;
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
