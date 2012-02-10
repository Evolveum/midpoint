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

package com.evolveum.midpoint.prism;

import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.schema.exception.SchemaException;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.JAXBUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;
import java.util.*;


/**
 * Property is a specific characteristic of an object. It may be considered
 * object "attribute" or "field". For example User has fullName property that
 * contains string value of user's full name.
 * <p/>
 * Properties may be single-valued or multi-valued
 * <p/>
 * Properties may contain primitive types or complex types (defined by XSD
 * schema)
 * <p/>
 * Property values are unordered, implementation may change the order of values
 * <p/>
 * Duplicate values of properties should be silently removed by implementations,
 * but clients must be able tolerate presence of duplicate values.
 * <p/>
 * Operations that modify the objects work with the granularity of properties.
 * They add/remove/replace the values of properties, but do not "see" inside the
 * property.
 * <p/>
 * Property is mutable.
 *
 * @author Radovan Semancik
 */
public class PrismProperty extends Item {

    private Set<PrismPropertyValue<Object>> values = new HashSet<PrismPropertyValue<Object>>();

    private static final Trace LOGGER = TraceManager.getTrace(PrismProperty.class);

//    public Property() {
//        super();
//    }
//
//    public Property(QName name) {
//        super(name);
//    }
//
//    public Property(QName name, PropertyDefinition definition) {
//        super(name, definition);
//    }
//
//    public Property(QName name, PropertyDefinition definition, Set<PropertyValue<Object>> values) {
//        super(name, definition);
//        if (values != null) {
//            this.values = values;
//        }
//    }

    public PrismProperty(QName name, PrismPropertyDefinition definition, PrismContext prismContext) {
        super(name, definition, prismContext);
    }

    /**
     * Returns applicable property definition.
     * <p/>
     * May return null if no definition is applicable or the definition is not
     * know.
     *
     * @return applicable property definition
     */
    public PrismPropertyDefinition getDefinition() {
        return (PrismPropertyDefinition) definition;
    }

    /**
     * Sets applicable property definition.
     *
     * @param definition the definition to set
     */
    public void setDefinition(PrismPropertyDefinition definition) {
        this.definition = definition;
    }

    /**
     * Returns property values.
     * <p/>
     * The values are returned as set. The order of values is not significant.
     *
     * @return property values
     */
    public Set<PrismPropertyValue<Object>> getValues() {
        return values;
    }

    /**
     * Returns property values.
     * <p/>
     * The values are returned as set. The order of values is not significant.
     * <p/>
     * The values are cast to the "T" java type.
     *
     * @param <T> Target class for property values
     * @param T   Target class for property values
     * @return property values
     * @throws ClassCastException if the values cannot be cast to "T"
     */
    @SuppressWarnings("unchecked")
    public <T> Set<PrismPropertyValue<T>> getValues(Class<T> T) {
        return (Set) values;
    }

	public <T> Collection<T> getRealValues(Class<T> type) {
		Collection<T> realValues = new ArrayList<T>(values.size());
		for (PrismPropertyValue<Object> pValue: values) {
			realValues.add((T) pValue.getValue());
		}
		return realValues;
	}


    /**
     * Returns value of a single-valued property.
     * <p/>
     * The value is cast to the "T" java type.
     *
     * @param <T> Target class for property values
     * @param T   Target class for property values
     * @return value of a single-valued property
     * @throws ClassCastException
     * @throws IllegalStateException more than one value is present
     */
    @SuppressWarnings("unchecked")
    public <T> PrismPropertyValue<T> getValue(Class<T> T) {
        // TODO: check schema definition if available
        if (values.size() > 1) {
            throw new IllegalStateException("Attempt to get single value from property " + name
                    + " with multiple values");
        }
        if (values.isEmpty()) {
            return null;
        }
        PrismPropertyValue<Object> o = values.iterator().next();
        if (o == null) {
            return null;
        }
        return (PrismPropertyValue<T>) o;
    }

    public PrismPropertyValue<Object> getValue() {
        if (values.size() > 1) {
            throw new IllegalStateException("Attempt to get single value from property " + name
                    + " with multiple values");
        }
        if (values.isEmpty()) {
            return null;
        }

        return values.iterator().next();
    }

    /**
     * Means as a short-hand for setting just a value for single-valued
     * attributes.
     * Will remove all existing values.
     * TODO
     */
    public void setValue(PrismPropertyValue value) {
        this.values.clear();
        this.values.add(value);
    }

    public void addValues(Collection<PrismPropertyValue<Object>> pValuesToAdd) {
    	for (PrismPropertyValue<Object> pValue: pValuesToAdd) {
    		addValue(pValue);
    	}
    }

