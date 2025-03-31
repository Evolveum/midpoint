/*
 * Copyright (C) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.component.search.panel;

import org.apache.wicket.Component;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;

import com.evolveum.midpoint.gui.impl.component.search.wrapper.DateSearchItemWrapper;

public class DateSearchItemPanel extends PropertySearchItemPanel<DateSearchItemWrapper> {

    private static final long serialVersionUID = 1L;

    public DateSearchItemPanel(String id, IModel<DateSearchItemWrapper> searchItemModel) {
        super(id, searchItemModel);
    }

    @Override
    protected Component getSearchItemFieldPanel() {
        Component component = super.getSearchItemFieldPanel();
        if (component != null) {
            return component.get(PopoverSearchPanel.ID_TEXT_FIELD);
        }
        return null;
    }

    @Override
    protected Component initSearchItemField(String id) {
        return new DateIntervalSearchPanel(id,
                new PropertyModel(getModel(), DateSearchItemWrapper.F_FROM_DATE),
                new PropertyModel(getModel(), DateSearchItemWrapper.F_TO_DATE),
                () -> getModelObject().getIntervalPresets(),
                () -> getModelObject().getSelectedIntervalPreset()) {

            @Override
            protected boolean isInterval() {
                return DateSearchItemPanel.this.getModelObject().isInterval();
            }
        };
    }
}
