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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.faces.event.ActionEvent;
import javax.faces.event.ValueChangeEvent;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

import com.evolveum.midpoint.common.refinery.RefinedResourceSchema;
import com.evolveum.midpoint.schema.exception.SchemaException;
import com.evolveum.midpoint.schema.processor.Definition;
import com.evolveum.midpoint.schema.processor.ResourceObjectDefinition;
import com.evolveum.midpoint.schema.processor.Schema;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ResourceTypeUtil;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.bean.ResourceListItem;
import com.evolveum.midpoint.web.bean.ResourceObjectType;
import com.evolveum.midpoint.web.controller.TemplateController;
import com.evolveum.midpoint.web.controller.util.ControllerUtil;
import com.evolveum.midpoint.web.controller.util.SortableListController;
import com.evolveum.midpoint.web.model.ObjectTypeCatalog;
import com.evolveum.midpoint.web.model.ResourceManager;
import com.evolveum.midpoint.web.model.dto.ConnectorDto;
import com.evolveum.midpoint.web.model.dto.ConnectorHostDto;
import com.evolveum.midpoint.web.model.dto.GuiResourceDto;
import com.evolveum.midpoint.web.model.dto.ResourceDto;
import com.evolveum.midpoint.web.util.FacesUtils;
import com.evolveum.midpoint.web.util.ResourceItemComparator;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceType;

/**
 * 
 * @author lazyman
 * 
 */
@Controller("resourceList")
@Scope("session")
public class ResourceListController extends SortableListController<ResourceListItem> {

	public static final String PAGE_NAVIGATION = "/admin/resource/index?faces-redirect=true";
	public static final String NAVIGATION_LEFT = "leftResourceList";
	private static final String PARAM_RESOURCE_OID = "resourceOid";
	private static final String PARAM_CONNECTOR_HOST_OID = "connectorHostOid";
	private static final long serialVersionUID = 8325385127604325633L;
	private static final Trace LOGGER = TraceManager.getTrace(ResourceListController.class);
	@Autowired(required = true)
	private transient ObjectTypeCatalog objectTypeCatalog;
	@Autowired(required = true)
	private transient TemplateController template;
	@Autowired(required = true)
	private transient ResourceDetailsController resourceDetails;
	@Autowired(required = true)
	private transient ResourceSyncController resourceSync;
	private boolean selectAll = false;
	private boolean showPopup = false;

	public ResourceListController() {
		super("name");
	}

	public boolean isShowPopup() {
		return showPopup;
	}

	public void hideConfirmDelete() {
		showPopup = false;
	}

	public boolean isSelectAll() {
		return selectAll;
	}

	public void setSelectAll(boolean selectAll) {
		this.selectAll = selectAll;
	}

	public void selectAllPerformed(ValueChangeEvent evt) {
		ControllerUtil.selectAllPerformed(evt, getObjects());
	}

	public void sortList(ActionEvent evt) {
		sort();
	}

	public void selectPerformed(ValueChangeEvent evt) {
		this.selectAll = ControllerUtil.selectPerformed(evt, getObjects());
	}

	private ResourceListItem getSelectedResourceItem() {
		String resourceOid = FacesUtils.getRequestParameter(PARAM_RESOURCE_OID);
		if (StringUtils.isEmpty(resourceOid)) {
			FacesUtils.addErrorMessage("Resource oid not defined in request.");
			return null;
		}

		ResourceListItem resourceItem = getResourceItem(resourceOid);
		if (StringUtils.isEmpty(resourceOid)) {
			FacesUtils.addErrorMessage("Resource for oid '" + resourceOid + "' not found.");
			return null;
		}

		if (resourceDetails == null) {
			FacesUtils.addErrorMessage("Resource details controller was not autowired.");
			return null;
		}

		return resourceItem;
	}

	public String showResourceDetails() {
		ResourceListItem resourceItem = getSelectedResourceItem();
		if (resourceItem == null) {
			return null;
		}

		resourceDetails.setResource(resourceItem);
		if(resourceDetails.listObjects() != null){
			resourceDetails.setOffset(0);
		}
			
		template.setSelectedLeftId(ResourceDetailsController.NAVIGATION_LEFT);
		return ResourceDetailsController.PAGE_NAVIGATION;
	}

