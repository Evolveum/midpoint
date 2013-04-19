/**
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
 * Portions Copyrighted 2012 [name of copyright owner]
 */
package com.evolveum.midpoint.prism.path;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.util.PrettyPrinter;

import java.io.Serializable;

/**
 * @author semancik
 *
 */
public class NameItemPathSegment extends ItemPathSegment {
	
	public static final NameItemPathSegment WILDCARD = NameItemPathSegment.createWildcard();
	
	private QName name;
	private boolean isVariable = false;
	
	public NameItemPathSegment(QName name) {
		this.name = name;
	}

	private static NameItemPathSegment createWildcard() {
		NameItemPathSegment segment = new NameItemPathSegment(null);
		segment.setWildcard(true);
		return segment;
	}

	public NameItemPathSegment(QName name, boolean isVariable) {
		this.name = name;
		this.isVariable = isVariable;
	}

	public QName getName() {
		return name;
	}

	public boolean isVariable() {
		return isVariable;
	}

	@Override
	public String toString() {
		return (isVariable ? "$" : "") + (isWildcard() ? "*" : PrettyPrinter.prettyPrint(name));
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + (isVariable ? 1231 : 1237);
		result = prime * result + ((name == null) ? 0 : name.hashCode());
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
		NameItemPathSegment other = (NameItemPathSegment) obj;
		if (isVariable != other.isVariable)
			return false;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		return true;
	}
	
}
