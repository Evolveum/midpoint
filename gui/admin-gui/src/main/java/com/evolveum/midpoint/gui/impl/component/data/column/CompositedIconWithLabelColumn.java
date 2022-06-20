package com.evolveum.midpoint.gui.impl.component.data.column;

import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

public abstract class CompositedIconWithLabelColumn<T> extends CompositedIconColumn<T> {

    public CompositedIconWithLabelColumn(IModel<String> displayModel) {
        super(displayModel);
    }

    @Override
    public void populateItem(Item<ICellPopulator<T>> cellItem, String componentId, IModel<T> rowModel) {
        cellItem.add(new CompositedIconWithLabelPanel(componentId, Model.of(getCompositedIcon(rowModel)), getLabelModel()));
    }

    public abstract IModel<String> getLabelModel();

}