    public void addValue(PrismPropertyValue<Object> pValueToAdd) {
    	Iterator<PrismPropertyValue<Object>> iterator = this.values.iterator();
    	while (iterator.hasNext()) {
    		PrismPropertyValue<Object> pValue = iterator.next();
    		if (pValue.equalsRealValue(pValueToAdd)) {
    			LOGGER.warn("Adding value to property "+getName()+" that already exists (overwriting), value: "+pValueToAdd);
    			iterator.remove();
    		}
    	}
    	this.values.add(pValueToAdd);
    }

    public boolean deleteValues(Collection<PrismPropertyValue<Object>> pValuesToDelete) {
        boolean changed = false;
    	for (PrismPropertyValue<Object> pValue: pValuesToDelete) {
            if (!changed) {
    		    changed = deleteValue(pValue);
            } else {
                deleteValue(pValue);
            }
    	}
        return changed;
    }

    public boolean deleteValue(PrismPropertyValue<Object> pValueToDelete) {
    	Iterator<PrismPropertyValue<Object>> iterator = this.values.iterator();
    	boolean found = false;
    	while (iterator.hasNext()) {
    		PrismPropertyValue<Object> pValue = iterator.next();
    		if (pValue.equalsRealValue(pValueToDelete)) {
    			iterator.remove();
    			found = true;
    		}
    	}
    	if (!found) {
    		LOGGER.warn("Deleting value of property "+getName()+" that does not exist (skipping), value: "+pValueToDelete);
    	}

        return found;
    }

    public void replaceValues(Collection<PrismPropertyValue<Object>> valuesToReplace) {
        this.values.clear();
        addValues(valuesToReplace);
    }

    public boolean hasValue(PrismPropertyValue<Object> value) {
        return values.contains(value);
    }

    public boolean hasRealValue(PrismPropertyValue<Object> value) {
        for (PrismPropertyValue<Object> propVal : values) {
            if (propVal.equalsRealValue(value)) {
                return true;
            }
        }

        return false;
    }

    public boolean isEmpty() {
        return (values == null || values.isEmpty());
    }

//    @Override
//    public void serializeToDom(Node parentNode) throws SchemaException {
//        serializeToDom(parentNode, null, null, false);
//    }

    public void serializeToDom(Node parentNode, PrismPropertyDefinition propDef, Set<PrismPropertyValue<Object>> alternateValues,
            boolean recordType) throws SchemaException {

        if (propDef == null) {
            propDef = getDefinition();
        }

        Set<PrismPropertyValue<Object>> serializeValues = getValues();
        if (alternateValues != null) {
            serializeValues = alternateValues;
        }

        for (PrismPropertyValue<Object> val : serializeValues) {
            // If we have a definition then try to use it. The conversion may be more realiable
            // Otherwise the conversion will be governed by Java type
            QName xsdType = null;
            if (propDef != null) {
                xsdType = propDef.getTypeName();
            }
            try {
                XmlTypeConverter.appendBelowNode(val.getValue(), xsdType, getName(), parentNode, recordType);
            } catch (SchemaException e) {
                throw new SchemaException(e.getMessage() + ", while converting " + propDef.getTypeName(), e);
            }
        }
    }

    /**
     * Serializes property to DOM or JAXB element(s).
     * <p/>
     * The property name will be used as an element QName.
     * The values will be in the element content. Single-value
     * properties will produce one element (on none), multi-valued
     * properies may produce several elements. All of the elements will
     * have the same QName.
     * <p/>
     * The property must have a definition (getDefinition() must not
     * return null).
     *
     * @param doc DOM Document
     * @return property serialized to DOM Element or JAXBElement
     * @throws SchemaException No definition or inconsistent definition
     */
    public List<Object> serializeToJaxb(Document doc) throws SchemaException {
        return serializeToJaxb(doc, null, null, false);
    }

    /**
     * Same as serializeToDom(Document doc) but allows external definition.
     * <p/>
     * Package-private. Useful for some internal calls inside schema processor.
     */
    List<Object> serializeToJaxb(Document doc, PrismPropertyDefinition propDef) throws SchemaException {
        // No need to record types, we have schema definition here
        return serializeToJaxb(doc, propDef, null, false);
    }

