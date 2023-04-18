/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.prism.panel;

import java.util.List;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.component.togglebutton.ToggleIconButton;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismPropertyWrapper;
import com.evolveum.midpoint.gui.impl.prism.wrapper.PrismPropertyValueWrapper;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExpressionType;

/**
 * Created by honchar
 */
public class ExpressionPropertyHeaderPanel extends ItemHeaderPanel<PrismPropertyValue<ExpressionType>, PrismProperty<ExpressionType>,
        PrismPropertyDefinition<ExpressionType>, PrismPropertyWrapper<ExpressionType>> {
    private static final long serialVersionUID = 1L;

    private static final String ID_ADD_BUTTON = "addButton";
    private static final String ID_REMOVE_BUTTON = "removeButton";
    private static final String ID_EXPAND_COLLAPSE_BUTTON = "expandCollapseButton";

    private boolean isExpanded;

    public ExpressionPropertyHeaderPanel(String id, IModel<PrismPropertyWrapper<ExpressionType>> model) {
        super(id, model);
        isExpanded = model.getObject() != null && CollectionUtils.isNotEmpty(model.getObject().getValues());
    }

    @Override
    protected void initButtons() {
        AjaxLink<Void> addButton = new AjaxLink<>(ID_ADD_BUTTON) {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                addExpressionValuePerformed(target);
            }
        };
        addButton.add(new VisibleBehaviour(this::isExpressionValueEmpty));
        add(addButton);

        AjaxLink<Void> removeButton = new AjaxLink<>(ID_REMOVE_BUTTON) {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                removeExpressionValuePerformed(target);
            }
        };
        removeButton.add(new VisibleBehaviour(() -> !isExpressionValueEmpty()));
        add(removeButton);

        ToggleIconButton<?> expandCollapseButton = new ToggleIconButton<Void>(ID_EXPAND_COLLAPSE_BUTTON,
                GuiStyleConstants.CLASS_ICON_EXPAND_CONTAINER, GuiStyleConstants.CLASS_ICON_COLLAPSE_CONTAINER) {

            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                onExpandClick(target);
            }

            @Override
            public boolean isOn() {
                return ExpressionPropertyHeaderPanel.this.getModelObject() != null && isExpanded;
            }
        };
        expandCollapseButton.setOutputMarkupId(true);
        add(expandCollapseButton);
    }

    @Override
    protected Component createTitle(IModel<String> label) {
        AjaxButton labelComponent = new AjaxButton(ID_LABEL, label) {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                onExpandClick(target);
            }
        };
        labelComponent.setOutputMarkupId(true);
        return labelComponent;
    }

    private boolean isExpressionValueEmpty() {
        if (getModelObject() == null || CollectionUtils.isEmpty(getModelObject().getValues())) {
            return true;
        }
        List<PrismPropertyValueWrapper<ExpressionType>> valueWrappers = getModelObject().getValues();
        for (PrismPropertyValueWrapper<ExpressionType> expressionValueWrapper : valueWrappers) {
            if (expressionValueWrapper.getOldValue() != null && expressionValueWrapper.getOldValue().getValue() != null) {
                return false;
            }
        }
        return true;
    }

    protected void onExpandClick(AjaxRequestTarget target) {
        isExpanded = !isExpanded;
    }

    protected void addExpressionValuePerformed(AjaxRequestTarget target) {

    }

    protected void removeExpressionValuePerformed(AjaxRequestTarget target) {

    }

    @Override
    protected void refreshPanel(AjaxRequestTarget target) {

    }

    @Override
    protected PrismPropertyValue<ExpressionType> createNewValue(PrismPropertyWrapper<ExpressionType> parent) {
        return null;
    }
}
