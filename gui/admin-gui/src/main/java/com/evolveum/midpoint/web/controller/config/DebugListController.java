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

import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.bean.ObjectBean;
import com.evolveum.midpoint.web.controller.TemplateController;
import com.evolveum.midpoint.web.controller.util.ListController;
import com.evolveum.midpoint.web.repo.RepositoryManager;
import com.evolveum.midpoint.web.util.FacesUtils;
import com.evolveum.midpoint.web.util.SelectItemComparator;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectType;

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
	private static final Trace LOGGER = TraceManager.getTrace(DebugListController.class);
	private static final List<SelectItem> objectTypes = new ArrayList<SelectItem>();
	static {
		for (ObjectTypes type : ObjectTypes.values()) {
			objectTypes.add(new SelectItem(type.getValue(),
					FacesUtils.translateKey(type.getLocalizationKey())));
		}

		Collections.sort(objectTypes, new SelectItemComparator());
	}
	@Autowired(required = true)
	private transient RepositoryManager repositoryManager;
	@Autowired(required = true)
	private transient TemplateController template;
	@Autowired(required = true)
	private transient DebugViewController debugView;
	private String objectType = ObjectTypes.USER.getValue();
	private boolean showPopup = false;
	private boolean showPopupForDeleteAllObjects = false;
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

	public boolean isShowPopup() {
		return showPopup;
	}
	
	public boolean isShowPopupForDeleteAllObjects() {
		return showPopupForDeleteAllObjects;
	}

	public String initController() {
		getObjects().clear();
		setOffset(0);
		setRowsCount(30);

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

		List<? extends ObjectType> list = null;
		try {
			list = repositoryManager.listObjects(ObjectTypes.getObjectTypeClass(objectType), getOffset(),
					getRowsCount());
		} catch (Exception ex) {
			LoggingUtils.logException(LOGGER, "Unknown error occured while listing objects of type {}", ex,
					objectType);
			FacesUtils.addErrorMessage("List object failed with exception " + ex.getMessage());
		}

		getObjects().clear();
		for (ObjectType object : list) {
			getObjects().add(new ObjectBean(object.getOid(), object.getName()));
		}

		if (getObjects().isEmpty()) {
			FacesUtils.addWarnMessage("Couldn't find any object.");
		}

		return null;
	}

	public void deleteObject() {
		showPopup = false;
		if (StringUtils.isEmpty(oidToDelete)) {
			FacesUtils.addErrorMessage("Object oid not defined.");
			return;
		}

		if (!repositoryManager.deleteObject(ObjectTypes.getObjectTypeClass(objectType), oidToDelete)) {
			FacesUtils.addErrorMessage("Delete object failed.");
		}

		oidToDelete = null;
		list();
	}
	
	public void deleteAllObjects(){
		showPopupForDeleteAllObjects = false;
		if(getObjects().isEmpty()){
			FacesUtils.addErrorMessage("The list is empty.");
			return;
		}
		
		for (int i = 0; i < getObjects().size(); i++) {
			if (!repositoryManager.deleteObject(ObjectTypes.getObjectTypeClass(objectType), getObjects().get(i).getOid())) {
				FacesUtils.addErrorMessage("Delete list failed.");
				return;
			}
		}
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
	
	public void showConfirmDeleteForAllObjects(){
		if (getObjects().isEmpty()) {
			FacesUtils.addErrorMessage("List is empty.");
			return;
		}
		showPopupForDeleteAllObjects = true;
	}
	
	public void hideConfirmDeleteForAllObjects() {
		showPopupForDeleteAllObjects = false;
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
