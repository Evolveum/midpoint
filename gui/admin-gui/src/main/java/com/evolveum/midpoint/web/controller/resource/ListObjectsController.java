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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.faces.model.ArrayDataModel;
import javax.faces.model.DataModel;
import javax.xml.namespace.QName;
import javax.xml.ws.Holder;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;
import org.w3c.dom.Element;

import com.evolveum.midpoint.api.logging.LoggingUtils;
import com.evolveum.midpoint.api.logging.Trace;
import com.evolveum.midpoint.common.result.OperationResult;
import com.evolveum.midpoint.logging.TraceManager;
import com.evolveum.midpoint.schema.PagingTypeFactory;
import com.evolveum.midpoint.schema.processor.PropertyContainerDefinition;
import com.evolveum.midpoint.schema.processor.ResourceObjectAttributeDefinition;
import com.evolveum.midpoint.schema.processor.ResourceObjectDefinition;
import com.evolveum.midpoint.schema.processor.Schema;
import com.evolveum.midpoint.schema.processor.SchemaProcessorException;
import com.evolveum.midpoint.web.bean.ResourceListItem;
import com.evolveum.midpoint.web.bean.ResourceObjectBean;
import com.evolveum.midpoint.web.bean.ResourceObjectType;
import com.evolveum.midpoint.web.controller.util.ControllerUtil;
import com.evolveum.midpoint.web.controller.util.ListController;
import com.evolveum.midpoint.web.model.ObjectTypeCatalog;
import com.evolveum.midpoint.web.util.FacesUtils;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectListType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.OperationResultType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.OrderDirectionType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceObjectShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceType;
import com.evolveum.midpoint.xml.ns._public.model.model_1.FaultMessage;
import com.evolveum.midpoint.xml.ns._public.model.model_1.ModelPortType;

/**
 * 
 * @author lazyman
 * 
 */
@Controller("listObjects")
@Scope("session")
public class ListObjectsController extends ListController<ResourceObjectBean> implements Serializable {

	public static final String PAGE_NAVIGATION = "/resource/listObjects?faces-redirect=true";
	private static final long serialVersionUID = -3538520581983462635L;
	private static final Trace LOGGER = TraceManager.getTrace(ListObjectsController.class);
	@Autowired(required = true)
	private ObjectTypeCatalog objectTypeCatalog;
	@Autowired(required = true)
	private transient ModelPortType model;
	private ResourceListItem resource;
	private String objectClass;
	private DataModel<String> columnModel;
	private DataModel<ResourceObjectBean> rowModel;

	public void setObjectClass(String objectClass) {
		this.objectClass = objectClass;
	}

	public String getObjectClass() {
		return objectClass;
	}

	public void setResource(ResourceListItem resource) {
		this.resource = resource;
	}

	public DataModel<ResourceObjectBean> getRowModel() {
		return rowModel;
	}

	public DataModel<String> getColumnModel() {
		return columnModel;
	}

	@Override
	protected String listObjects() {
		if (resource == null) {
			FacesUtils.addErrorMessage("Resource was not defined.");
			return null;
		}

		if (objectClass == null) {
			FacesUtils.addErrorMessage("Object class was not defined.");
			return null;
		}

		getObjects().clear();

		List<QName> columnHeaders = prepareHeader(resource.getOid());
		List<String> header = new ArrayList<String>();
		for (QName qname : columnHeaders) {
			header.add(qname.getLocalPart());
		}
		columnModel = new ArrayDataModel<String>(header.toArray(new String[header.size()]));

		List<ObjectType> objects = getResourceObjects();
		if (objects == null || objects.isEmpty()) {
			// TODO: show & log error
			FacesUtils.addWarnMessage("No object found for objet class '" + objectClass + "'.");
			return null;
		}

		for (ObjectType objectType : objects) {
			String oid = "Unknown";
			if (StringUtils.isNotEmpty(objectType.getOid())) {
				oid = objectType.getOid();
			}

			Map<String, String> attributes = new HashMap<String, String>();
			ResourceObjectShadowType account = (ResourceObjectShadowType) objectType;
			List<Element> elements = account.getAttributes().getAny();
			for (QName qname : columnHeaders) {
				attributes.put(qname.getLocalPart(), getElementValue(elements, qname));
			}

			getObjects().add(new ResourceObjectBean(oid, objectType.getName(), attributes));
		}

		rowModel = new ArrayDataModel<ResourceObjectBean>(getObjects().toArray(
				new ResourceObjectBean[getObjects().size()]));

		return PAGE_NAVIGATION;
	}

	private List<ObjectType> getResourceObjects() {
		ObjectListType list = null;
		try {
			OperationResultType resultType = new OperationResultType();
			list = model.listResourceObjects(resource.getOid(), objectClass,
					PagingTypeFactory.createPaging(0, 30, OrderDirectionType.ASCENDING, "name"),
					new Holder<OperationResultType>(resultType));
		} catch (FaultMessage ex) {
			LoggingUtils.logException(LOGGER, "Couldn't list objects", ex);
			// TODO: show & log error
		}

		if (list == null) {
			return null;
		}

		return list.getObject();
	}

	private List<QName> prepareHeader(String resourceOid) {
		List<QName> qnames = new ArrayList<QName>();
		OperationResult result = new OperationResult("Prepare Header");
		ResourceType resource = ControllerUtil.getObjectFromModel(resourceOid, model, result,
				ResourceType.class);
		if (resource == null || resource.getSchema() == null || resource.getSchema().getAny().isEmpty()) {
			return qnames;
		}

		Schema schema = null;
		try {
			schema = Schema.parse(resource.getSchema().getAny().get(0));
		} catch (SchemaProcessorException ex) {
			LoggingUtils.logException(LOGGER, "Couldn't parse resource schema", ex);
			// TODO: error handling
		}
		if (schema == null) {
			return qnames;
		}

		QName objectType = findObjectType(this.objectClass);
		if (objectType == null) {
			return qnames;
		}
		PropertyContainerDefinition container = schema.findContainerDefinitionByType(objectType);
		if (container instanceof ResourceObjectDefinition) {
			ResourceObjectDefinition definition = (ResourceObjectDefinition) container;
			for (ResourceObjectAttributeDefinition attribute : definition.getIdentifiers()) {
				qnames.add(attribute.getName());
			}
			for (ResourceObjectAttributeDefinition attribute : definition.getSecondaryIdentifiers()) {
				qnames.add(attribute.getName());
			}

			if (definition.getDisplayNameAttribute() != null) {
				qnames.add(definition.getDisplayNameAttribute().getName());
			}
		}

		return qnames;
	}

	private QName findObjectType(String objectClass) {
		for (ResourceObjectType type : this.resource.getObjectTypes()) {
			if (type.getSimpleType().equals(objectClass)) {
				return type.getType();
			}
		}

		return null;
	}

	private String getElementValue(List<Element> elements, QName qname) {
		List<Element> elementList = new ArrayList<Element>();
		for (Element element : elements) {
			if (element.getLocalName().equals(qname.getLocalPart())
					&& element.getNamespaceURI().equals(qname.getNamespaceURI())) {
				elementList.add(element);
			}
		}

		StringBuilder builder = new StringBuilder();
		for (Element element : elementList) {
			builder.append(element.getTextContent());
			if (elementList.indexOf(element) != elementList.size() - 1) {
				builder.append("\n");
			}
		}

		return builder.toString();
	}

	public Object getAttributeValue() {
		if (getRowModel().isRowAvailable()) {
			ResourceObjectBean row = getRowModel().getRowData();
			return row.getAttributes().get(getColumnModel().getRowData());
		}

		return null;
	}
}