	private ResourceListItem getResourceItem(String resourceOid) {
		for (ResourceListItem item : getObjects()) {
			if (item.getOid().equals(resourceOid)) {
				return item;
			}
		}

		return null;
	}

	public void testConnection(ActionEvent evt) {
		String resourceOid = FacesUtils.getRequestParameter(PARAM_RESOURCE_OID);
		if (StringUtils.isEmpty(resourceOid)) {
			FacesUtils.addErrorMessage("Resource oid not defined in request.");
			return;
		}

		ResourceListItem resource = getResourceItem(resourceOid);
		if (resource == null) {
			FacesUtils.addErrorMessage("Resource with oid '" + resourceOid + "' not found.");
			return;
		}

		try {
			ResourceManager manager = ControllerUtil.getResourceManager(objectTypeCatalog);
			LOGGER.debug("Testing connection for resource {}, oid '{}'.", new Object[] { resource.getName(),
					resource.getOid() });
			OperationResult result = manager.testConnection(resource.getOid());
			ControllerUtil.updateResourceState(resource.getState(), result);
		} catch (Exception ex) {
			LoggingUtils.logException(LOGGER, "Couldn't test resource {}", ex, resource.getName());
			FacesUtils.addErrorMessage("Couldn't test resource '" + resource.getName() + "'.", ex);
		}
	}

	private ResourceListItem createResourceListItem(ResourceType resource) {
		ObjectReferenceType reference = resource.getConnectorRef();
		if (reference == null) {
			// TODO: error handling
			return null;
		}

		ResourceManager manager = ControllerUtil.getResourceManager(objectTypeCatalog);
		ConnectorDto connector = manager.getConnector(reference.getOid());

		String type = connector.getConnectorType();
		String version = connector.getConnectorVersion();

		ResourceListItem item = new ResourceListItem(resource.getOid(), resource.getName(), type, version);
		try {
			Schema schema = RefinedResourceSchema.getResourceSchema(resource);
			if (schema == null) {
				LOGGER.debug("Schema for resource {} was null.", new Object[] { resource.getName() });
				return item;
			}
			Collection<Definition> definitions = schema.getDefinitions();
			for (Definition definition : definitions) {
				if (!(definition instanceof ResourceObjectDefinition)) {
					continue;
				}

				ResourceObjectDefinition objectDefinition = (ResourceObjectDefinition) definition;
				item.getObjectTypes().add(new ResourceObjectType(objectDefinition));
			}
		} catch (SchemaException ex) {
			FacesUtils.addErrorMessage("Couldn't parse schema for resource '" + resource.getName()
					+ "', reason: " + ex.getMessage());
		}

		return item;
	}

	public String showSyncStatus() {
		ResourceListItem resourceItem = getSelectedResourceItem();
		if (resourceItem == null) {
			return null;
		}

		resourceDetails.setResource(resourceItem);
		resourceSync.setResource(resourceItem);

		template.setSelectedLeftId(ResourceSyncController.NAVIGATION_LEFT);
		return ResourceSyncController.PAGE_NAVIGATION;
	}

	// TODO: MOVE TO MODEL !!!!!!!!!!!!!!!!!!!!!!! test before delete resource
	// private List<ResourceListItem> testResourcesBeforeDelete() {
	// List<ResourceListItem> toBeDeleted = new ArrayList<ResourceListItem>();
	// for (ResourceListItem item : getObjects()) {
	// OperationResult result = new
	// OperationResult("List Resource Object Shadows");
	//
	// boolean canDelete = true;
	// for (ResourceObjectType objectType : item.getObjectTypes()) {
	// try {
	// // TODO: model not available not, user managers
	// ResourceObjectShadowListType list =
	// model.listResourceObjectShadows(item.getOid(),
	// objectType.getSimpleType(),
	// new Holder<OperationResultType>(result.createOperationResultType()));
	// if (list != null && !list.getObject().isEmpty()) {
	// FacesUtils.addErrorMessage("Can't delete resource '" + item.getName()
	// + "', it's referenced by " + list.getObject().size() +
	// " objects of type '"
	// + objectType.getSimpleType() + "'.");
	// canDelete = false;
	// break;
	// }
	// } catch (FaultMessage ex) {
	// LoggingUtils.logException(TRACE,
	// "Couldn't list resource objects of type {} for resource {}", ex,
	// objectType.getSimpleType(), item.getName());
	// // TODO: error handling
	// canDelete = false;
	// }
	// }
	//
	// if (canDelete) {
	// toBeDeleted.add(item);
	// }
	// }
	//
	// return toBeDeleted;
	// }

