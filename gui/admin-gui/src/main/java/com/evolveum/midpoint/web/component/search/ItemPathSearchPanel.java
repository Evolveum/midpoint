/*
 * Copyright (c) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.component.search;

import com.evolveum.midpoint.gui.api.component.path.ItemPathDto;
import com.evolveum.midpoint.schema.constants.ObjectTypes;

import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;

/**
 * @author honchar
 */
public class ItemPathSearchPanel extends PopoverSearchPanel<ItemPathType> {

    private static final long serialVersionUID = 1L;

    public ItemPathSearchPanel(String id, IModel<ItemPathType> itemPathModel) {
        super(id, itemPathModel);
    }

//    @Override
//    protected void onInitialize() {
//        if (getModelObject() == null) {
//            getModel().setObject(new ItemPathType());
//        }
//        super.onInitialize();
//    }

    @Override
    protected PopoverSearchPopupPanel createPopupPopoverPanel(String id) {
        return new ItemPathSearchPopupPanel(id, getModel()) {

            private static final long serialVersionUID = 1L;

            @Override
            protected void confirmPerformed(AjaxRequestTarget target) {
                target.add(ItemPathSearchPanel.this);
            }
        };
    }

    @Override
    public IModel<String> getTextValue() {
        return new IModel<String>() {

            @Override
            public String getObject() {
                if (getModelObject() == null) {
                    return "";
                }
                return getModelObject().toString();
            }
        };
    }

}
