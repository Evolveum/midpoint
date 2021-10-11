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
import com.evolveum.midpoint.gui.impl.prism.PrismReferenceHeaderPanel;
import com.evolveum.midpoint.gui.impl.prism.PrismReferenceWrapper;
import com.evolveum.midpoint.gui.impl.prism.PrismValueWrapper;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.Referencable;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.web.model.PrismReferenceWrapperHeaderModel;
import com.evolveum.midpoint.web.model.PrismReferenceWrapperModel;

/**
 * @author skublik
 */
public class PrismReferenceWrapperColumn<C extends Containerable, R extends Referencable> extends AbstractItemWrapperColumn<C, PrismValueWrapper<R, PrismReferenceValue>> {

    private static final long serialVersionUID = 1L;

    private static final String ID_HEADER = "header";
    private static final String ID_INPUT = "input";

    private PageBase pageBase;

    public PrismReferenceWrapperColumn(IModel<? extends PrismContainerDefinition<C>> mainModel, ItemPath itemName, ColumnType columnType, PageBase pageBase) {
        super(mainModel, itemName, columnType);
        this.pageBase = pageBase;
    }


    @Override
    public IModel<?> getDataModel(IModel<PrismContainerValueWrapper<C>> rowModel) {
        return PrismReferenceWrapperModel.fromContainerValueWrapper(rowModel, itemName);
    }

    @Override
    protected Component createHeader(String componentId, IModel<? extends PrismContainerDefinition<C>> mainModel) {
        return new PrismReferenceHeaderPanel<>(componentId, new PrismReferenceWrapperHeaderModel(mainModel, itemName, pageBase));
    }


    @Override
    protected <IW extends ItemWrapper> Component createColumnPanel(String componentId, IModel<IW> rowModel) {
        return new PrismReferenceWrapperColumnPanel<R>(componentId, (IModel<PrismReferenceWrapper<R>>)rowModel, getColumnType()) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onClick(AjaxRequestTarget target, PrismContainerValueWrapper<?> rowModel) {
                PrismReferenceWrapperColumn.this.onClick(target, (IModel) Model.of(rowModel));
            }
        };
    }

    protected void onClick(AjaxRequestTarget target, IModel<PrismContainerValueWrapper<C>> model) {

    }
}


