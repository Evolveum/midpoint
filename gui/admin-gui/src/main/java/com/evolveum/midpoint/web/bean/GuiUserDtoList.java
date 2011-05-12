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

package com.evolveum.midpoint.web.bean;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.evolveum.midpoint.web.dto.GuiUserDto;
import com.evolveum.midpoint.web.util.GuiUserDtoComparator;
import com.evolveum.midpoint.web.util.SortableList;

/**
 * 
 * @author lazyman
 */
public class GuiUserDtoList extends SortableList implements Serializable {

	private static final long serialVersionUID = 6011326995693930079L;
	private List<GuiUserDto> users;

	public GuiUserDtoList(String sortColumnName) {
		setSortColumnName(sortColumnName);
	}

	@Override
	public void sort() {
		Collections.sort(users, new GuiUserDtoComparator(getSortColumnName(), isAscending()));
	}

	public List<GuiUserDto> getUsers() {
		if (users == null) {
			users = new ArrayList<GuiUserDto>();
		}
		return users;
	}
}
