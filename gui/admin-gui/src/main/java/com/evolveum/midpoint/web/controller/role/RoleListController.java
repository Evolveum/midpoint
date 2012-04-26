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

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

import com.evolveum.midpoint.schema.PagingTypeFactory;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.bean.RoleListItem;
import com.evolveum.midpoint.web.controller.TemplateController;
import com.evolveum.midpoint.web.controller.util.ControllerUtil;
import com.evolveum.midpoint.web.controller.util.SearchableListController;
import com.evolveum.midpoint.web.model.ObjectTypeCatalog;
import com.evolveum.midpoint.web.model.RoleManager;
import com.evolveum.midpoint.web.model.dto.RoleDto;
import com.evolveum.midpoint.web.util.FacesUtils;
import com.evolveum.midpoint.web.util.RoleListItemComparator;
import com.evolveum.midpoint.xml.ns._public.common.api_types_2.OrderDirectionType;
import com.evolveum.midpoint.xml.ns._public.common.api_types_2.PagingType;
import com.evolveum.midpoint.xml.ns._public.common.api_types_2.PropertyReferenceListType;

/**
 * 
 * @author lazyman
 * 
 */
@Controller("roleList")
@Scope("session")
public class RoleListController extends SearchableListController<RoleListItem> {

	public static final String PAGE_NAVIGATION_LIST = "/admin/role/index?faces-redirect=true";
	private static final long serialVersionUID = -2220151123394281052L;
	private static final Trace LOGGER = TraceManager.getTrace(RoleListController.class);
	private static final String PARAM_ROLE_OID = "roleOid";
	@Autowired(required = true)
	private transient TemplateController template;
	@Autowired(required = true)
	private transient ObjectTypeCatalog catalog;
	@Autowired(required = true)
	private transient RoleEditController roleEditor;
	private boolean selectAll;
	private boolean showPopup;

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
			RoleListItem item;
			for (RoleDto role : list) {
				// TODO: assignments
				item = new RoleListItem(role.getOid(), role.getName(), role.getDescription(), null);
				getObjects().add(item);
			}
		}

		return null;
	}

	public String initController() {
		listFirst();

		template.setSelectedLeftId("leftAvailableRoles");
		roleEditor.setNewRole(true);

		return PAGE_NAVIGATION_LIST;
	}

	public String showRoleDetails() {
		String roleOid = FacesUtils.getRequestParameter(PARAM_ROLE_OID);
		if (StringUtils.isEmpty(roleOid)) {
			FacesUtils.addErrorMessage("Couldn't show role details, unidentified oid.");
			return null;
		}

		RoleListItem item = getRoleItem(roleOid);
		if (item == null) {
			FacesUtils.addErrorMessage("Couldn't show role details, unidentified role list item.");
			return null;
		}

		String nextPage = null;
		try {
			RoleManager manager = ControllerUtil.getRoleManager(catalog);
			RoleDto role = manager.get(roleOid, null);
			roleEditor.setRole(role);
			
			nextPage = roleEditor.viewObject();
			//nextPage = RoleEditController.PAGE_NAVIGATION;
		} catch (Exception ex) {
			LoggingUtils.logException(LOGGER, "Couldn't show role '{}' details", ex, item.getName());
			FacesUtils.addErrorMessage("Couldn't show details for role '" + item.getName() + "'.", ex);
		}

		return nextPage;
	}

	private RoleListItem getRoleItem(String oid) {
		for (RoleListItem item : getObjects()) {
			if (item.getOid().equals(oid)) {
				return item;
			}
		}

		return null;
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

	public void deleteRoles() {
		showPopup = false;
		for (RoleListItem role : getObjects()) {
			if (role.isSelected()) {
				LOGGER.debug("Deleting role {}.", new Object[] { role.getName() });
				try {
					RoleManager roleManager = ControllerUtil.getRoleManager(catalog);
					roleManager.delete(role.getOid());
				} catch (Exception ex) {
					LoggingUtils.logException(LOGGER, "Deleting role '{}' failed", ex, role.getName());
					FacesUtils.addErrorMessage("Deleting role '" + role.getName() + "' failed.", ex);
				}
			}

		}
		listFirst();
	}

	public boolean isShowPopup() {
		return showPopup;
	}

	public void hideConfirmDelete() {
		showPopup = false;
	}

	public void showConfirmDelete() {
		boolean selected = false;

		for (RoleListItem role : getObjects()) {
			if (role != null && role.isSelected()) {
				selected = true;
				break;
			}
		}

		if (selected) {
			showPopup = true;
		} else {
			FacesUtils.addErrorMessage("No user selected.");
		}
	}
	
	public boolean isTableFull(){
		if(getObjects().size() < getRowsCount()){
			return false;
		}
		return true;
	}
}
