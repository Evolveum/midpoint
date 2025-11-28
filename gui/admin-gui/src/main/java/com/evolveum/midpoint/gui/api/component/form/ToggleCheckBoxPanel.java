/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
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
import org.jetbrains.annotations.NotNull;

import java.io.Serial;
import java.util.LinkedHashMap;
import java.util.Map;

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

        boolean state = getState();
        String checkboxId = getPanelComponent().getMarkupId();

        Map<String, Object> options = createSwitchOptions();
        options.putIfAbsent("onSwitchChange",
                String.format("function(e, s) { $('#%s').trigger('change'); }", checkboxId));

        StringBuilder optionsJs = buildOptionsJs(options);
        String js = String.format(
                "$('#%s')" +
                        "    .bootstrapSwitch(%s)" +
                        "    .bootstrapSwitch('state', %s, true);",
                checkboxId, optionsJs, state
        );

        response.render(OnDomReadyHeaderItem.forScript(js));
    }

    private static @NotNull StringBuilder buildOptionsJs(@NotNull Map<String, Object> options) {
        StringBuilder optionsJs = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, Object> entry : options.entrySet()) {
            if (!first) {optionsJs.append(",");}
            first = false;

            String key = entry.getKey();
            Object value = entry.getValue();

            if (value instanceof String && ((String) value).trim().startsWith("function")) {
                optionsJs.append(key).append(": ").append(value);
            } else if (value instanceof String) {
                optionsJs.append(key).append(": '").append(value).append("'");
            } else {
                optionsJs.append(key).append(": ").append(value);
            }
        }
        optionsJs.append("}");
        return optionsJs;
    }

    private boolean getState() {
        return Boolean.TRUE.equals(getPanelComponent().getModelObject());
    }

    /**
     * ToggleCheckBoxPanel integrates a Bootstrap Switch control with Wicket.
     *
     * <p><b>Supported Bootstrap Switch options:</b></p>
     * <ul>
     *   <li>{@code state}</li> ignore it, handled by the panel
     *   <li>{@code size}</li>
     *   <li>{@code animate}</li>
     *   <li>{@code disabled}</li>
     *   <li>{@code readonly}</li>
     *   <li>{@code indeterminate}</li>
     *   <li>{@code inverse}</li>
     *   <li>{@code radioAllOff}</li>
     *   <li>{@code onColor}</li>
     *   <li>{@code offColor}</li>
     *   <li>{@code onText}</li>
     *   <li>{@code offText}</li>
     *   <li>{@code labelText}</li>
     *   <li>{@code handleWidth}</li>
     *   <li>{@code labelWidth}</li>
     *   <li>{@code baseClass}</li>
     *   <li>{@code wrapperClass}</li>
     *   <li>{@code onInit}</li>
     *   <li>{@code onSwitchChange}</li>
     * </ul>
     *
     * Each of these corresponds to a Bootstrap Switch configuration parameter.
     * See <a href="https://bttstrp.github.io/bootstrap-switch/options.html">official documentation</a>
     * for details and valid values.
     */
    protected @NotNull Map<String, Object> createSwitchOptions() {
        return new LinkedHashMap<>();
    }
}
