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
package com.evolveum.midpoint.web.controller.resource;

import java.io.Serializable;

import javax.xml.ws.Holder;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

import com.evolveum.midpoint.api.logging.LoggingUtils;
import com.evolveum.midpoint.api.logging.Trace;
import com.evolveum.midpoint.logging.TraceManager;
import com.evolveum.midpoint.web.bean.ObjectBean;
import com.evolveum.midpoint.web.bean.ResourceListItem;
import com.evolveum.midpoint.web.bean.ResourceObjectType;
import com.evolveum.midpoint.web.controller.TemplateController;
import com.evolveum.midpoint.web.controller.config.DebugViewController;
import com.evolveum.midpoint.web.util.FacesUtils;
import com.evolveum.midpoint.xml.ns._public.common.common_1.OperationResultType;
import com.evolveum.midpoint.xml.ns._public.model.model_1.FaultMessage;
import com.evolveum.midpoint.xml.ns._public.model.model_1.ModelPortType;

@Controller("resourceDetails")
@Scope("session")
public class ResourceDetailsController implements Serializable {

	public static final String PAGE_NAVIGATION = "/resource/resourceDetails?faces-redirect=true";
	public static final String NAVIGATION_LEFT = "leftResourceDetails";
	public static final String PARAM_OBJECT_TYPE = "objectType";
	private static final long serialVersionUID = 8325385127604325634L;
	private static final Trace LOGGER = TraceManager.getTrace(ResourceDetailsController.class);
	@Autowired(required = true)
	private transient ModelPortType model;
	@Autowired(required = true)
	private transient DebugViewController debugView;
	@Autowired(required = true)
	private transient ResourceImportController importController;
	@Autowired(required = true)
	private transient ListObjectsController listObjects;
	@Autowired(required = true)
	private transient TemplateController template;
	private ResourceListItem resource;

	public ResourceListItem getResource() {
		if (resource == null) {
			resource = new ResourceListItem("Unknown", "Unknown", "Unknown", "Unknown");
		}
		return resource;
	}

	public void setResource(ResourceListItem resource) {
		this.resource = resource;
	}

	public String testConnection() {
		if (resource == null) {
			FacesUtils.addErrorMessage("Resource not found.");
			return null;
		}

		ResourceListController.testConnection(resource, model);
		return null;
	}

	public String showDebugPages() {
		debugView.setObject(new ObjectBean(getResource().getOid(), getResource().getName()));
		debugView.setEditOther(false);
		String returnPage = debugView.viewObject();
		if (DebugViewController.PAGE_NAVIGATION.equals(returnPage)) {
			template.setSelectedLeftId(DebugViewController.NAVIGATION_LEFT);
			template.setSelectedTopId(TemplateController.TOP_CONFIGURATION);
		}

		return returnPage;
	}

	public String importPerformed() {
		String objectType = FacesUtils.getRequestParameter(PARAM_OBJECT_TYPE);
		if (StringUtils.isEmpty(objectType)) {
			FacesUtils.addErrorMessage("Can't import objects. Object type not defined.");
			return null;
		}

		String objectClass = getObjectClass(objectType);
		if (StringUtils.isEmpty(objectClass)) {
			FacesUtils.addErrorMessage("Can't import objects. Object class for object type '" + objectType
					+ "' not found.");
			return null;
		}

		String nextPage = null;
		try {
			OperationResultType result = new OperationResultType();
			model.launchImportFromResource(getResource().getOid(), objectClass,
					new Holder<OperationResultType>(result));

			importController.setResource(getResource());
			nextPage = importController.initController();
		} catch (FaultMessage ex) {
			LoggingUtils.logException(LOGGER, "Couldn't launch import", ex);
			// TODO: result and error handling
		}

		if (ResourceImportController.PAGE_NAVIGATION.equals(nextPage)) {
			template.setSelectedLeftId(ResourceImportController.NAVIGATION_LEFT);
		}
		return nextPage;
	}

	public String deletePerformed() {
		// TODO: delete dialog & stuff

		template.setSelectedLeftId(ResourceListController.NAVIGATION_LEFT);
		return ResourceListController.PAGE_NAVIGATION;
	}

	public String showSyncStatus() {

		template.setSelectedLeftId(ResourceSyncController.NAVIGATION_LEFT);
		return ResourceSyncController.PAGE_NAVIGATION;
	}

	public String listPerformed() {
		String objectType = FacesUtils.getRequestParameter(PARAM_OBJECT_TYPE);
		if (StringUtils.isEmpty(objectType)) {
			FacesUtils.addErrorMessage("Can't list objects. Object type not defined.");
			return null;
		}

		String objectClass = getObjectClass(objectType);
		if (StringUtils.isEmpty(objectClass)) {
			FacesUtils.addErrorMessage("Can't import objects. Object class for object type '" + objectType
					+ "' not found.");
			return null;
		}

		listObjects.setResource(getResource());
		listObjects.setObjectClass(objectClass);
		return listObjects.listFirst();
	}

	private String getObjectClass(String objectType) {
		for (ResourceObjectType resObjectType : getResource().getObjectTypes()) {
			if (objectType.equals(resObjectType.getQualifiedType())) {
				// TODO: use native object class
				// objectClass = resObjectType.getNativeObjectClass();
				return resObjectType.getType();
			}
		}

		return null;
	}
}
