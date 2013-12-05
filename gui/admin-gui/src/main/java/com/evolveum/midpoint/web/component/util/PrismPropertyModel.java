/*
 * Copyright (c) 2010-2013 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.web.component.util;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.page.error.PageError;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectType;
import org.apache.commons.lang.Validate;
import org.apache.wicket.RestartResponseException;
import org.apache.wicket.model.IModel;

import javax.xml.namespace.QName;

/**
 * Simple implementation, now it can't handle multivalue properties.
 *
 * @author lazyman
 */
public class PrismPropertyModel<T extends ObjectType> implements IModel {

    private static final Trace LOGGER = TraceManager.getTrace(PrismPropertyModel.class);

    private IModel<PrismObject<T>> model;
    private ItemPath path;

    private boolean multivalue;
    private PrismPropertyList values;

    public PrismPropertyModel(IModel<PrismObject<T>> model, QName item) {
        this(model, new ItemPath(item), false);
    }

    public PrismPropertyModel(IModel<PrismObject<T>> model, ItemPath path) {
        this(model, path, false);
    }

    public PrismPropertyModel(IModel<PrismObject<T>> model, QName item, boolean multivalue) {
        this(model, new ItemPath(item), multivalue);
    }

    public PrismPropertyModel(IModel<PrismObject<T>> model, ItemPath path, boolean multivalue) {
        Validate.notNull(model, "Prism object model must not be null.");
        Validate.notNull(path, "Item path must not be null.");

        this.model = model;
        this.path = path;
        this.multivalue = multivalue;
    }

    @Override
    public Object getObject() {
        PrismObject object = model.getObject();
        PrismProperty property;
        try {
            property = object.findOrCreateProperty(path);
        } catch (SchemaException ex) {
            LoggingUtils.logException(LOGGER, "Couldn't create property in path {}", ex, path);
            //todo show message in page error [lazyman]
            throw new RestartResponseException(PageError.class);
        }

        if (multivalue) {
            if (values == null) {
                values = new PrismPropertyList(property);
            }
            return values;
        }

        return getRealValue(property != null ? property.getRealValue() : null);
    }

    @Override
    public void setObject(Object object) {
        try {
            PrismObject obj = model.getObject();
            PrismProperty property = obj.findOrCreateProperty(path);

            if (object != null) {
                PrismPropertyDefinition def = property.getDefinition();
                if (PolyString.class.equals(def.getTypeClass())) {
                    object = new PolyString((String) object);
                }

                property.setValue(new PrismPropertyValue(object, OriginType.USER_ACTION, null));
            } else {
                PrismContainerValue parent = (PrismContainerValue) property.getParent();
                parent.remove(property);
            }
        } catch (Exception ex) {
            LoggingUtils.logException(LOGGER, "Couldn't update prism property model", ex);
        }
    }

    @Override
    public void detach() {
    }

    private Object getRealValue(Object value) {
        if (value instanceof PolyString) {
            value = ((PolyString) value).getOrig();
        }

        return value;
    }
}
