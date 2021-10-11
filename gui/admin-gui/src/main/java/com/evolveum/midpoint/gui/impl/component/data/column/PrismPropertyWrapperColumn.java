/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.component.data.column;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.prism.ItemWrapper;
import com.evolveum.midpoint.gui.impl.prism.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.impl.prism.PrismPropertyHeaderPanel;
import com.evolveum.midpoint.gui.impl.prism.PrismPropertyValueWrapper;
import com.evolveum.midpoint.gui.impl.prism.PrismPropertyWrapper;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.web.model.PrismPropertyWrapperHeaderModel;
import com.evolveum.midpoint.web.model.PrismPropertyWrapperModel;

/**
 * @author skublik
 */
public class PrismPropertyWrapperColumn<C extends Containerable, T> extends AbstractItemWrapperColumn<C, PrismPropertyValueWrapper<T>> {

    private static final long serialVersionUID = 1L;

    private static final String ID_HEADER = "header";
    private static final String ID_INPUT = "input";

    private PageBase pageBase;

    public PrismPropertyWrapperColumn(IModel<? extends PrismContainerDefinition<C>> mainModel, ItemPath itemName, ColumnType columnType, PageBase pageBase) {
        super(mainModel, itemName, columnType);
        this.pageBase = pageBase;
    }


    @Override
    public IModel<?> getDataModel(IModel<PrismContainerValueWrapper<C>> rowModel) {
        return PrismPropertyWrapperModel.fromContainerValueWrapper(rowModel, itemName);
    }

    @Override
    protected Component createHeader(String componentId, IModel<? extends PrismContainerDefinition<C>> mainModel) {
        return new PrismPropertyHeaderPanel<>(componentId, new PrismPropertyWrapperHeaderModel(mainModel, itemName, pageBase));
    }


    @Override
    protected <IW extends ItemWrapper> Component createColumnPanel(String componentId, IModel<IW> rowModel) {
        return new PrismPropertyWrapperColumnPanel<T>(componentId, (IModel<PrismPropertyWrapper<T>>) rowModel, getColumnType()) {

            private static final long serialVersionUID = 1L;

            @Override
            protected void onClick(AjaxRequestTarget target, PrismContainerValueWrapper<?> rowModel) {
                PrismPropertyWrapperColumn.this.onClick(target, (IModel) Model.of(rowModel));
            }
        };
    }

    protected void onClick(AjaxRequestTarget target, IModel<PrismContainerValueWrapper<C>> model) {

    }
}


