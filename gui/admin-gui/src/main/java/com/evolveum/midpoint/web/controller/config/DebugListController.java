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

import java.io.Serializable;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.faces.model.SelectItem;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

import com.evolveum.midpoint.api.logging.Trace;
import com.evolveum.midpoint.common.Utils;
import com.evolveum.midpoint.logging.TraceManager;
import com.evolveum.midpoint.web.bean.DebugObject;
import com.evolveum.midpoint.web.util.FacesUtils;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectListType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.OrderDirectionType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PagingType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PropertyReferenceType;
import com.evolveum.midpoint.xml.ns._public.repository.repository_1.FaultMessage;
import com.evolveum.midpoint.xml.ns._public.repository.repository_1.RepositoryPortType;

/**
 * 
 * @author lazyman
 * 
 */
@Controller("debugList")
@Scope("session")
public class DebugListController implements Serializable {

	public static final String PAGE_NAVIGATION_LIST = "/config/debugList?faces-redirect=true";
	public static final String PAGE_NAVIGATION_VIEW = "/config/debugView?faces-redirect=true";
	public static final String PARAM_DELETE_OBJECT_OID = "deleteObjectOid";
	public static final String PARAM_VIEW_OBJECT_OID = "viewObjectOid";
	private static final long serialVersionUID = -6260309359121248205L;
	private static final Trace TRACE = TraceManager.getTrace(DebugListController.class);

	private static enum Type {

		ACCOUNT("controller.debugList.account", "AccountType"),

		GENERIC_OBJECT("controller.debugList.genericObject", "GenericObjectType"),

		RESOURCE("controller.debugList.resource", "ResourceType"),

		RESOURCE_STATE("controller.debugList.resourceState", "ResourceStateType"),

		USER("controller.debugList.user", "UserType"),

		USER_TEMPLATE("controller.debugList.userTemplate", "UserTemplateType");

		private String key;
		private String value;

		private Type(String key, String value) {
			this.key = key;
			this.value = value;
		}

		public String getKey() {
			return key;
		}

		public String getValue() {
			return value;
		}
	}

	@Autowired(required = true)
	private transient RepositoryPortType repositoryService;
	@Autowired(required = true)
	private transient TemplateController template;
	@Autowired(required = true)
	private transient DebugViewController debugView;
	private List<DebugObject> objects;
	private String objectType = "UserType";
	private int offset = 0;
	private int rowsCount = 30;
	private boolean showPopup = false;
	private String oidToDelete;

	public List<SelectItem> getObjectTypes() {
		List<SelectItem> objectTypes = new ArrayList<SelectItem>();
		for (Type type : Type.values()) {
			objectTypes.add(new SelectItem(type.getValue(), FacesUtils.translateKey(type.getKey())));
		}
		Collections.sort(objectTypes, new Comparator<SelectItem>() {
			@Override
			public int compare(SelectItem o1, SelectItem o2) {
				return String.CASE_INSENSITIVE_ORDER.compare(o1.getLabel(), o2.getLabel());
			}
		});
		return objectTypes;
	}

	public List<DebugObject> getObjects() {
		if (objects == null) {
			objects = new ArrayList<DebugObject>();
		}
		return objects;
	}

	public void setObjectType(String objectType) {
		this.objectType = objectType;
	}

	public String getObjectType() {
		return objectType;
	}

	public int getRowsCount() {
		return rowsCount;
	}

	public boolean isShowPopup() {
		return showPopup;
	}

	public String initController() {
		getObjects().clear();
		offset = 0;
		rowsCount = 30;

		return PAGE_NAVIGATION_LIST;
	}

	public String listLast() {
		offset = 0;
		return listObjects();
	}

	public String listNext() {
		offset += rowsCount;
		return listObjects();
	}

	public String listFirst() {
		offset = 0;
		return listObjects();
	}

