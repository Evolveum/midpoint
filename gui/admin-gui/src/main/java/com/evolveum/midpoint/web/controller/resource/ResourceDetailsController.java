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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

import com.evolveum.midpoint.api.logging.Trace;
import com.evolveum.midpoint.logging.TraceManager;
import com.evolveum.midpoint.web.bean.ResourceListItem;
import com.evolveum.midpoint.web.util.FacesUtils;
import com.evolveum.midpoint.xml.ns._public.model.model_1.FaultMessage;
import com.evolveum.midpoint.xml.ns._public.model.model_1.ModelPortType;

@Controller("resourceDetails")
@Scope("session")
public class ResourceDetailsController implements Serializable {

	public static final String PAGE_NAVIGATION_LIST = "/resource/index?faces-redirect=true";
	private static final long serialVersionUID = 8325385127604325634L;
	private static final Trace TRACE = TraceManager.getTrace(ResourceDetailsController.class);
	@Autowired(required = true)
	private transient ModelPortType model;
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
			model.launchImportFromResource(resource.getOid(), objectClass);
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
}
