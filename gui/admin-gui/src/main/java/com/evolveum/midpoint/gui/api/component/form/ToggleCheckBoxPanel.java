/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.api.component.form;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.web.component.prism.InputPanel;
import com.evolveum.midpoint.web.component.util.EnableBehaviour;
import com.evolveum.midpoint.web.page.admin.configuration.component.EmptyOnChangeAjaxFormUpdatingBehavior;

import org.apache.wicket.model.Model;

import java.io.Serial;

public class ToggleCheckBoxPanel extends InputPanel {
    @Serial private static final long serialVersionUID = 1L;

    private static final String ID_CONTAINER = "container";
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

    public ToggleCheckBoxPanel(String id,
            IModel<Boolean> checkboxModel) {
        super(id);
        this.checkboxModel = checkboxModel;
        this.labelModel = Model.of();
        this.descriptionModel = Model.of();
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();

        add(AttributeAppender.append("class", getComponentCssClass()));

        WebMarkupContainer container = new WebMarkupContainer(ID_CONTAINER);
        container.setOutputMarkupId(true);
        container.add(AttributeAppender.replace("class", getContainerCssClass()));
        add(container);

        CheckBox check = new CheckBox(ID_CHECK, checkboxModel);
        check.add(new EmptyOnChangeAjaxFormUpdatingBehavior() {
            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                ToggleCheckBoxPanel.this.onToggle(target);
            }
        });
        check.setOutputMarkupId(true);
        check.add(new EnableBehaviour(this::isCheckboxEnabled));
        container.add(check);

        Component titleComponent = getTitleComponent(ID_LABEL);
        container.add(titleComponent);

        Label description = new Label(ID_DESCRIPTION, descriptionModel);
        description.setOutputMarkupId(true);
        description.add(AttributeAppender.append("class", getDescriptionCssClass()));
        container.add(description);
    }

    protected void onToggle(AjaxRequestTarget target) {
        // Hook for subclasses to implement custom behavior on toggle
    }

    public String getComponentCssClass() {
        return "d-flex flex-row gap-3";
    }

    // Because panel is used on multiple places, probably not in correct way. In the feature we should refactor it.
    public String getContainerCssClass() {
        return "d-flex flex-row gap-3";
    }

    public String getDescriptionCssClass() {
        return null;
    }

    public Component getTitleComponent(String id) {
        return new Label(id, labelModel);
    }

    public CheckBox getPanelComponent() {
        return (CheckBox) get(createComponentPath(ID_CONTAINER, ID_CHECK));
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

        boolean state = Boolean.TRUE.equals(getPanelComponent().getModelObject());

        String js =
                "$('#" + getPanelComponent().getMarkupId() + "')" +
                        "    .bootstrapSwitch({" +
                        "        onSwitchChange: function(e, s) {" +
                        "            $('#" + getPanelComponent().getMarkupId() + "').trigger('change');" +
                        "        }" +
                        "    })" +
                        "    .bootstrapSwitch('state'," + state + ", true);";

        response.render(OnDomReadyHeaderItem.forScript(js));

    }
}
