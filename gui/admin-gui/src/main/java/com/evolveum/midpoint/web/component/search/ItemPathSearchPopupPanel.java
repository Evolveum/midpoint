/*
 * Copyright (c) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.component.search;

import com.evolveum.midpoint.gui.api.component.path.ItemPathDto;
import com.evolveum.midpoint.gui.api.component.path.ItemPathPanel;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.web.component.form.MidpointForm;

import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

import org.apache.wicket.model.IModel;

public class ItemPathSearchPopupPanel extends PopoverSearchPopupPanel {

    private static final long serialVersionUID = 1L;

    private static final String ID_ITEM_PATH = "itemPath";

    IModel<ItemPathDto> itemPathDtoModel;

    public ItemPathSearchPopupPanel(String id, IModel<ItemPathType> itemPathModel) {
        super(id);
        this.itemPathDtoModel = new LoadableModel<ItemPathDto>(){

            @Override
            protected ItemPathDto load() {
                return new ItemPathDto(itemPathModel.getObject());
            }

            @Override
            public void setObject(ItemPathDto object) {
                super.setObject(object);
                itemPathModel.setObject(new ItemPathType(object.toItemPath()));
            }
        };
    }

    @Override
    protected void customizationPopoverForm(MidpointForm popoverForm) {
        ItemPathPanel itemPathPanel = new ItemPathPanel(ID_ITEM_PATH, itemPathDtoModel);
        popoverForm.add(itemPathPanel);
    }
}
