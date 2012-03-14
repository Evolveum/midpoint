/*
 * Copyright (c) 2012 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 *
 * Portions Copyrighted 2012 [name of copyright owner]
 */

package com.evolveum.midpoint.schema.xjc;

import com.evolveum.midpoint.prism.*;

import org.apache.commons.lang.Validate;

import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.List;

/**
 * @author lazyman
 */
public final class PrismForJAXBUtil {

    private PrismForJAXBUtil() {
    }

    public static <T> List<T> getPropertyValues(PrismContainer container, QName name, Class<T> clazz) {
        Validate.notNull(container, "Container must not be null.");
        Validate.notNull(name, "QName must not be null.");
        Validate.notNull(clazz, "Class type must not be null.");

        PrismProperty property = container.findOrCreateProperty(name);
        return new PropertyArrayList<T>(property);
    }

    public static <T> T getPropertyValue(PrismContainerValue container, QName name, Class<T> clazz) {
        Validate.notNull(container, "Container must not be null.");
        Validate.notNull(name, "QName must not be null.");
        Validate.notNull(clazz, "Class type must not be null.");

        PrismProperty property = container.findProperty(name);
        return getPropertyValue(property, clazz);
    }

    public static <T> T getPropertyValue(PrismContainer container, QName name, Class<T> clazz) {
        Validate.notNull(container, "Container must not be null.");
        Validate.notNull(name, "QName must not be null.");
        Validate.notNull(clazz, "Class type must not be null.");

        PrismProperty property = container.findProperty(name);
        return getPropertyValue(property, clazz);
    }

    private static <T> T getPropertyValue(PrismProperty property, Class<T> clazz) {
        if (property == null) {
            return null;
        }

        PrismPropertyValue<Object> value = property.getValue();
        if (value == null) {
            return null;
        }

        return (T) value.getValue();
    }

    public static <T> void setPropertyValue(PrismContainerValue container, QName name, T value) {
        Validate.notNull(container, "Container must not be null.");
        Validate.notNull(name, "QName must not be null.");

        PrismProperty property = container.findOrCreateProperty(name);
        property.setValue(new PrismPropertyValue(value));
    }

    public static <T> void setPropertyValue(PrismContainer container, QName name, T value) {
        Validate.notNull(container, "Container must not be null.");
        Validate.notNull(name, "QName must not be null.");

        PrismProperty property = container.findOrCreateProperty(name);
        property.setValue(new PrismPropertyValue(value));
    }

    public static <T> List<T> getPropertyValues(PrismContainerValue container, QName name, Class<T> clazz) {
        Validate.notNull(container, "Container must not be null.");
        Validate.notNull(name, "QName must not be null.");
        Validate.notNull(clazz, "Class type must not be null.");

        PrismProperty property = container.findOrCreateProperty(name);
        return new PropertyArrayList<T>(property);
    }

    public static <T extends Containerable> PrismContainerValue<T> getFieldContainerValue(PrismContainer<?> parent, QName fieldName) {
        Validate.notNull(parent, "Container must not be null.");
        Validate.notNull(fieldName, "Field QName must not be null.");

        return getFieldContainerValue(parent.getValue(), fieldName);
    }

    public static <T extends Containerable> PrismContainerValue<T> getFieldContainerValue(PrismContainerValue<?> parent, QName fieldName) {
        Validate.notNull(parent, "Container value must not be null.");
        Validate.notNull(fieldName, "Field QName must not be null.");

        PrismContainer<T> container = parent.findItem(fieldName, PrismContainer.class);
        return container != null ? container.getValue() : null;
    }
    
    public static <T extends Containerable> T getFieldSingleContainerable(PrismContainerValue<?> parent, QName fieldName, Class<T> fieldClass) {
    	PrismContainerValue<T> fieldContainerValue = getFieldContainerValue(parent, fieldName);
    	if (fieldContainerValue == null) {
    		return null;
    	}
    	return fieldContainerValue.asContainerable(fieldClass);
    }

    public static <T extends PrismContainer<?>> T getContainer(PrismContainer<?> parent, QName name, Class<T> clazz) {
        Validate.notNull(parent, "Container must not be null.");
        Validate.notNull(name, "QName must not be null.");
        Validate.notNull(clazz, "Class type must not be null.");

        return parent.findItem(name, clazz);
    }

