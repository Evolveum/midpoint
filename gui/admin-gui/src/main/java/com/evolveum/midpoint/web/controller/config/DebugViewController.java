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
package com.evolveum.midpoint.web.controller.config;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.JAXBException;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.evolveum.midpoint.common.validator.EventHandler;
import com.evolveum.midpoint.common.validator.EventResult;
import com.evolveum.midpoint.common.validator.Validator;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.JAXBUtil;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.bean.ObjectBean;
import com.evolveum.midpoint.web.controller.TemplateController;
import com.evolveum.midpoint.web.controller.util.ControllerUtil;
import com.evolveum.midpoint.web.repo.RepositoryManager;
import com.evolveum.midpoint.web.util.FacesUtils;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectFactory;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectType;

/**
 * 
 * @author lazyman
 * 
 */
@Controller("debugView")
@Scope("session")
public class DebugViewController implements Serializable {

	public static final String PAGE_NAVIGATION = "/admin/config/debugView?faces-redirect=true";
	public static final String NAVIGATION_LEFT = "leftViewEdit";
	private static final long serialVersionUID = -6260309359121248206L;
	private static final Trace TRACE = TraceManager.getTrace(DebugViewController.class);
	@Autowired(required = true)
	private transient RepositoryManager repositoryManager;
	@Autowired(required = true)
	private transient TemplateController template;
	private boolean editOther = false;
	private String editOtherName;
	private ObjectBean object;
	private boolean editable = false;
	private String xml;

	public String getEditOtherName() {
		return editOtherName;
	}

	public void setEditOtherName(String editOtherName) {
		this.editOtherName = editOtherName;
	}

	public boolean isEditOther() {
		if (!isViewEditable()) {
			editOther = true;
		}
		return editOther;
	}

	public void setEditOther(boolean editOther) {
		this.editOther = editOther;
	}

	public ObjectBean getObject() {
		return object;
	}

	public void setObject(ObjectBean object) {
		this.object = object;
	}

	public boolean isEditable() {
		return editable;
	}

	public void setEditable(boolean editable) {
		this.editable = editable;
	}
	
	public void editable(){
		if(this.editable){
			editable = false;
		} else {
			editable = true;
		}
	}


	public String getXml() {
		return xml;
	}

	public void setXml(String xml) {
		this.xml = xml;
	}

	public boolean isViewEditable() {
		if (StringUtils.isEmpty(xml) || object == null) {
			return false;
		}

		return true;
	}

	public String initController() {
		object = null;
		xml = null;
		editable = false;

		return PAGE_NAVIGATION;
	}

	public String back() {
		initController();
		template.setSelectedLeftId("leftList");

		return DebugListController.PAGE_NAVIGATION;
	}

	public String editOtherObject() {
		if (StringUtils.isEmpty(editOtherName)) {
			FacesUtils.addErrorMessage("Object name must not be null.");
			return null;
		}

		try {
			List<? extends ObjectType> list = repositoryManager.searchObjects(editOtherName);

			if (list.isEmpty()) {
				FacesUtils.addErrorMessage("Couldn't find object that matches name '" + editOtherName + "'.");
				return null;
			}
			if (list.size() > 1) {
				FacesUtils.addErrorMessage("Found more than one object that matches name '" + editOtherName
						+ "'.");
				return null;
			}
			ObjectType objectType = list.get(0);
			object = new ObjectBean(objectType.getOid(), objectType.getName());
			xml = JAXBUtil.marshal(new ObjectFactory().createObject(objectType));
		} catch (Exception ex) {
			LoggingUtils.logException(TRACE, "Unknown error occured while searching objects by name {}", ex,
					editOtherName);
			FacesUtils.addErrorMessage("Unknown error occured.", ex);

			return DebugListController.PAGE_NAVIGATION;
		}
		return viewObject();
	}

	public String viewObject() {
		if (object == null) {
			FacesUtils.addErrorMessage("Debug object not defined.");
			return DebugListController.PAGE_NAVIGATION;
		}

		try {
			ObjectType objectType = repositoryManager.getObject(object.getOid());
			if (objectType == null) {
				return DebugListController.PAGE_NAVIGATION;
			}

			object = new ObjectBean(objectType.getOid(), objectType.getName());
			xml = JAXBUtil.marshal(new ObjectFactory().createObject(objectType));
		} catch (JAXBException ex) {
			LoggingUtils.logException(TRACE, "Couldn't show object {} in editor", ex, object.getName());
			FacesUtils.addErrorMessage("Couldn't show object '" + object.getName() + "' in editor.", ex);

			return DebugListController.PAGE_NAVIGATION;
		} catch (Exception ex) {
			LoggingUtils.logException(TRACE, "Unknown error occured.", ex);
			FacesUtils.addErrorMessage("Unknown error occured.", ex);

			return DebugListController.PAGE_NAVIGATION;
		}

		return PAGE_NAVIGATION;
	}

	public String savePerformed() {
		
		OperationResult result = new OperationResult("Save changes");
		if (StringUtils.isEmpty(xml)) {
			FacesUtils.addErrorMessage("Xml editor is empty.");
			return null;
		}

		ObjectType newObject = getObjectFromXml(xml, result);
		if (newObject == null) {
			ControllerUtil.printResults(TRACE, result, "Changes saved sucessfully");
			return null;
		}

		if (!repositoryManager.saveObject(newObject)) {
			result.recordFatalError("Couln't update object '" + newObject.getName() + "'.");
//			FacesUtils.addErrorMessage("Couln't update object '" + newObject.getName() + "'.");
		}
		initController();
		template.setSelectedLeftId("leftList");
		result.recordSuccess();
		ControllerUtil.printResults(TRACE, result, "Changes saved sucessfully.");

		return DebugListController.PAGE_NAVIGATION;
	}

	private ObjectType getObjectFromXml(String xml, OperationResult parentResult) {
		final List<ObjectType> objects = new ArrayList<ObjectType>();
		Validator validator = new Validator(new EventHandler() {

			@Override
			public EventResult postMarshall(ObjectType object, Element objectElement, OperationResult objectResult) {
				if (objects.isEmpty()) {
					objects.add(object);
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
//			FacesUtils.addErrorMessage("Couldn't create object from xml.", ex);
			result.recordFatalError("Couldn't create object from xml.", ex);
			LoggingUtils.logException(TRACE, "Couldn't create object from xml.", ex, new Object());
//			return null;
		}

		if (objects.isEmpty()) {
//			FacesUtils.addErrorMessage("Couldn't create object from xml.");
			LoggingUtils.logException(TRACE, "Couldn't create object from xml.", new IllegalArgumentException(), new Object());
			result.recordFatalError("Couldn't create object from xml.");
//			ControllerUtil.printResults(TRACE, result, "");
			return null;
		}
		
//		ControllerUtil.printResults(TRACE, result);
		
		return objects.get(0);
	}
	
	
}
