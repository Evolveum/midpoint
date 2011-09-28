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
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.faces.model.ArrayDataModel;
import javax.faces.model.DataModel;
import javax.xml.namespace.QName;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

import com.evolveum.midpoint.schema.PagingTypeFactory;
import com.evolveum.midpoint.schema.exception.SchemaException;
import com.evolveum.midpoint.schema.processor.PropertyContainerDefinition;
import com.evolveum.midpoint.schema.processor.PropertyDefinition;
import com.evolveum.midpoint.schema.processor.ResourceObjectAttributeDefinition;
import com.evolveum.midpoint.schema.processor.ResourceObjectDefinition;
import com.evolveum.midpoint.schema.processor.Schema;
import com.evolveum.midpoint.schema.util.JAXBUtil;
import com.evolveum.midpoint.schema.util.ResourceTypeUtil;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.bean.ResourceListItem;
import com.evolveum.midpoint.web.bean.ResourceObjectBean;
import com.evolveum.midpoint.web.controller.util.ControllerUtil;
import com.evolveum.midpoint.web.controller.util.ListController;
import com.evolveum.midpoint.web.model.ObjectTypeCatalog;
import com.evolveum.midpoint.web.model.ResourceManager;
import com.evolveum.midpoint.web.model.dto.ResourceDto;
import com.evolveum.midpoint.web.model.dto.ResourceObjectShadowDto;
import com.evolveum.midpoint.web.util.FacesUtils;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.OrderDirectionType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PropertyReferenceListType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceObjectShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceType;

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
	private static final int MAX_COLUMNS = 6;
	@Autowired(required = true)
	private ObjectTypeCatalog objectTypeCatalog;
	private ResourceListItem resource;
	private QName objectClass;
	private List<String> columns;
	private DataModel<ResourceObjectBean> rowModel;
	private DataModel<String> columnModel;

	public void setObjectClass(QName objectClass) {
		this.objectClass = objectClass;
	}

	public QName getObjectClass() {
		return objectClass;
	}

	public void setResource(ResourceListItem resource) {
		this.resource = resource;
	}

	public DataModel<ResourceObjectBean> getRowModel() {
		if (rowModel == null) {
			rowModel = new ArrayDataModel<ResourceObjectBean>(getObjects().toArray(
					new ResourceObjectBean[getObjects().size()]));
		}
		return rowModel;
	}

	public DataModel<String> getColumnModel() {
		if (columns == null) {
			columns = new ArrayList<String>();
		}

		if (columnModel == null) {
			columnModel = new ArrayDataModel<String>(columns.toArray(new String[columns.size()]));
		}
		return columnModel;
	}

	public String backPerformed() {
		rowModel = null;
		columnModel = null;
		return ResourceDetailsController.PAGE_NAVIGATION;
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
		columns = new ArrayList<String>();
		


		List<ObjectType> objects = getResourceObjects();
		if (objects == null || objects.isEmpty()) {
			FacesUtils.addWarnMessage("No object found for object class '" + objectClass + "'.");
			return null;
		}
		ResourceObjectShadowType account = (ResourceObjectShadowType) objects.get(0);
		List<Object> accountElements = account.getAttributes().getAny();
		for (Iterator<QName> i = columnHeaders.iterator(); i.hasNext();){
			QName qname = i.next();
			String elementValue = getElementValue(accountElements, qname);
			if (StringUtils.isNotEmpty(elementValue) && columns.size() < MAX_COLUMNS) {
				columns.add(qname.getLocalPart());
			} else{
				i.remove();
			}

		}

		for (ObjectType objectType : objects) {
			String oid = "Unknown";
			if (StringUtils.isNotEmpty(objectType.getOid())) {
				oid = objectType.getOid();
			}
			String name = "Unknown";
			if (StringUtils.isNotEmpty(objectType.getName())) {
				name = objectType.getName();
			}

			Map<String, String> attributes = new HashMap<String, String>();
			account = (ResourceObjectShadowType) objectType;
			List<Object> elements = account.getAttributes().getAny();
			for (QName qname : columnHeaders) {
				String elementValue = getElementValue(elements, qname);
				if (StringUtils.isNotEmpty(elementValue) && attributes.size() < MAX_COLUMNS) {
					attributes.put(qname.getLocalPart(), elementValue);
				}

			}

			getObjects().add(new ResourceObjectBean(oid, name, attributes));
		}
//
//		if (!getObjects().isEmpty()) {
//			for (String qname : getObjects().get(0).getAttributes().keySet()) {
//				columns.add(qname);
//			}
//		}
		setRowsCount(getObjects().size());

		return PAGE_NAVIGATION;
	}

	private List<ObjectType> getResourceObjects() {
		List<ObjectType> objects = new ArrayList<ObjectType>();
		try {
			ResourceManager manager = ControllerUtil.getResourceManager(objectTypeCatalog);
			Collection<ResourceObjectShadowDto<ResourceObjectShadowType>> collection = manager
					.listResourceObjects(resource.getOid(), objectClass, PagingTypeFactory.createPaging(
							getOffset(), getRowsCount(), OrderDirectionType.ASCENDING, "name"));
			for (ResourceObjectShadowDto<ResourceObjectShadowType> shadow : collection) {
				objects.add(shadow.getXmlObject());
			}
		} catch (Exception ex) {
			LoggingUtils.logException(LOGGER, "Couldn't list resource objects", ex);
		}

		return objects;
	}

	private List<QName> prepareHeader(String resourceOid) {
		List<QName> qnames = new ArrayList<QName>();

		ResourceType resource = null;
		try {
			ResourceManager manager = ControllerUtil.getResourceManager(objectTypeCatalog);
			ResourceDto resourceDto = manager.get(resourceOid, new PropertyReferenceListType());
			if (resourceDto != null) {
				resource = resourceDto.getXmlObject();
			}
		} catch (Exception ex) {
			LoggingUtils.logException(LOGGER, "Couldn't get resource with oid {}", ex, resourceOid);
			FacesUtils.addErrorMessage("Couldn't get resource with oid '" + resourceOid + "'.", ex);
			return qnames;
		}

		if (resource == null) {
			return qnames;
		}

		Schema schema = null;
		try {
			schema = ResourceTypeUtil.getResourceSchema(resource);
		} catch (SchemaException ex) {
			LoggingUtils.logException(LOGGER, "Couldn't parse resource schema", ex);
			FacesUtils.addErrorMessage("Couldn't parse resource schema.", ex);
		}

		if (schema == null) {
			return qnames;
		}

		if (this.objectClass == null) {
			return qnames;
		}

		PropertyContainerDefinition container = schema.findContainerDefinitionByType(objectClass);
		if (container instanceof ResourceObjectDefinition) {
			ResourceObjectDefinition definition = (ResourceObjectDefinition) container;

			for (PropertyDefinition attribute : definition.getPropertyDefinitions()) {
				LOGGER.debug("Adding {} as header (property).", new Object[] { attribute.getName() });
				if (isPropertyAccetable(attribute.getName(), resource)) {
					qnames.add(attribute.getName());
				}
			}
			// for (ResourceObjectAttributeDefinition attribute :
			// definition.getIdentifiers()) {
			// LOGGER.debug("Adding {} as header (identifier).", new Object[] {
			// attribute.getName() });
			// qnames.add(attribute.getName());
			// }
			// for (ResourceObjectAttributeDefinition attribute :
			// definition.getSecondaryIdentifiers()) {
			// LOGGER.debug("Adding {} as header (secondary identifier).",
			// new Object[] { attribute.getName() });
			// qnames.add(attribute.getName());
			// }
//
//			if (definition.getDisplayNameAttribute() != null) {
//				qnames.add(definition.getDisplayNameAttribute().getName());
//			}
		}

		return qnames;
	}

	private boolean isPropertyAccetable(QName attributeName, ResourceType resource) {
		if (attributeName.equals(new QName(resource.getNamespace(), "userPassword"))) {
			return false;
		} else if (attributeName.equals(new QName(resource.getNamespace(), "description"))) {
			return false;
		} else if (attributeName.equals(new QName(resource.getNamespace(), "ou"))) {
			return false;
		} else {
			return true;
		}
	}

	private String getElementValue(List<Object> elements, QName qname) {
		List<Object> elementList = new ArrayList<Object>();
		for (Object element : elements) {
			if (qname.equals(JAXBUtil.getElementQName(element))) {
				elementList.add(element);
			}
		}

		StringBuilder builder = new StringBuilder();
		for (Object element : elementList) {
			builder.append(JAXBUtil.getTextContentDump(element));
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
