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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.faces.event.ActionEvent;
import javax.faces.event.ValueChangeEvent;
import javax.faces.model.SelectItem;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.bean.AssignmentBean;
import com.evolveum.midpoint.web.bean.AssignmentBeanType;
import com.evolveum.midpoint.web.controller.TemplateController;
import com.evolveum.midpoint.web.controller.util.ControllerUtil;
import com.evolveum.midpoint.web.model.ObjectTypeCatalog;
import com.evolveum.midpoint.web.model.RoleManager;
import com.evolveum.midpoint.web.model.dto.RoleDto;
import com.evolveum.midpoint.web.util.FacesUtils;
import com.evolveum.midpoint.web.util.SelectItemComparator;
import com.evolveum.midpoint.xml.ns._public.common.common_1.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.RoleType;

/**
 * 
 * @author lazyman
 * 
 */
@Controller("roleEdit")
@Scope("session")
public class RoleEditController implements Serializable {

	public static final String PAGE_NAVIGATION = "/role/roleEdit?faces-redirect=true";
	public static final String PARAM_ASSIGNMENT_ID = "assignmentId";
	private static final long serialVersionUID = 6390559677870495118L;
	private static final Trace LOGGER = TraceManager.getTrace(RoleEditController.class);
	@Autowired(required = true)
	private transient ObjectTypeCatalog catalog;
	@Autowired(required = true)
	private transient TemplateController template;
	private boolean newRole = true;
	private RoleDto role;
	private boolean selectAll;

	public RoleDto getRole() {
		if (role == null) {
			role = new RoleDto(new RoleType());
		}
		return role;
	}

	public List<SelectItem> getAssignmentTypes() {
		List<SelectItem> items = new ArrayList<SelectItem>();
		for (AssignmentBeanType type : AssignmentBeanType.values()) {
			items.add(new SelectItem(type.name(), FacesUtils.translateKey(type.getLocalizationKey())));
		}
		Collections.sort(items, new SelectItemComparator());

		return items;
	}

	/**
	 * True if this controller is used for creating new role, false if we're
	 * editing existing role
	 */
	public boolean isNewRole() {
		return newRole;
	}

	void setNewRole(boolean newRole) {
		this.newRole = newRole;
	}

	void setRole(RoleDto role) {
		Validate.notNull(role, "Role must not be null.");
		this.role = role;
		newRole = false;
		template.setSelectedLeftId("leftRoleEdit");
	}

	public String initController() {
		role = new RoleDto(new RoleType());
		newRole = true;
		template.setSelectedLeftId("leftRoleCreate");

		return PAGE_NAVIGATION;
	}

	public void save(ActionEvent evt) {
		if (role == null) {
			FacesUtils.addErrorMessage("Role must not be null.");
			return;
		}

		try {
			role.pushAssigmentBeansToRole();
			RoleManager manager = ControllerUtil.getRoleManager(catalog);
			if (isNewRole()) {
				manager.add(role);
			} else {
				manager.submit(getRole());				
			}
		} catch (Exception ex) {
			LoggingUtils.logException(LOGGER, "Couldn't submit role {}", ex, role.getName());
			FacesUtils.addErrorMessage("Couldn't submit role '" + role.getName() + "'.", ex);
		}
	}

	public boolean isSelectAll() {
		return selectAll;
	}

	public void setSelectAll(boolean selectAll) {
		this.selectAll = selectAll;
	}

	public void selectAllPerformed(ValueChangeEvent event) {
		ControllerUtil.selectAllPerformed(event, role.getAssignments());
	}

	public void selectPerformed(ValueChangeEvent evt) {
		this.selectAll = ControllerUtil.selectPerformed(evt, role.getAssignments());
	}

	private int getNewId() {
		int id = 0;
		for (AssignmentBean bean : role.getAssignments()) {
			if (bean.getId() > id) {
				id = bean.getId();
			}
		}

		return ++id;
	}

	public void addAssignment() {
		getRole().getAssignments().add(new AssignmentBean(getNewId(), new AssignmentType()));
		selectAll = false;
	}

	public void deleteAssignments() {
		Iterator<AssignmentBean> iterator = role.getAssignments().iterator();
		while (iterator.hasNext()) {
			AssignmentBean bean = iterator.next();
			if (bean.isSelected()) {
				iterator.remove();
			}
		}

		selectAll = false;
	}

	public void editAssignmentObject() {
		// TODO: show object browser or xml editor
	}

	public void editAssignment() {
		String id = FacesUtils.getRequestParameter(PARAM_ASSIGNMENT_ID);
		if (StringUtils.isEmpty(id) || !id.matches("[0-9]+")) {
			FacesUtils.addErrorMessage("Couldn't get internal assignment bean id.");
			return;
		}
		int beanId = Integer.parseInt(id);
		AssignmentBean bean = null;
		for (AssignmentBean assignmentBean : role.getAssignments()) {
			if (assignmentBean.getId() == beanId) {
				bean = assignmentBean;
				break;
			}
		}

		if (bean == null) {
			FacesUtils.addErrorMessage("Couldn't find assignment bean with selected internal id.");
			return;
		}

		bean.setEditing(true);
	}
}
