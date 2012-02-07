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
package com.evolveum.midpoint.schema.processor;

import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.exception.SchemaException;
import com.evolveum.midpoint.schema.holder.XPathHolder;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PropertyModificationType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PropertyModificationType.Value;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PropertyModificationTypeType;
import org.w3c.dom.Document;

import javax.xml.namespace.QName;
import java.util.HashSet;
import java.util.Set;

/**
 * Experimental ... kind of
 * <p/>
 * Mutable? Immutable?
 *
 * @author Radovan Semancik
 */
public class PropertyModification {

    private XPathHolder path;
    // Storing property instead of property name, so a property definition that may be associated with property will
    // be passed on
    private Property property;
    private Set<PropertyValue<Object>> modifyValues;
    private ModificationType modificationType;

    public enum ModificationType {
        ADD(PropertyModificationTypeType.add),
        REPLACE(PropertyModificationTypeType.replace),
        DELETE(PropertyModificationTypeType.delete);

        private PropertyModificationTypeType xmlType;

        private ModificationType(PropertyModificationTypeType xmlType) {
            this.xmlType = xmlType;
        }

        public PropertyModificationTypeType getPropertyModificationTypeType() {
            return this.xmlType;
        }
    }

    public PropertyModification() {
        modifyValues = new HashSet<PropertyValue<Object>>();
    }

    /**
     * @param path
     * @param modificationType
     * @param path
     * @param values
     */
    public PropertyModification(Property property, ModificationType modificationType, XPathHolder path, Set<PropertyValue<Object>> values) {
        super();
        this.path = path;
        this.property = property;
        this.modifyValues = values;
        this.modificationType = modificationType;
    }

    /**
     * Assumes empty path (default)
     *
     * @param property
     * @param modificationType
     * @param values
     */
    public PropertyModification(Property property, ModificationType modificationType, Set<PropertyValue<Object>> values) {
        super();
        this.path = new XPathHolder();
        this.property = property;
        this.modifyValues = values;
        this.modificationType = modificationType;
    }

    public XPathHolder getPath() {
        return path;
    }

    public Property getProperty() {
        return property;
    }

    public QName getPropertyName() {
        return property.getName();
    }

    public Set<PropertyValue<Object>> getValues() {
        return modifyValues;
    }

    public ModificationType getModificationType() {
        return modificationType;
    }

    public PropertyModificationType toPropertyModificationType() throws SchemaException {
        return toPropertyModificationType(null, false);
    }

    /**
     * With single-element parent path. It will "transpose" the path in the modification.
     *
     * @param parentPath single-element parent path
     * @return
     * @throws SchemaException
     */
    public PropertyModificationType toPropertyModificationType(QName parentPath, boolean recordType) throws SchemaException {
        XPathHolder absolutePath = path;
        if (parentPath != null) {
            absolutePath = path.transposedPath(parentPath);
        }
        Document doc = DOMUtil.getDocument();
        PropertyModificationType pmt = new PropertyModificationType();
        pmt.setPath(absolutePath.toElement(SchemaConstants.I_PROPERTY_CONTAINER_REFERENCE_PATH, doc));
        pmt.setModificationType(modificationType.getPropertyModificationTypeType());
        Value value = new Value();
        value.getAny().addAll(property.serializeToJaxb(doc, null, modifyValues, recordType));
        pmt.setValue(value);
        return pmt;
    }

}
