/**
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
 * Portions Copyrighted 2011 [name of copyright owner]
 */
package com.evolveum.midpoint.prism.path;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.util.PrettyPrinter;

import java.io.Serializable;

/**
 * @author semancik
 *
 */
public class ItemPathSegment implements Serializable {
	
	private QName name;
	private String id;
	private boolean isVariable = false;
	
	public ItemPathSegment(QName name) {
		this.name = name;
		this.id = null;
	}

	public ItemPathSegment(QName name, String id) {
		this.name = name;
		this.id = id;
	}

	public ItemPathSegment(QName name, String id, boolean isVariable) {
		this.name = name;
		this.id = id;
		this.isVariable = isVariable;
	}

	public QName getName() {
		return name;
	}

	public String getId() {
		return id;
	}

	public boolean isVariable() {
		return isVariable;
	}

	@Override
	public String toString() {
		if (id == null) {
			return (isVariable ? "$" : "") + PrettyPrinter.prettyPrint(name);
		} else {
			return (isVariable ? "$" : "") + PrettyPrinter.prettyPrint(name) + "[" + id + "]";
		}
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result + ((name == null) ? 0 : name.hashCode());
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
		ItemPathSegment other = (ItemPathSegment) obj;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		return true;
	}
	
}
