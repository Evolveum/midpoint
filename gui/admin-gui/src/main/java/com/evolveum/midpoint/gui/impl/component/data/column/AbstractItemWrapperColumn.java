/*
 * Copyright (C) 2018-2020 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.component.data.column;

import org.apache.commons.lang3.Validate;
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
public abstract class AbstractItemWrapperColumn<C extends Containerable, VW extends PrismValueWrapper>
        extends AbstractColumn<PrismContainerValueWrapper<C>, String>
        implements IExportableColumn<PrismContainerValueWrapper<C>, String> {

    public enum ColumnType {
        LINK,
        STRING,
        VALUE,
        EXISTENCE_OF_VALUE;
    }

    private static final long serialVersionUID = 1L;

    private static final String ID_VALUE = "value";

    protected ItemPath itemName;

    private final ColumnType columnType;

    private IModel<? extends PrismContainerDefinition<C>> mainModel;

    AbstractItemWrapperColumn(IModel<? extends PrismContainerDefinition<C>> mainModel, ItemPath itemName, ColumnType columnType) {
        super(null);
        Validate.notNull(mainModel, "no model");
        Validate.notNull(mainModel.getObject(), "no ContainerWrapper from model");
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
