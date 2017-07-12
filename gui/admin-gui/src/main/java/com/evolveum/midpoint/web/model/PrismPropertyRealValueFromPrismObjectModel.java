/*
 * Copyright (c) 2010-2017 Evolveum
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

package com.evolveum.midpoint.web.model;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.page.error.PageError;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

import org.apache.commons.lang.Validate;
import org.apache.wicket.RestartResponseException;
import org.apache.wicket.model.IModel;

import javax.xml.namespace.QName;

/**
 * Model that returns property real values. This implementation works on PrismObject models (not wrappers).
 * 
 * Simple implementation, now it can't handle multivalue properties.
 * 
 * @author lazyman
 * @author semancik
 */
@Deprecated     // use combination of PrismPropertyRealValueFromContainerableModel and ContainerableFromPrismObjectModel instead
public class PrismPropertyRealValueFromPrismObjectModel<T,O extends ObjectType> implements IModel<T> {

    private static final Trace LOGGER = TraceManager.getTrace(PrismPropertyRealValueFromPrismObjectModel.class);

    private IModel<PrismObject<O>> model;
    private ItemPath path;

    public PrismPropertyRealValueFromPrismObjectModel(IModel<PrismObject<O>> model, QName item) {
        this(model, new ItemPath(item));
    }

    public PrismPropertyRealValueFromPrismObjectModel(IModel<PrismObject<O>> model, ItemPath path) {
        Validate.notNull(model, "Prism object model must not be null.");
        Validate.notNull(path, "Item path must not be null.");

        this.model = model;
        this.path = path;
    }

    @Override
    public T getObject() {
        PrismObject<O> object = model.getObject();
        PrismProperty<T> property;
        try {
            property = object.findOrCreateProperty(path);
        } catch (SchemaException ex) {
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't create property in path {}", ex, path);
            //todo show message in page error [lazyman]
            throw new RestartResponseException(PageError.class);
        }

        return getRealValue(property != null ? property.getRealValue() : null);
    }

    @Override
    public void setObject(T object) {
        try {
            PrismObject<O> obj = model.getObject();
            PrismProperty<T> property = obj.findOrCreateProperty(path);

            if (object != null) {
                PrismPropertyDefinition<T> def = property.getDefinition();
                if (PolyString.class.equals(def.getTypeClass())) {
                    object = (T) new PolyString((String) object);
                }

                property.setValue(new PrismPropertyValue<T>(object, OriginType.USER_ACTION, null));
            } else {
                PrismContainerValue parent = (PrismContainerValue) property.getParent();
                parent.remove(property);
            }
        } catch (Exception ex) {
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't update prism property model", ex);
        }
    }

    @Override
    public void detach() {
    }

    private T getRealValue(T value) {
        if (value instanceof PolyString) {
            value = (T) ((PolyString) value).getOrig();
        }

        return value;
    }
}
