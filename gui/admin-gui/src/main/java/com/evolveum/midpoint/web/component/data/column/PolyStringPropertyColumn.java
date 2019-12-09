/**
 * Copyright (c) 2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.component.data.column;

import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.web.security.MidPointApplication;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;

/**
 * @author honchar
 *
 */
public class PolyStringPropertyColumn<T> extends PropertyColumn<T, String> {
    private static final long serialVersionUID = 1L;

    public PolyStringPropertyColumn(IModel<String> displayModel, String sortProperty, String propertyExpression) {
        super(displayModel, sortProperty, propertyExpression);
    }

    public PolyStringPropertyColumn(IModel<String> displayModel, String propertyExpression) {
        super(displayModel, propertyExpression);
    }

    @Override
    public IModel<?> getDataModel(IModel<T> rowModel) {
        PropertyModel propertyModel = (PropertyModel)super.getDataModel(rowModel);
        if (propertyModel == null){
            return null;
        }
        if (PolyStringType.class.equals(propertyModel.getObjectClass())){
            MidPointApplication application = MidPointApplication.get();
            return Model.of(WebComponentUtil.getTranslatedPolyString((PolyStringType) propertyModel.getObject()));
        }
        return propertyModel;
    }
}
