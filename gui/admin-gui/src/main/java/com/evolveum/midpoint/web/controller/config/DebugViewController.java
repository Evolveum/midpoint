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

import com.evolveum.midpoint.api.logging.LoggingUtils;
import com.evolveum.midpoint.api.logging.Trace;
import com.evolveum.midpoint.logging.TraceManager;
import com.evolveum.midpoint.schema.util.JAXBUtil;
import com.evolveum.midpoint.validator.ObjectHandler;
import com.evolveum.midpoint.validator.ValidationMessage;
import com.evolveum.midpoint.validator.Validator;
import com.evolveum.midpoint.web.bean.ObjectBean;
import com.evolveum.midpoint.web.controller.TemplateController;
import com.evolveum.midpoint.web.repo.RepositoryManager;
import com.evolveum.midpoint.web.util.FacesUtils;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectFactory;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectListType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectType;

/**
 * 
 * @author lazyman
 * 
 */
@Controller("debugView")
@Scope("session")
public class DebugViewController implements Serializable {

	public static final String PAGE_NAVIGATION = "/config/debugView?faces-redirect=true";
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
		if (StringUtils.isEmpty(xml)) {
			FacesUtils.addErrorMessage("Xml editor is empty.");
			return null;
		}

		ObjectType newObject = getObjectFromXml(xml);
		if (newObject == null) {
			return null;
		}

		if (!repositoryManager.saveObject(newObject)) {
			FacesUtils.addErrorMessage("Couln't update object '" + newObject.getName() + "'.");
		}

		template.setSelectedLeftId("leftList");

		return DebugListController.PAGE_NAVIGATION;
	}

	private ObjectType getObjectFromXml(String xml) {
		final List<ObjectType> objects = new ArrayList<ObjectType>();
		Validator validator = new Validator(new ObjectHandler() {

			@Override
			public void handleObject(ObjectType object, List<ValidationMessage> objectErrors) {
				if (objects.isEmpty()) {
					objects.add(object);
				}
			}
		});
		try {
			List<ValidationMessage> messages = validator.validate(IOUtils.toInputStream(xml, "utf-8"));
			if (messages != null && !messages.isEmpty()) {
				StringBuilder builder;
				for (ValidationMessage message : messages) {
					builder = new StringBuilder();
					builder.append(message.getType());
					builder.append(": Object with oid '");
					builder.append(message.getOid());
					builder.append("' is not valid, reason: ");
					builder.append(message.getMessage());
					builder.append(".");
					if (!StringUtils.isEmpty(message.getProperty())) {
						builder.append(" Property: ");
						builder.append(message.getProperty());
					}
					FacesUtils.addErrorMessage(builder.toString());
				}
				return null;
			}
		} catch (IOException ex) {
			FacesUtils.addErrorMessage("Couldn't create object from xml.", ex);
			// TODO: logging
			return null;
		}

		if (objects.isEmpty()) {
			FacesUtils.addErrorMessage("Couldn't create object from xml.");
			return null;
		}

		return objects.get(0);
	}
}
