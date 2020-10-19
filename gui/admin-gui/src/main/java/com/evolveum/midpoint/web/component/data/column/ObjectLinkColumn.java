/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.component.data.column;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.export.IExportableColumn;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;

import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

/**
 * @author lazyman
 */
public class ObjectLinkColumn<T> extends AjaxLinkColumn<T> implements IExportableColumn<T, String> {
    private static final long serialVersionUID = 1L;

    public ObjectLinkColumn(IModel<String> displayModel) {
        super(displayModel);
    }

    public ObjectLinkColumn(IModel<String> displayModel, String propertyExpression) {
        super(displayModel, null, propertyExpression);
    }

    public ObjectLinkColumn(IModel<String> displayModel, String sortProperty, String propertyExpression) {
        super(displayModel, sortProperty);
    }

    @Override
    public void populateItem(Item<ICellPopulator<T>> cellItem, String componentId,
                             final IModel<T> rowModel) {

        IModel<ObjectType> superModel = createLinkModel(rowModel);
        final ObjectType targetObjectType = superModel.getObject();
        IModel<String> nameModel = new PropertyModel<>(superModel, FocusType.F_NAME.getLocalPart() + ".orig");
        cellItem.add(new AjaxLinkPanel(componentId, nameModel) {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                ObjectLinkColumn.this.onClick(target, rowModel, targetObjectType);
            }

            @Override
            public boolean isEnabled() {
                return ObjectLinkColumn.this.isEnabled(rowModel);
            }
        });
    }



    public boolean isEnabled(IModel<T> rowModel) {
        return true;
    }

    public void onClick(AjaxRequestTarget target, IModel<T> rowModel, ObjectType targetObjectType) {
        super.onClick(target, rowModel);
    }

    @Override
    public IModel<String> getDataModel(IModel<T> rowModel) {
        IModel<ObjectType> superModel = createLinkModel(rowModel);
        return new PropertyModel<>(superModel, FocusType.F_NAME.getLocalPart() + ".orig");
    }

}
