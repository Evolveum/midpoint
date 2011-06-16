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
import com.evolveum.midpoint.logging.TraceManager;
import com.evolveum.midpoint.schema.PagingTypeFactory;
import com.evolveum.midpoint.web.bean.ResourceListItem;
import com.evolveum.midpoint.web.bean.ResourceObjectBean;
import com.evolveum.midpoint.web.controller.util.ListController;
import com.evolveum.midpoint.web.util.FacesUtils;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectListType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.OperationResultType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.OrderDirectionType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceObjectShadowType;
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

		if (list == null || list.getObject().isEmpty()) {
			// TODO: show & log error
			FacesUtils.addWarnMessage("No object found for objet class '" + objectClass + "'.");
			return null;
		}

		List<ObjectType> objects = list.getObject();
		for (ObjectType objectType : objects) {
			String oid = "Unknown";
			if (StringUtils.isNotEmpty(objectType.getOid())) {
				oid = objectType.getOid();
			}

			ResourceObjectShadowType account = (ResourceObjectShadowType) objectType;
			List<Element> elements = account.getAttributes().getAny();

			Map<String, String> attributes = null;
			if ("Account".equals(objectClass)) {
				attributes = new HashMap<String, String>();
				attributes
						.put("__NAME__",
								getElementValue(
										elements,
										new QName(
												"http://midpoint.evolveum.com/xml/ns/public/resource/idconnector/resource-schema-1.xsd",
												"__NAME__")));
				attributes
						.put("cn",
								getElementValue(
										elements,
										new QName(
												"http://midpoint.evolveum.com/xml/ns/public/resource/instances/ef2bc95b-76e0-48e2-86d6-3d4f02d3e1a2",
												"cn")));
			}

			getObjects().add(new ResourceObjectBean(oid, objectType.getName(), attributes));
		}

		List<String> header = new ArrayList<String>();
		// TODO: improve hardcoded values
		if ("Account".equals(getObjectClass())) {
			header.add("__NAME__");
			header.add("cn");
		}
		columnModel = new ArrayDataModel<String>(header.toArray(new String[header.size()]));
		rowModel = new ArrayDataModel<ResourceObjectBean>(getObjects().toArray(
				new ResourceObjectBean[getObjects().size()]));

		return PAGE_NAVIGATION;
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

	public DataModel<ResourceObjectBean> getRowModel() {
		return rowModel;
	}

	public Object getAttributeValue() {
		if (getRowModel().isRowAvailable()) {
			ResourceObjectBean row = getRowModel().getRowData();
			return row.getAttributes().get(getColumnModel().getRowData());
		}

		return null;
	}

	public DataModel<String> getColumnModel() {
		return columnModel;
	}
}
