/*
 * Copyright (c) 2011 Evolveum
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
 * Portions Copyrighted 2011 [name of copyright owner]
 */

package com.evolveum.midpoint.prism;

import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.JAXBUtil;
import com.evolveum.midpoint.util.exception.SchemaException;

import org.w3c.dom.Element;

import javax.xml.namespace.QName;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * Property Definition.
 * <p/>
 * Property is a basic unit of information in midPoint. This class provides
 * definition of property type, multiplicity and so on.
 * <p/>
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
 * This class represents schema definition for property. See {@link Definition}
 * for more details.
 *
 * @author Radovan Semancik
 */
public class PrismPropertyDefinition extends ItemDefinition {

    private static final long serialVersionUID = 7259761997904371009L;
    private QName valueType;
    private Object[] allowedValues;
    private boolean create = true;
    private boolean read = true;
    private boolean update = true;

    public PrismPropertyDefinition(QName name, QName defaultName, QName typeName, PrismContext prismContext) {
        super(name, defaultName, typeName, prismContext);
    }

    /**
     * Returns allowed values for this property.
     *
     * @return Object array. May be null.
     */
    public Object[] getAllowedValues() {
        return allowedValues;
    }

    /**
     * TODO:
     *
     * @return
     */
    public boolean canRead() {
        return read;
    }

    /**
     * TODO:
     *
     * @return
     */
    public boolean canUpdate() {
        return update;
    }

    /**
     *
     */
    public void setReadOnly() {
        create = false;
        read = true;
        update = false;
    }

    /**
     * Returns QName of the property value type.
     * <p/>
     * The returned type is either XSD simple type or complex type. It may not
     * be defined in the same schema (especially if it is standard XSD simple
     * type).
     *
     * @return QName of the property value type
     */
    public QName getValueType() {
        return valueType;
    }

    void setValueType(QName valueType) {
        this.valueType = valueType;
    }


    @Override
    public PrismProperty instantiate() {
        return instantiate(getNameOrDefaultName());
    }

    @Override
    public PrismProperty instantiate(QName name) {
        return new PrismProperty(name, this, prismContext);
    }

    // TODO: factory methods for DOM and JAXB elements

    public void setRead(boolean read) {
        this.read = read;
    }

    public void setUpdate(boolean update) {
        this.update = update;
    }

    public void setCreate(boolean create) {
        this.create = create;
    }

    public boolean canCreate() {
        return create;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getDebugDumpClassName()).append(":").append(DebugUtil.prettyPrint(getName()));
        sb.append(" (").append(DebugUtil.prettyPrint(getTypeName())).append(")");
        if (isMultiValue()) {
            sb.append(" multi");
        }
        if (isOptional()) {
            sb.append(" opt");
        }
        return sb.toString();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + Arrays.hashCode(allowedValues);
        result = prime * result + (create ? 1231 : 1237);
        result = prime * result + (read ? 1231 : 1237);
        result = prime * result + (update ? 1231 : 1237);
        result = prime * result + ((valueType == null) ? 0 : valueType.hashCode());
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
        PrismPropertyDefinition other = (PrismPropertyDefinition) obj;
        if (!Arrays.equals(allowedValues, other.allowedValues))
            return false;
        if (create != other.create)
            return false;
        if (read != other.read)
            return false;
        if (update != other.update)
            return false;
        if (valueType == null) {
            if (other.valueType != null)
                return false;
        } else if (!valueType.equals(other.valueType))
            return false;
        return true;
    }
    
    /**
     * Return a human readable name of this class suitable for logs.
     */
    @Override
    protected String getDebugDumpClassName() {
        return "PPD";
    }


}
