/*
 * Copyright (c) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.component.search.panel;

import java.io.Serial;

import com.evolveum.midpoint.gui.api.model.LoadableModel;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;

import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

/**
 * @author honchar
 */
public class ItemPathSearchPanel extends PopoverSearchPanel<ItemPathType> {

    @Serial private static final long serialVersionUID = 1L;

    public ItemPathSearchPanel(String id, IModel<ItemPathType> itemPathModel) {
        super(id, itemPathModel);
    }

    @Override
    protected PopoverSearchPopupPanel createPopupPopoverPanel(Popover popover) {
        return new ItemPathSearchPopupPanel(PopoverSearchPanel.ID_POPOVER_PANEL, popover, getModel()) {

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
    public LoadableModel<String> getTextValue() {
        return new LoadableModel<String>() {
            @Override
            protected String load() {
                if (getModelObject() == null) {
                    return "";
                }
                return getModelObject().toString();
            }
        };
    }

}
