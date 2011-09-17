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

import org.apache.commons.lang.StringUtils;

import com.evolveum.midpoint.web.bean.RoleListItem;

/**
 * 
 * @author lazyman
 * 
 */
public class RoleListItemComparator extends SortableListComparator<RoleListItem> {

	private static final long serialVersionUID = 2527252805778431141L;

	public RoleListItemComparator(String attribute, boolean ascending) {
		super(attribute, ascending);
	}

	@Override
	public int compare(RoleListItem role1, RoleListItem role2) {
		if (StringUtils.isEmpty(getAttribute())) {
			return 0;
		}

		int value = 0;
		if (getAttribute().equals("name")) {
			value = String.CASE_INSENSITIVE_ORDER.compare(role1.getName(), role2.getName());
		} else if (getAttribute().equals("description")) {
			value = String.CASE_INSENSITIVE_ORDER.compare(role1.getDescription(), role2.getDescription());
		}

		return isAscending() ? value : -value;
	}
}
