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
 * Portions Copyrighted 2010 Forgerock
 */

package com.evolveum.midpoint.web.model.impl;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import javax.xml.ws.Holder;

import org.apache.commons.lang.Validate;
import org.springframework.beans.factory.annotation.Autowired;

import com.evolveum.midpoint.api.logging.Trace;
import com.evolveum.midpoint.common.Utils;
import com.evolveum.midpoint.logging.TraceManager;
import com.evolveum.midpoint.schema.ObjectTypes;
import com.evolveum.midpoint.web.model.PagingDto;
import com.evolveum.midpoint.web.model.PropertyAvailableValues;
import com.evolveum.midpoint.web.model.PropertyChange;
import com.evolveum.midpoint.web.model.ResourceDto;
import com.evolveum.midpoint.web.model.ResourceManager;
import com.evolveum.midpoint.web.model.ResourceObjectShadowDto;
import com.evolveum.midpoint.web.model.UserDto;
import com.evolveum.midpoint.web.model.WebModelException;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectListType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.OperationResultType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PagingType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PropertyReferenceListType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceObjectShadowListType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceObjectShadowType;
import com.evolveum.midpoint.xml.ns._public.model.model_1.FaultMessage;
import com.evolveum.midpoint.xml.ns._public.model.model_1.ModelPortType;

/**
 * 
 * @author katuska
 */
public class ResourceTypeManager implements ResourceManager, Serializable {

	private static final long serialVersionUID = 8238616310118713517L;
	private static final Trace TRACE = TraceManager.getTrace(ResourceTypeManager.class);
	private Class<?> constructResourceType;

	@Autowired(required = true)
	private transient ModelPortType model;

	public ResourceTypeManager(Class<?> constructResourceType) {
		this.constructResourceType = constructResourceType;
	}

	@Override
	public Collection<ResourceDto> list() {
		try { // Call Web Service Operation
			String objectType = ObjectTypes.RESOURCE.getObjectTypeUri();
			// TODO: more reasonable handling of paging info
			PagingType paging = new PagingType();
			ObjectListType result = model.listObjects(objectType, paging, new Holder<OperationResultType>(
					new OperationResultType()));
			List<ObjectType> objects = result.getObject();
			Collection<ResourceDto> items = new ArrayList<ResourceDto>(objects.size());

			for (ObjectType o : objects) {
				ResourceDto resourceDto = (ResourceDto) constructResourceType.newInstance();
				resourceDto.setXmlObject(o);
				items.add(resourceDto);
			}

			return items;
		} catch (Exception ex) {
			TRACE.error("List resources failed");
			TRACE.error("Exception was: ", ex);
			return null;
		}
	}

	@Override
	public ResourceDto get(String oid, PropertyReferenceListType resolve) throws WebModelException {
		TRACE.info("oid = {}", new Object[] { oid });
		Validate.notNull(oid);
		try { // Call Web Service Operation
			ObjectType result = model.getObject(oid, resolve, new Holder<OperationResultType>(
					new OperationResultType()));

			ResourceDto resourceDto = (ResourceDto) constructResourceType.newInstance();
			resourceDto.setXmlObject(result);

			return resourceDto;
		} catch (FaultMessage ex) {
			throw new WebModelException(ex.getMessage(), "Failed to get resource with oid " + oid);
		} catch (InstantiationException ex) {
			TRACE.error("Instantiation failed: {}", ex);
			return null;
			// throw new WebModelException(ex.getMessage(),
			// "Instatiation failed.");
		} catch (IllegalAccessException ex) {
			TRACE.error("Class or its nullary constructor is not accessible: {}", ex);
			return null;
			// throw new WebModelException(ex.getMessage(),
			// "Class or its nullary constructor is not accessible.");
		}
	}

	@Override
	public ResourceDto create() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public String add(ResourceDto newObject) throws WebModelException {
		Validate.notNull(newObject);

		try { // Call Web Service Operation
			String result = model.addObject(newObject.getXmlObject(), new Holder<OperationResultType>(
					new OperationResultType()));
			return result;
		} catch (FaultMessage ex) {
			throw new WebModelException(ex.getMessage(), "[Web Service Error] Add resource failed");

		}

	}

	@Override
	public Set<PropertyChange> submit(ResourceDto changedObject) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public void delete(String oid) throws WebModelException {
		Validate.notNull(oid);
		try {
			model.deleteObject(oid, new Holder<OperationResultType>(new OperationResultType()));
		} catch (FaultMessage ex) {
			throw new WebModelException(ex.getMessage(),
					"[Web Service Error] Failed to delete resource with oid " + oid);
		}

	}

	@Override
	public List<PropertyAvailableValues> getPropertyAvailableValues(String oid, List<String> properties) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public List<ResourceObjectShadowDto> listObjectShadows(String oid, Class<?> resourceObjectShadowType) {
		Validate.notNull(oid);
		try {
			ResourceObjectShadowListType resourceObjectShadowListType = model.listResourceObjectShadows(oid,
					resourceObjectShadowType.getName(), new Holder<OperationResultType>(
							new OperationResultType()));
			List<ResourceObjectShadowDto> resourceObjectShadowDtoList = new ArrayList<ResourceObjectShadowDto>();
			for (ResourceObjectShadowType resourceObjectShadow : resourceObjectShadowListType.getObject()) {
				ResourceObjectShadowDto resourceObjectShadowDto = new ResourceObjectShadowDto(
						resourceObjectShadow);
				resourceObjectShadowDtoList.add(resourceObjectShadowDto);
			}
			return resourceObjectShadowDtoList;
		} catch (Exception ex) {
			TRACE.error("Delete user failed for oid = {}", oid);
			TRACE.error("Exception was: ", ex);
			return null;
		}

	}

	@Override
	public Collection<UserDto> list(PagingDto pagingDto) throws WebModelException {
		throw new UnsupportedOperationException("Not supported yet.");
	}
}
