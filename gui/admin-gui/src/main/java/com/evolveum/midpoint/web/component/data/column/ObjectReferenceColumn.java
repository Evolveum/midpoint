package com.evolveum.midpoint.web.component.data.column;


import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.web.util.ObjectTypeGuiDescriptor;
import com.evolveum.midpoint.xml.ns._public.common.common_3.DisplayType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import javax.xml.namespace.QName;

public abstract class ObjectReferenceColumn<T> extends PropertyColumn<T, String> {


    public ObjectReferenceColumn(IModel<String> displayModel, String sortProperty, String propertyExpression) {
        super(displayModel, sortProperty, propertyExpression);
    }

    public ObjectReferenceColumn(IModel<String> displayModel, String propertyExpression) {
        super(displayModel, propertyExpression);
    }

    @Override
    public void populateItem(Item<ICellPopulator<T>> item, String componentId, IModel<T> rowModel) {
        IModel<ObjectReferenceType> dataModel = extractDataModel(rowModel);
        ObjectReferenceType referenceType = dataModel.getObject();
        item.add(new ObjectReferenceColumnPanel(componentId, Model.of(referenceType)));

    }

    @Override
    public IModel<?> getDataModel(IModel<T> rowModel) {
        return extractDataModel(rowModel);
    }

    public abstract IModel<ObjectReferenceType> extractDataModel(IModel<T> rowModel);




}
