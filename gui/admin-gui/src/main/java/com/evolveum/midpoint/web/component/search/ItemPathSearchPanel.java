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
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.web.component.DateLabelComponent;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.IModel;

import javax.xml.datatype.XMLGregorianCalendar;

/**
 * @author honchar
 */
public class ItemPathSearchPanel extends SpecialPopoverSearchPanel {

    private static final long serialVersionUID = 1L;

    private IModel<ItemPathDto> itemPathModel;

    public ItemPathSearchPanel(String id, IModel<ItemPathDto> itemPathModel) {
        super(id);
        this.itemPathModel = itemPathModel;
    }

    @Override
    protected void onInitialize() {
        if (itemPathModel.getObject() == null) {
            itemPathModel.setObject(new ItemPathDto());
        }
        super.onInitialize();
    }

    @Override
    protected SpecialPopoverSearchPopupPanel createPopupPopoverPanel(String id) {
        return new ItemPathSearchPopupPanel(id, itemPathModel) {

            private static final long serialVersionUID = 1L;

            @Override
            protected void confirmPerformed(AjaxRequestTarget target) {
                target.add(ItemPathSearchPanel.this);
            }
        };
    }

    @Override
    public String getTextValue() {
        if (itemPathModel.getObject().toItemPath() == null) {
            return "";
        }
        ObjectTypes object = ObjectTypes.getObjectTypeFromTypeQName(itemPathModel.getObject().getObjectType());
        StringBuilder sb = new StringBuilder();
        if (object != null) {
            sb.append("(")
                .append(getPageBase().createStringResource(object).getString())
                .append(") ");
        }
        sb.append(itemPathModel.getObject().toItemPath().toString());
        return sb.toString();
    }

}
