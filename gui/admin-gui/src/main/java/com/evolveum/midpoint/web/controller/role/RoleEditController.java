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

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.faces.event.ActionEvent;
import javax.faces.model.SelectItem;
import javax.xml.bind.JAXBException;

import com.evolveum.midpoint.prism.PrismObject;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.evolveum.midpoint.common.validator.EventHandler;
import com.evolveum.midpoint.common.validator.EventResult;
import com.evolveum.midpoint.common.validator.Validator;
import com.evolveum.midpoint.model.security.api.PrincipalUser;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.util.JAXBUtil;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.bean.AssignmentBeanType;
import com.evolveum.midpoint.web.controller.TemplateController;
import com.evolveum.midpoint.web.controller.util.AssignmentEditor;
import com.evolveum.midpoint.web.controller.util.ControllerUtil;
import com.evolveum.midpoint.web.model.ObjectTypeCatalog;
import com.evolveum.midpoint.web.model.RoleManager;
import com.evolveum.midpoint.web.model.dto.RoleDto;
import com.evolveum.midpoint.web.repo.RepositoryManager;
import com.evolveum.midpoint.web.security.SecurityUtils;
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
	@Autowired(required = true)
	private transient RepositoryManager repositoryManager;
	private boolean newRole = true;
	private RoleDto role;
	private String xml;

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

	public String getXml() {
		return xml;
	}

	public void setXml(String xml) {
		this.xml = xml;
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

		xml = null;
		newRole = true;
		template.setSelectedLeftId("leftRoleCreate");

		return PAGE_NAVIGATION;
	}

	public void save(ActionEvent evt) {
		Task task = taskManager.createTaskInstance("Save Role Changes.");
		OperationResult result = task.getResult();
		ObjectType newObject = null;

		if (StringUtils.isEmpty(xml)) {
			FacesUtils.addErrorMessage("Xml editor is empty.");
		}

		if (role == null) {
			FacesUtils.addErrorMessage("Role must not be null.");
		}

		try {
			SecurityUtils security = new SecurityUtils();
			PrincipalUser principal = security.getPrincipalUser();
	        task.setOwner(principal.getUser().asPrismObject());
			role.normalizeAssignments();
			newObject = getObjectFromXml(xml, result);
			RoleManager manager = ControllerUtil.getRoleManager(catalog);
			if (isNewRole()) {
				manager.add(role);
			} else {
				manager.submit(getRole(), task, result);
			}
		} catch (Exception ex) {
			LoggingUtils.logException(LOGGER, "Couldn't submit role {}", ex, role.getName());
			result.recordFatalError("Couldn't submit role '" + role.getName() + "'.", ex);
		} finally {
			if (!repositoryManager.saveObject(newObject.asPrismObject(), xml)) {
				result.recordFatalError("Couln't update role '" + newObject.getName() + "'.");
			}
			initController();
			result.computeStatus();
			ControllerUtil.printResults(LOGGER, result, "Role changes saved sucessfully.");
		}
	}

	public String viewObject() {
		if (role == null) {
			FacesUtils.addErrorMessage("Debug role not defined.");
			return RoleListController.PAGE_NAVIGATION_LIST;
		}

		try {
			PrismObject<?> objectType = repositoryManager.getObject(role.getOid());
			if (objectType == null) {
				return RoleListController.PAGE_NAVIGATION_LIST;
			}

			// role = new ObjectBean(objectType.getOid(), objectType.getName());
			xml = JAXBUtil.marshal(new ObjectFactory().createObject(objectType.asObjectable()));
		} catch (JAXBException ex) {
			LoggingUtils.logException(LOGGER, "Couldn't show role {} in editor", ex, role.getName());
			FacesUtils.addErrorMessage("Couldn't show role '" + role.getName() + "' in editor.", ex);

			return RoleListController.PAGE_NAVIGATION_LIST;
		} catch (Exception ex) {
			LoggingUtils.logException(LOGGER, "Unknown error occured.", ex);
			FacesUtils.addErrorMessage("Unknown error occured.", ex);

			return RoleListController.PAGE_NAVIGATION_LIST;
		}

		return PAGE_NAVIGATION;
	}

	private ObjectType getObjectFromXml(String xml, OperationResult parentResult) {
		final List<ObjectType> objects = new ArrayList<ObjectType>();
		Validator validator = new Validator(new EventHandler() {

			@Override
			public <T extends ObjectType> EventResult postMarshall(PrismObject<T> object, Element objectElement,
					OperationResult objectResult) {
				if (objects.isEmpty()) {
					objects.add(object.asObjectable());
				}
				return EventResult.cont();
			}

			@Override
			public void handleGlobalError(OperationResult currentResult) {
				// no reaction
			}

			@Override
			public EventResult preMarshall(Element objectElement, Node postValidationTree,
					OperationResult objectResult) {
				// no reaction
				return EventResult.cont();
			}
		});
		// TODO: fix operation names
		OperationResult result = parentResult.createSubresult("Get Object from XML");
		try {
			validator.validate(IOUtils.toInputStream(xml, "utf-8"), result, "processing object");
			result.computeStatus("Object processing failed");

		} catch (IOException ex) {
			// FacesUtils.addErrorMessage("Couldn't create object from xml.",
			// ex);
			result.recordFatalError("Couldn't create object from xml.", ex);
			LoggingUtils.logException(LOGGER, "Couldn't create object from xml.", ex, new Object());
			// return null;
		}

		if (objects.isEmpty()) {
			// FacesUtils.addErrorMessage("Couldn't create object from xml.");
			LoggingUtils.logException(LOGGER, "Couldn't create object from xml.",
					new IllegalArgumentException(), new Object());
			result.recordFatalError("Couldn't create object from xml.");
			// ControllerUtil.printResults(TRACE, result, "");
			return null;
		}

		// ControllerUtil.printResults(TRACE, result);

		return objects.get(0);
	}
}
