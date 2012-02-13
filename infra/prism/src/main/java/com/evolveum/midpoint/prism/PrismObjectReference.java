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

import javax.xml.namespace.QName;

import com.evolveum.midpoint.util.DebugUtil;

/**
 * Object Reference is a property that describes reference to an object. It is
 * used to represent association between objects. For example reference from
 * User object to Account objects that belong to the user. The reference is a
 * simple uni-directional link using an OID as an identifier.
 * 
 * This type should be used for all object references so the implementations can
 * detect them and automatically resolve them.
 * 
 * @author semancik
 * 
 */
public class PrismObjectReference extends PrismProperty {

	public PrismObjectReference(QName name, PrismPropertyDefinition definition, PrismContext prismContext) {
		super(name, definition, prismContext);
	}

	private String oid;
	private QName targetTypeName;

	/**
	 * {@inheritDoc}
	 */
	public PrismObjectReferenceDefinition getDefinition() {
		return (PrismObjectReferenceDefinition) super.getDefinition();
	}

	/**
	 * OID of the object that this reference refers to (reference target).
	 * 
	 * May return null, but the reference is in that case incomplete and
	 * unusable.
	 * 
	 * @return the target oid
	 */
	public String getOid() {
		return oid;
	}

	/**
	 * Returns XSD type of the object that this reference refers to. It may be
	 * used in XPath expressions and similar filters.
	 * 
	 * May return null if the type name is not set.
	 * 
	 * @return the target type name
	 */
	public QName getTargetTypeName() {
		return targetTypeName;
	}
	
	@Override
	public PrismPropertyValue<Object> getValue() {
		return new PrismPropertyValue<Object>(oid);
	}
	
	@Override
    public String toString() {
        return getClass().getSimpleName() + "(" + DebugUtil.prettyPrint(getName()) + ", " + oid + ", " + DebugUtil.prettyPrint(targetTypeName) + "):" + getValues();
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < indent; i++) {
            sb.append(INDENT_STRING);
        }
        sb.append(getDebugDumpClassName()).append(": ").append(DebugUtil.prettyPrint(getName())).append(" = ");
        sb.append(oid);
        sb.append(" (");
        sb.append(DebugUtil.prettyPrint(targetTypeName));
        sb.append(")");
        if (getValues() != null) {
            sb.append(": [ ");
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
        return "Ref";
    }
}
