/*
 * Copyright (c) 2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.component.data.column;

import java.io.Serial;
import java.io.Serializable;
import java.util.Collections;
import java.util.List;

import com.evolveum.midpoint.web.component.util.VisibleBehaviour;

import org.apache.wicket.Component;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import com.evolveum.midpoint.gui.impl.component.action.AbstractGuiAction;
import com.evolveum.midpoint.gui.impl.component.action.ActionsPanel;
import com.evolveum.midpoint.prism.Containerable;

public abstract class GuiActionColumn<T extends Serializable, C extends Containerable> extends AbstractColumn<T, String> {

    @Serial private static final long serialVersionUID = 1L;

    protected List<AbstractGuiAction<C>> actionList;

    public GuiActionColumn(List<AbstractGuiAction<C>> actionList) {
        super(null);
        this.actionList = actionList;
    }

    @Override
    public void populateItem(final Item<ICellPopulator<T>> cellItem, String componentId, final IModel<T> rowModel) {
        Component panel = getPanel(componentId, rowModel);
        cellItem.add(panel);
    }

    private Component getPanel(String componentId, IModel<T> rowModel) {
        C obj = unwrapRowModelObject(rowModel.getObject());
        return new ActionsPanel<>(componentId, Model.ofList(actionList), obj) {
            @Serial
            private static final long serialVersionUID = 1L;

            @Override
            protected List<C> getObjectsToProcess() {
                return Collections.singletonList(obj);
            }
        };
    }

    @Override
    public Component getHeader(String componentId) {
        List<AbstractGuiAction<C>> headerActions = showHeaderActions() ? getHeaderActions() : Collections.emptyList();
        ActionsPanel actionsPanel = new ActionsPanel<C>(componentId, Model.ofList(headerActions)) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            protected List<C> getObjectsToProcess() {
                return getSelectedItems();
            }
        };
        actionsPanel.add(new VisibleBehaviour(() -> !headerActions.isEmpty() && showHeaderActions()));
        return actionsPanel;
    }

    private List<AbstractGuiAction<C>> getHeaderActions() {
        return actionList
                .stream()
                .filter(AbstractGuiAction::isBulkAction)
                .toList();
    }

    protected boolean showHeaderActions() {
        return true;
    }

    protected abstract C unwrapRowModelObject(T rowModelObject);

    protected abstract List<C> getSelectedItems();

}
