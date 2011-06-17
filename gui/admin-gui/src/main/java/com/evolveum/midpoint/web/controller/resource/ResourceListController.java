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
import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.faces.event.ActionEvent;
import javax.faces.event.ValueChangeEvent;
import javax.xml.bind.JAXBElement;
import javax.xml.ws.Holder;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import com.evolveum.midpoint.api.logging.LoggingUtils;
import com.evolveum.midpoint.api.logging.Trace;
import com.evolveum.midpoint.common.result.OperationResult;
import com.evolveum.midpoint.logging.TraceManager;
import com.evolveum.midpoint.schema.ObjectTypes;
import com.evolveum.midpoint.schema.processor.Definition;
import com.evolveum.midpoint.schema.processor.ResourceObjectDefinition;
import com.evolveum.midpoint.schema.processor.Schema;
import com.evolveum.midpoint.schema.processor.SchemaProcessorException;
import com.evolveum.midpoint.web.bean.ResourceListItem;
import com.evolveum.midpoint.web.bean.ResourceObjectType;
import com.evolveum.midpoint.web.bean.ResourceState;
import com.evolveum.midpoint.web.bean.ResourceStatus;
import com.evolveum.midpoint.web.controller.TemplateController;
import com.evolveum.midpoint.web.controller.util.ControllerUtil;
import com.evolveum.midpoint.web.controller.util.SortableListController;
import com.evolveum.midpoint.web.util.FacesUtils;
import com.evolveum.midpoint.web.util.ResourceItemComparator;
import com.evolveum.midpoint.xml.ns._public.common.common_1.Configuration;
import com.evolveum.midpoint.xml.ns._public.common.common_1.DiagnosticsMessageType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectListType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.OperationResultType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PagingType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceObjectShadowListType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceTestResultType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceTestResultType.ExtraTest;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.TestResultType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.XmlSchemaType;
import com.evolveum.midpoint.xml.ns._public.model.model_1.FaultMessage;
import com.evolveum.midpoint.xml.ns._public.model.model_1.ModelPortType;

/**
 * 
 * @author lazyman
 * 
 */
@Controller("resourceList")
@Scope("session")
public class ResourceListController extends SortableListController<ResourceListItem> {

	public static final String PAGE_NAVIGATION = "/resource/index?faces-redirect=true";
	public static final String NAVIGATION_LEFT = "leftResourceList";
	private static final String PARAM_RESOURCE_OID = "resourceOid";
	private static final long serialVersionUID = 8325385127604325633L;
	private static final Trace TRACE = TraceManager.getTrace(ResourceListController.class);
	@Autowired(required = true)
	private transient ModelPortType model;
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
	
	public void hideConfirmDelete(){
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

		testConnection(resource, model);
	}

	static void testConnection(ResourceListItem resourceItem, ModelPortType model) {
		try {
			ResourceTestResultType result = model.testResource(resourceItem.getOid(),
					new Holder<OperationResultType>(new OperationResultType()));
			updateResourceState(resourceItem.getState(), result);
		} catch (FaultMessage ex) {
			String message = "Couldn't test conection on resource '" + resourceItem.getName() + "'.";
			FacesUtils.addErrorMessage(message, ex);
			TRACE.trace(message, ex);
		}
	}

	private static void updateResourceState(ResourceState state, ResourceTestResultType result) {
		ExtraTest extra = result.getExtraTest();
		if (extra != null) {
			state.setExtraName(extra.getName());
			state.setExtra(getStatusFromResultType(extra.getResult()));
		}
		state.setConConnection(getStatusFromResultType(result.getConnectorConnection()));
		state.setConfValidation(getStatusFromResultType(result.getConfigurationValidation()));
		state.setConInitialization(getStatusFromResultType(result.getConnectorInitialization()));
		state.setConSanity(getStatusFromResultType(result.getConnectorSanity()));
		state.setConSchema(getStatusFromResultType(result.getConnectorSchema()));
	}

