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
 * Portions Copyrighted 2010 Forgerock
 */

package com.evolveum.midpoint.schema.holder;

import javax.xml.namespace.QName;

/**
 *
 * @author Radovan Semancik
 */
public class XPathSegment {

    private QName qName;
    boolean variable;

    public XPathSegment(QName qName) {
        this.qName = qName;
        this.variable = false;
    }

    public XPathSegment(QName qName, boolean variable) {
        this.qName = qName;
        this.variable = variable;
    }

    // TODO: Attribute predicate

    /**
     * Get the value of qName
     *
     * @return the value of qName
     */
    public QName getQName() {
        return qName;
    }

    /**
     * Set the value of qName
     *
     * @param qName new value of qName
     */
    public void setQName(QName qName) {
        this.qName = qName;
    }

    public boolean isVariable() {
        return variable;
    }

    public void setVariable(boolean variable) {
        this.variable = variable;
    }

    @Override
    public String toString() {
        if (variable) {
            return "$"+qName.toString();
        } else {
            return qName.toString();
        }
    }

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((qName == null) ? 0 : qName.hashCode());
		result = prime * result + (variable ? 1231 : 1237);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		XPathSegment other = (XPathSegment) obj;
		if (qName == null) {
			if (other.qName != null)
				return false;
		} else if (!qName.equals(other.qName))
			return false;
		if (variable != other.variable)
			return false;
		return true;
	}

}
