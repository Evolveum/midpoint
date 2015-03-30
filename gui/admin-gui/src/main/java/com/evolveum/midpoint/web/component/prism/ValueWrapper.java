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

package com.evolveum.midpoint.web.component.prism;

import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.util.DisplayableValue;

import org.apache.commons.lang.Validate;

import java.io.Serializable;

/**
 * @author lazyman
 */
public class ValueWrapper<T> implements Serializable {

    private PropertyWrapper property;
    private PrismPropertyValue<T> value;
    private PrismPropertyValue<T> oldValue;
    private ValueStatus status;

    public ValueWrapper(PropertyWrapper property, PrismPropertyValue<T> value) {
        this(property, value, ValueStatus.NOT_CHANGED);
    }

    public ValueWrapper(PropertyWrapper property, PrismPropertyValue<T> value, ValueStatus status) {
        this(property, value, null, status);
    }

    public ValueWrapper(PropertyWrapper property, PrismPropertyValue<T> value, PrismPropertyValue<T> oldValue,
            ValueStatus status) {
        Validate.notNull(property, "Property wrapper must not be null.");
        Validate.notNull(value, "Property value must not be null.");

        this.property = property;
        this.status = status;
        
        if (value != null) {
            T val = value.getValue();
            if (val instanceof PolyString) {    
                PolyString poly = (PolyString)val;
                this.value = new PrismPropertyValue(new PolyString(poly.getOrig(), poly.getNorm()),
                        value.getOriginType(), value.getOriginObject());
            } else {
                this.value = value.clone();
            }
        }

        if (oldValue == null) {
            T val = this.value.getValue();
            if (val instanceof PolyString) {
                PolyString poly = (PolyString)val;
                val = (T) new PolyString(poly.getOrig(), poly.getNorm());
            }
            oldValue = new PrismPropertyValue<T>(val, this.value.getOriginType(), this.value.getOriginObject());
        }
        this.oldValue = oldValue;
    }

    public PropertyWrapper getProperty() {
        return property;
    }

    public ValueStatus getStatus() {
        return status;
    }

    public PrismPropertyValue<T> getValue() {
        return value;
    }

    public PrismPropertyValue<T> getOldValue() {
        return oldValue;
    }

    public void setStatus(ValueStatus status) {
        this.status = status;
    }

    public void normalize() {
        if (value.getValue() instanceof PolyString)  {
            PolyString poly = (PolyString) value.getValue();
            if (poly.getOrig()==null) {
                value.setValue((T) new PolyString(""));
            }
        } else if (value.getValue() instanceof DisplayableValue){
        	DisplayableValue displayableValue = (DisplayableValue) value.getValue();
        	value.setValue((T) displayableValue.getValue());
        }
    }

    public boolean hasValueChanged() {
        return oldValue != null ? !oldValue.equals(value) : value != null;
    }

    public boolean isReadonly() {
        return property.isReadonly();
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("value: ");
        builder.append(value);
        builder.append(", old value: ");
        builder.append(oldValue);
        builder.append(", status: ");
        builder.append(status);

        return builder.toString();
    }
}
