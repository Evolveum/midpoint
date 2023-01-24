/*
 * Copyright (c) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.component.search.panel;

import com.evolveum.midpoint.gui.api.component.path.ItemPathDto;
import com.evolveum.midpoint.gui.api.component.path.ItemPathPanel;
import com.evolveum.midpoint.web.component.form.MidpointForm;

import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

public class ItemPathSearchPopupPanel extends PopoverSearchPopupPanel {

    private static final long serialVersionUID = 1L;

    private static final String ID_ITEM_PATH = "itemPath";

    IModel<ItemPathType> itemPathModel;

    public ItemPathSearchPopupPanel(String id, IModel<ItemPathType> itemPathModel) {
        super(id);
        this.itemPathModel = itemPathModel;
    }

    @Override
    protected void customizationPopoverForm(MidpointForm popoverForm) {
        ItemPathPanel itemPathPanel = new ItemPathPanel(ID_ITEM_PATH, Model.of(new ItemPathDto(itemPathModel.getObject()))){
            @Override
            protected void onUpdate(ItemPathDto itemPathDto) {
                itemPathModel.setObject(new ItemPathType(itemPathDto.toItemPath()));
            }
        };
        popoverForm.add(itemPathPanel);
    }


}
