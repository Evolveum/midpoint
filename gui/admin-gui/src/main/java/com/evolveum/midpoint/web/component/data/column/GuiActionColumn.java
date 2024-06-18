/*
 * Copyright (c) 2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.component.data.column;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.impl.component.AjaxCompositedIconButton;
import com.evolveum.midpoint.gui.impl.component.icon.CompositedIconBuilder;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.web.component.action.AbstractGuiAction;
import com.evolveum.midpoint.web.component.action.ActionsPanel;
import com.evolveum.midpoint.web.component.data.MenuMultiButtonPanel;
import com.evolveum.midpoint.web.component.menu.cog.ButtonInlineMenuItem;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;

import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItemAction;
import com.evolveum.midpoint.web.component.util.EnableBehaviour;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
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
        C obj = unwrapRowModel(rowModel);
        List<C> objectsToProcess = Collections.singletonList(obj);
        return new ActionsPanel<C>(componentId, Model.ofList(actionList), objectsToProcess);
    }

    @Override
    public Component getHeader(String componentId) {
        //todo
        return new Label(componentId, "Actions");
    }

    protected abstract C unwrapRowModel(IModel<T> rowModel);

}
