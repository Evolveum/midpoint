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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.faces.model.SelectItem;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

import com.evolveum.midpoint.api.logging.Trace;
import com.evolveum.midpoint.common.Utils;
import com.evolveum.midpoint.logging.TraceManager;
import com.evolveum.midpoint.schema.ObjectTypes;
import com.evolveum.midpoint.schema.PagingTypeFactory;
import com.evolveum.midpoint.web.bean.ObjectBean;
import com.evolveum.midpoint.web.controller.TemplateController;
import com.evolveum.midpoint.web.controller.util.ListController;
import com.evolveum.midpoint.web.util.FacesUtils;
import com.evolveum.midpoint.web.util.SelectItemComparator;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectListType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.OrderDirectionType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PagingType;
import com.evolveum.midpoint.xml.ns._public.repository.repository_1.FaultMessage;
import com.evolveum.midpoint.xml.ns._public.repository.repository_1.RepositoryPortType;

/**
 * 
 * @author lazyman
 * 
 */
@Controller("debugList")
@Scope("session")
public class DebugListController extends ListController<ObjectBean> {
	
	public static final String PAGE_NAVIGATION = "/config/debugList?faces-redirect=true";
	public static final String PAGE_NAVIGATION_VIEW = "/config/debugView?faces-redirect=true";
	public static final String PARAM_DELETE_OBJECT_OID = "deleteObjectOid";
	public static final String PARAM_VIEW_OBJECT_OID = "viewObjectOid";
	private static final long serialVersionUID = -6260309359121248205L;
	private static final Trace TRACE = TraceManager.getTrace(DebugListController.class);
	private static final List<SelectItem> objectTypes = new ArrayList<SelectItem>();
	static {
		for (ObjectTypes type : ObjectTypes.values()) {
			objectTypes.add(new SelectItem(type.getValue(),
					FacesUtils.translateKey(type.getLocalizationKey())));
		}

		Collections.sort(objectTypes, new SelectItemComparator());
	}
	@Autowired(required = true)
	private transient RepositoryPortType repositoryService;
	@Autowired(required = true)
	private transient TemplateController template;
	@Autowired(required = true)
	private transient DebugViewController debugView;
	private String objectType = "UserType";
	private int offset = 0;
	private int rowsCount = 30;
	private boolean showPopup = false;
	private String oidToDelete;

	public List<SelectItem> getObjectTypes() {
		return objectTypes;
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

		return PAGE_NAVIGATION;
	}

	public boolean isShowTable() {
		if (!getObjects().isEmpty()) {
			return true;
		}

		return false;
	}

	public String list() {
		initController();
		return listFirst();
	}

	@Override
	protected String listObjects() {
		if (StringUtils.isEmpty(objectType)) {
			FacesUtils.addErrorMessage("Object type not defined.");
			return null;
		}
		TRACE.info("listObjects start for object type {}", objectType);

		String xsdName = Utils.getObjectType(objectType);
		ObjectListType result = null;
		try {
			PagingType paging = PagingTypeFactory.createPaging(offset, rowsCount,
					OrderDirectionType.ASCENDING, "name");
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
			getObjects().add(new ObjectBean(object.getOid(), object.getName()));
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

		ObjectBean object = null;
		for (ObjectBean dObject : getObjects()) {
			if (objectOid.equals(dObject.getOid())) {
				object = dObject;
				break;
			}
		}
		if (object == null) {
			FacesUtils.addErrorMessage("Couldn't find object with oid '" + objectOid + "' in session.");
			return PAGE_NAVIGATION;
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
