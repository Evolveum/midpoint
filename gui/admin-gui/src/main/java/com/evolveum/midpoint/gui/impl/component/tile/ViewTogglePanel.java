/*
 * Copyright (C) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */
package com.evolveum.midpoint.gui.impl.component.tile;

import java.util.List;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.component.Toggle;
import com.evolveum.midpoint.gui.api.component.TogglePanel;

/** Exclusive view selector whose rendered state follows the displayed view. */
public class ViewTogglePanel extends TogglePanel<ViewToggle> {

    private final IModel<ViewToggle> selectedView;

    public ViewTogglePanel(String id, IModel<List<Toggle<ViewToggle>>> items, IModel<ViewToggle> selectedView) {
        super(id, items);
        this.selectedView = selectedView;
    }

    @Override
    protected boolean isItemActive(Toggle<ViewToggle> item) {
        return item.getValue() == selectedView.getObject();
    }

    @Override
    protected String getBootstrapToggle() {
        return null;
    }

    @Override
    protected void itemSelected(AjaxRequestTarget target, IModel<Toggle<ViewToggle>> item) {
        selectedView.setObject(item.getObject().getValue());
        target.add(this);
    }
}