    /**
     * Same as serializeToDom(Document doc) but allows external definition.
     * <p/>
     * Allows alternate values.
     * Allows option to record type in the serialized output (using xsi:type)
     * <p/>
     * Package-private. Useful for some internal calls inside schema processor.
     */
    List<Object> serializeToJaxb(Document doc, PrismPropertyDefinition propDef, Set<PrismPropertyValue<Object>> alternateValues,
            boolean recordType) throws SchemaException {


        // Try to locate definition
        List<Object> elements = new ArrayList<Object>();

        //check if the property has value..if not, return empty elemnts list..

        if (propDef == null) {
            propDef = getDefinition();
        }

        Set<PrismPropertyValue<Object>> serializeValues = getValues();
        if (alternateValues != null) {
            serializeValues = alternateValues;
        }


        for (PrismPropertyValue<Object> val : serializeValues) {
            // If we have a definition then try to use it. The conversion may be more realiable
            // Otherwise the conversion will be governed by Java type
            QName xsdType = null;
            if (propDef != null) {
                xsdType = propDef.getTypeName();
                //FIXME: we do not want to send ignored attribute to the other layers..
                //but this place is maybe not suitable to skip the ignored property..
                if (propDef.isIgnored()) {
                    continue;
                }
            }

            try {
                elements.add(XmlTypeConverter.toXsdElement(val.getValue(), xsdType, getName(), doc, recordType));
            } catch (SchemaException e) {
                throw new SchemaException(e.getMessage() + ", while converting " + propDef.getTypeName(), e);
            }

        }
        return elements;
    }

    @Override
    public PrismProperty clone() {
        PrismProperty clone = new PrismProperty(getName(), getDefinition(), prismContext);
        copyValues(clone);
        return clone;
    }

    protected void copyValues(PrismProperty clone) {
        super.copyValues(clone);
        clone.values = new HashSet<PrismPropertyValue<Object>>();
//        for (PropertyValue<Object> value : values) {
//            clone.values.add(value.clone());
//        }
        clone.values.addAll(values);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ((values == null) ? 0 : values.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!super.equals(obj))
            return false;
        if (getClass() != obj.getClass())
            return false;
        PrismProperty other = (PrismProperty) obj;
        if (values == null) {
            if (other.values != null)
                return false;
        } else if (!values.equals(other.values))
            return false;
        return true;
    }


    /**
     * This method compares "this" property with other property. Comparing only real values wrapped in
     * {@link PrismPropertyValue}.
     *
     * @param other can be null, property delta will be add all values from "this" property.
     * @return The result is {@link PropertyDelta} which represents differences between them. That means when
     *         resulting property delta is applied on other property then other property and "this" property
     *         will be equal.
     */
    public PropertyDelta compareRealValuesTo(PrismProperty other) {
        return compareTo(other, true);
    }

    /**
     * This method compares "this" property with other property. Comparing property values as whole.
     *
     * @param other can be null, property delta will be add all values from "this" property.
     * @return The result is {@link PropertyDelta} which represents differences between them. That means when
     *         resulting property delta is applied on other property then other property and "this" property
     *         will be equal.
     */
    public PropertyDelta compareTo(PrismProperty other) {
        return compareTo(other, false);
    }

    private PropertyDelta compareTo(PrismProperty other, boolean compareReal) {
        PropertyDelta delta = null; //new PropertyDelta(getPath());

        PrismPropertyDefinition def = getDefinition();

        if (other != null) {
            for (PrismPropertyValue<Object> value : getValues()) {
                if ((!compareReal && !other.hasValue(value))
                        || (compareReal && !other.hasRealValue(value))) {
                    delta.addValueToDelete(value.clone());
                }
            }
            for (PrismPropertyValue<Object> otherValue : other.getValues()) {
                if ((!compareReal && !hasValue(otherValue))
                        || (compareReal && !hasRealValue(otherValue))) {
                    delta.addValueToAdd(otherValue.clone());
                }
            }
            if (def != null && def.isSingleValue() && !delta.isEmpty()) {
            	// Drop the current delta (it was used only to detect that something has changed
            	// Generate replace delta instead of add/delete delta
            	// FIXME delta = new PropertyDelta(getPath());
        		Collection<PrismPropertyValue<Object>> replaceValues = new ArrayList<PrismPropertyValue<Object>>(other.getValues().size());
                for (PrismPropertyValue<Object> value : other.getValues()) {
                	replaceValues.add(value.clone());
                }
    			delta.setValuesToReplace(replaceValues);
    			return delta;
            }
        } else {
        	//other doesn't exist, so delta means delete all values
            for (PrismPropertyValue<Object> value : getValues()) {
                delta.addValueToDelete(value.clone());
            }
        }

        return delta;
    }

	@Override
    public String toString() {
        return getClass().getSimpleName() + "(" + DebugUtil.prettyPrint(getName()) + "):" + getValues();
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < indent; i++) {
            sb.append(INDENT_STRING);
        }
        sb.append(getDebugDumpClassName()).append(": ").append(DebugUtil.prettyPrint(getName())).append(" = ");
        if (getValues() == null) {
            sb.append("null");
        } else {
            sb.append("[ ");
            for (Object value : getValues()) {
                sb.append(DebugUtil.prettyPrint(value));
                sb.append(", ");
            }
            sb.append(" ]");
        }
        if (getDefinition() != null) {
            sb.append(" def");
        }
        return sb.toString();
    }

    /**
     * Return a human readable name of this class suitable for logs.
     */
    @Override
    protected String getDebugDumpClassName() {
        return "PP";
    }

}
