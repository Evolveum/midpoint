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

package com.evolveum.midpoint.prism.xjc;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.xnode.MapXNode;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.prism.xml.ns._public.query_3.SearchFilterType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

import org.apache.commons.lang.Validate;
import org.w3c.dom.Element;

import javax.xml.bind.annotation.XmlAnyElement;
import javax.xml.namespace.QName;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * @author lazyman
 */
public final class PrismForJAXBUtil {

    private PrismForJAXBUtil() {
    }

    public static <T> T getPropertyValue(PrismContainerValue container, QName name, Class<T> clazz) {
        Validate.notNull(container, "Container must not be null.");
        Validate.notNull(name, "QName must not be null.");
        Validate.notNull(clazz, "Class type must not be null.");

        PrismProperty property = container.findProperty(name);
        return getPropertyValue(property, clazz);
    }

    private static <T> T getPropertyValue(PrismProperty<?> property, Class<T> requestedType) {
        if (property == null) {
            return null;
        }

        PrismPropertyValue<?> pvalue = property.getValue();
        if (pvalue == null) {
            return null;
        }
        
        Object propertyRealValue = pvalue.getValue();
        
        if (propertyRealValue instanceof Element) {
        	if (requestedType.isAssignableFrom(Element.class)) {
        		return (T) propertyRealValue;
        	}
        	Field anyField = getAnyField(requestedType);
        	if (anyField == null) {
        		throw new IllegalArgumentException("Attempt to read raw property "+property+" while the requested class ("+requestedType+") does not have 'any' field");
        	}
        	anyField.setAccessible(true);
        	Collection<?> anyElementList = property.getRealValues();
        	T requestedTypeInstance;
			try {
				requestedTypeInstance = requestedType.newInstance();
				anyField.set(requestedTypeInstance, anyElementList);
			} catch (InstantiationException e) {
				throw new IllegalArgumentException("Instantiate error while reading raw property "+property+", requested class ("+requestedType+"):"
						+e.getMessage(), e);
			} catch (IllegalAccessException e) {
				throw new IllegalArgumentException("Illegal access error while reading raw property "+property+", requested class ("+requestedType+")"
						+", field "+anyField+": "+e.getMessage(), e);
			}
			return requestedTypeInstance;
        }
        
        return JaxbTypeConverter.mapPropertyRealValueToJaxb(propertyRealValue);
    }
    
    private static <T> Field getAnyField(Class<T> clazz) {
    	for (Field field: clazz.getDeclaredFields()) {
    		XmlAnyElement xmlAnyElementAnnotation = field.getAnnotation(XmlAnyElement.class);
    		if (xmlAnyElementAnnotation != null) {
    			return field;
    		}
    	}
    	return null;
    }
    
