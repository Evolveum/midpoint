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
import java.util.List;

import javax.xml.ws.Holder;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

import com.evolveum.midpoint.api.logging.LoggingUtils;
import com.evolveum.midpoint.api.logging.Trace;
import com.evolveum.midpoint.logging.TraceManager;
import com.evolveum.midpoint.schema.PagingTypeFactory;
import com.evolveum.midpoint.web.bean.ObjectBean;
import com.evolveum.midpoint.web.bean.ResourceListItem;
import com.evolveum.midpoint.web.controller.util.ListController;
import com.evolveum.midpoint.web.util.FacesUtils;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectListType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.OperationResultType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.OrderDirectionType;
import com.evolveum.midpoint.xml.ns._public.model.model_1.FaultMessage;
import com.evolveum.midpoint.xml.ns._public.model.model_1.ModelPortType;

/**
 * 
 * @author lazyman
 * 
 */
@Controller("listObjects")
@Scope("session")
public class ListObjectsController extends ListController<ObjectBean> implements Serializable {

	public static final String PAGE_NAVIGATION = "/resource/listObjects?faces-redirect=true";
	private static final long serialVersionUID = -3538520581983462635L;
	private static final Trace LOGGER = TraceManager.getTrace(ListObjectsController.class);
	@Autowired(required = true)
	private transient ModelPortType model;
	private ResourceListItem resource;
	private String objectClass;

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
		if (resource == null || objectClass == null) {
			// TODO: show & log error
			return null;
		}

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
			// TODO: better type handling
			String oid = "Unknown";
			if (StringUtils.isNotEmpty(objectType.getOid())) {
				oid = objectType.getOid();
			}
			getObjects().add(new ObjectBean(oid, objectType.getName()));
		}

		return PAGE_NAVIGATION;
	}
}
