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
package com.evolveum.midpoint.web.bean;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.faces.model.SelectItem;
import javax.xml.ws.Holder;

import org.apache.commons.lang.StringUtils;

import com.evolveum.midpoint.api.logging.Trace;
import com.evolveum.midpoint.logging.TraceManager;
import com.evolveum.midpoint.schema.ObjectTypes;
import com.evolveum.midpoint.schema.PagingTypeFactory;
import com.evolveum.midpoint.web.controller.util.ControllerUtil;
import com.evolveum.midpoint.web.controller.util.ListController;
import com.evolveum.midpoint.web.util.FacesUtils;
import com.evolveum.midpoint.web.util.SelectItemComparator;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectListType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.OperationResultType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.OrderDirectionType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PagingType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.QueryType;
import com.evolveum.midpoint.xml.ns._public.model.model_1.FaultMessage;
import com.evolveum.midpoint.xml.ns._public.model.model_1.ModelPortType;

/**
 * 
 * @author lazyman
 * 
 */
public class BrowserBean extends ListController<BrowserItem> {

	private static final long serialVersionUID = 8414430107567167458L;
	private static final Trace LOGGER = TraceManager.getTrace(BrowserBean.class);
	private static final List<SelectItem> types = new ArrayList<SelectItem>();
	static {
		for (ObjectTypes type : ObjectTypes.values()) {
			types.add(new SelectItem(type.getValue(), FacesUtils.translateKey(type.getLocalizationKey())));
		}

		Collections.sort(types, new SelectItemComparator());
	}
	private transient ModelPortType model;
	private String type;
	private String name;
	private boolean listByName = true;

	public List<SelectItem> getTypes() {
		return types;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getType() {
		if (type == null && !getTypes().isEmpty()) {
			type = (String) getTypes().get(0).getValue();
		}
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public void setModel(ModelPortType model) {
		this.model = model;
	}

	public void searchByName() {
		if (StringUtils.isEmpty(name)) {
			FacesUtils.addErrorMessage("Name not defined.");
			return;
		}
		listByName = true;
		listFirst();
	}

	public void searchByType() {
		if (StringUtils.isEmpty(type)) {
			FacesUtils.addErrorMessage("Type not defined.");
			return;
		}
		listByName = false;
		listFirst();
	}

	@Override
	public void cleanup() {
		super.cleanup();
		type = null;
		name = null;
	}

	@Override
	protected String listObjects() {
		if (listByName) {
			return listByName();
		} else {
			return listByType();
		}
	}

	private String listByName() {
		QueryType query = new QueryType();
		query.setFilter(ControllerUtil.createQuery(name, null));
		ObjectListType list = null;
		try {
			list = model.searchObjects(query, new PagingType(), new Holder<OperationResultType>(
					new OperationResultType()));
		} catch (FaultMessage ex) {
			FacesUtils.addErrorMessage("Couldn't search for object '" + name + "'.", ex);
			LOGGER.debug("Couldn't search for object '" + name + "'.", ex);
		}

		updateObjectList(list);

		return null;
	}

	private String listByType() {
		ObjectListType result = null;
		try {
			PagingType paging = PagingTypeFactory.createPaging(getOffset(), getRowsCount(),
					OrderDirectionType.ASCENDING, "name");
			result = model.listObjects(ObjectTypes.getObjectTypeUri(type), paging, new Holder<OperationResultType>(
					new OperationResultType()));
		} catch (FaultMessage ex) {
			String message = (ex.getFaultInfo().getMessage() != null ? ex.getFaultInfo().getMessage() : ex
					.getMessage());
			FacesUtils.addErrorMessage("List object failed with exception " + message);
		} catch (Exception ex) {
			FacesUtils.addErrorMessage("List object failed with exception " + ex.getMessage());
			LOGGER.info("List object failed");
			LOGGER.error("Exception was {} ", ex);
		}

		updateObjectList(result);

		return null;
	}

	private void updateObjectList(ObjectListType result) {
		if (result == null) {
			FacesUtils.addWarnMessage("No objects found for type '" + type + "'.");
			return;
		}

		getObjects().clear();
		for (ObjectType object : result.getObject()) {
			// TODO: refactor - object type from class name??? wtf
			ObjectTypes objectType = ObjectTypes.getObjectType(object.getClass().getSimpleName());

			String localizationKey = objectType == null ? "Unknown" : objectType.getLocalizationKey();
			getObjects().add(
					new BrowserItem(object.getOid(), object.getName(), FacesUtils
							.translateKey(localizationKey)));
		}
	}
}
