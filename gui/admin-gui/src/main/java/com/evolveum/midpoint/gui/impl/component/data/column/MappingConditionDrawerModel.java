/*
 * Copyright (C) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.component.data.column;

import java.util.List;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismPropertyWrapper;
import com.evolveum.midpoint.gui.impl.component.input.expression.ExpressionPanel;
import com.evolveum.midpoint.gui.impl.component.wizard.collapse.CollapsedItem;
import com.evolveum.midpoint.gui.impl.component.wizard.collapse.DrawerFooterPanel;
import com.evolveum.midpoint.gui.impl.component.wizard.collapse.DrawerModel;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExpressionType;

/**
 * Drawer of a mapping condition, with buttons to apply, remove or drop the edited copy.
 */
public abstract class MappingConditionDrawerModel extends DrawerModel {

    private final IModel<ExpressionType> editedCondition;

    public MappingConditionDrawerModel(
            IModel<PrismPropertyWrapper<ExpressionType>> conditionWrapper, IModel<ExpressionType> editedCondition) {
        super(Model.ofList(createItems(conditionWrapper, editedCondition)));
        this.editedCondition = editedCondition;
    }

    private static List<CollapsedItem<DrawerModel>> createItems(
            IModel<PrismPropertyWrapper<ExpressionType>> conditionWrapper, IModel<ExpressionType> editedCondition) {

        CollapsedItem<DrawerModel> item = new CollapsedItem<>() {

            @Override
            public IModel<String> getIcon() {
                return Model.of("fa fa-code");
            }

            @Override
            public IModel<String> getTitle() {
                return new StringResourceModel("MappingConditionDrawerModel.title");
            }

            @Override
            public Component getPanel(String id, DrawerModel model) {
                ExpressionPanel panel = new ExpressionPanel(id, conditionWrapper, editedCondition);
                panel.setEvaluatorPanelExpanded(true);
                return panel;
            }
        };
        item.setSelected(true);
        return List.of(item);
    }

    @Override
    public Component getFooter(String id, DrawerModel model) {
        return new DrawerFooterPanel(id) {

            @Override
            protected void customizeFooterButtons(RepeatingView repeater) {
                repeater.add(createRemoveButton(repeater.newChildId()));
                super.customizeFooterButtons(repeater);
            }

            @Override
            protected IModel<String> createNoLabel() {
                return createStringResource("MappingConditionDrawerModel.close");
            }

            @Override
            protected IModel<String> createYesLabel() {
                return createStringResource("MappingConditionDrawerModel.apply");
            }

            @Override
            public void noPerformed(AjaxRequestTarget target) {
                closePerformed(target);
            }

            @Override
            public void yesPerformed(AjaxRequestTarget target) {
                storePerformed(editedCondition.getObject(), target);
            }

            private Component createRemoveButton(String buttonId) {
                AjaxButton removeButton = new AjaxButton(
                        buttonId, createStringResource("MappingConditionDrawerModel.remove")) {

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        storePerformed(null, target);
                    }
                };
                removeButton.add(AttributeAppender.append("class", "btn btn-link text-danger"));
                return removeButton;
            }
        };
    }

    /**
     * Writes the edited copy into the mapping.
     *
     * @param condition edited copy of the condition, null removes it from the mapping.
     * @param target target of the ajax request.
     */
    protected abstract void storePerformed(ExpressionType condition, AjaxRequestTarget target);

    /**
     * Closes the drawer and leaves the mapping as it was.
     *
     * @param target target of the ajax request.
     */
    protected abstract void closePerformed(AjaxRequestTarget target);
}
