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
import java.math.BigInteger;
import java.util.ArrayList;
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

	public static final String PAGE_NAVIGATION_DEBUG_LIST = "/config/debugList?faces-redirect=true";
	public static final String PAGE_NAVIGATION_VIEW = "/config/debugView?faces-redirect=true";
	public static final String PARAM_OBJECT_OID = "objectOid";
	private static final long serialVersionUID = -6260309359121248205L;
	private static final Trace TRACE = TraceManager.getTrace(DebugListController.class);
	private static final List<SelectItem> objectTypes = new ArrayList<SelectItem>();
	static {
		objectTypes.add(new SelectItem("UserType", "User"));
		objectTypes.add(new SelectItem("AccountType", "Account"));
		objectTypes.add(new SelectItem("ResourceStateType", "Resource State"));
		objectTypes.add(new SelectItem("ResourceType", "Resource"));
		objectTypes.add(new SelectItem("UserTemplateType", "User Template"));
		objectTypes.add(new SelectItem("GenericObjectType", "Generic Object"));
	}
	@Autowired(required = true)
	private transient RepositoryPortType repositoryService;
	private List<DebugObject> objects;
	private String objectType = "UserType";
	private int offset = 0;
	private int rowsCount = 30;
	private boolean showPopup = false;

	public List<SelectItem> getObjectTypes() {
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

	private void clearController() {
		getObjects().clear();
		offset = 0;
		rowsCount = 30;
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
		clearController();
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
		String objectOid = FacesUtils.getRequestParameter(PARAM_OBJECT_OID);
		if (StringUtils.isEmpty(objectOid)) {
			FacesUtils.addErrorMessage("Object oid not defined.");
			return;
		}

		try {
			repositoryService.deleteObject(objectOid);
		} catch (FaultMessage ex) {
			String message = (ex.getFaultInfo().getMessage() != null ? ex.getFaultInfo().getMessage() : ex
					.getMessage());
			FacesUtils.addErrorMessage("Delete object failed with exception " + message);
		} catch (Exception ex) {
			FacesUtils.addErrorMessage("Delete object failed with exception " + ex.getMessage());
			TRACE.info("Delete object failed");
			TRACE.error("Exception was {} ", ex);
		}
		list();
	}

	public void showConfirmDelete() {
		showPopup = true;
	}

	public void hideConfirmDelete() {
		showPopup = false;
	}

	public String viewOrEdit() {
		return PAGE_NAVIGATION_VIEW;
	}
}
