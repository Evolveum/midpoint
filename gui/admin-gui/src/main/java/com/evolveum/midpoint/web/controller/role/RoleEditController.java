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
import java.util.List;

import javax.faces.event.ActionEvent;
import javax.faces.model.SelectItem;
import javax.xml.bind.JAXBException;

import org.apache.commons.lang.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.JAXBUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.bean.AssignmentBeanType;
import com.evolveum.midpoint.web.bean.ObjectBean;
import com.evolveum.midpoint.web.controller.TemplateController;
import com.evolveum.midpoint.web.controller.config.DebugListController;
import com.evolveum.midpoint.web.controller.util.AssignmentEditor;
import com.evolveum.midpoint.web.controller.util.ControllerUtil;
import com.evolveum.midpoint.web.model.ObjectTypeCatalog;
import com.evolveum.midpoint.web.model.RoleManager;
import com.evolveum.midpoint.web.model.dto.RoleDto;
import com.evolveum.midpoint.web.util.FacesUtils;
import com.evolveum.midpoint.web.util.SelectItemComparator;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectFactory;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.RoleType;

/**
 * 
 * @author lazyman
 * 
 */
@Controller("roleEdit")
@Scope("session")
public class RoleEditController implements Serializable {

	public static final String PAGE_NAVIGATION = "/admin/role/roleEdit?faces-redirect=true";
	public static final String PARAM_ASSIGNMENT_ID = "assignmentId";
	private static final long serialVersionUID = 6390559677870495118L;
	private static final Trace LOGGER = TraceManager.getTrace(RoleEditController.class);
	@Autowired(required = true)
	private transient ObjectTypeCatalog catalog;
	@Autowired(required = true)
	private transient TemplateController template;
	@Autowired(required = true)
	private transient AssignmentEditor<RoleDto> assignmentEditor;
	@Autowired(required = true)
	private transient TaskManager taskManager;
	private boolean newRole = true;
	private RoleDto role;
	
	public AssignmentEditor<RoleDto> getAssignmentEditor() {
		return assignmentEditor;
	}

	public RoleDto getRole() {
		if (role == null) {
			role = new RoleDto(new RoleType());
			assignmentEditor.initController(role);
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
		assignmentEditor.initController(role);

		newRole = true;
		template.setSelectedLeftId("leftRoleCreate");

		return PAGE_NAVIGATION;
	}

	public void save(ActionEvent evt) {
		Task task = taskManager.createTaskInstance("Save Role Changes.");
		OperationResult result = task.getResult();		
		if (role == null) {
			FacesUtils.addErrorMessage("Role must not be null.");
			return;
		}

		try {
			role.normalizeAssignments();
			RoleManager manager = ControllerUtil.getRoleManager(catalog);
			if (isNewRole()) {
				manager.add(role);
			} else {
				manager.submit(getRole(), task, result);
			}
		} catch (Exception ex) {
			LoggingUtils.logException(LOGGER, "Couldn't submit role {}", ex, role.getName());
//			FacesUtils.addErrorMessage("Couldn't submit role '" + role.getName() + "'.", ex);
			result.recordFatalError("Couldn't submit role '" + role.getName() + "'.", ex);
		} finally {
			result.computeStatus();
			ControllerUtil.printResults(LOGGER, result, "Role changes saved sucessfully.");
		}
	}
	
	public String viewObject() {
		if (role == null) {
			FacesUtils.addErrorMessage("Debug role not defined.");
			return DebugListController.PAGE_NAVIGATION;
		}

		/*try {
			ObjectType objectType = repositoryManager.getObject(object.getOid());
			if (objectType == null) {
				return DebugListController.PAGE_NAVIGATION;
			}

			role = new ObjectBean(objectType.getOid(), objectType.getName());
			xml = JAXBUtil.marshal(new ObjectFactory().createObject(objectType));
		} catch (JAXBException ex) {
			LoggingUtils.logException(TRACE, "Couldn't show object {} in editor", ex, object.getName());
			FacesUtils.addErrorMessage("Couldn't show object '" + object.getName() + "' in editor.", ex);

			return DebugListController.PAGE_NAVIGATION;
		} catch (Exception ex) {
			LoggingUtils.logException(TRACE, "Unknown error occured.", ex);
			FacesUtils.addErrorMessage("Unknown error occured.", ex);

			return DebugListController.PAGE_NAVIGATION;
		}*/

		return PAGE_NAVIGATION;
	}
}
