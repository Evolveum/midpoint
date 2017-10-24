/*
 * Copyright (c) 2010-2015 Evolveum
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

import java.io.Serializable;

import org.apache.commons.lang.Validate;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.util.CloneUtil;
import com.evolveum.midpoint.prism.util.PrismUtil;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.DisplayableValue;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;

/**
 * @author lazyman
 */
public class ValueWrapper<T> implements Serializable, DebugDumpable {

	private static final Trace LOGGER = TraceManager.getTrace(ValueWrapper.class);

    private PropertyOrReferenceWrapper item;
    private PrismValue value;
    private PrismValue oldValue;
//    private PrismPropertyValue<T> value;
//    private PrismPropertyValue<T> oldValue;
    private ValueStatus status;
    private boolean isEditEnabled = true;
    
    public ValueWrapper(PropertyOrReferenceWrapper property, PrismValue value) {
        this(property, value, ValueStatus.NOT_CHANGED);
    }

    public ValueWrapper(PropertyOrReferenceWrapper property, PrismValue value, ValueStatus status) {
        this(property, value, null, status);
    }

    public ValueWrapper(PropertyOrReferenceWrapper property, PrismValue value, PrismValue oldValue,
            ValueStatus status) {
        Validate.notNull(property, "Property wrapper must not be null.");
        Validate.notNull(value, "Property value must not be null.");

        this.item = property;
        this.status = status;

		if (value != null) {
			if (value instanceof PrismPropertyValue) {

				T val = ((PrismPropertyValue<T>) value).getValue();
				if (val instanceof PolyString) {
					PolyString poly = (PolyString) val;
					this.value = new PrismPropertyValue<PolyString>(new PolyString(poly.getOrig(), poly.getNorm()),
							value.getOriginType(), value.getOriginObject());
				} else if (val instanceof ProtectedStringType) {
					this.value = value.clone();
					// prevents
					// "Attempt to encrypt protected data that are already encrypted"
					// when applying resulting delta
					((ProtectedStringType) (((PrismPropertyValue) this.value).getValue()))
							.setEncryptedData(null);
				} else {
					this.value = value.clone();
				}
			} else {
				this.value = value.clone();
			}
		}

		if (oldValue == null && value instanceof PrismPropertyValue && ValueStatus.ADDED == property.getStatus()) {
			oldValue = new PrismPropertyValue<T>(null);
		}
		
		if (oldValue == null && value instanceof PrismReferenceValue && ValueStatus.ADDED == property.getStatus()) {
			oldValue = new PrismReferenceValue();
		}
		
		if (oldValue == null && value instanceof PrismReferenceValue && ValueStatus.ADDED != property.getStatus()) {
			oldValue = value.clone();
		}
		
        if (oldValue == null && value instanceof PrismPropertyValue && ValueStatus.ADDED != property.getStatus()) {
            T val = ((PrismPropertyValue<T>) this.value).getValue();
            if (val instanceof PolyString) {
                PolyString poly = (PolyString)val;
                val = (T) new PolyString(poly.getOrig(), poly.getNorm());
            }
            oldValue = new PrismPropertyValue<T>(CloneUtil.clone(val), this.value.getOriginType(), this.value.getOriginObject());
        }

        this.oldValue = oldValue;
    }
    
    public void setEditEnabled(boolean isEditEnabled) {
		this.isEditEnabled = isEditEnabled;
	}
	
	public boolean isEditEnabled() {
		if (getItem().isDeprecated()) {
			return false;
		}
		return isEditEnabled;
	}
	
    
    public ItemWrapper getItem() {
        return item;
    }

    public ValueStatus getStatus() {
        return status;
    }

    public PrismValue getValue() {
        return value;
    }

    public PrismValue getOldValue() {
        return oldValue;
    }

    public void setStatus(ValueStatus status) {
        this.status = status;
    }

    public void normalize(PrismContext prismContext) {
		if (value instanceof PrismPropertyValue) {
			PrismPropertyValue ppVal = (PrismPropertyValue) value;
			if (ppVal.getValue() instanceof PolyString) {
				PolyString poly = (PolyString) ppVal.getValue();
				if (poly.getOrig() == null) {
					ppVal.setValue((T) new PolyString(""));
				}
				if (prismContext != null){
					PrismUtil.recomputePrismPropertyValue(ppVal, prismContext);
				}

			} else if (ppVal.getValue() instanceof DisplayableValue) {
				DisplayableValue displayableValue = (DisplayableValue) ppVal.getValue();
				ppVal.setValue((T) displayableValue.getValue());
			}
		}
    }

    public boolean hasValueChanged() {
    	if (value instanceof PrismPropertyValue) {
    		return oldValue != null ? !oldValue.equals(value) : value != null;
    	} else {
    		return oldValue != null ? !oldValue.equals(value) : value != null && !value.isEmpty();
    	}
        
    }

    public boolean isReadonly() {
        return item.isReadonly();
    }

    public boolean isEmpty() {
    	if (value == null || value.isEmpty()) {
    		return true;
		}
		Object realValue = value.getRealValue();
    	if (realValue instanceof String) {
    		return ((String) realValue).isEmpty();
		} else if (realValue instanceof PolyString) {
    		return ((PolyString) realValue).isEmpty();
		} else {
    		return false;
		}
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

    @Override
	public String debugDump() {
		return debugDump(0);
	}

	@Override
	public String debugDump(int indent) {
		StringBuilder sb = new StringBuilder();
		DebugUtil.indentDebugDump(sb, indent);
		sb.append("ValueWrapper:\n");
		DebugUtil.debugDumpWithLabel(sb, "status", status == null?null:status.toString(), indent+1);
		sb.append("\n");
		DebugUtil.debugDumpWithLabel(sb, "value", value, indent+1);
		sb.append("\n");
		DebugUtil.debugDumpWithLabel(sb, "oldValue", oldValue, indent+1);
		return sb.toString();
	}
}
