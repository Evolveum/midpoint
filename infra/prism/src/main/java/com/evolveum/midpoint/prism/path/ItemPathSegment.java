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
public abstract class ItemPathSegment implements Serializable {
	
	private boolean wildcard = false;

	public boolean isWildcard() {
		return wildcard;
	}

	protected void setWildcard(boolean wildcard) {
		this.wildcard = wildcard;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (wildcard ? 1231 : 1237);
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
		if (wildcard != other.wildcard)
			return false;
		return true;
	}
	
}
