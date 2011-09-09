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

package com.evolveum.midpoint.web.test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.xml.namespace.QName;

import org.testng.annotations.Test;

import com.evolveum.midpoint.common.result.OperationResult;
import com.evolveum.midpoint.web.model.ResourceManager;
import com.evolveum.midpoint.web.model.dto.ConnectorDto;
import com.evolveum.midpoint.web.model.dto.ConnectorHostDto;
import com.evolveum.midpoint.web.model.dto.GuiResourceDto;
import com.evolveum.midpoint.web.model.dto.PropertyAvailableValues;
import com.evolveum.midpoint.web.model.dto.PropertyChange;
import com.evolveum.midpoint.web.model.dto.ResourceObjectShadowDto;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PagingType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.PropertyReferenceListType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceObjectShadowType;

/**
 * 
 * @author katuska
 */
public class ResourceManagerImplMock implements ResourceManager {

	private static final long serialVersionUID = -2673752961587849731L;

	Map<String, GuiResourceDto> resourceTypeList = new HashMap<String, GuiResourceDto>();

	@Override
	public Collection<GuiResourceDto> list() {
		return resourceTypeList.values();
	}

	@Override
	public GuiResourceDto get(String oid, PropertyReferenceListType resolve) {
		for (GuiResourceDto resource : resourceTypeList.values()) {
			if (resource.getOid().equals(oid)) {
				return resource;
			}
		}
		return null;
	}

	@Override
	public GuiResourceDto create() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public String add(GuiResourceDto newObject) {
		resourceTypeList.clear();
		if (newObject.getOid() == null) {
			newObject.setOid(UUID.randomUUID().toString());
		}
		resourceTypeList.put(newObject.getOid(), newObject);
		return newObject.getOid();
	}

	@Override
	public Set<PropertyChange> submit(GuiResourceDto changedObject) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public void delete(String oid) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public List<PropertyAvailableValues> getPropertyAvailableValues(String oid, List<String> properties) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public Collection<GuiResourceDto> list(PagingType paging) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public <T extends ResourceObjectShadowType> List<ResourceObjectShadowDto<T>> listObjectShadows(
			String oid, Class<T> resourceObjectShadowType) {
		return new ArrayList<ResourceObjectShadowDto<T>>();
	}

	@Test
	@Override
	public OperationResult testConnection(String resourceOid) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public void importFromResource(String resourceOid, QName objectClass) {
		throw new UnsupportedOperationException("Not supported yet.");
	}


	@Override
	public Collection<ResourceObjectShadowDto<ResourceObjectShadowType>> listResourceObjects(
			String resourceOid, QName objectClass, PagingType paging) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public Collection<ConnectorDto> listConnectors() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public ConnectorDto getConnector(String oid) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public Collection<ConnectorHostDto> listConnectorHosts() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public void discoverConnectorsOnHost(ConnectorHostDto connectorHost) {
		throw new UnsupportedOperationException("Not supported yet.");
	}
}
