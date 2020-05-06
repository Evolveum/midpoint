/*
 * Copyright (c) 2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.component.data.column;

import org.apache.commons.lang.Validate;
import org.apache.wicket.Component;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.export.IExportableColumn;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.prism.wrapper.ItemWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismValueWrapper;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.path.ItemPath;

/**
 * @author skublik
 */
public abstract class AbstractItemWrapperColumn<C extends Containerable, VW extends PrismValueWrapper> extends AbstractColumn<PrismContainerValueWrapper<C>, String> implements IExportableColumn<PrismContainerValueWrapper<C>, String>{

    public enum ColumnType {
        LINK,
        STRING,
        VALUE;
    }

    private static final long serialVersionUID = 1L;
    protected ItemPath itemName;

    private ColumnType columnType;

    private static final String ID_VALUE = "value";


    private IModel<? extends PrismContainerDefinition<C>> mainModel = null;

    AbstractItemWrapperColumn(IModel<? extends PrismContainerDefinition<C>> mainModel, ItemPath itemName, ColumnType columnType) {
        super(null);
        Validate.notNull(mainModel, "no model");
        Validate.notNull(mainModel.getObject(), "no ContainerWrappe from model");
        Validate.notNull(itemName, "no qName");
        this.mainModel = mainModel;
        this.itemName = itemName;
        this.columnType = columnType;
    }


    @Override
    public Component getHeader(String componentId) {
        return createHeader(componentId, mainModel);
    }

    @Override
    public void populateItem(Item<ICellPopulator<PrismContainerValueWrapper<C>>> cellItem, String componentId,
            IModel<PrismContainerValueWrapper<C>> rowModel) {

        cellItem.add(createColumnPanel(componentId, (IModel) getDataModel(rowModel)));

    }

    protected abstract Component createHeader(String componentId, IModel<? extends PrismContainerDefinition<C>> mainModel);
    protected abstract <IW extends ItemWrapper> Component createColumnPanel(String componentId, IModel<IW> rowModel);


    public ColumnType getColumnType() {
        return columnType;
    }


}
