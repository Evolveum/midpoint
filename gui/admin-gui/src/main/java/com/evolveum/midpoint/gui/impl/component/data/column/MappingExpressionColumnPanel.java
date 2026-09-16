/*
 * Copyright (C) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.component.data.column;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismPropertyWrapper;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MappingType;

/**
 * Panel providing an expression of a mapping with the condition indicator below it.
 */
public class MappingExpressionColumnPanel<T> extends BasePanel<PrismPropertyWrapper<T>> {

    private static final String ID_EXPRESSION = "expression";
    private static final String ID_CONDITION = "condition";

    private final AbstractItemWrapperColumn.ColumnType columnType;

    public MappingExpressionColumnPanel(
            String id, IModel<PrismPropertyWrapper<T>> model, AbstractItemWrapperColumn.ColumnType columnType) {
        super(id, model);
        this.columnType = columnType;
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();

        add(createExpressionPanel());
        add(createConditionPanel());
    }

    private Component createConditionPanel() {
        return new MappingConditionPanel(ID_CONDITION, getModel().map(MappingExpressionColumnPanel::findMappingValue));
    }

    private Component createExpressionPanel() {
        return new PrismPropertyWrapperColumnPanel<>(ID_EXPRESSION, getModel(), columnType) {

            @Override
            protected void onClick(AjaxRequestTarget target, PrismContainerValueWrapper<?> rowModel) {
                MappingExpressionColumnPanel.this.onClick(target, rowModel);
            }
        };
    }

    protected void onClick(AjaxRequestTarget target, PrismContainerValueWrapper<?> rowModel) {
    }


    @SuppressWarnings("unchecked")
    private static <T> PrismContainerValueWrapper<MappingType> findMappingValue(PrismPropertyWrapper<T> expressionWrapper) {
        PrismContainerValueWrapper<?> parent = expressionWrapper != null ? expressionWrapper.getParent() : null;
        if (parent == null || !(parent.getRealValue() instanceof MappingType)) {
            return null;
        }

        return (PrismContainerValueWrapper<MappingType>) parent;
    }
}
