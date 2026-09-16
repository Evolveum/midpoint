/*
 * Copyright (C) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.component.data.column;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.prism.wrapper.ItemWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismPropertyWrapper;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.path.ItemPath;

/**
 * Expression column of a mapping table.
 */
public class MappingExpressionColumn<C extends Containerable, T> extends PrismPropertyWrapperColumn<C, T> {

    public MappingExpressionColumn(
            IModel<? extends PrismContainerDefinition<C>> mainModel, ItemPath itemName, ColumnType columnType, PageBase pageBase) {
        super(mainModel, itemName, columnType, pageBase);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    protected <IW extends ItemWrapper> Component createColumnPanel(String componentId, IModel<IW> rowModel) {
        return new MappingExpressionColumnPanel<T>(
                componentId, (IModel<PrismPropertyWrapper<T>>) rowModel, getColumnType()) {

            @Override
            protected void onClick(AjaxRequestTarget target, PrismContainerValueWrapper<?> rowModel) {
                MappingExpressionColumn.this.onClick(target, (IModel) Model.of(rowModel));
            }
        };
    }
}
