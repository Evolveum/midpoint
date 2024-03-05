/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.component.input;

import com.evolveum.midpoint.web.component.prism.InputPanel;

import org.apache.commons.text.StringEscapeUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.OnChangeAjaxBehavior;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.model.IModel;

import java.util.ArrayList;
import java.util.List;

/***
 * Panel for Icon css class with insight. We need define abstract method createPanel for InputPanel.
 */
public abstract class IconInputPanel extends InputPanel {

    private static final String ID_INSIGHT = "insight";
    private static final String ID_PANEL = "panel";

    private final IModel<String> valueModel;

    public IconInputPanel(String componentId, IModel<String> valueModel) {
        super(componentId);
        this.valueModel = valueModel;
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();

        initLayout();
    }

    /***
     * Method for creating input panel for string field.
     */
    protected abstract InputPanel createPanel(String idPanel);

    private void initLayout() {
        setOutputMarkupId(true);

        WebMarkupContainer insight = new WebMarkupContainer(ID_INSIGHT);
        insight.add(AttributeModifier.replace(
                "class", () -> "fa-fw " + getCssIconClass()));
        customProcessOfInsight(insight);
        insight.setOutputMarkupId(true);
        add(insight);

        InputPanel panel = createPanel(ID_PANEL);
        panel.setOutputMarkupId(true);
        panel.getBaseFormComponent().add(new OnChangeAjaxBehavior() {
            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                target.add(IconInputPanel.this.get(ID_INSIGHT));
            }
        });
        add(panel);
    }

    protected void customProcessOfInsight(WebMarkupContainer insight) {
    }

    protected String getCssIconClass() {
        return (valueModel.getObject() == null ? "" : StringEscapeUtils.escapeHtml4(valueModel.getObject()));
    }

    @Override
    public FormComponent getBaseFormComponent() {
        InputPanel panel = (InputPanel) get(ID_PANEL);
        return panel == null ? null : panel.getBaseFormComponent();
    }

    @Override
    public List<FormComponent> getFormComponents() {
        InputPanel panel = (InputPanel) get(ID_PANEL);
        return panel == null ? new ArrayList<>() : panel.getFormComponents();
    }

    protected final IModel<String> getValueModel() {
        return valueModel;
    }
}
