/*
 * Copyright (c) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.component.search.panel;

import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;

import java.io.Serial;

/**
 * @author honchar
 */
public class ItemPathSearchPanel extends PopoverSearchPanel<ItemPathType> {

    @Serial private static final long serialVersionUID = 1L;

    public ItemPathSearchPanel(String id, IModel<ItemPathType> itemPathModel) {
        super(id, itemPathModel);
    }

    @Override
    protected PopoverSearchPopupPanel createPopupPopoverPanel() {
        return new ItemPathSearchPopupPanel(PopoverSearchPanel.ID_POPOVER_PANEL, getModel()) {

            @Serial private static final long serialVersionUID = 1L;

            @Override
            protected void confirmPerformed(AjaxRequestTarget target) {
                target.add(ItemPathSearchPanel.this);
            }

            @Override
            protected void removeSearchValue(AjaxRequestTarget target) {
                ItemPathSearchPanel.this.getModel().setObject(null);
                target.add(this);
            }
        };
    }

    @Override
    public IModel<String> getTextValue() {
        return () -> {
            if (getModelObject() == null) {
                return "";
            }
            return getModelObject().toString();
        };
    }

}
