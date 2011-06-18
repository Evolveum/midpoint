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

package com.evolveum.midpoint.web.util;

import org.apache.commons.lang.StringUtils;

import com.evolveum.midpoint.web.model.dto.GuiUserDto;

/**
 * 
 * @author lazyman
 */
public class GuiUserDtoComparator extends SortableListComparator<GuiUserDto> {

	private static final long serialVersionUID = 3362224931050518444L;

	public GuiUserDtoComparator(String attribute, boolean ascending) {
		super(attribute, ascending);
	}

	@Override
	public int compare(GuiUserDto o1, GuiUserDto o2) {
		if (StringUtils.isEmpty(getAttribute())) {
			return 0;
		}

		int value = 0;
		if (getAttribute().equals("fullName")) {
			value = String.CASE_INSENSITIVE_ORDER.compare(o1.getFullName(), o2.getFullName());
		} else if (getAttribute().equals("givenName")) {
			value = String.CASE_INSENSITIVE_ORDER.compare(o1.getGivenName(), o2.getGivenName());
		} else if (getAttribute().equals("familyName")) {
			value = String.CASE_INSENSITIVE_ORDER.compare(o1.getFamilyName(), o2.getFamilyName());
		} else if (getAttribute().equals("oid")) {
			value = String.CASE_INSENSITIVE_ORDER.compare(o1.getOid(), o2.getOid());
		} else if (getAttribute().equals("name")) {
			value = String.CASE_INSENSITIVE_ORDER.compare(o1.getName(), o2.getName());
		}

		return isAscending() ? value : -value;
	}
}
