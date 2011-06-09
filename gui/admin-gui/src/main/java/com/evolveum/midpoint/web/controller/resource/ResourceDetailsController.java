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

import com.evolveum.midpoint.api.logging.Trace;
import com.evolveum.midpoint.logging.TraceManager;
import com.evolveum.midpoint.web.bean.DebugObject;
import com.evolveum.midpoint.web.bean.ResourceListItem;
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
	private static final Trace TRACE = TraceManager.getTrace(ResourceDetailsController.class);
	@Autowired(required = true)
	private transient ModelPortType model;
	@Autowired(required = true)
	private transient DebugViewController debugView;
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
		debugView.setObject(new DebugObject(getResource().getOid(), getResource().getName()));
		debugView.setEditOther(false);
		String returnPage = debugView.viewObject();
		if (DebugViewController.PAGE_NAVIGATION.equals(returnPage)) {
			template.setSelectedLeftId(DebugViewController.NAVIGATION_LEFT);
			template.setSelectedTopId(TemplateController.TOP_CONFIGURATION);
		}

		return returnPage;
	}

	public String importFromResource() {
		if (resource == null) {
			FacesUtils.addErrorMessage("Resource must be selected");
			return null;
		}

		try {
			// TODO: HACK: this should be determined from the resource schema.
			// But ICF always generates the name for __ACCOUNT__ like this.
			String objectClass = "Account";

			TRACE.debug("Calling launchImportFromResource({})", resource.getOid());
			model.launchImportFromResource(resource.getOid(), objectClass, new Holder<OperationResultType>(
					new OperationResultType()));
		} catch (FaultMessage ex) {
			String message = (ex.getFaultInfo().getMessage() != null) ? ex.getFaultInfo().getMessage() : ex
					.getMessage();
			FacesUtils.addErrorMessage("Launching import from resource failed: " + message);
			TRACE.error("Launching import from resources failed.", ex);
		} catch (RuntimeException ex) {
			// Due to insane preferrence of runtime exception in "modern" Java
			// we need to catch all of them. These may happen in JBI layer and
			// we are not even sure which exceptions to cacht.
			// To a rough hole a rough patch.
			FacesUtils.addErrorMessage("Launching import from resource failed. Runtime error: "
					+ ex.getClass().getSimpleName() + ": " + ex.getMessage());
			TRACE.error("Launching import from resources failed. Runtime error.", ex);
		}

		return null;
	}

	public String showImportStatus() {

		template.setSelectedLeftId(ResourceImportController.NAVIGATION_LEFT);
		return ResourceImportController.PAGE_NAVIGATION;
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
		
		return null;
	}
	
	public String importPerformed() {
		String objectType = FacesUtils.getRequestParameter(PARAM_OBJECT_TYPE);
		if (StringUtils.isEmpty(objectType)) {
			FacesUtils.addErrorMessage("Can't import objects. Object type not defined.");
			return null;
		}
		
		
		template.setSelectedLeftId(ResourceImportController.NAVIGATION_LEFT);
		return ResourceImportController.PAGE_NAVIGATION;
	}
	
	public String deletePerformed() {
		
		template.setSelectedLeftId(ResourceListController.NAVIGATION_LEFT);
		return ResourceListController.PAGE_NAVIGATION;
	}
}