	public String listPrevious() {
		if (offset < rowsCount) {
			return null;
		}
		offset -= rowsCount;
		return listObjects();
	}

	public boolean isShowTable() {
		if (objects != null && !objects.isEmpty()) {
			return true;
		}

		return false;
	}

	public String list() {
		initController();
		return listFirst();
	}

	private String listObjects() {
		if (StringUtils.isEmpty(objectType)) {
			FacesUtils.addErrorMessage("Object type not defined.");
			return null;
		}
		TRACE.info("listObjects start for object type {}", objectType);

		String xsdName = Utils.getObjectType(objectType);
		ObjectListType result = null;
		try {
			PagingType paging = new PagingType();

			PropertyReferenceType propertyReferenceType = Utils.fillPropertyReference("name");
			paging.setOrderBy(propertyReferenceType);
			paging.setOffset(BigInteger.valueOf(offset));
			paging.setMaxSize(BigInteger.valueOf(rowsCount));
			paging.setOrderDirection(OrderDirectionType.ASCENDING);
			result = repositoryService.listObjects(xsdName, paging);
		} catch (FaultMessage ex) {
			String message = (ex.getFaultInfo().getMessage() != null ? ex.getFaultInfo().getMessage() : ex
					.getMessage());
			FacesUtils.addErrorMessage("List object failed with exception " + message);
		} catch (Exception ex) {
			FacesUtils.addErrorMessage("List object failed with exception " + ex.getMessage());
			TRACE.info("List object failed");
			TRACE.error("Exception was {} ", ex);
		}

		if (result == null) {
			FacesUtils.addWarnMessage("No objects found for type '" + objectType + "'.");
			return null;
		}

		getObjects().clear();
		for (ObjectType object : result.getObject()) {
			objects.add(new DebugObject(object.getOid(), object.getName()));
		}

		return null;
	}

	public void deleteObject() {
		showPopup = false;
		if (StringUtils.isEmpty(oidToDelete)) {
			FacesUtils.addErrorMessage("Object oid not defined.");
			return;
		}

		try {
			repositoryService.deleteObject(oidToDelete);
		} catch (FaultMessage ex) {
			String message = (ex.getFaultInfo().getMessage() != null ? ex.getFaultInfo().getMessage() : ex
					.getMessage());
			FacesUtils.addErrorMessage("Delete object failed with exception " + message);
		} catch (Exception ex) {
			FacesUtils.addErrorMessage("Delete object failed with exception " + ex.getMessage());
			TRACE.info("Delete object failed");
			TRACE.error("Exception was {} ", ex);
		}
		oidToDelete = null;

		list();
	}

	public void showConfirmDelete() {
		oidToDelete = FacesUtils.getRequestParameter(PARAM_DELETE_OBJECT_OID);
		if (StringUtils.isEmpty(oidToDelete)) {
			FacesUtils.addErrorMessage("Object oid not defined.");
			return;
		}

		showPopup = true;
	}

	public void hideConfirmDelete() {
		showPopup = false;
	}

	public String viewOrEdit() {
		String objectOid = FacesUtils.getRequestParameter(PARAM_VIEW_OBJECT_OID);
		if (StringUtils.isEmpty(objectOid)) {
			FacesUtils.addErrorMessage("Object oid not defined.");
			return null;
		}

		DebugObject object = null;
		for (DebugObject dObject : objects) {
			if (objectOid.equals(dObject.getOid())) {
				object = dObject;
				break;
			}
		}
		if (object == null) {
			FacesUtils.addErrorMessage("Couldn't find object with oid '" + objectOid + "' in session.");
			return PAGE_NAVIGATION_LIST;
		}

		debugView.setObject(object);
		debugView.setEditOther(false);
		String returnPage = debugView.viewObject();
		if (PAGE_NAVIGATION_VIEW.equals(returnPage) && template != null) {
			template.setSelectedLeftId("leftViewEdit");
		}

		return returnPage;
	}
}
