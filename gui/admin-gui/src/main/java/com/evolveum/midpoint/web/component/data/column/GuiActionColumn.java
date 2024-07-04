/*
 * Copyright (c) 2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.component.data.column;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.gui.impl.component.action.AbstractGuiAction;
import com.evolveum.midpoint.gui.impl.component.action.ActionsPanel;

import org.apache.wicket.Component;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import java.io.Serial;
import java.io.Serializable;
import java.util.Collections;
import java.util.List;

public abstract class GuiActionColumn<T extends Serializable, C extends Containerable> extends AbstractColumn<T, String> {

    @Serial private static final long serialVersionUID = 1L;

    protected List<AbstractGuiAction<C>> actionList;
    private PageBase pageBase;

    public GuiActionColumn(List<AbstractGuiAction<C>> actionList, PageBase pageBase) {
        super(null);
        this.actionList = actionList;
        this.pageBase = pageBase;
    }

    @Override
    public void populateItem(final Item<ICellPopulator<T>> cellItem, String componentId, final IModel<T> rowModel) {
        Component panel = getPanel(componentId, rowModel);
        cellItem.add(panel);
    }

    private Component getPanel(String componentId, IModel<T> rowModel) {
        return new ActionsPanel<C>(componentId, Model.ofList(actionList)) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            protected List<C> getObjectsToProcess() {
                C obj = unwrapRowModelObject(rowModel.getObject());
                return Collections.singletonList(obj);
            }
        };
    }

    @Override
    public Component getHeader(String componentId) {
        return new ActionsPanel<C>(componentId, Model.ofList(actionList)) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            protected List<C> getObjectsToProcess() {
                return getSelectedItems();
            }
        };
    }

    protected abstract C unwrapRowModelObject(T rowModelObject);

    protected abstract List<C> getSelectedItems();

}
