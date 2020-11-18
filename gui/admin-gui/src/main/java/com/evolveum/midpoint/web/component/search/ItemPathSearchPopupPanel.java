/*
 * Copyright (c) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.component.search;

import com.evolveum.midpoint.gui.api.component.path.ItemPathDto;
import com.evolveum.midpoint.gui.api.component.path.ItemPathPanel;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.web.component.form.MidpointForm;
import com.evolveum.midpoint.web.component.input.DatePanel;
import com.evolveum.midpoint.web.page.admin.configuration.component.EmptyOnBlurAjaxFormUpdatingBehaviour;
import com.evolveum.midpoint.web.util.DateValidator;

import org.apache.wicket.extensions.yui.calendar.DateTimeField;
import org.apache.wicket.model.IModel;

import javax.xml.datatype.XMLGregorianCalendar;

public class ItemPathSearchPopupPanel extends SpecialPopoverSearchPopupPanel {

    private static final long serialVersionUID = 1L;

    private static final String ID_ITEM_PATH = "itemPath";

    IModel<ItemPathDto> itemPathModel;

    public ItemPathSearchPopupPanel(String id, IModel<ItemPathDto> itemPathModel) {
        super(id);
        this.itemPathModel = itemPathModel;
    }

    @Override
    protected void customizationPopoverForm(MidpointForm popoverForm) {
        ItemPathPanel itemPathPanel = new ItemPathPanel(ID_ITEM_PATH, itemPathModel);
        popoverForm.add(itemPathPanel);
    }
}