    public static <T> List<T> getPropertyValues(PrismContainerValue<?> container, QName name, Class<T> clazz) {
        Validate.notNull(container, "Container must not be null.");
        Validate.notNull(name, "QName must not be null.");
        Validate.notNull(clazz, "Class type must not be null.");

		PrismProperty<?> property;
		try {
			property = container.findProperty(name);
			if (property == null) {
				property = container.createDetachedSubItem(name, PrismProperty.class, null, container.isImmutable());
			}
		} catch (SchemaException e) {
			// This should not happen. Code generator and compiler should take care of that.
			throw new IllegalStateException("Internal schema error: "+e.getMessage(),e);
		}
        return new PropertyArrayList<>(property, container);
    }

    
    public static <T> void setPropertyValue(PrismContainerValue<?> container, QName name, T value) {
        Validate.notNull(container, "Container must not be null.");
        Validate.notNull(name, "QName must not be null.");

        if (value == null) {
        	container.removeProperty(name);
        } else {
	        PrismProperty<?> property;
			try {
				property = container.findOrCreateProperty(name);
			} catch (SchemaException e) {
				// This should not happen. Code generator and compiler should take care of that.
				throw new IllegalStateException("Internal schema error: "+e.getMessage(),e);
			}
	    	Object propertyRealValue = JaxbTypeConverter.mapJaxbToPropertyRealValue(value);
	    	if (propertyRealValue == null) {
	    		container.removeProperty(name);
	    	} else {
	    		property.setValue(new PrismPropertyValue(propertyRealValue));
	    	}
        }
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

    public static <T extends PrismContainer<?>> T getContainer(PrismContainerValue parentValue, QName name) {
        Validate.notNull(parentValue, "Parent container value must not be null.");
        Validate.notNull(name, "QName must not be null.");

        try {
			PrismContainer container = parentValue.findContainer(name);
			if (container != null) {
				return (T) container;
			} else {
				return (T) parentValue.createDetachedSubItem(name, PrismContainer.class, null, parentValue.isImmutable());
			}
        } catch (SchemaException ex) {
            throw new SystemException(ex.getMessage(),  ex);
        }
    }

    public static PrismContainer<?> createContainer(PrismContainerValue parentValue, QName name) {
    	return createItem(parentValue, name, PrismContainer.class);
	}

    public static PrismReference createReference(PrismContainerValue parentValue, QName name) {
    	return createItem(parentValue, name, PrismReference.class);
	}

    public static PrismProperty<?> createProperty(PrismContainerValue parentValue, QName name) {
    	return createItem(parentValue, name, PrismProperty.class);
	}

    public static <IV extends PrismValue,ID extends ItemDefinition,I extends Item<IV,ID>> I createItem(PrismContainerValue parentValue, QName name, Class<I> type) {
        Validate.notNull(parentValue, "Parent container value must not be null.");
        Validate.notNull(name, "QName must not be null.");
        try {
			return (I) parentValue.findOrCreateItem(name, type);
        } catch (SchemaException ex) {
            throw new SystemException(ex.getMessage(),  ex);
        }
    }

    public static <T extends Containerable> boolean setFieldContainerValue(PrismContainerValue<?> parent, QName fieldName,
    		PrismContainerValue<T> fieldContainerValue) {
        Validate.notNull(parent, "Prism container value must not be null.");
        Validate.notNull(fieldName, "QName must not be null.");

        try {
	        PrismContainer<T> fieldContainer = null;
	        if (fieldContainerValue == null) {
	        	parent.removeContainer(fieldName);
	        } else {
	        	if (fieldContainerValue.getParent() != null && fieldContainerValue.getParent() != parent) {
	        		// This value is already part of another prism. We need to clone it to add it here.
	        		fieldContainerValue = fieldContainerValue.clone();
	        	}
	            fieldContainer = new PrismContainer<T>(fieldName, parent.getPrismContext());
	            fieldContainer.add(fieldContainerValue);
	            if (parent.getParent() == null) {
	                parent.add(fieldContainer);			// TODO what if fieldContainer is already there?
	            } else {
                    parent.addReplaceExisting(fieldContainer);
	            }
	        }
//	        // Make sure that the definition from parent is applied to new field container
//	        if (fieldContainer.getDefinition() == null) {
//	        	PrismContainer<?> parentContainer = parent.getContainer();
//	        	if (parentContainer != null) {
//		        	PrismContainerDefinition<?> parentDefinition = parentContainer.getDefinition();
//		        	if (parentDefinition != null) {
//		        		PrismContainerDefinition<T> fieldDefinition = parentDefinition.findContainerDefinition(fieldName);
//		        		fieldContainer.setDefinition(fieldDefinition);
//		        	}
//	        	}
//	        }
        } catch (SchemaException e) {
        	// This should not happen. Code generator and compiler should take care of that.
			throw new IllegalStateException("Internal schema error: "+e.getMessage(),e);
        }
        return true;
    }

    public static PrismReferenceValue getReferenceValue(PrismContainerValue<?> parent, QName name) {
        Validate.notNull(parent, "Prism container value must not be null.");
        Validate.notNull(name, "QName must not be null.");

        PrismReference reference = parent.findItem(name, PrismReference.class);
        return reference != null ? reference.getValue() : null;
    }

    /**
     * This method must merge new value with potential existing value of the reference.
     * E.g. it is possible to call setResource(..) and then setResourceRef(..) with the
     * same OID. In that case the result should be one reference that has both OID/type/filter
     * and object.
     * Assumes single-value reference
     */
    public static void setReferenceValueAsRef(PrismContainerValue<?> parentValue, QName referenceName,
            PrismReferenceValue value) {
        Validate.notNull(parentValue, "Prism container value must not be null.");
        Validate.notNull(referenceName, "QName must not be null.");

        PrismReference reference;
		try {
			reference = parentValue.findOrCreateItem(referenceName, PrismReference.class);
		} catch (SchemaException e) {
			// This should not happen. Code generator and compiler should take care of that.
			throw new IllegalStateException("Internal schema error: "+e.getMessage(),e);
		}
		if (reference == null) {
        	throw new IllegalArgumentException("No reference "+referenceName+" in "+parentValue);
        }
        if (value == null) {
            parentValue.remove(reference);
        } else {
            if (reference.isEmpty()) {
                if (value.getParent() != null) {
                    value = value.clone();
                }
                reference.add(value);
            } else {
                reference.getValue().setOid(value.getOid());
                reference.getValue().setTargetType(value.getTargetType());
                reference.getValue().setFilter(value.getFilter());
                reference.getValue().setDescription(value.getDescription());
            }
        }
    }

    /**
     * This method must merge new value with potential existing value of the reference.
     * E.g. it is possible to call setResource(..) and then setResourceRef(..) with the
     * same OID. In that case the result should be one reference that has both OID/type/filter
     * and object.
     * Assumes single-value reference
     */
    public static void setReferenceValueAsObject(PrismContainerValue parentValue, QName referenceQName, PrismObject targetObject) {
        Validate.notNull(parentValue, "Prism container value must not be null.");
        Validate.notNull(referenceQName, "QName must not be null.");

        PrismReference reference;
		try {
			reference = parentValue.findOrCreateReference(referenceQName);
		} catch (SchemaException e) {
			// This should not happen. Code generator and compiler should take care of that.
			throw new IllegalStateException("Internal schema error: "+e.getMessage(),e);
		}
        if (reference == null) {
        	throw new IllegalArgumentException("No reference "+referenceQName+" in "+parentValue);
        }
        PrismReferenceValue referenceValue = reference.getValue();
        referenceValue.setObject(targetObject);
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

    public static <T> List<T> getAny(PrismContainerValue value, Class<T> clazz) {
    	return new AnyArrayList(value);
    }

	public static PrismObject setupContainerValue(PrismObject prismObject, PrismContainerValue containerValue) {
		PrismContainerable parent = containerValue.getParent();
		if (parent != null && parent instanceof PrismObject) {
			return (PrismObject)parent;
		}
		try {
			prismObject.setValue(containerValue);
			return prismObject;
		} catch (SchemaException e) {
			// This should not happen. Code generator and compiler should take care of that.
			throw new IllegalStateException("Internal schema error: "+e.getMessage(),e);
		}
	}

	public static PrismReference getReference(PrismContainerValue parent, QName fieldName) {
		try {
			PrismReference reference = parent.findReference(fieldName);
			if (reference != null) {
				return reference;
			} else {
				return (PrismReference) parent.createDetachedSubItem(fieldName, PrismReference.class, null, parent.isImmutable());
			}
		} catch (SchemaException e) {
			// This should not happen. Code generator and compiler should take care of that.
			throw new IllegalStateException("Internal schema error: "+e.getMessage(),e);
		}
	}

    public static void setReferenceFilterClauseXNode(PrismReferenceValue rval, SearchFilterType filterType) {
        if (filterType != null) {
            rval.setFilter(filterType.clone());
        } else {
            rval.setFilter(null);
        }
    }

    public static MapXNode getReferenceFilterClauseXNode(PrismReferenceValue rval) {
        SearchFilterType filter = rval.getFilter();
        if (filter == null || !filter.containsFilterClause()) {
            return null;
        }
        return filter.getFilterClauseXNode();
    }
    
    public static PolyStringType getReferenceTargetName(PrismReferenceValue rval) {
        PolyString targetName = rval.getTargetName();
        if (targetName == null) {
            return null;
        }
        return new PolyStringType(targetName);
    }
    
    public static void setReferenceTargetName(PrismReferenceValue rval, PolyStringType name) {
        if (name == null) {
        	rval.setTargetName((PolyString) null);
        } else {
        	rval.setTargetName(name.toPolyString());
        }
    }

}
