/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.component.data.column;

import org.apache.wicket.Component;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.prism.wrapper.ItemWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.impl.prism.panel.PrismContainerColumnHeaderPanel;
import com.evolveum.midpoint.gui.impl.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.web.model.PrismContainerWrapperHeaderModel;
import com.evolveum.midpoint.web.model.PrismContainerWrapperModel;

/**
 * @author katka
 */
public class PrismContainerWrapperColumn<C extends Containerable> extends AbstractItemWrapperColumn<C, PrismContainerValueWrapper<C>>{

    private static final long serialVersionUID = 1L;

    private PageBase pageBase;

    public PrismContainerWrapperColumn(IModel<? extends PrismContainerDefinition<C>> rowModel, ItemPath itemName, PageBase pageBase) {
        super(rowModel, itemName, ColumnType.STRING);
        this.pageBase = pageBase;
    }

    @Override
    public IModel<?> getDataModel(IModel<PrismContainerValueWrapper<C>> rowModel) {
        return PrismContainerWrapperModel.fromContainerValueWrapper(rowModel, itemName);
    }

    @Override
    protected Component createHeader(String componentId, IModel<? extends PrismContainerDefinition<C>> mainModel) {
        return new PrismContainerColumnHeaderPanel<>(componentId, new PrismContainerWrapperHeaderModel(mainModel, itemName, pageBase));
    }

    @Override
    protected <IW extends ItemWrapper> Component createColumnPanel(String componentId, IModel<IW> rowModel) {
        return new PrismContainerWrapperColumnPanel<C>(componentId, (IModel<PrismContainerWrapper<C>>) rowModel, getColumnType());
    }

}
