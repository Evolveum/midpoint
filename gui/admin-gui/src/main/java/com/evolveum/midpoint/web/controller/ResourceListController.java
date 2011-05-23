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
package com.evolveum.midpoint.web.controller;

import java.io.Serializable;
import java.util.List;

import javax.faces.event.ActionEvent;
import javax.faces.event.PhaseId;
import javax.faces.event.ValueChangeEvent;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import com.evolveum.midpoint.api.logging.Trace;
import com.evolveum.midpoint.common.Utils;
import com.evolveum.midpoint.logging.TraceManager;
import com.evolveum.midpoint.web.bean.ResourceListItem;
import com.evolveum.midpoint.web.bean.SortedResourceList;
import com.evolveum.midpoint.web.bean.ResourceListItem.ConnectionStatus;
import com.evolveum.midpoint.web.util.FacesUtils;
import com.evolveum.midpoint.xml.ns._public.common.common_1.Configuration;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectListType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PagingType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceTestResultType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceType;
import com.evolveum.midpoint.xml.ns._public.model.model_1.FaultMessage;
import com.evolveum.midpoint.xml.ns._public.model.model_1.ModelPortType;

@Controller("resourceList")
@Scope("session")
public class ResourceListController implements Serializable {

	public static final String PAGE_NAVIGATION_LIST = "/resource/index?faces-redirect=true";
	public static final String PAGE_NAVIGATION_DETAILS = "/resource/resourceDetails?faces-redirect=true";
	private static final long serialVersionUID = 8325385127604325633L;
	private static final Trace TRACE = TraceManager.getTrace(ResourceListController.class);
	@Autowired
	private transient ModelPortType model;
	private static final String PARAM_RESOURCE_OID = "resourceOid";
	private static final String DEFAULT_SORT_COLUMN = "name";
	private boolean selectAll = false;
	private SortedResourceList resources;

	public boolean isSelectAll() {
		return selectAll;
	}

	public void setSelectAll(boolean selectAll) {
		this.selectAll = selectAll;
	}

	public boolean isAscending() {
		return getResources().isAscending();
	}

	public void setAscending(boolean ascending) {
		getResources().setAscending(ascending);
	}

	public String getSortColumnName() {
		return getResources().getSortColumnName();
	}

	public void setSortColumnName(String sortColumnName) {
		getResources().setSortColumnName(sortColumnName);
	}

	public List<ResourceListItem> getResourceList() {
		SortedResourceList resources = getResources();

		return resources.getResources();
	}

	public void selectAllPerformed(ValueChangeEvent evt) {
		if (evt.getPhaseId() != PhaseId.INVOKE_APPLICATION) {
			evt.setPhaseId(PhaseId.INVOKE_APPLICATION);
			evt.queue();
		} else {
			boolean selectAll = ((Boolean) evt.getNewValue()).booleanValue();
			for (ResourceListItem item : getResourceList()) {
				item.setSelected(selectAll);
			}
		}
	}

	public void sortList(ActionEvent evt) {
		resources.sort();
	}

	// TODO: if check is unchecked remove selectAll flag
	public void selectPerformed(ValueChangeEvent evt) {
		boolean selected = ((Boolean) evt.getNewValue()).booleanValue();
		if (!selected) {
			selectAll = false;
		}
	}

	public String showResourceDetails() {
		String resourceOid = FacesUtils.getRequestParameter(PARAM_RESOURCE_OID);
		// TODO: check details

		return PAGE_NAVIGATION_DETAILS;
	}

	public void testConnection(ActionEvent evt) {
		String resourceOid = FacesUtils.getRequestParameter(PARAM_RESOURCE_OID);
		if (StringUtils.isEmpty(resourceOid)) {
			FacesUtils.addErrorMessage("Resource oid not defined in request.");
			return;
		}

		try {
			ResourceTestResultType result = model.testResource(resourceOid);
			// TODO: update connection status on resource list item
			ResourceListItem resource = null;
			for (ResourceListItem item : getResourceList()) {
				if (item.getOid().equals(resourceOid)) {
					resource = item;
					break;
				}
			}
			if (resource != null) {
				resource.setStatus(ConnectionStatus.SUCCESS);
			}
		} catch (FaultMessage ex) {
			String resourceName = resourceOid;
			for (ResourceListItem item : getResourceList()) {
				if (item.getOid().equals(resourceOid)) {
					resourceName = item.getName();
					break;
				}
			}
			FacesUtils.addErrorMessage("Couldn't test conection on resource '" + resourceName + "'.", ex);
			TRACE.trace("Couldn't test connection on resource '" + resourceName + "'", ex);
		}
	}

	public String updateController() {
		try {
			String objectType = Utils.getObjectType("ResourceType");
			ObjectListType objectList = model.listObjects(objectType, new PagingType());
			List<ObjectType> objects = objectList.getObject();

			List<ResourceListItem> list = getResourceList();
			list.clear();
			for (ObjectType object : objects) {
				list.add(createResourceListItem((ResourceType) object));
			}
			resources.sort();
		} catch (FaultMessage ex) {
			String message = (ex.getFaultInfo().getMessage() != null) ? ex.getFaultInfo().getMessage() : ex
					.getMessage();
			FacesUtils.addErrorMessage("List resources failed.");
			FacesUtils.addErrorMessage("Exception was: " + message);
			TRACE.error("List resources failed.", ex);
			return null;
		}

		return PAGE_NAVIGATION_LIST;
	}

	private ResourceListItem createResourceListItem(ResourceType resource) {
		String type = getConnectorInfo("bundleName", resource);
		String version = getConnectorInfo("bundleVersion", resource);

		return new ResourceListItem(resource.getOid(), resource.getName(), type, version);
	}

	private String getConnectorInfo(String name, ResourceType resource) {
		Configuration configuration = resource.getConfiguration();
		if (configuration != null) {
			for (Element element : configuration.getAny()) {
				NamedNodeMap attributes = element.getFirstChild().getAttributes();
				Node attribute = attributes.getNamedItem(name);
				if (attribute != null) {
					return attribute.getTextContent();
				}
			}
		}

		return "Unknown";
	}

	private SortedResourceList getResources() {
		if (resources == null) {
			resources = new SortedResourceList();
			resources.setSortColumnName(DEFAULT_SORT_COLUMN);
		}

		return resources;
	}
}
