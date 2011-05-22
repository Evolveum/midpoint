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
package com.evolveum.midpoint.web.util;

import java.io.Serializable;
import java.util.Comparator;

/**
 * 
 * @author lazyman
 * @param <T>
 * 
 */
public abstract class SortableListComparator<T> implements Comparator<T>, Serializable {

	private static final long serialVersionUID = -2789696277916175755L;
	private boolean ascending;
	private String attribute;

	public SortableListComparator(String attribute, boolean ascending) {
		this.attribute = attribute;
		this.ascending = ascending;
	}

	protected String getAttribute() {
		return attribute;
	}

	protected boolean isAscending() {
		return ascending;
	}
}
