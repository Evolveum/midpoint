/*
 * Copyright (c) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.page.admin.server;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.form.OnChangeAjaxBehavior;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.factory.GuiComponentFactory;
import com.evolveum.midpoint.gui.impl.prism.*;
import com.evolveum.midpoint.web.component.prism.InputPanel;

public class TaskHandlerSelectorPanel extends PrismPropertyPanel<String> {

    private static final String ID_TASK_SPECIFICATION = "taskSpecification";

    /**
     * @param id
     * @param model
     * @param settings
     */
    public TaskHandlerSelectorPanel(String id, IModel<PrismPropertyWrapper<String>> model, ItemPanelSettings settings) {
        super(id, model, settings);

        Label label = new Label(ID_TASK_SPECIFICATION, createStringResource("TaskHandlerSelectorPanel.seletor.header"));
        add(label);
        label.setOutputMarkupId(true);
    }

    @Override
    protected Component createValuePanel(ListItem<PrismPropertyValueWrapper<String>> item, GuiComponentFactory factory, ItemVisibilityHandler visibilityHandler, ItemEditabilityHandler editabilityHandler) {
        Component handlerPanel = super.createValuePanel(item, factory, visibilityHandler, editabilityHandler);
        if (handlerPanel.get("form:input") instanceof InputPanel) {
            ((InputPanel) handlerPanel.get("form:input")).getBaseFormComponent().add(new OnChangeAjaxBehavior() {

                @Override
                protected void onUpdate(AjaxRequestTarget ajaxRequestTarget) {
                    onUpdatePerformed(ajaxRequestTarget);
                }
            });
        }
        return handlerPanel;
    }

    protected void onUpdatePerformed(AjaxRequestTarget target) {

    }
}