	private static ResourceStatus getStatusFromResultType(TestResultType result) {
		if (result == null) {
			return ResourceStatus.NOT_TESTED;
		}

		ResourceStatus status = result.isSuccess() ? ResourceStatus.SUCCESS : ResourceStatus.ERROR;

		List<JAXBElement<DiagnosticsMessageType>> messages = result.getErrorOrWarning();
		for (JAXBElement<DiagnosticsMessageType> element : messages) {
			DiagnosticsMessageType message = element.getValue();
			StringBuilder builder = new StringBuilder();
			builder.append(message.getMessage());
			if (!StringUtils.isEmpty(message.getDetails())) {
				builder.append("Reason: ");
				builder.append(message.getDetails());
			}
			if (message.getTimestamp() != null) {
				builder.append("Time: ");
				builder.append(message.getTimestamp().toGregorianCalendar().getTime());
			}
			FacesUtils.addErrorMessage(builder.toString());
		}

		return status;
	}

	private ResourceListItem createResourceListItem(ResourceType resource) {
		String type = getConnectorInfo("bundleName", resource);
		String version = getConnectorInfo("bundleVersion", resource);

		ResourceListItem item = new ResourceListItem(resource.getOid(), resource.getName(), type, version);
		XmlSchemaType xmlSchema = resource.getSchema();
		if (xmlSchema == null || xmlSchema.getAny().isEmpty()) {
			return item;
		}

		try {
			Schema schema = Schema.parse(xmlSchema.getAny().get(0));
			Set<Definition> definitions = schema.getDefinitions();
			for (Definition definition : definitions) {
				if (!(definition instanceof ResourceObjectDefinition)) {
					continue;
				}

				ResourceObjectDefinition objectDefinition = (ResourceObjectDefinition) definition;
				item.getObjectTypes().add(new ResourceObjectType(objectDefinition));
			}
		} catch (SchemaProcessorException ex) {
			FacesUtils.addErrorMessage("Couldn't parse schema for resource '" + resource.getName()
					+ "', reason: " + ex.getMessage());
		}

		return item;
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

	//TODO: MOVE TO MODEL !!!!!!!!!!!!!!!!!!!!!!! test before delete resource
	private List<ResourceListItem> testResourcesBeforeDelete() {
		List<ResourceListItem> toBeDeleted = new ArrayList<ResourceListItem>();
		for (ResourceListItem item : getObjects()) {
			OperationResult result = new OperationResult("List Resource Object Shadows");

			boolean canDelete = true;
			for (ResourceObjectType objectType : item.getObjectTypes()) {
				try {
					ResourceObjectShadowListType list = model.listResourceObjectShadows(item.getOid(),
							objectType.getSimpleType(),
							new Holder<OperationResultType>(result.createOperationResultType()));
					if (list != null && !list.getObject().isEmpty()) {
						FacesUtils.addErrorMessage("Can't delete resource '" + item.getName()
								+ "', it's referenced by " + list.getObject().size() + " objects of type '"
								+ objectType.getSimpleType() + "'.");
						canDelete = false;
						break;
					}
				} catch (FaultMessage ex) {
					LoggingUtils.logException(TRACE,
							"Couldn't list resource objects of type {} for resource {}", ex,
							objectType.getSimpleType(), item.getName());
					// TODO: error handling
					canDelete = false;
				}
			}

			if (canDelete) {
				toBeDeleted.add(item);
			}
		}

		return toBeDeleted;
	}
	
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
				// TODO: holder and result
				model.deleteObject(item.getOid(), new Holder<OperationResultType>());				
				toBeDeleted.add(item);
			} catch (FaultMessage ex) {
				LoggingUtils.logException(TRACE, "Couldn't delete resource {}", ex, item.getName());
				//TODO: error handling
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
			String objectType = ObjectTypes.RESOURCE.getObjectTypeUri();
			ObjectListType objectList = model.listObjects(objectType, new PagingType(),
					new Holder<OperationResultType>(new OperationResultType()));
			List<ObjectType> objects = objectList.getObject();

			List<ResourceListItem> list = getObjects();
			list.clear();
			for (ObjectType object : objects) {
				list.add(createResourceListItem((ResourceType) object));
			}
			sort();
		} catch (FaultMessage ex) {
			String message = (ex.getFaultInfo().getMessage() != null) ? ex.getFaultInfo().getMessage() : ex
					.getMessage();
			FacesUtils.addErrorMessage("List resources failed.");
			FacesUtils.addErrorMessage("Exception was: " + message);
			TRACE.error("List resources failed.", ex);
			return null;
		}

		template.setSelectedLeftId(NAVIGATION_LEFT);
		return PAGE_NAVIGATION;
	}
}
