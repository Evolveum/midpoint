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

package com.evolveum.midpoint.repo.sql.query;

import com.evolveum.midpoint.util.DebugDumpable;

import javax.xml.namespace.QName;

/**
 * @author lazyman
 */
public class AttributeDefinition extends Definition implements DebugDumpable {

    private boolean indexed;
    private boolean reference;
    private boolean enumerated;
    private boolean polyString;
    private boolean multiValue;
    private Class<?> classType;

    public Class<?> getClassType() {
        return classType;
    }

    void setClassType(Class<?> classType) {
        this.classType = classType;
    }

    public boolean isPolyString() {
        return polyString;
    }

    public void setPolyString(boolean polyString) {
        this.polyString = polyString;
    }

    public boolean isEnumerated() {
        return enumerated;
    }

    void setEnumerated(boolean enumerated) {
        this.enumerated = enumerated;
    }

    public boolean isReference() {
        return reference;
    }

    public void setReference(boolean reference) {
        this.reference = reference;
    }

    public boolean isIndexed() {
        return indexed;
    }

    public void setIndexed(boolean indexed) {
        this.indexed = indexed;
    }
    
    public void setMultiValue(boolean multiValue) {
		this.multiValue = multiValue;
	}
    
    public boolean isMultiValue() {
		return multiValue;
	}

    @Override
    public Definition findDefinition(QName qname) {
        return null;
    }

    @Override
    public <T extends Definition> T findDefinition(QName qname, Class<T> type) {
        return null;
    }

    @Override
    public boolean isEntity() {
        return false;
    }

    @Override
    public String toString() {
        return debugDump();
    }

    @Override
    public String debugDump() {
        return debugDump(0);
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < indent; i++) {
            sb.append(DebugDumpable.INDENT_STRING);
        }
        sb.append("n: ");
        sb.append(getName());
        sb.append(",f: ");
        sb.append(getJpaName());
        sb.append(", t: ");
        sb.append(getType());
        sb.append(", i: ");
        sb.append(isIndexed());
        sb.append(", r: ");
        sb.append(isReference());

        return sb.toString();
    }
}