    public static <T extends Containerable> boolean setFieldContainerValue(PrismContainerValue<?> parent, QName fieldName, PrismContainerValue<T> fieldContainerValue) {
        Validate.notNull(parent, "Prism container value must not be null.");
        Validate.notNull(fieldName, "QName must not be null.");

        if (fieldContainerValue == null) {
            PrismContainer<T> container = parent.findOrCreateContainer(fieldName);
            if (container != null) {
                container.clear();
            }
        } else {
            PrismContainer<T> newValue = new PrismContainer<T>(fieldName);
            newValue.add(fieldContainerValue);
            if (parent.getContainer() == null) {
                parent.getItems().add(newValue);
            } else {
                parent.getContainer().getValue().addReplaceExisting(newValue);
            }
        }

        return true;
    }

    public static boolean setFieldContainerValue(PrismContainer<?> parent, QName fieldName, PrismContainerValue<?> fieldContainerValue) {
        return setFieldContainerValue(parent.getValue(), fieldName, fieldContainerValue);
    }

    public static PrismReferenceValue getReferenceValue(PrismContainerValue<?> parent, QName name) {
        Validate.notNull(parent, "Prism container value must not be null.");
        Validate.notNull(name, "QName must not be null.");

        PrismReference reference = parent.findItem(name, PrismReference.class);
        return reference != null ? reference.getValue() : null;
    }

    public static PrismReferenceValue getReferenceValue(PrismContainer parent, QName name) {
        Validate.notNull(parent, "Prism container must not be null.");
        Validate.notNull(name, "QName must not be null.");

        PrismReference reference = getReference(parent, name);
        return reference != null ? reference.getValue() : null;
    }

    public static PrismReference getReference(PrismContainer parent, QName name) {
        Validate.notNull(parent, "Prism container must not be null.");
        Validate.notNull(name, "QName must not be null.");

        return parent.findReference(name);
    }

    public static void setReferenceValue(PrismContainerValue<?> parent, QName name,
            PrismReferenceValue value) {
        Validate.notNull(parent, "Prism container value must not be null.");
        Validate.notNull(name, "QName must not be null.");

        PrismReference reference = parent.findOrCreateItem(name, PrismReference.class);
        if (reference != null) {
            if (value == null) {
            	if (reference.getValue() != null) {
            		reference.getValue().setObject(null);
            	}
	        } else {
	        	if (reference.getValue() == null) {
	        		reference.add(new PrismReferenceValue());
	        	}
	            reference.getValue().setObject(value.getObject());
	        }
        }
    }

    public static void setReferenceValue(PrismContainer parent, QName name, PrismReferenceValue value) {
        setReferenceValue(parent.getValue(), name, value);
    }

    // Assumes single-value reference
    public static void setReferenceObject(PrismContainerValue parentValue, QName referenceQName, PrismObject targetObject) {
        Validate.notNull(parentValue, "Prism container value must not be null.");
        Validate.notNull(referenceQName, "QName must not be null.");

        PrismReference reference = parentValue.findOrCreateReference(referenceQName);
        if (reference == null) {
        	throw new IllegalArgumentException("No reference "+referenceQName+" in "+parentValue);
        }
        PrismReferenceValue referenceValue = reference.getValue();
        if (referenceValue == null) {
        	referenceValue = new PrismReferenceValue();
        	reference.add(referenceValue);
        }
        referenceValue.setObject(targetObject);
    }

    // Assumes single-value reference
    public static void setReferenceObject(PrismContainer parent, QName referenceQName, PrismObject targetObject) {
        setReferenceObject(parent.getValue(), referenceQName, targetObject);
    }
    
    public static <T extends Objectable> PrismReferenceValue objectableAsReferenceValue(T objectable, PrismReference reference ) {
    	PrismObject<T> object = objectable.asPrismObject();
        for (PrismReferenceValue refValue: reference.getValues()) {
            if (object == refValue.getObject()) {
                return refValue;
            }
        }
        PrismReferenceValue referenceValue = new PrismReferenceValue();
        referenceValue.setObject(object);
        return referenceValue;
    }

    public static <T extends Containerable> List<PrismContainerValue<T>> getContainerValues(PrismContainerValue<T> parent, QName name, Class<T> clazz) {
        return getContainerValues(parent.getContainer(), name, clazz);
    }

    public static <T extends Containerable> List<PrismContainerValue<T>> getContainerValues(PrismContainer<T> parent, QName name, Class<T> clazz) {
        Validate.notNull(parent, "Container must not be null.");
        Validate.notNull(name, "QName must not be null.");
        
        PrismContainer container = parent.findOrCreateContainer(name);
        return container.getValues();
    }
    
    public static <T> List<T> getAny(PrismContainerValue value, Class<T> clazz) {
    	return new AnyArrayList(value);
    }
}