	public void deletePerformed() {
		showPopup = true;
	}

	public String deleteResources() {
		hideConfirmDelete();

		List<ResourceListItem> toBeDeleted = new ArrayList<ResourceListItem>();
		for (ResourceListItem item : getObjects()) {
			if (!item.isSelected()) {
				continue;
			}
			try {
				ResourceManager manager = ControllerUtil.getResourceManager(objectTypeCatalog);
				manager.delete(item.getOid());

				toBeDeleted.add(item);
			} catch (Exception ex) {
				LoggingUtils.logException(LOGGER, "Couldn't delete resource {}", ex, item.getName());
				FacesUtils.addErrorMessage("Couldn't delete resource.", ex);
			}
		}
		getObjects().removeAll(toBeDeleted);

		return PAGE_NAVIGATION;
	}

	@Override
	protected void sort() {
		Collections.sort(getObjects(), new ResourceItemComparator(getSortColumnName(), isAscending()));
	}

	@Override
	protected String listObjects() {
		try {
			ResourceManager manager = ControllerUtil.getResourceManager(objectTypeCatalog);
			Collection<GuiResourceDto> resources = manager.list();

			List<ResourceListItem> list = getObjects();
			list.clear();
			for (ResourceDto resource : resources) {
				if(resource.getXmlObject().getFetchResult() != null){
					FacesUtils.addErrorMessage(resource.getXmlObject().getName() + ": " +resource.getXmlObject().getFetchResult().getMessage());
				}
				list.add(createResourceListItem(resource.getXmlObject()));
				
			}
			sort();
		} catch (Exception ex) {
			final String message = "Unknown error occured while listing resources";

			LoggingUtils.logException(LOGGER, message, ex);
			FacesUtils.addErrorMessage(message);

			return null;
		}

		template.setSelectedLeftId(NAVIGATION_LEFT);
		return PAGE_NAVIGATION;
	}

	public List<ConnectorHostDto> getConnectorHosts() {
		List<ConnectorHostDto> connectors = new ArrayList<ConnectorHostDto>();
		try {
			ResourceManager manager = ControllerUtil.getResourceManager(objectTypeCatalog);
			Collection<ConnectorHostDto> collection = manager.listConnectorHosts();
			if (collection != null) {
				connectors.addAll(collection);
			}
		} catch (Exception ex) {
			final String message = "Unknown error occured while listing connector hosts";

			LoggingUtils.logException(LOGGER, message, ex);
			FacesUtils.addErrorMessage(message);
		}

		return connectors;
	}

	public void discoverPerformed() {
		String connectorHostOid = FacesUtils.getRequestParameter(PARAM_CONNECTOR_HOST_OID);
		if (StringUtils.isEmpty(connectorHostOid)) {
			FacesUtils.addErrorMessage("Connector host oid not defined in request.");
			return;
		}

		try {
			List<ConnectorHostDto> hosts = getConnectorHosts();
			ConnectorHostDto connectorHost = null;
			for (ConnectorHostDto host : hosts) {
				if (connectorHostOid.equals(host.getOid())) {
					connectorHost = host;
					break;
				}
			}

			if (connectorHost == null) {
				FacesUtils.addErrorMessage("Connector host with oid '" + connectorHostOid
						+ "' was not found.");
				return;
			}

			ResourceManager manager = ControllerUtil.getResourceManager(objectTypeCatalog);
			manager.discoverConnectorsOnHost(connectorHost);
		} catch (Exception ex) {
			final String message = "Couldn't discover connectors";

			LoggingUtils.logException(LOGGER, message, ex);
			FacesUtils.addErrorMessage(message);
		}
	}
}
