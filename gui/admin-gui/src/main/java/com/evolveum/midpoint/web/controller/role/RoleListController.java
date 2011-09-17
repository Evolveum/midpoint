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
package com.evolveum.midpoint.web.controller.role;

import java.util.Collection;
import java.util.Collections;

import javax.faces.event.ActionEvent;
import javax.faces.event.ValueChangeEvent;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

import com.evolveum.midpoint.schema.PagingTypeFactory;
import com.evolveum.midpoint.web.bean.RoleListItem;
import com.evolveum.midpoint.web.controller.TemplateController;
import com.evolveum.midpoint.web.controller.util.ControllerUtil;
import com.evolveum.midpoint.web.controller.util.SearchableListController;
import com.evolveum.midpoint.web.model.ObjectTypeCatalog;
import com.evolveum.midpoint.web.model.RoleManager;
import com.evolveum.midpoint.web.model.dto.RoleDto;
import com.evolveum.midpoint.web.util.RoleListItemComparator;
import com.evolveum.midpoint.xml.ns._public.common.common_1.OrderDirectionType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PagingType;

/**
 * 
 * @author lazyman
 * 
 */
@Controller("roleList")
@Scope("session")
public class RoleListController extends SearchableListController<RoleListItem> {

	private static final long serialVersionUID = -2220151123394281052L;
	public static final String PAGE_NAVIGATION_LIST = "/role/index?faces-redirect=true";
	@Autowired(required = true)
	private transient TemplateController template;
	@Autowired(required = true)
	private transient ObjectTypeCatalog catalog;
	private boolean selectAll;

	@Override
	protected void sort() {
		Collections.sort(getObjects(), new RoleListItemComparator(getSortColumnName(), isAscending()));
	}

	@Override
	protected String listObjects() {
		RoleManager manager = ControllerUtil.getRoleManager(catalog);

		OrderDirectionType direction = isAscending() ? OrderDirectionType.ASCENDING
				: OrderDirectionType.DESCENDING;
		PagingType paging = PagingTypeFactory.createPaging(getOffset(), getRowsCount(), direction,
				getSortColumnName());

		getObjects().clear();
		Collection<RoleDto> list = manager.list(paging);
		if (list != null) {
			for (RoleDto role : list) {
				// TODO: assignments
				getObjects()
						.add(new RoleListItem(role.getOid(), role.getName(), role.getDescription(), null));
			}
		}

		return null;
	}

	public String initController() {
		listFirst();

		template.setSelectedLeftId("leftAvailableRoles");
		return PAGE_NAVIGATION_LIST;
	}

	public String showRoleDetails() {
		// TODO:
		return RoleEditController.PAGE_NAVIGATION;
	}

	public boolean isSelectAll() {
		return selectAll;
	}

	public void setSelectAll(boolean selectAll) {
		this.selectAll = selectAll;
	}

	public void selectAllPerformed(ValueChangeEvent event) {
		ControllerUtil.selectAllPerformed(event, getObjects());
	}

	public void selectPerformed(ValueChangeEvent evt) {
		this.selectAll = ControllerUtil.selectPerformed(evt, getObjects());
	}

	public void sortItem(ActionEvent e) {
		sort();
	}
}
